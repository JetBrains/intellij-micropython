package com.jetbrains.micropython.nova

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
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

private const val PASSWORD_PROMPT = "Password:"
private const val LOGIN_SUCCESS = "WebREPL connected"
private const val LOGIN_FAIL = "Access denied"

private const val BOUNDARY = "*********FSOP************"
private const val START = "START"
private const val END = "END"


private const val TIMEOUT = 2000
private const val LONG_TIMEOUT = 20000
private const val SHORT_DELAY = 20L

class WebSocketComm(private val errorLogger: (Throwable) -> Any = {}) : Disposable, Closeable {

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

    private var url: String = "---"

    val ttyConnector: TtyConnector = WebSocketTtyConnector()

    @Throws(IOException::class)
    suspend fun connect(uri: URI, password: String) {
        url = uri.toString()
        webSocketMutex.withLock {
            connected = false
            ttySuspended = true
            client?.close()
            client = MpyWebSocketClient(uri).apply {
                connectBlocking()
                try {
                    withTimeout(TIMEOUT.toLong()) {
                        while (true) {
                            when {
                                offTtyBuffer.length < PASSWORD_PROMPT.length -> delay(SHORT_DELAY)
                                offTtyBuffer.length > PASSWORD_PROMPT.length * 2 -> {
                                    offTtyBuffer.setLength(PASSWORD_PROMPT.length * 2)
                                    throw ConnectException("Password exchange error. Received prompt: $offTtyBuffer")
                                }

                                else -> {
                                    if (offTtyBuffer.toString().contains(PASSWORD_PROMPT)) {
                                        offTtyBuffer.clear()
                                        send("$password\n")
                                        while (!connected) {
                                            when {
                                                offTtyBuffer.contains(LOGIN_SUCCESS) -> connected = true
                                                offTtyBuffer.contains(LOGIN_FAIL) -> throw ConnectException("Access denied")
                                                else -> delay(SHORT_DELAY)
                                            }
                                        }
                                    } else {
                                        throw ConnectException("Password exchange error. Received prompt: $offTtyBuffer")
                                    }
                                    return@withTimeout
                                }
                            }
                        }
                    }
                    ttySuspended = false
                } catch (e: TimeoutCancellationException) {
                    throw ConnectException("Password exchange timeout. Received prompt: $offTtyBuffer")
                } finally {
                    offTtyBuffer.clear()
                }
            }

        }
    }

    @Throws(IOException::class)
    suspend fun blindExecute(command: String): String {
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
                client?.send("\u0001")
                client?.send("print('$BOUNDARY' + '$START')\n")
                command.lines().forEach {
                    client?.send("$it\n")
                    delay(SHORT_DELAY)
                    offTtyBuffer.setLength(0)
                }
                client?.send("print('$BOUNDARY' + '$END')\n")
                delay(SHORT_DELAY)
                offTtyBuffer.setLength(0)
                client?.send("\u0004")
                withTimeout(LONG_TIMEOUT.toLong()) {
                    while (offTtyBuffer.indexOf(BOUNDARY + END) <= 0) {
                        delay(SHORT_DELAY)
                    }
                }
                return offTtyBuffer
                    .toString().substringAfter(BOUNDARY + START)
                    .substringBefore(BOUNDARY + END)
                    .trim()
            } finally {
                client?.send("\u0002")
                offTtyBuffer.clear()
                ttySuspended = false
            }
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
                ttySuspended = false
            } finally {
                client?.send("\u0004")
            }
        }
    }

    inner class MpyWebSocketClient(uri: URI) : WebSocketClient(uri) {
        init {
            isTcpNoDelay = true
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
        override fun getName(): String = url
        override fun close() = Disposer.dispose(this@WebSocketComm)//todo Am I right?
        override fun isConnected(): Boolean = true
        override fun ready(): Boolean {
            return runBlocking {
                    inPipe.ready() || client?.hasBufferedData() ?: false
            }
        }

        override fun waitFor(): Int = 0

        override fun write(bytes: ByteArray) = write(bytes.toString(StandardCharsets.UTF_8))

        override fun write(text: String) {
            if (connected && !ttySuspended) {
                client?.send(text)
            }
        }

        override fun read(text: CharArray, offset: Int, length: Int): Int {
            return runBlocking {
                    inPipe.read(text, offset, length)
            }
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
}