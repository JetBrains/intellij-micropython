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
 * @author timsavage
 */
class RPiPicoDeviceProvider : MicroPythonDeviceProvider {
  override val persistentName: String
    get() = "Raspberry Pi Pico"

  override val documentationURL: String
    get() = "https://www.raspberrypi.org/documentation/pico/getting-started/"

  override val usbIds: List<MicroPythonUsbId>
    get() = listOf(MicroPythonUsbId(0x2E8A, 0x05))

  override val typeHints: MicroPythonTypeHints by lazy {
    MicroPythonTypeHints(listOf("stdlib", "micropython", "rpi_pico"))
  }

  override fun getPackageRequirements(sdk: Sdk): List<PyRequirement> {
    val manager = PyPackageManager.getInstance(sdk)
    return manager.parseRequirements("""|pyserial>=3.5,<4.0
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
