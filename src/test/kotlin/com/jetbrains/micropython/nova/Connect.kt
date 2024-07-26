package com.jetbrains.micropython.nova

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.net.ConnectException
import java.net.URI
import java.util.concurrent.TimeUnit.MILLISECONDS

class Connect {

    private var tcpPort = 58756

    private lateinit var server: WebSocketTestServer

    @BeforeEach
    fun init() {
        tcpPort = (2000..60000).random()
        server = WebSocketTestServer(tcpPort)
    }

    @AfterEach
    fun teardown() {
        server.close()
    }

    private fun expect(vararg sentences: Pair<String, String?>) = server.expect(*sentences)
    private fun test(block: suspend CoroutineScope.(WebSocketComm) -> Unit) = server.test(block)

    @Test
    @Timeout(5000, unit = MILLISECONDS)
    fun successfulConnect() {
        expect(
            "Password: " to "pa55wd\n",
            "WebREPL connected" to null,
        )
        test { comm ->
            comm.connect(URI.create("ws://localhost:$tcpPort"), "pa55wd")
            assertTrue(comm.isConnected())
            assertFalse(comm.isTtySuspended())
        }
    }

    @Test
    @Disabled("Not sure if this test is useful")
    @Timeout(5000, unit = MILLISECONDS)
    fun wrongPassword() {
        expect(
            "Password: " to "blahblah\n",
            "Access denied" to null,
        )
        assertThrows<ConnectException> {
            test { comm ->
                comm.connect(URI.create("ws://localhost:$tcpPort"), "pa55wd")
                assertTrue(comm.isConnected())
                assertFalse(comm.isTtySuspended())
                server.ignoreExceptions()
            }
        }
    }

    @Test
    @Timeout(5000, unit = MILLISECONDS)
    fun accessDenied() {
        expect(
            "Password: " to "pa55wd\n",
            "Access denied" to null,
        )
        val ex = assertThrows<ConnectException> {
            test { comm ->
                comm.connect(URI.create("ws://localhost:$tcpPort"), "pa55wd")
            }
        }
        assertEquals("Access denied", ex.message)
    }

    @Test
    @Timeout(5000, unit = MILLISECONDS)
    fun blindExecute() {
        expect(
            "Password: " to "passwd\n",
            "WebREPL connected" to "\u0003",
            ">>>" to "\u0003",
            ">>>" to "\u0003",
            ">>>" to "\u0001",
            "raw REPL; CTRL-B to exit\n>" to "print('*********FSOP************' + 'START')\n",
            "" to "print('Test me')\n",
            "" to "print('*********FSOP************' + 'END')\n",
            "" to "\u0004",
            ">OK*********FSOP************START\nTest me\n*********FSOP************END" to "\u0002",
            ">>>" to "\u0003",
            ">>>" to "\u0003",
            ">>>" to "\u0003",
            ">>>" to "\u0001",
            "raw REPL; CTRL-B to exit\n>" to "print('*********FSOP************' + 'START')\n",
            "" to "print('Test me 2')\n",
            "" to "print('*********FSOP************' + 'END')\n",
            "" to "\u0004",
            ">OK*********FSOP************START\nTest me 2\n*********FSOP************END" to "\u0002",
            ">>>" to null,
        )
        test { comm ->
            comm.connect(URI.create("ws://localhost:$tcpPort"), "passwd")
            assertTrue(comm.isConnected())
            assertFalse(comm.isTtySuspended())
            val responseA = comm.blindExecute("print('Test me')")
            assertEquals("Test me", responseA)
            val responseB = comm.blindExecute("print('Test me 2')")
            assertEquals("Test me 2", responseB)
            assertTrue(comm.isConnected())
            assertFalse(comm.isTtySuspended())
            delay(300)
        }
    }


    @Test
    @Timeout(5000, unit = MILLISECONDS)
    fun realInstantRun() {
        expect(
            "Password: " to "passwd\n",
            "WebREPL connected" to "\u0003",
            ">>>" to "\u0003",
            ">>>" to "\u0003",
            ">>>" to "\u0005",
            "paste mode; Ctrl-C to cancel, Ctrl-D to finish\n===" to "print('Test me')\n",
            "" to "print('Test me 2')\n",
            "" to "#*********FSOP************\n",
            "#*********FSOP************\n" to "\u0004",
            "Test me\nTest me 2\n>>>" to null
        )
        test { comm ->
            comm.connect(URI.create("ws://localhost:$tcpPort"), "passwd")
            assertTrue(comm.isConnected())
            assertFalse(comm.isTtySuspended())
            comm.instantRun("print('Test me')\nprint('Test me 2')")
            assertTrue(comm.isConnected())
            assertFalse(comm.isTtySuspended())
            assertTrue(comm.ttyConnector.isConnected)
            val buf = CharArray(100)
            delay(500)
            val len = comm.ttyConnector.read(buf, 0, buf.size)
            val linesReceived = String(buf, 0, len).trim().lines()
            assertIterableEquals(
                listOf("Test me", "Test me 2", ">>>"),
                linesReceived
            )
        }
    }

}