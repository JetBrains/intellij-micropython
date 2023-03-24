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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.firstMicroPythonFacet
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

/**
 * @author vlan
 */
abstract class MicroPythonCommandAction : MicroPythonAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val facet = project.firstMicroPythonFacet ?: return
    val command = getCommand(facet) ?: return
    TerminalToolWindowManager.getInstance(project).createNewSession(object : LocalTerminalDirectRunner(project) {
      override fun getInitialCommand(envs: Map<String, String>): List<String> = command
    })
  }

  protected abstract fun getCommand(facet: MicroPythonFacet): List<String>?
}
