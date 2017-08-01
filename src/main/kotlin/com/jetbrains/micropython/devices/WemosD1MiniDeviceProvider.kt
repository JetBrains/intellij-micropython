package com.jetbrains.micropython.devices

import com.jetbrains.micropython.settings.MicroPythonTypeHints
import com.jetbrains.micropython.settings.MicroPythonUsbId
import com.jetbrains.python.packaging.PyRequirement

/**
 * @author vlan
 */
class WemosD1MiniDeviceProvider : MicroPythonDeviceProvider {
  override val persistentName: String
    get() = "WEMOS D1 mini"

  override val documentationURL: String
    get() = "https://github.com/vlasovskikh/intellij-micropython/wiki/WEMOS-D1-mini"

  override val usbId: MicroPythonUsbId?
    get() = MicroPythonUsbId(0x1A86, 0x7523)

  override val typeHints: MicroPythonTypeHints by lazy {
    MicroPythonTypeHints("esp8266/latest")
  }

  override val packageRequirements: List<PyRequirement> by lazy {
    PyRequirement.fromText("""pyserial>=3.3,<3.4""")
  }
}