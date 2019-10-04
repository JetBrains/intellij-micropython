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
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.util.PathUtil
import com.jetbrains.micropython.settings.microPythonFacet
import org.jdom.Element

/**
 * @author Mikhail Golubev
 */
class MicroPythonRunConfiguration(project: Project, factory: ConfigurationFactory)
  : AbstractRunConfiguration(project, factory), RunConfigurationWithSuppressedDefaultDebugAction {

  var targetPath: String = ""
  var contentRootPath: String = ""
  
  override fun getValidModules() =
      allModules.filter { it.microPythonFacet != null }.toMutableList()

  override fun getConfigurationEditor() = MicroPythonRunConfigurationEditor(this)

  override fun getState(executor: Executor, environment: ExecutionEnvironment) =
      module?.microPythonFacet?.configuration?.deviceProvider?.getRunCommandLineState(this, environment)

  override fun checkConfiguration() {
    super.checkConfiguration()
    if (StringUtil.isEmpty(targetPath)) {
      throw RuntimeConfigurationError("Path is not specified")
    }
    val m = module ?: throw RuntimeConfigurationError("Module for path is not found")
    val facet = m.microPythonFacet ?:
        throw RuntimeConfigurationError("MicroPython support is not enabled for selected module")
    val validationResult = facet.checkValid()
    if (validationResult != ValidationResult.OK) {
      throw RuntimeConfigurationError(validationResult.errorMessage)
    }
    facet.pythonPath ?: throw RuntimeConfigurationError("Python interpreter is not found")
  }

  override fun suggestedName() = "Flash ${PathUtil.getFileName(targetPath)}"

  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    element.setAttribute("path", targetPath)
  }

  override fun readExternal(element: Element) {
    super.readExternal(element)
    configurationModule.readExternal(element)
    element.getAttributeValue("path")?.let {
      targetPath = it
    }
  }

  val module: Module?
    get() {
      val file = StandardFileSystems.local().findFileByPath(targetPath) ?: return null
      return ModuleUtil.findModuleForFile(file, project)
    }
}

