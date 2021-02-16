package com.jetbrains.micropython.repl

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jediterm.terminal.TtyConnector
import com.jetbrains.micropython.settings.MicroPythonDevicesConfiguration
import org.jetbrains.plugins.terminal.JBTerminalSystemSettingsProvider
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import java.awt.BorderLayout
import javax.swing.JPanel

class ToolWindowReplTab(val project: Project, parent: Disposable) : CommsEventObserver {
    val deviceCommsManager = DeviceCommsManager.getInstance(project)
    private val deviceConfiguration = MicroPythonDevicesConfiguration.getInstance(project)
    val terminalWidget: ShellTerminalWidget

    init {
        val mySettingsProvider = JBTerminalSystemSettingsProvider()
        terminalWidget = ShellTerminalWidget(project, mySettingsProvider, parent)

        Disposer.register(terminalWidget, mySettingsProvider)

        deviceCommsManager.registerObserver(this)
        if (!deviceCommsManager.isRunning()) {
            deviceCommsManager.startREPL()
        }
    }

    private fun connectWidgetTty(
        terminalWidget: ShellTerminalWidget,
        connector: TtyConnector
    ) {
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

    override fun onCommsEvent(event: CommsEvent) {
        when (event) {
            is CommsEvent.ProcessStarted -> {
                if (deviceConfiguration.clearReplOnLaunch) {
                    terminalWidget.terminal.clearScreen()
                } else {
                    terminalWidget.terminal.nextLine()
                }
                connectWidgetTty(terminalWidget, event.ttyConnector)
            }
            is CommsEvent.ProcessCreationFailed -> {
                terminalWidget.terminal.nextLine()
                terminalWidget.currentSession.terminal.writeCharacters(event.reason)
            }
            CommsEvent.ProcessDestroyed -> terminalWidget.stop()
        }
    }

    fun createUI(): JPanel {
        val toolbarActions = DefaultActionGroup()

        toolbarActions.add(replStartAction())
        toolbarActions.add(replStopAction())

        return JPanel().apply {
            val actionToolbar =
                ActionManager.getInstance().createActionToolbar("MicroPythonREPL", toolbarActions, false)
            layout = BorderLayout()

            add(actionToolbar.component, BorderLayout.WEST)
            add(terminalWidget.component)
        }
    }

    private fun replStopAction() = object : AnAction(
        "Stop", "Stop REPL session", AllIcons.Actions.Suspend
    ), DumbAware {
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = deviceCommsManager.isRunning()
        }

        override fun actionPerformed(e: AnActionEvent) {
            terminalWidget.stop()
            deviceCommsManager.stopREPL()
        }
    }

    private fun replStartAction() =
        object : AnAction("Start", "Start REPL session", AllIcons.Actions.Execute), DumbAware {
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = !deviceCommsManager.isRunning()
            }

            override fun actionPerformed(e: AnActionEvent) {
                if (!terminalWidget.isSessionRunning) {
                    deviceCommsManager.startREPL()
                }
            }
        }
}
