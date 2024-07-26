package com.jetbrains.micropython.nova

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.Closeable
import java.net.InetSocketAddress

class WebSocketTestServer(tcpPort: Int, private vararg val sentences: Pair<String, String?>) :
    WebSocketServer(InetSocketAddress("localhost", tcpPort)), Closeable {

    private var idx = 0
    private val exceptions = mutableListOf<Throwable>()


    override fun onError(conn: WebSocket?, ex: Exception) {
        exceptions.add(ex)
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        conn.send(sentences[idx].first)
    }

    override fun onMessage(conn: WebSocket, message: String) {
        try {
            assertEquals(sentences[idx].second, message)
            idx++
            conn.send(sentences[idx].first)
        } catch (e: Throwable) {
            exceptions.add(e)
        }
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        try {
            if (sentences.last().second == null) {
                assertEquals(sentences.size - 1, idx, "Sentenced processed")
            } else {
                assertEquals(sentences.size, idx, "Sentenced processed")
            }
        } catch (e: Throwable) {
            exceptions.add(e)
        }
    }

    @Volatile
    var started = false

    override fun onStart(){
        started = true
    }
    override fun close() {
        stop()
        exceptions.forEach { it.printStackTrace() }
        if (exceptions.isNotEmpty()) {
            throw exceptions.first()
        }
    }

    fun test(block: suspend CoroutineScope.(WebSocketComm) -> Unit) {
        start()
        while (!started) {
            Thread.sleep(100)
        }
        use {
            WebSocketComm { exceptions.add(it) }
                .use {
                    runBlocking { block(it) }
                }
        }
    }
}