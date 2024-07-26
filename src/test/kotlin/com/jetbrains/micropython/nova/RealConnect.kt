package com.jetbrains.micropython.nova

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.net.URI
import java.util.concurrent.TimeUnit.MILLISECONDS

private const val URL = "ws://192.168.50.68:8266"

private const val PASSWORD = "passwd"

@Disabled("Works only id a real board is at address $URL having password $PASSWORD")
class RealConnect {

    lateinit var comm: WebSocketComm

    @BeforeEach
    fun init() {
        comm = WebSocketComm { fail(it) }
    }

    @AfterEach
    fun teardown() {
        comm.close()
    }

    @Test
    @Timeout(5000, unit = MILLISECONDS)
    fun realBlindExecute() {
        runBlocking {
            comm.connect(URI.create(URL), PASSWORD)
            assertTrue(comm.isConnected())
            assertFalse(comm.isTtySuspended())
            val responseA = comm.blindExecute("print('Test me')")
            assertEquals("Test me", responseA)
            val responseB = comm.blindExecute("print('Test me 2')")
            assertEquals("Test me 2", responseB)
            assertTrue(comm.isConnected())
            assertFalse(comm.isTtySuspended())
        }
    }

    @Test
    @Timeout(5000, unit = MILLISECONDS)
    fun realBlindExecuteLong() {
        runBlocking {
            comm.connect(URI.create(URL), PASSWORD)
            assertTrue(comm.isConnected())
            assertFalse(comm.isTtySuspended())
            val responseA = comm.blindExecute(
                "print('Test me ', end='')\n".repeat(60)
            )
            assertEquals("Test me ".repeat(60).trim(), responseA)
            assertTrue(comm.isConnected())
            assertFalse(comm.isTtySuspended())
        }
    }

    @Test
    @Timeout(5000, unit = MILLISECONDS)
    fun realInstantRun() {
        runBlocking {
            comm.connect(URI.create(URL), PASSWORD)
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