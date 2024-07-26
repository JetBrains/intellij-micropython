package com.jetbrains.micropython.nova

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assertions.fail
import java.io.IOException
import java.net.ConnectException
import java.net.URI
import java.util.concurrent.TimeUnit.MILLISECONDS

class Connect {

    private var tcpPort = 58756

    @BeforeEach
    fun init() {
        tcpPort = (2000..60000).random()
    }


    @Test
    @Timeout(5000, unit = MILLISECONDS)
    fun successfulConnect() {
        WebSocketTestServer(
            tcpPort,
            "Password: " to "pa55wd\n",
            "WebREPL connected" to null,
        ).test {
            it.connect(URI.create("ws://localhost:$tcpPort"), "pa55wd")
            assertTrue(it.isConnected())
            assertFalse(it.isTtySuspended())
        }
    }

    @Test
    @Timeout(5000, unit = MILLISECONDS)
    fun wrongPassword() {
        assertThrows<IOException> {
            WebSocketTestServer(
                tcpPort,
                "Password: " to "blahblah\n",
                "Access denied" to null,
            ).test {
                it.connect(URI.create("ws://localhost:$tcpPort"), "pa55wd")
                assertTrue(it.isConnected())
                assertFalse(it.isTtySuspended())
            }
        }
    }

    @Test
    @Timeout(5000, unit = MILLISECONDS)
    fun accessDenied() {
        val ex = assertThrows<ConnectException> {
            WebSocketTestServer(
                tcpPort,
                "Password: " to "pa55wd\n",
                "Access denied" to null,
            ).test {
                it.connect(URI.create("ws://localhost:$tcpPort"), "pa55wd")
                assertTrue(it.isConnected())
                assertFalse(it.isTtySuspended())
            }
        }
        assertEquals("Access denied", ex.message)
    }

    @Test
    @Disabled
    @Timeout(5000, unit = MILLISECONDS)
    fun realConnect() {
        WebSocketComm { fail(it) }
            .use {
                runBlocking {
                    it.connect(URI.create("ws://192.168.50.68:8266"), "passwd")
                    assertTrue(it.isConnected())
                    assertFalse(it.isTtySuspended())
                }
            }
    }

}