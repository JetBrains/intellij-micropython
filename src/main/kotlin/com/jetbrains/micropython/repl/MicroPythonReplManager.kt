package com.jetbrains.micropython.repl

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic


interface MicroPythonReplControl {
    fun stopRepl()
    fun startOrRestartRepl()
}

@Service(Service.Level.PROJECT)
class MicroPythonReplManager(private val project: Project) : MicroPythonReplControl {
    override fun stopRepl() =
        project.messageBus.syncPublisher(MICROPYTHON_REPL_CONTROL).stopRepl()


    override fun startOrRestartRepl() =
        project.messageBus.syncPublisher(MICROPYTHON_REPL_CONTROL).startOrRestartRepl()

}

public val MICROPYTHON_REPL_CONTROL = Topic(MicroPythonReplControl::class.java)