package com.jetbrains.micropython.devices

import com.jetbrains.micropython.settings.MicroPythonUsbId
import com.jetbrains.python.packaging.PyRequirement

/**
 * @author vlan
 */
class WemosD1MiniProDeviceProvider : MicroPythonDeviceProvider {
  override val persistentName: String
    get() = "WEMOS D1 mini Pro"

  override val usbId: MicroPythonUsbId?
    get() = MicroPythonUsbId(0x10C4, 0xEA60)

  override val documentationURL: String
    get() = "https://github.com/vlasovskikh/intellij-micropython/wiki/WEMOS-D1-mini"

  override val packageRequirements: List<PyRequirement> by lazy {
    PyRequirement.fromText("""pyserial>=3.3,<3.4""")
  }
}