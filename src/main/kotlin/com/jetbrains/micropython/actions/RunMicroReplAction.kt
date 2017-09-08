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

package com.jetbrains.micropython.actions

import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.MicroPythonFacet.Companion.scriptsPath
import com.jetbrains.micropython.settings.microPythonFacet
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.TerminalView

/**
 * @author Mikhail Golubev
 */
class RunMicroReplAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent?) {
    val project = e?.project ?: return
    val facet = getFacet(project) ?: return
    val provider = facet.configuration.deviceProvider
    val command = getReplTerminalCommand(facet)
    if (command == null) {
      Messages.showErrorDialog(project,
                               "Cannot find the ${provider.presentableName} device. Make sure you have installed " +
                                   "any necessary USB drivers and plugged the device into a USB port.",
                               "Device Not Found")
      return
    }

    TerminalView.getInstance(project).createNewSession(project, object : LocalTerminalDirectRunner(project) {
      override fun getCommand(envs: MutableMap<String, String>?) = command.toTypedArray()
    })
  }

  override fun update(e: AnActionEvent?) {
    val project = e?.project ?: return
    val facet = getFacet(project)
    if (facet != null) {
      e.presentation.isEnabled = facet.checkValid() == ValidationResult.OK
    }
    else {
      e.presentation.isVisible = false
      e.presentation.isEnabled = false
    }
  }

  private fun getFacet(project: Project) = ModuleManager.getInstance(project).modules
      .asSequence()
      .map { it.microPythonFacet }
      .firstOrNull()

  private fun getReplTerminalCommand(facet: MicroPythonFacet): List<String>? {
    val pythonPath = facet.pythonPath ?: return null
    val devicePath = facet.devicePath ?: return null
    return listOf(pythonPath, "$scriptsPath/microrepl.py", devicePath)
  }
}
