package com.jetbrains.micropython.devices

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.micropython.run.MicroPythonRunConfiguration
import com.jetbrains.micropython.run.getMicroUploadCommand
import com.jetbrains.micropython.settings.MicroPythonTypeHints
import com.jetbrains.micropython.settings.MicroPythonUsbId
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.packaging.PyRequirement

/**
 * @author Gor-Ren
 */
class Esp32DeviceProvider : MicroPythonDeviceProvider {
  override val persistentName: String
    get() = "ESP32"

  override val documentationURL: String
    get() = "https://github.com/vlasovskikh/intellij-micropython/wiki/ESP32"  // FIXME: add wiki page

  override val usbIds: List<MicroPythonUsbId>
    get() = listOf(MicroPythonUsbId(0x1A86, 0x7523),
                   MicroPythonUsbId(0x10C4, 0xEA60),
                   MicroPythonUsbId(0x0403, 0x6001))

  override val typeHints: MicroPythonTypeHints by lazy {
    MicroPythonTypeHints(listOf("stdlib", "micropython", "esp32"))
  }

  override fun getPackageRequirements(sdk: Sdk): List<PyRequirement> {
    val manager = PyPackageManager.getInstance(sdk)
    return manager.parseRequirements("""|pyserial>=3.3,<4.0
                                        |docopt>=0.6.2,<0.7
                                        |adafruit-ampy>=1.0.5,<1.1""".trimMargin())
  }

  override fun getRunCommandLineState(configuration: MicroPythonRunConfiguration,
                                      environment: ExecutionEnvironment): CommandLineState? {
    val module = configuration.module ?: return null
    val command = getMicroUploadCommand(configuration.path, module) ?: return null

    return object : CommandLineState(environment) {
      override fun startProcess() =
          OSProcessHandler(GeneralCommandLine(command))
    }
  }
}
