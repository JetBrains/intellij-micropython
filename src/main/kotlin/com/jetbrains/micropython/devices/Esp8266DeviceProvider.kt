package com.jetbrains.micropython.devices

import com.jetbrains.micropython.settings.MicroPythonTypeHints
import com.jetbrains.micropython.settings.MicroPythonUsbId

/**
 * @author vlan
 */
class Esp8266DeviceProvider : MicrouploadDeviceProvider() {
  override val persistentName: String
    get() = "ESP8266"

  override val documentationURL: String
    get() = "https://github.com/vlasovskikh/intellij-micropython/wiki/ESP8266"

  override val usbIds: List<MicroPythonUsbId>
    get() = listOf(MicroPythonUsbId(0x1A86, 0x7523),
                   MicroPythonUsbId(0x10C4, 0xEA60))

  override val typeHints: MicroPythonTypeHints by lazy {
    MicroPythonTypeHints(listOf("stdlib", "micropython", "esp8266"))
  }
}
