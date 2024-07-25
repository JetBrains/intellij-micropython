package com.jetbrains.micropython.nova

import com.intellij.execution.ExecutionException
import com.intellij.openapi.Disposable
import com.jediterm.core.util.TermSize
import com.jediterm.terminal.TtyConnector
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.jetbrains.annotations.NonNls
import java.io.IOException
import java.io.PipedReader
import java.io.PipedWriter
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

private const val BOUNDARY = "*********FSOP************"
private const val START = "START"
private const val END = "END"

class WebSocketTtyConnector(
  private val url: String,
  private val password: String?,
  private val errorLogger: (Throwable) -> Any,
) : TtyConnector, Disposable {

  enum class Status {
    NOT_OPEN, TERMINAL_CONNECTED, TERMINAL_DISCONNECTED, CLOSED, FAILED
  }

  @Volatile
  private var status = Status.NOT_OPEN

  private val PASSWORD_PROMPT = "Password: "
  private val outPipe = PipedWriter()
  private val inPipe = PipedReader(outPipe, 1000)

  private val client = object : WebSocketClient(URI.create(url)) {
      init {
        isTcpNoDelay = true
        connectionLostTimeout = 0
      }

    override fun onOpen(handshakedata: ServerHandshake?) {
      status = Status.TERMINAL_DISCONNECTED
    }

    override fun onMessage(message: String) {
      if (status == Status.TERMINAL_DISCONNECTED) {
        disconnectedBuffer.append(message)
      }
      else {
      synchronized(inPipe) {
        outPipe.write(message)
        outPipe.flush()
      }
      }
    }

    override fun onMessage(bytes: ByteBuffer) = onMessage(String(bytes.array(), StandardCharsets.UTF_8))

    override fun onClose(code: Int, reason: String, remote: Boolean) {
      status = Status.CLOSED
    }

    override fun onError(ex: Exception) {
      errorLogger(ex)
    }
  }

  @Synchronized
  fun connect() {
    if (status == Status.NOT_OPEN) {
      try {
        client.connectBlocking()
        if (password != null) { //todo check if logged in
          val started = System.currentTimeMillis()
          while (disconnectedBuffer.contains(PASSWORD_PROMPT).not()) {
            Thread.sleep(10)
            if (System.currentTimeMillis() - started > 5000) {
              throw ExecutionException("Timeout while waiting for password prompt")
            }
          }
          write("$password\r\n")
        }
        status = Status.TERMINAL_CONNECTED
      }
      catch (e: IOException) {
        status = Status.FAILED
        errorLogger(e)
      }
    }
  }

  private val disconnectedBuffer = StringBuffer()

  private fun terminalDisconnect(banner: Boolean = true) {
    if (status == Status.TERMINAL_CONNECTED) {
      status = Status.TERMINAL_DISCONNECTED
      if(banner)outPipe.write("\r\nOperation in progress...")
    }
  }

  private fun terminalConnect(banner: Boolean = true) {
    if (status == Status.TERMINAL_DISCONNECTED) {
      status = Status.TERMINAL_CONNECTED
      disconnectedBuffer.setLength(0)
      if(banner)outPipe.write("\r\nOperation completed\r\n")
    }
  }

  override fun read(buf: CharArray, offset: Int, length: Int): Int {
    try {
        when (status) {
          Status.TERMINAL_CONNECTED, Status.TERMINAL_DISCONNECTED -> synchronized(inPipe) {
            return inPipe.read(buf, offset, length)
          }
          Status.NOT_OPEN -> return 0
          Status.CLOSED, Status.FAILED -> return -1
        }
    }
    catch (e: IOException) {
      status = Status.FAILED
      errorLogger(e)
    }
    return -1
  }

  override fun write(bytes: ByteArray) {
    try {
      client.send(bytes.toString(StandardCharsets.US_ASCII))
    }
    catch (e: IOException) {
      status = Status.FAILED
      errorLogger(e)
    }
  }

  override fun write(string: String) {
    try {
      client.send(string)
    }
    catch (e: IOException) {
      status = Status.FAILED
      errorLogger(e)
    }
  }

  override fun isConnected(): Boolean = client.isOpen

  override fun waitFor(): Int = 0

  override fun ready(): Boolean = client.hasBufferedData()

  override fun getName(): String = url

  override fun close() {
    status = Status.CLOSED
    client.close()
  }

  override fun dispose() {
    try {
      close()
    }
    catch (_: IOException) {
    }
  }

  override fun resize(termSize: TermSize) {}

  fun blindExecute(command: String): String {
    terminalDisconnect()
    try {
      write("\u0003")
      write("\u0005")
      write("print('$BOUNDARY' + '$START')\r\n")
      command.lines().forEach {
        write("$it\r\n")
        Thread.sleep(10)
        disconnectedBuffer.setLength(0)
      }
      write("print('$BOUNDARY' + '$END')\r\n")
      Thread.sleep(10)
      disconnectedBuffer.setLength(0)
      write("\u0004")
      while (disconnectedBuffer.lastIndexOf(BOUNDARY + END) <= 0) {
        Thread.sleep(10)
      }
      return disconnectedBuffer
        .toString().substringAfter(BOUNDARY + START)
        .substringBefore(BOUNDARY + END)
    }
    finally {
      write("\u0002")
      terminalConnect()
    }
  }

  fun instantRun(command: @NonNls String) {
    terminalDisconnect(false)
    try {
      write("\u0003")
      write("\u0005")
      command.lines().forEach {
        write("$it\r\n")
        Thread.sleep(10)
        disconnectedBuffer.setLength(0)
      }
    }
    finally {
      terminalConnect()
      write("\u0004")
      write("\u0002")
    }
  }

  fun upload(fullName: @NonNls String, code: @NonNls String) {
    val escaped = code.map {
      when (it) {
        '\n' -> "\\n"
        '\r' -> "\\r"
        in 0.toChar()..31.toChar(), in 127.toChar()..255.toChar() -> "\\x%02x".format(it)
        else -> it
      }
    }.joinToString("")
    blindExecute("""
with open('$fullName', 'wb') as f:
    f.write(b'$escaped')
    f.close()
""")
  }
}