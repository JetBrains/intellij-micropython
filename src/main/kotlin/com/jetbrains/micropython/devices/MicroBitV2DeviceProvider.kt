package com.jetbrains.micropython.devices

import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.packaging.PyRequirement

class MicroBitV2DeviceProvider : MicroBitDeviceProvider() {
    override val persistentName: String
        get() = "Micro:bit V2"

    override fun getPackageRequirements(sdk: Sdk): List<PyRequirement> {
        val manager = PyPackageManager.getInstance(sdk)
        return manager.parseRequirements("""|uflash>=2.0
                                            |docopt>=0.6.2,<0.7
                                            |pyserial>=3.5,<4.0""".trimMargin())
    }

    override val isDefault: Boolean
        get() = false
}