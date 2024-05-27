package com.jetbrains.micropython.repl

import com.intellij.icons.AllIcons
import com.intellij.ide.ActivityTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.application
import com.jediterm.terminal.TtyConnector
import com.jetbrains.micropython.settings.MicroPythonDevicesConfiguration
import com.jetbrains.micropython.settings.microPythonFacet
import com.jetbrains.python.sdk.basePath
import org.jetbrains.plugins.terminal.JBTerminalSystemSettingsProvider
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellStartupOptions
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import java.awt.BorderLayout
import java.util.concurrent.TimeUnit
import javax.swing.JPanel

class ToolWindowReplTab(val module: Module, parent: Disposable) : MicroPythonReplControl {
    private val deviceConfiguration = MicroPythonDevicesConfiguration.getInstance(module.project)
    val terminalWidget: ShellTerminalWidget

    init {
        val mySettingsProvider = JBTerminalSystemSettingsProvider()
        terminalWidget = ShellTerminalWidget(module.project, mySettingsProvider, parent)
        terminalWidget.isEnabled = false
        module.project.messageBus.connect(parent).subscribe(MICROPYTHON_REPL_CONTROL, this)

    }

    private fun connectWidgetTty(terminalWidget: ShellTerminalWidget, connector: TtyConnector) {
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
            add(replStartAction)
            add(replStopAction)
            add(clearReplOnLaunch)
        }
        val actionToolbar = actionManager.createActionToolbar("MicroPythonREPL", toolbarActions, false)
        actionToolbar.targetComponent = terminalWidget.component

        return JPanel().apply {
            layout = BorderLayout()

            add(actionToolbar.component, BorderLayout.WEST)
            add(terminalWidget.component)
        }
    }

    private val replStopAction = object : DumbAwareAction(
        "Stop", "Stop REPL session", AllIcons.Actions.Suspend
    ) {
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = terminalWidget.isSessionRunning
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) {
            stopRepl()
        }
    }

    private val replStartAction =
        object : DumbAwareAction() {
            override fun update(e: AnActionEvent) {
                with(e.presentation) {
                    isEnabled = true
                    if(terminalWidget.isSessionRunning) {
                        text="Restart"
                        description= "Restart REPL session"
                        icon= AllIcons.Actions.Restart
                    } else {
                        text="Start"
                        description= "Start REPL session"
                        icon= AllIcons.Toolwindows.ToolWindowRun
                    }
                }

            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

            override fun actionPerformed(e: AnActionEvent) =
                module.project.service<MicroPythonReplManager>().startOrRestartRepl()
        }

    private val clearReplOnLaunch = object : ToggleAction(
        "Clear Window On Start",
        "Clear REPL window on every start", AllIcons.Actions.GC
    ) {
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

    private fun onProcessCreationFailed(@NlsContexts.SystemNotificationText reason: String) {
        terminalWidget.terminal.nextLine()
        terminalWidget.terminal.writeCharacters(reason)
    }

    private fun interruptBanner() {
        application.invokeLater(
            {
                with(terminalWidget.terminal) {
                    nextLine()
                    writeCharacters("=== SESSION HAS BEEN INTERRUPTED ===")
                    nextLine()
                }
            },
            { module.isDisposed }
        )

    }
    override fun stopRepl() {
        interruptBanner()
        application.executeOnPooledThread {
            synchronized(this) {
                terminalWidget.processTtyConnector?.process?.destroy()
            }
        }
    }

    override fun startOrRestartRepl() {
        interruptBanner()
        application.executeOnPooledThread {
            synchronized(this) {
                terminalWidget.processTtyConnector?.process?.apply {
                    if (isAlive) destroy()
                    waitFor(10, TimeUnit.SECONDS)
                }
                while(terminalWidget.isSessionRunning){
                    Thread.sleep(100)
                }
                application.invokeLater(
                    { startRepl() },
                    { module.project.isDisposed })
            }
        }
    }

    private fun startRepl() {
        val facet = module.microPythonFacet ?: return
        val devicePath = facet.getOrDetectDevicePathSynchronously()

        if (facet.pythonPath == null) {
            onProcessCreationFailed("Valid Python interpreter is needed to start REPL!")
            return
        }

        if (devicePath == null) {
            onProcessCreationFailed("Device path is not specified, please check settings.")
            return
        }

        val initialShellCommand = listOf(
            facet.pythonPath!!,"-m", "mpremote",
            "connect", devicePath, "soft-reset",
            "repl")

        val terminalRunner = LocalTerminalDirectRunner(module.project)

        synchronized(this) {
            val terminalOptions = terminalRunner.configureStartupOptions(
                ShellStartupOptions.Builder()
                    .shellCommand(initialShellCommand)
                    .workingDirectory(module.basePath)
                    .build()
            )

            if (deviceConfiguration.clearReplOnLaunch) {
                terminalWidget.terminalTextBuffer.clearHistory()
                terminalWidget.terminal.apply {
                    reset(true)
                    writeCharacters("Quit: Ctrl+X | Stop program: Ctrl+C | Reset: Ctrl+D")
                    nextLine()
                    writeCharacters("Type 'help()' (without the quotes) then press ENTER.")
                    nextLine()
                    scrollUp(6)
                }
            } else {
                terminalWidget.terminal.nextLine()
            }
            val process = terminalRunner.createProcess(terminalOptions)
            val ttyConnector = terminalRunner.createTtyConnector(process)
            process.onExit().whenComplete { _, _ -> ActivityTracker.getInstance().inc() }
            connectWidgetTty(terminalWidget, ttyConnector)
            terminalWidget.isEnabled = true
        }
    }
}
