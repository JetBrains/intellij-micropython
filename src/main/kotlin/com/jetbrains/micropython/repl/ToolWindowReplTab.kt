package com.jetbrains.micropython.repl

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbAware
import com.jediterm.terminal.TtyConnector
import com.jetbrains.micropython.settings.MicroPythonDevicesConfiguration
import org.jetbrains.plugins.terminal.JBTerminalSystemSettingsProvider
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import java.awt.BorderLayout
import javax.swing.JPanel

class ToolWindowReplTab(val module: Module, parent: Disposable) : CommsEventListener, Disposable {
    private val deviceConfiguration = MicroPythonDevicesConfiguration.getInstance(module.project)
    private val deviceCommsManager = MicroPythonReplManager.getInstance(module)
    val terminalWidget: ShellTerminalWidget

    init {
        val mySettingsProvider = JBTerminalSystemSettingsProvider()
        terminalWidget = ShellTerminalWidget(module.project, mySettingsProvider, parent)
        terminalWidget.isEnabled = false
        deviceCommsManager.addListener(this)
    }

    private fun connectWidgetTty(terminalWidget: ShellTerminalWidget, connector: TtyConnector) {
        if (terminalWidget.isSessionRunning) {
            terminalWidget.stop()
        }
        terminalWidget.start(connector)
        val modalityState = ModalityState.stateForComponent(terminalWidget.component)

        ApplicationManager.getApplication().invokeLater(
                {
                    try {
                        terminalWidget.component.revalidate()
                        terminalWidget.notifyStarted()
                    } catch (e: RuntimeException) {
                        TODO("You can't cut back on error reporting! You will regret this!")
                    }
                },
                modalityState
        )
    }

    fun createUI(): JPanel {
        val actionManager = ActionManager.getInstance()
        val toolbarActions = DefaultActionGroup().apply {
            add(replStartAction())
            add(replStopAction())
            add(clearReplOnLaunch())
        }
        val actionToolbar = actionManager.createActionToolbar("MicroPythonREPL", toolbarActions, false)
        actionToolbar.targetComponent = terminalWidget.component

        return JPanel().apply {
            layout = BorderLayout()

            add(actionToolbar.component, BorderLayout.WEST)
            add(terminalWidget.component)
        }
    }

    private fun replStopAction() = object : AnAction(
            "Stop", "Stop REPL session", AllIcons.Actions.Suspend
    ), DumbAware {
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = deviceCommsManager.isRunning
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) {
            terminalWidget.stop()
            deviceCommsManager.stopREPL()
        }
    }

    private fun replStartAction() =
        object : AnAction("Restart", "Restart REPL session", AllIcons.Actions.Restart), DumbAware {
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = true
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

            override fun actionPerformed(e: AnActionEvent) {
                if (terminalWidget.isSessionRunning) {
                    deviceCommsManager.stopREPL()
                }
                deviceCommsManager.startREPL()
            }
        }

    private fun clearReplOnLaunch() = object : ToggleAction("Clear Window On Start",
            "Clear REPL window on every start", AllIcons.Actions.GC) {
        override fun isSelected(e: AnActionEvent): Boolean {
            return deviceConfiguration.clearReplOnLaunch
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            deviceConfiguration.clearReplOnLaunch = state
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }
    }

    override fun dispose() {
        deviceCommsManager.removeListener(this)
    }

    override fun onProcessStarted(ttyConnector: TtyConnector) {
        if (deviceConfiguration.clearReplOnLaunch) {
            terminalWidget.terminalTextBuffer.clearHistory()
            terminalWidget.terminal.reset()
        } else {
            terminalWidget.terminal.nextLine()
        }
        connectWidgetTty(terminalWidget, ttyConnector)
        terminalWidget.isEnabled = true
    }

    override fun onProcessDestroyed() {
        terminalWidget.stop()

        terminalWidget.terminal.nextLine()
        terminalWidget.terminal.writeCharacters("=== SESSION HAS BEEN INTERRUPTED ===")
        terminalWidget.terminal.nextLine()
    }

    override fun onProcessCreationFailed(reason: String) {
        terminalWidget.terminal.nextLine()
        terminalWidget.terminal.writeCharacters(reason)
    }
}
