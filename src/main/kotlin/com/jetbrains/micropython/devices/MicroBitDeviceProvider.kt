/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.micropython.devices

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.micropython.run.MicroPythonRunConfiguration
import com.jetbrains.micropython.settings.MicroPythonTypeHints
import com.jetbrains.micropython.settings.MicroPythonUsbId
import com.jetbrains.micropython.settings.microPythonFacet
import com.jetbrains.python.packaging.PyRequirement

/**
 * @author vlan
 */
class MicroBitDeviceProvider : MicroPythonDeviceProvider {
  override val persistentName: String
    get() = "Micro:bit"

  override val documentationURL: String
    get() = "https://github.com/vlasovskikh/intellij-micropython/wiki/BBC-Micro:bit"

  override val usbId: MicroPythonUsbId
    get() = MicroPythonUsbId(0x0D28, 0x0204)

  override val packageRequirements: List<PyRequirement> by lazy {
    PyRequirement.fromText("""|uflash>=1.0.8,<1.1
                              |pyserial>=3.3,<3.4""".trimMargin())
  }

  override val typeHints: MicroPythonTypeHints by lazy {
    MicroPythonTypeHints("microbit")
  }

  override val detectedModuleNames: Set<String>
    get() = linkedSetOf("microbit")

  override fun getRunCommandLineState(configuration: MicroPythonRunConfiguration,
                                      environment: ExecutionEnvironment): CommandLineState? {
    val pythonPath = configuration.module?.microPythonFacet?.pythonPath ?: return null
    return object : CommandLineState(environment) {
      override fun startProcess() =
          OSProcessHandler(GeneralCommandLine(pythonPath, "-m", "uflash", configuration.path))
    }
  }

  override val isDefault: Boolean
    get() = true
}