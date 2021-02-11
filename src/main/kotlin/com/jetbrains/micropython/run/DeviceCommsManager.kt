package com.jetbrains.micropython.run

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.jediterm.terminal.TtyConnector
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.firstMicroPythonFacet
import com.pty4j.PtyProcess
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
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
class DeviceCommsManager(project: Project) {
    private val currentProject = project
    private var observerRef: WeakReference<CommsEventObserver>? = null
    private var currentProcess: Process? = null
    var currentConnector: TtyConnector? = null

    companion object {
        fun getInstance(project: Project): DeviceCommsManager =
            ServiceManager.getService(project, DeviceCommsManager::class.java)
    }

    fun startREPL() {
        stopREPL()

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

            val process = terminalRunner.createProcess(null)
            currentProcess = process
            terminalRunner.getTtyConnector(process).let { newConnector ->
                currentConnector = newConnector
                notifyObservers(CommsEvent.ProcessStarted(newConnector))
            }
        }
    }

    fun stopREPL() {
        currentProcess?.let {
            it.destroy()
            notifyObservers(CommsEvent.ProcessDestroyed)
        }
        currentProcess = null
    }

    fun isRunning() : Boolean = currentProcess?.isAlive ?: false

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