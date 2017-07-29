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

package com.jetbrains.micropython.run

import com.intellij.execution.Executor
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.facet.FacetManager
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PathUtil
import com.intellij.util.attribute
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.MicroPythonFacetType
import com.jetbrains.python.sdk.PythonSdkType
import org.jdom.Element

/**
 * @author Mikhail Golubev
 */
class MicroPythonRunConfiguration(project: Project?, factory: ConfigurationFactory?) : AbstractRunConfiguration(project, factory),
                                                                                       RunConfigurationWithSuppressedDefaultDebugAction {
  var scriptPath: String = ""
  
  // Module is either set up in run configuration provider or the first suitable is used instead 
  private val module: Module?
    get() = modules.getOrElse(0, {validModules.firstOrNull()})

  // Find selected SDK properly
  val selectedSdkPath: String? get() {
    // TODO: find the actual module of a script 
    val pySdk = PythonSdkType.findPythonSdk(module)
    return pySdk?.homePath
  }

  override fun getValidModules() =
      allModules
          .filter { FacetManager.getInstance(it)?.getFacetByType(MicroPythonFacetType.ID) != null }
          .toMutableList()

  override fun getConfigurationEditor() = MicroPythonRunConfigurationEditor(this)

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    val m = module ?: return null
    val deviceProvider = MicroPythonFacet.getInstance(m)?.configuration?.deviceProvider ?: return null
    return deviceProvider.getRunCommandLineState(this, environment)
  }

  override fun checkConfiguration() {
    super.checkConfiguration()
    if (StringUtil.isEmpty(scriptPath)) {
      throw RuntimeConfigurationError("Script is not specified")
    }
    if (selectedSdkPath == null) {
      throw RuntimeConfigurationError("SDK is not selected")
    }
    val activeModule = module
    if (activeModule != null) {
      val facet = FacetManager.getInstance(activeModule)?.getFacetByType(MicroPythonFacetType.ID)
      val validationResult = facet?.checkValid()
      if (validationResult != null && validationResult != ValidationResult.OK) {
        throw RuntimeConfigurationError(validationResult.errorMessage)
      }
    }
  }

  override fun suggestedName() = "Flash ${PathUtil.getFileName(scriptPath)} to device"

  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    element.attribute("scriptPath", scriptPath)
  }

  override fun readExternal(element: Element) {
    super.readExternal(element)
    configurationModule.readExternal(element)
    element.getAttributeValue("scriptPath")?.let {
      scriptPath = it
    }
  }
}

