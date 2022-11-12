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

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.serviceContainer.ComponentManagerImpl
import com.intellij.util.PathUtil
import com.intellij.util.PlatformUtils
import com.jetbrains.micropython.repl.MicroPythonReplManager
import com.jetbrains.micropython.settings.MicroPythonProjectConfigurable
import com.jetbrains.micropython.settings.microPythonFacet
import org.jdom.Element

/**
 * @author Mikhail Golubev
 */

class RunStateWrapper(private val original: RunProfileState, val block: () -> Unit) : RunProfileState by original {
  override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
    val result = original.execute(executor, runner)

    result?.processHandler?.addProcessListener(object : ProcessListener {
      override fun startNotified(event: ProcessEvent) {}

      override fun processTerminated(event: ProcessEvent) {
        if (event.exitCode == 0) {
          block()
        }
      }

      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {}
    })

    return result
  }
}

class MicroPythonRunConfiguration(project: Project, factory: ConfigurationFactory) : AbstractRunConfiguration(project, factory), RunConfigurationWithSuppressedDefaultDebugAction {

  var path: String = ""
  var runReplOnSuccess: Boolean = false
  override fun getValidModules() =
          allModules.filter { it.microPythonFacet != null }.toMutableList()

  override fun getConfigurationEditor() = MicroPythonRunConfigurationEditor(this)

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    val currentModule = environment.dataContext?.getData(LangDataKeys.MODULE) ?: module
    val state = currentModule?.microPythonFacet?.configuration?.deviceProvider?.getRunCommandLineState(this, environment)
//    ComponentManagerImpl
    if (runReplOnSuccess && state != null) {
      return RunStateWrapper(state) {
        ApplicationManager.getApplication().invokeLater {
          MicroPythonReplManager.getInstance(currentModule).startREPL()
          ToolWindowManager.getInstance(project).getToolWindow("MicroPython")?.show()
        }
      }
    }

    return state
  }

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
            showSettings
    )
    val validationResult = facet.checkValid()
    if (validationResult != ValidationResult.OK) {
      val runQuickFix = Runnable {
        validationResult.quickFix.run(null)
      }
      throw RuntimeConfigurationError(validationResult.errorMessage, runQuickFix)
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
    element.setAttribute("runReplOnSuccess", if (runReplOnSuccess) "yes" else "no")
  }

  override fun readExternal(element: Element) {
    super.readExternal(element)
    configurationModule.readExternal(element)
    element.getAttributeValue("path")?.let {
      path = it
    }
    element.getAttributeValue("runReplOnSuccess")?.let {
      runReplOnSuccess = it == "yes"
    }
  }

  val module: Module?
    get() {
      val file = StandardFileSystems.local().findFileByPath(path) ?: return null
      return ModuleUtil.findModuleForFile(file, project)
    }
}
