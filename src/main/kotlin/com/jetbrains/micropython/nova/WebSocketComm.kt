package com.jetbrains.micropython.nova

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.Strings
import com.jediterm.terminal.TtyConnector
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.jetbrains.annotations.NonNls
import java.io.Closeable
import java.io.IOException
import java.io.PipedReader
import java.io.PipedWriter
import java.net.ConnectException
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.coroutines.cancellation.CancellationException

private const val PASSWORD_PROMPT = "Password:"
private const val LOGIN_SUCCESS = "WebREPL connected"
private const val LOGIN_FAIL = "Access denied"

private const val BOUNDARY = "*********FSOP************"


private const val TIMEOUT = 2000
private const val LONG_TIMEOUT = 20000L
private const val SHORT_DELAY = 20L

typealias ExecResponse = List<WebSocketComm.SingleExecResponse>

fun ExecResponse.extractSingleResponse(): String {
    if (this.size != 1 || this[0].stderr.isNotEmpty()) {
        val message = this.joinToString("\n") { it.stderr }
        throw IOException(message)
    } else {
        return this[0].stdout
    }
}

class WebSocketComm(private val errorLogger: (Throwable) -> Any = {}) : Disposable, Closeable {

    data class SingleExecResponse(
        val stdout: String,
        val stderr: String
    )

    @Volatile
    private var client: WebSocketClient? = null

    @Volatile
    private var connected: Boolean = false

    fun isConnected(): Boolean = connected

    @Volatile
    private var ttySuspended: Boolean = true

    fun isTtySuspended(): Boolean = ttySuspended

    private var offTtyBuffer = StringBuilder()

    private val webSocketMutex = Mutex()

    private val outPipe = PipedWriter()

    private val inPipe = PipedReader(outPipe, 1000)

    private var uri: URI? = null
    private var password: String? = null

    val ttyConnector: TtyConnector = WebSocketTtyConnector()

    @Throws(IOException::class)
    suspend fun connect(uri: URI, password: String) {
        this.uri = uri
        this.password = password
        reconnect()
    }


    @Throws(IOException::class, CancellationException::class, TimeoutCancellationException::class)
    suspend fun upload(fullName: @NonNls String, content: ByteArray) {
        val commands = mutableListOf<String>()
        var slashIdx = 0
        while (slashIdx >= 0) {
            slashIdx = fullName.indexOf('/', slashIdx + 1)
            if (slashIdx > 0) {
                val folderName = fullName.substring(0, slashIdx)
                commands.add("import errno\n" +
                        "try: os.mkdir('$folderName'); \n" +
                        "except OSError as e:\n" +
                        "\tif e.errno != errno.EEXIST: raise ")
            }
        }
        commands.add("___f=open('$fullName','wb')")
        val chunk = StringBuilder()
        val maxDataChunk = 220
        var contentIdx = 0
        while (contentIdx < content.size) {
            chunk.setLength(0)
            while (chunk.length < maxDataChunk && contentIdx < content.size) {
                val b = content[contentIdx++]
                chunk.append(
                    when (b) {
                        0x0D.toByte() -> "\\n"
                        0xA.toByte() -> "\\r"
                        '\''.code.toByte() -> "\\'"
                        '\\'.code.toByte() -> "\\\\"
                        in 0..31, in 127..255 -> "\\x%02x".format(b)
                        else -> b.toInt().toChar()
                    }
                )
            }
            commands.add("___f.write(b'$chunk')")
        }
        commands.add("___f.close()")
        commands.add("del(___f)")
        commands.add("print(os.stat('$fullName'))")


        val result = webSocketMutex.withLock {
            doBlindExecute(*commands.toTypedArray())
        }
        val error = result.mapNotNull { Strings.nullize(it.stderr) }.joinToString(separator = "\n", limit = 1000)
        if (error.isNotEmpty()) {
            throw IOException(error)
        }
        val filedata = result.last().stdout.split('(', ')', ',').map { it.trim().toIntOrNull() }
        if (filedata.getOrNull(7) != content.size) {
            throw IOException("Expected size is ${content.size}, uploaded ${filedata[5]}")
        } else if (filedata.getOrNull(1) != 32768) {
            throw IOException("Expected type is 32768, uploaded ${filedata[1]}")
        }
    }

    @Throws(IOException::class)
    private suspend fun doBlindExecute(vararg commands: String): ExecResponse {
        ttySuspended = true
        val result = mutableListOf<SingleExecResponse>()
        try {

            client?.send("\u0003")
            client?.send("\u0003")
            client?.send("\u0003")
            client?.send("\u0001")
            while (!offTtyBuffer.endsWith("\n>")) {
                delay(SHORT_DELAY)
            }
            offTtyBuffer.clear()
            for (command in commands) {
                try {
                    withTimeout(LONG_TIMEOUT) {
                        command.lines().forEachIndexed { index, s ->
                            if (index > 0) {
                                delay(SHORT_DELAY)
                            }
                            client?.send("$s\n")
                        }
                        client?.send("\u0004")
                        while (!(offTtyBuffer.startsWith("OK") && offTtyBuffer.endsWith("\u0004>") && offTtyBuffer.count { it == '\u0004' } == 2)) {
                            delay(SHORT_DELAY)
                        }
                        val eotPos = offTtyBuffer.indexOf('\u0004')
                        val stdout = offTtyBuffer.substring(2, eotPos).trim()
                        val stderr = offTtyBuffer.substring(eotPos + 1, offTtyBuffer.length - 2).trim()
                        result.add(SingleExecResponse(stdout, stderr))
                        offTtyBuffer.clear()
                    }
                } catch (e: TimeoutCancellationException) {
                    throw IOException("Timeout during command execution:$command", e)
                }
            }
            return result
        } finally {
            client?.send("\u0002")
            offTtyBuffer.clear()
            ttySuspended = false
        }
    }

    @Throws(IOException::class)
    suspend fun blindExecute(vararg commands: String): ExecResponse {
        if (!connected) {
            throw IOException("Not connected")
        }
        if (ttySuspended) {
            throw IOException("Not ready")
        }
        webSocketMutex.withLock {
            return doBlindExecute(*commands)
        }
    }

    @Throws(IOException::class)
    suspend fun instantRun(command: @NonNls String) {
        if (!connected) {
            throw IOException("Not connected")
        }
        if (ttySuspended) {
            throw IOException("Not ready")
        }
        webSocketMutex.withLock {
            ttySuspended = true
            try {
                client?.send("\u0003")
                client?.send("\u0003")
                client?.send("\u0003")
                client?.send("\u0005")
                while (!offTtyBuffer.contains("===")) {
                    delay(SHORT_DELAY)
                }
                command.lines().forEach {
                    offTtyBuffer.clear()
                    client?.send("$it\n")
                    offTtyBuffer.clear()
                }
                client?.send("#$BOUNDARY\n")
                while (!offTtyBuffer.contains(BOUNDARY)) {
                    delay(SHORT_DELAY)
                }
                offTtyBuffer.clear()
            } finally {
                ttySuspended = false
                client?.send("\u0004")
            }
        }
    }

    inner class MpyWebSocketClient(uri: URI) : WebSocketClient(uri) {
        init {
            isTcpNoDelay = true
            connectionLostTimeout = 0
        }

        override fun onOpen(handshakedata: ServerHandshake) = Unit //Nothing to do

        override fun onMessage(message: String) {
            if (ttySuspended) {
                offTtyBuffer.append(message)
            } else {
                runBlocking {
                    outPipe.write(message)
                    outPipe.flush()
                }
            }
        }

        override fun onMessage(bytes: ByteBuffer) = onMessage(String(bytes.array(), StandardCharsets.UTF_8))

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            try {
                if (remote && connected) {
                    //CounterParty closed the connection
                    throw IOException("Connection closed. Code:$code ($reason)")
                }
            } finally {
                connected = false
            }
        }

        override fun onError(ex: Exception) {
            errorLogger(ex)
        }
    }

    override fun dispose() {
        close()
    }

    inner class WebSocketTtyConnector : TtyConnector {
        override fun getName(): String = (uri ?: "---").toString()
        override fun close() = Disposer.dispose(this@WebSocketComm)//todo Am I right?
        override fun isConnected(): Boolean = true
        override fun ready(): Boolean {
            return inPipe.ready() || client?.hasBufferedData() ?: false
        }

        override fun waitFor(): Int = 0

        override fun write(bytes: ByteArray) = write(bytes.toString(StandardCharsets.UTF_8))

        override fun write(text: String) {
            if (connected && !ttySuspended) {
                client?.send(text)
            }
        }

        override fun read(text: CharArray, offset: Int, length: Int): Int {
            while (isConnected) {
                try {
                    return inPipe.read(text, offset, length)
                } catch (ex: IOException) {
                    Thread.sleep(SHORT_DELAY)
                }
            }
            return -1
        }
    }

    override fun close() {
        connected = false
        try {
            inPipe.close()
        } catch (_: IOException) {
        }
        try {
            outPipe.close()
        } catch (_: IOException) {
        }
        try {
            client?.close()
        } catch (_: IOException) {
        }
    }

    @Throws(IOException::class)
    suspend fun reconnect() {
        val uri = this.uri ?: throw IOException("Not connected")
        webSocketMutex.withLock {
            connected = false
            ttySuspended = true
            client?.close()
            client = MpyWebSocketClient(uri).also { newClient ->
                newClient.connectBlocking()
                try {
                    withTimeout(TIMEOUT.toLong()) {
                        while (newClient.isOpen) {
                            when {
                                offTtyBuffer.length < PASSWORD_PROMPT.length -> delay(SHORT_DELAY)
                                offTtyBuffer.length > PASSWORD_PROMPT.length * 2 -> {
                                    offTtyBuffer.setLength(PASSWORD_PROMPT.length * 2)
                                    throw ConnectException("Password exchange error. Received prompt: $offTtyBuffer")
                                }

                                offTtyBuffer.toString().contains(PASSWORD_PROMPT) -> break
                                else -> throw ConnectException("Password exchange error. Received prompt: $offTtyBuffer")
                            }
                        }
                        offTtyBuffer.clear()
                        newClient.send("$password\n")
                        while (!connected && newClient.isOpen) {
                            when {
                                offTtyBuffer.contains(LOGIN_SUCCESS) -> connected = true
                                offTtyBuffer.contains(LOGIN_FAIL) -> throw ConnectException("Access denied")
                                else -> delay(SHORT_DELAY)
                            }
                        }
                        ttySuspended = false
                    }
                } catch (e: TimeoutCancellationException) {
                    try {
                        newClient.close()
                    } catch (_: IOException) {
                    }
                    throw ConnectException("Password exchange timeout. Received prompt: $offTtyBuffer")
                } finally {
                    offTtyBuffer.clear()
                }
            }
        }
    }

    fun ping() {
        if (isConnected()) {
            client?.sendPing()
        }
    }
}