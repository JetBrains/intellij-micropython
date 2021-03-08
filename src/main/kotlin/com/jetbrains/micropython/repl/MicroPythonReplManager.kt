package com.jetbrains.micropython.repl

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.jediterm.terminal.TtyConnector
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.firstMicroPythonFacet
import com.pty4j.PtyProcess
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.TerminalProcessOptions
import java.lang.ref.WeakReference

sealed class CommsEvent {
    class ProcessStarted(val ttyConnector: TtyConnector) : CommsEvent()
    class ProcessCreationFailed(val reason: String) : CommsEvent()
    object ProcessDestroyed : CommsEvent()
}

interface CommsEventObserver {
    fun onCommsEvent(event: CommsEvent)
}

@Service
class MicroPythonReplManager(project: Project) {
    private val currentProject = project
    private var observerRef: WeakReference<CommsEventObserver>? = null
    private var currentProcess: Process? = null
    var currentConnector: TtyConnector? = null

    companion object {
        fun getInstance(project: Project): MicroPythonReplManager =
            ServiceManager.getService(project, MicroPythonReplManager::class.java)
    }

    fun startREPL() {
        if (isRunning) {
            stopREPL()
        }

        currentProject.firstMicroPythonFacet?.let {
            val devicePath = it.getOrDetectDevicePathSynchronously()

            if (it.pythonPath == null) {
                notifyObservers(
                    CommsEvent.ProcessCreationFailed("Valid Python interpreter is needed to start a REPL!")
                )
                return
            }

            if (devicePath == null) {
                notifyObservers(CommsEvent.ProcessCreationFailed("Device path is not specified, please check settings."))
                return
            }

            val terminalRunner = object : LocalTerminalDirectRunner(currentProject) {
                override fun getInitialCommand(envs: MutableMap<String, String>): MutableList<String> {
                    return mutableListOf(
                        it.pythonPath!!,
                        "${MicroPythonFacet.scriptsPath}/microrepl.py",
                        devicePath
                    )
                }

                fun getTtyConnector(process: PtyProcess): TtyConnector {
                    return this.createTtyConnector(process)
                }
            }

            synchronized(this) {
                val terminalOptions = TerminalProcessOptions(null, null, null)
                val process = terminalRunner.createProcess(terminalOptions, null)
                val ttyConnector = terminalRunner.getTtyConnector(process)

                currentProcess = process
                currentConnector = ttyConnector
                notifyObservers(CommsEvent.ProcessStarted(ttyConnector))
            }
        }
    }

    fun stopREPL() {
        synchronized(this) {
            currentProcess?.let {
                it.destroy()
                notifyObservers(CommsEvent.ProcessDestroyed)
            }
            currentProcess = null
        }
    }

    val isRunning: Boolean
        get() = currentProcess?.isAlive ?: false

    fun registerObserver(observer: CommsEventObserver) {
        observerRef = WeakReference(observer)

        currentConnector?.let {
            observer.onCommsEvent(CommsEvent.ProcessStarted(it))
        }
    }

    private fun notifyObservers(event: CommsEvent) {
        observerRef?.get()?.onCommsEvent(event)
    }
}