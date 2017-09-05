package com.jetbrains.micropython.devices

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.micropython.run.MicroPythonRunConfiguration
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.MicroPythonTypeHints
import com.jetbrains.micropython.settings.MicroPythonUsbId
import com.jetbrains.micropython.settings.microPythonFacet
import com.jetbrains.python.packaging.PyRequirement

/**
 * @author vlan
 */
class Esp8266DeviceProvider : MicroPythonDeviceProvider {
  override val persistentName: String
    get() = "ESP 8266"

  override val documentationURL: String
    get() = "https://github.com/vlasovskikh/intellij-micropython/wiki/ESP-8266"

  override val usbIds: List<MicroPythonUsbId>
    get() = listOf(MicroPythonUsbId(0x1A86, 0x7523),
                   MicroPythonUsbId(0x10C4, 0xEA60))

  override val typeHints: MicroPythonTypeHints by lazy {
    MicroPythonTypeHints(listOf("stdlib", "micropython", "esp8266"))
  }

  override val packageRequirements: List<PyRequirement> by lazy {
    PyRequirement.fromText("""|pyserial>=3.3,<3.4
                              |docopt>=0.6.2,<0.7
                              |adafruit-ampy>=1.0.1,<1.1""".trimMargin())
  }

  override fun getRunCommandLineState(configuration: MicroPythonRunConfiguration,
                                      environment: ExecutionEnvironment): CommandLineState? {
    val facet = configuration.module?.microPythonFacet ?: return null
    val pythonPath = facet.pythonPath ?: return null
    val devicePath = facet.devicePath ?: return null
    val rootPath = configuration.project.basePath ?: return null
    return object : CommandLineState(environment) {
      override fun startProcess() =
          OSProcessHandler(GeneralCommandLine(pythonPath,
                                              "${MicroPythonFacet.scriptsPath}/microupload.py",
                                              "-C",
                                              rootPath,
                                              "-v",
                                              devicePath,
                                              configuration.path))
    }
  }
}