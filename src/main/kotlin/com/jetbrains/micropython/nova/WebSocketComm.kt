package com.jetbrains.micropython.nova

import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.Strings
import com.intellij.platform.util.progress.withProgressText
import com.intellij.util.text.nullize
import com.jediterm.core.util.TermSize
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
import kotlin.properties.Delegates

private const val PASSWORD_PROMPT = "Password:"
private const val LOGIN_SUCCESS = "WebREPL connected"
private const val LOGIN_FAIL = "Access denied"

private const val BOUNDARY = "*********FSOP************"


private const val TIMEOUT = 2000
private const val LONG_TIMEOUT = 20000L
private const val SHORT_DELAY = 20L

data class SingleExecResponse(
    val stdout: String,
    val stderr: String
)

typealias ExecResponse = List<SingleExecResponse>

enum class State {
    DISCONNECTING, DISCONNECTED, CONNECTING, CONNECTED, TTY_DETACHED
}

typealias StateListener = (State) -> Unit

fun ExecResponse.extractSingleResponse(): String {
    if (this.size != 1 || this[0].stderr.isNotEmpty()) {
        val message = this.joinToString("\n") { it.stderr }
        throw IOException(message)
    } else {
        return this[0].stdout
    }
}

fun ExecResponse.extractResponse(): String {
    val stderr = this.mapNotNull { it.stderr.nullize(true) }.joinToString("\n")
    if (stderr.isNotEmpty()) {
        throw IOException(stderr)
    }
    return this.mapNotNull { it.stdout.nullize(true) }.joinToString("\n")

}

open class WebSocketComm(private val errorLogger: (Throwable) -> Any = {}) : Disposable, Closeable {

    val stateListeners = mutableListOf<StateListener>()

    @Volatile
    private var client: MpyWebSocketClient? = null

    fun isTtySuspended(): Boolean = state == State.TTY_DETACHED

    private var offTtyBuffer = StringBuilder()

    private val webSocketMutex = Mutex()

    private val outPipe = PipedWriter()

    private val inPipe = PipedReader(outPipe, 1000)

    private var uri: URI? = null
    private var password: String? = null

    val ttyConnector: TtyConnector = WebSocketTtyConnector()

    fun setConnectionParams(uri: URI, password: String) {
        this.uri = uri
        this.password = password
    }


    var state: State by Delegates.observable(State.DISCONNECTED){ _, _, newValue ->
        stateListeners.forEach { it(newValue) }
    }

    @Throws(IOException::class, CancellationException::class, TimeoutCancellationException::class)
    suspend fun upload(fullName: @NonNls String, content: ByteArray) {
        checkConnected()
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
        state = State.TTY_DETACHED
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
            if (state == State.TTY_DETACHED) {
                state = State.CONNECTED
            }
        }
    }

    fun checkConnected() {
        when (state) {
            State.CONNECTED -> {}
            State.DISCONNECTING, State.DISCONNECTED, State.CONNECTING -> throw IOException("Not connected")
            State.TTY_DETACHED -> throw IOException("Websocket is busy")
        }
    }

    @Throws(IOException::class)
    suspend fun blindExecute(vararg commands: String): ExecResponse {
        checkConnected()
        webSocketMutex.withLock {
            return doBlindExecute(*commands)
        }
    }

    @Throws(IOException::class)
    suspend fun instantRun(command: @NonNls String) {
        checkConnected()
        webSocketMutex.withLock {
            state = State.TTY_DETACHED
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
                    delay(SHORT_DELAY)
                }
                client?.send("#$BOUNDARY\n")
                while (!offTtyBuffer.contains(BOUNDARY)) {
                    delay(SHORT_DELAY)
                }
                offTtyBuffer.clear()
            } finally {
                if (state == State.TTY_DETACHED) {
                    state = State.CONNECTED
                }
                client?.send("\u0004")
            }
        }
    }

    open inner class MpyWebSocketClient(uri: URI) : WebSocketClient(uri) {
        init {
            isTcpNoDelay = true
            connectionLostTimeout = 0
        }

        override fun onOpen(handshakedata: ServerHandshake) = Unit //Nothing to do

        override fun onMessage(message: String) {
            if (state == State.TTY_DETACHED || state == State.CONNECTING) {
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
                if (remote && state == State.CONNECTED) {
                    //CounterParty closed the connection
                    throw IOException("Connection closed. Code:$code ($reason)")
                }
            } finally {
                state = State.DISCONNECTED
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
        override fun close() = Disposer.dispose(this@WebSocketComm)
        override fun isConnected(): Boolean = true
        override fun ready(): Boolean {
            return inPipe.ready() || client?.hasBufferedData() == true
        }

        override fun resize(termSize: TermSize) = Unit

        override fun waitFor(): Int = 0

        override fun write(bytes: ByteArray) = write(bytes.toString(StandardCharsets.UTF_8))

        override fun write(text: String) {
            if (state == State.CONNECTED) {
                client?.send(text)
            }
        }

        override fun read(text: CharArray, offset: Int, length: Int): Int {
            while (isConnected) {
                try {
                    return inPipe.read(text, offset, length)
                } catch (_: IOException) {
                    try {
                        Thread.sleep(SHORT_DELAY)
                    } catch (_: InterruptedException) {
                    }
                }
            }
            return -1
        }
    }

    override fun close() {
        try {
            client?.close()
            client = null
        } catch (_: IOException) {
        }
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
        state = State.DISCONNECTED
    }

    protected open fun createClient(uri: URI): MpyWebSocketClient = MpyWebSocketClient(uri)

    @Throws(IOException::class)
    suspend fun connect() {
        val uri = this.uri ?: throw IOException("Not connected")
        webSocketMutex.withLock {
            client = createClient(uri).also { newClient ->
                state = State.CONNECTING
                offTtyBuffer.clear()
                newClient.connect()
                try {
                    var time = LONG_TIMEOUT
                    withProgressText("Connecting to $uri") {
                        while (!newClient.isOpen && time > 0) {
                            checkCanceled()
                            delay(SHORT_DELAY)
                            time -= SHORT_DELAY.toInt()
                        }
                        if (!newClient.isOpen) {
                            throw ConnectException("Webrepl connection failed")
                        }
                    }

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
                        while (state == State.CONNECTING && newClient.isOpen) {
                            when {
                                offTtyBuffer.contains(LOGIN_SUCCESS) -> break
                                offTtyBuffer.contains(LOGIN_FAIL) -> throw ConnectException("Access denied")
                                else -> delay(SHORT_DELAY)
                            }
                        }
                        state = State.CONNECTED
                    }
                } catch (e: Exception) {
                    try {
                        newClient.close()
                    } catch (_: IOException) {
                    }
                    state = State.DISCONNECTED
                    when (e) {
                        is TimeoutCancellationException -> throw ConnectException("Password exchange timeout. Received prompt: $offTtyBuffer")
                        is InterruptedException -> throw ConnectException("Connection interrupted")
                        else -> throw e
                    }

                } finally {
                    offTtyBuffer.clear()
                }
            }
        }
    }

    suspend fun disconnect() {
        webSocketMutex.withLock {
            state = State.DISCONNECTING
            client?.closeBlocking()
            client = null
        }
    }

    fun ping() {
        if (state == State.CONNECTED) {
            client?.sendPing()
        }
    }

}