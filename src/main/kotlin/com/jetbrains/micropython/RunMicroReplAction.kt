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

package com.jetbrains.micropython

import com.intellij.execution.Executor
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.MicroPythonFacetType
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.TerminalView

/**
 * @author Mikhail Golubev
 */

class RunMicroReplAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent?) {
    val project = e?.project ?: return
    TerminalView.getInstance(project).createNewSession(project, object : LocalTerminalDirectRunner(project) {
      override fun getCommand(envs: MutableMap<String, String>?): Array<String> {
        val facet = getFacet(project) ?: return emptyArray()
        return facet.configuration.deviceProvider.getReplTerminalCommand(facet).toTypedArray()
      }

      override fun createCloseAction(defaultExecutor: Executor?, myDescriptor: RunContentDescriptor?): AnAction {
        return super.createCloseAction(defaultExecutor, myDescriptor)
      }
    })
  }

  override fun update(e: AnActionEvent?) {
    val project = e?.project ?: return
    e.presentation.icon = MicroPythonFacetType.LOGO
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
      .map { MicroPythonFacet.getInstance(it) }
      .firstOrNull()
}
