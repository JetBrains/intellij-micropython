package com.jetbrains.micropython.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.jetbrains.micropython.repl.MicroPythonReplManager
import com.jetbrains.micropython.repl.ToolWindowReplTab
import com.jetbrains.micropython.settings.firstMicroPythonFacet

class MicroPythonToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val terminalContent = contentFactory.createContent(null, "REPL", false)

        project.firstMicroPythonFacet?.let {
            terminalContent.component = ToolWindowReplTab(it.module, terminalContent).createUI()
        }

        toolWindow.contentManager.addContent(terminalContent)

        project.firstMicroPythonFacet?.let {
            MicroPythonReplManager.getInstance(it.module).startREPL()
        }
    }
}
