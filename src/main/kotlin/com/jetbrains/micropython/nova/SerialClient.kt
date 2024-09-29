package com.jetbrains.micropython.nova

import jssc.SerialPort
import jssc.SerialPort.FLOWCONTROL_RTSCTS_IN
import jssc.SerialPort.FLOWCONTROL_RTSCTS_OUT
import jssc.SerialPortEvent
import jssc.SerialPortEventListener
import java.nio.charset.StandardCharsets

class SerialClient(serialPortName: String, private val comm: MpyComm) : Client<SerialClient> {

    private val port = SerialPort(serialPortName)
    override fun send(string: String) {
        port.writeString(string)
    }

    override fun hasPendingData(): Boolean = port.inputBufferBytesCount > 0

    override fun close() = closeBlocking()

    private val listener = object : SerialPortEventListener {
        override fun serialEvent(event: SerialPortEvent) {
            if (event.eventType and SerialPort.MASK_RXCHAR != 0) {
                val count = event.eventValue
                comm.dataReceived(port.readBytes(count).toString(StandardCharsets.UTF_8))
            }
        }
    }

    override fun connect():SerialClient {
        port.flowControlMode = FLOWCONTROL_RTSCTS_IN or FLOWCONTROL_RTSCTS_OUT
        port.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE)
        port.openPort()
        port.addEventListener(listener)
        return this
    }

    override fun closeBlocking() {
        port.closePort()
    }

    override fun sendPing() = Unit

    override val isConnected: Boolean
        get() = port.isOpened
}