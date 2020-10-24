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
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.firstMicroPythonFacet
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.TerminalView

/**
 * @author vlan
 */
abstract class MicroPythonCommandAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val facet = project.firstMicroPythonFacet ?: return
    val command = getCommand(facet) ?: return

    TerminalView.getInstance(project).createNewSession(object : LocalTerminalDirectRunner(project) {
      // XXX: This method is deprecated, but it's the only one available in both 2020.2.* and 2020.3 EAP
      override fun getCommands(envs: Map<String, String>) = command
    })
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    val facet = project.firstMicroPythonFacet
    if (facet != null) {
      e.presentation.isEnabled = facet.checkValid() == ValidationResult.OK
    }
    else {
      e.presentation.isVisible = false
      e.presentation.isEnabled = false
    }
  }

  protected abstract fun getCommand(facet: MicroPythonFacet): List<String>?
}