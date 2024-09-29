package com.jetbrains.micropython.nova

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

open class MpyWebSocketClient(uri: URI, private val comm: MpyComm) : Client<MpyWebSocketClient> {

    protected open fun open() = Unit
    protected open fun close(code: Int, reason: String, remote: Boolean) = Unit
    protected open fun error(ex: Exception) = Unit

    protected open fun message(message: String) = Unit


    val webSocketClient = object : WebSocketClient(uri) {
        override fun onOpen(handshakedata: ServerHandshake) = open() //Nothing to do

        override fun onMessage(message: String) {
            this@MpyWebSocketClient.message(message)
            comm.dataReceived(message)
        }

        override fun onMessage(bytes: ByteBuffer) = onMessage(String(bytes.array(), StandardCharsets.UTF_8))

        override fun onError(ex: Exception) {
            error(ex)
            comm.errorLogger(ex)
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            close(code, reason, remote)
            try {
                if (remote && comm.state == State.CONNECTED) {
                    //Counterparty closed the connection
                    throw IOException("Connection closed. Code:$code ($reason)")
                }
            } finally {
                comm.state = State.DISCONNECTED
            }
        }

    }
    init {
        webSocketClient.isTcpNoDelay = true
        webSocketClient.connectionLostTimeout = 0
    }


    override fun connect(): MpyWebSocketClient {
        webSocketClient.connect()
        return this
    }

    override fun close() = webSocketClient.close()

    override fun closeBlocking() = webSocketClient.closeBlocking()

    override fun send(string: String) = webSocketClient.send(string)

    override fun sendPing() = webSocketClient.sendPing()

    override fun hasPendingData(): Boolean = webSocketClient.hasBufferedData()

    override val isConnected: Boolean
        get() = webSocketClient.isOpen
}