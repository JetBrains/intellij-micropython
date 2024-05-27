package com.jetbrains.micropython.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.jetbrains.micropython.repl.MICROPYTHON_REPL_CONTROL
import com.jetbrains.micropython.repl.MicroPythonReplControl
import com.jetbrains.micropython.repl.MicroPythonReplManager
import com.jetbrains.micropython.repl.ToolWindowReplTab
import com.jetbrains.micropython.settings.firstMicroPythonFacet

class MicroPythonToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val terminalContent = contentFactory.createContent(null, "REPL", false)

        project.firstMicroPythonFacet?.let {
            terminalContent.component = ToolWindowReplTab(it.module, terminalContent).createUI()
            toolWindow.contentManager.addContent(terminalContent)
            project.service<MicroPythonReplManager>().startOrRestartRepl()
            project.messageBus.connect().subscribe(MICROPYTHON_REPL_CONTROL,
                object : MicroPythonReplControl {
                    override fun stopRepl() {}
                    override fun startOrRestartRepl() {
                        toolWindow.contentManager.setSelectedContent(terminalContent)
                        toolWindow.show()
                    }
                }
            )
        }
    }

}
