package com.jetbrains.micropython.repl

import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.jediterm.terminal.TtyConnector
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.microPythonFacet
import com.pty4j.PtyProcess
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellStartupOptions

interface CommsEventListener {
    fun onProcessStarted(ttyConnector: TtyConnector)
    fun onProcessDestroyed()
    fun onProcessCreationFailed(reason: String)
}

@Service
class MicroPythonReplManager(module: Module) {
    private val currentModule: Module = module
    private var listeners: MutableList<CommsEventListener> = mutableListOf()
    private var currentProcess: Process? = null
    private var currentConnector: TtyConnector? = null

    companion object {
        fun getInstance(module: Module): MicroPythonReplManager =
                module.getService(MicroPythonReplManager::class.java)
    }

    fun startREPL() {
        if (isRunning) {
            stopREPL()
        }

        val facet = currentModule.microPythonFacet ?: return
        val devicePath = facet.getOrDetectDevicePathSynchronously()

        if (facet.pythonPath == null) {
            notifyProcessCreationFailed("Valid Python interpreter is needed to start a REPL!")
            return
        }

        if (devicePath == null) {
            notifyProcessCreationFailed("Device path is not specified, please check settings.")
            return
        }

        val initialShellCommand = mutableListOf(
            facet.pythonPath!!,
            "${MicroPythonFacet.scriptsPath}/microrepl.py",
            devicePath
        )

        val terminalRunner = object : LocalTerminalDirectRunner(currentModule.project) {
            override fun getInitialCommand(envs: MutableMap<String, String>): MutableList<String> {
                return initialShellCommand
            }

            fun getTtyConnector(process: PtyProcess): TtyConnector {
                return this.createTtyConnector(process)
            }
        }

        synchronized(this) {
            val terminalOptions = ShellStartupOptions.Builder()
                .workingDirectory(devicePath)
                .shellCommand(initialShellCommand)
                .build()
            val process = terminalRunner.createProcess(terminalOptions)
            val ttyConnector = terminalRunner.getTtyConnector(process)

            currentProcess = process
            currentConnector = ttyConnector
            notifyProcessStarted(ttyConnector)
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