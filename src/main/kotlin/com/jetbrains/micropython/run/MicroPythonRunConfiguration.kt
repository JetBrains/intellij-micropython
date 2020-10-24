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
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.util.PathUtil
import com.intellij.util.PlatformUtils
import com.jetbrains.micropython.settings.MicroPythonProjectConfigurable
import com.jetbrains.micropython.settings.microPythonFacet
import org.jdom.Element

/**
 * @author Mikhail Golubev
 */
class MicroPythonRunConfiguration(project: Project, factory: ConfigurationFactory)
  : AbstractRunConfiguration(project, factory), RunConfigurationWithSuppressedDefaultDebugAction {

  var path: String = ""

  override fun getValidModules() =
      allModules.filter { it.microPythonFacet != null }.toMutableList()

  override fun getConfigurationEditor() = MicroPythonRunConfigurationEditor(this)

  override fun getState(executor: Executor, environment: ExecutionEnvironment) =
      module?.microPythonFacet?.configuration?.deviceProvider?.getRunCommandLineState(this, environment)

  override fun checkConfiguration() {
    super.checkConfiguration()
    if (StringUtil.isEmpty(path)) {
      throw RuntimeConfigurationError("Path is not specified")
    }
    val m = module ?: throw RuntimeConfigurationError("Module for path is not found")
    val showSettings = Runnable {
      when {
        PlatformUtils.isPyCharm() ->
          ShowSettingsUtil.getInstance().showSettingsDialog(project, MicroPythonProjectConfigurable::class.java)
        PlatformUtils.isIntelliJ() ->
          ProjectSettingsService.getInstance(project).openModuleSettings(module)
        else ->
          ShowSettingsUtil.getInstance().showSettingsDialog(project)
      }
    }
    val facet = m.microPythonFacet ?: throw RuntimeConfigurationError(
        "MicroPython support is not enabled for selected module in IDE settings",
        showSettings)
    val validationResult = facet.checkValid()
    if (validationResult != ValidationResult.OK) {
      throw RuntimeConfigurationError(validationResult.errorMessage) {
        validationResult.quickFix.run(null)
      }
    }
    facet.pythonPath ?: throw RuntimeConfigurationError("Python interpreter is not found")
    if (!facet.autoDetectDevicePath && facet.devicePath == null) {
      throw RuntimeConfigurationError("Device path is not specified in IDE settings", showSettings)
    }
  }

  override fun suggestedName() = "Flash ${PathUtil.getFileName(path)}"

  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    element.setAttribute("path", path)
  }

  override fun readExternal(element: Element) {
    super.readExternal(element)
    configurationModule.readExternal(element)
    element.getAttributeValue("path")?.let {
      path = it
    }
  }

  val module: Module?
    get() {
      val file = StandardFileSystems.local().findFileByPath(path) ?: return null
      return ModuleUtil.findModuleForFile(file, project)
    }
}

