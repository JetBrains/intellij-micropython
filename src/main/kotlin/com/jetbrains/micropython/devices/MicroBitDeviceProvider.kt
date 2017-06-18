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
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.micropython.run.MicroPythonRunConfiguration
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.MicroPythonTypeHints
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.sdk.PythonSdkType

/**
 * @author vlan
 */
class MicroBitDeviceProvider : MicroPythonDeviceProvider {
  override val presentableName: String
    get() = "Micro:bit"

  override val packageRequirements: List<PyRequirement> by lazy {
    val requirements = listOf(
        "uflash",
        "pyserial")
    requirements.map { PyRequirement(it) }
  }

  override val typeHints: MicroPythonTypeHints by lazy {
    MicroPythonTypeHints("Micro:bit", "microbit/latest")
  }

  override val detectedModuleNames: Set<String>
    get() = linkedSetOf("microbit")

  override fun getRunCommandLineState(configuration: MicroPythonRunConfiguration,
                                      environment: ExecutionEnvironment): CommandLineState {
    return object : CommandLineState(environment) {
      override fun startProcess(): ProcessHandler {
        return OSProcessHandler(GeneralCommandLine(configuration.selectedSdkPath,
                                                   "-m",
                                                   "uflash",
                                                   configuration.scriptPath))
      }
    }
  }

  override fun getReplTerminalCommand(facet: MicroPythonFacet): List<String> {
    val pythonPath = PythonSdkType.findPythonSdk(facet.module)?.homePath ?: return emptyList()
    val pluginPath = MicroPythonFacet.getPluginDescriptor().path
    return listOf(pythonPath, "$pluginPath/scripts/microbit/microrepl.py")
  }

  override val isDefault: Boolean
    get() = true
}