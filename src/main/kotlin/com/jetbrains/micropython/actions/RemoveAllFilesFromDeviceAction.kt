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

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.ui.content.ContentFactory
import com.jetbrains.micropython.repl.MicroPythonReplManager
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.MicroPythonFacetType
import com.jetbrains.micropython.settings.firstMicroPythonFacet

private val COMMAND_KEY = com.intellij.openapi.util.Key<Boolean>("MicroPythonCommand")

class RemoveAllFilesFromDeviceAction : MicroPythonAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val facet = project?.firstMicroPythonFacet
        val pythonPath = facet?.pythonPath ?: return
        val devicePath = facet.getOrDetectDevicePathSynchronously() ?: return
        project.service<MicroPythonReplManager>().stopRepl()
        val cmd = GeneralCommandLine()
            .withExePath(pythonPath)
            .withParameters("-m", "mpremote")
            .withParameters("connect", devicePath)
            .withParameters("run", "${MicroPythonFacet.scriptsPath}/microcleanfs.py")

        val osProcessHandler = OSProcessHandler(cmd)
        val console = TerminalExecutionConsole(project, osProcessHandler)
            .withConvertLfToCrlfForNonPtyProcess(true)
        Disposer.register(facet, console)

        osProcessHandler.addProcessListener(
            object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    with(console.terminalWidget.terminal) {
                        nextLine()
                        writeCharacters("==== Finished with exit code ${event.exitCode} ====")
                    }
                }
            }
        )

        val toolWindow =
            ToolWindowManager.getInstance(project).getToolWindow(MicroPythonFacetType.PRESENTABLE_NAME) ?: return
        toolWindow.show()
        val contentManager = toolWindow.contentManager
        var content = contentManager.contents.firstOrNull { it.getUserData(COMMAND_KEY) ?: false }
        if (content == null) {
            content = ContentFactory.getInstance().createContent(console.component, "Command", true)
            content.putUserData(COMMAND_KEY, true)
            content.isCloseable = true
            contentManager.addContent(content)
        }

        toolWindow.activate(null)
        contentManager.setSelectedContent(content)
        osProcessHandler.startNotify()
    }

}