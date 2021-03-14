package com.jetbrains.micropython.repl

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.jediterm.terminal.TtyConnector
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.firstMicroPythonFacet
import com.pty4j.PtyProcess
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.TerminalProcessOptions

interface CommsEventListener {
    fun onProcessStarted(ttyConnector: TtyConnector)
    fun onProcessDestroyed()
    fun onProcessCreationFailed(reason: String)
}

@Service
class MicroPythonReplManager(project: Project) {
    private val currentProject = project
    private var listeners: MutableList<CommsEventListener> = mutableListOf()
    private var currentProcess: Process? = null
    private var currentConnector: TtyConnector? = null

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
                notifyProcessCreationFailed("Valid Python interpreter is needed to start a REPL!")
                return
            }

            if (devicePath == null) {
                notifyProcessCreationFailed("Device path is not specified, please check settings.")
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
                notifyProcessStarted(ttyConnector)
            }
        }
    }

    fun stopREPL() {
        synchronized(this) {
            currentProcess?.let {
                it.destroy()
                notifyProcessDestroyed()
            }
            currentProcess = null
        }
    }

    val isRunning: Boolean
        get() = currentProcess?.isAlive ?: false

    fun addListener(listener: CommsEventListener) {
        listeners.add(listener)

        currentConnector?.let { listener.onProcessStarted(it) }
    }

    fun removeListener(listener: CommsEventListener) {
        listeners.remove(listener)
    }

    private fun notifyProcessStarted(connector: TtyConnector) {
        listeners.forEach { it.onProcessStarted(connector) }
    }

    private fun notifyProcessDestroyed() {
        listeners.forEach { it.onProcessDestroyed() }
    }

    private fun notifyProcessCreationFailed(reason: String) {
        listeners.forEach { it.onProcessCreationFailed(reason) }
    }
}