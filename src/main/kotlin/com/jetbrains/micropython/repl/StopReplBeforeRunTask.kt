package com.jetbrains.micropython.repl

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import com.jetbrains.micropython.run.MicroPythonRunConfiguration
import com.jetbrains.micropython.settings.MicroPythonFacetType

class StopReplBeforeRunTask : BeforeRunTask<StopReplBeforeRunTask>(StopReplBeforeRunTaskProvider.ID) {
    init {
        isEnabled = true
    }
}

class StopReplBeforeRunTaskProvider : BeforeRunTaskProvider<StopReplBeforeRunTask>() {
    companion object {
        val ID = Key.create<StopReplBeforeRunTask>("MicroPython.StopREPL.Before.Run")
    }

    override fun getId() = ID

    override fun getIcon() = MicroPythonFacetType.LOGO

    override fun getName() = "Stop MicroPython REPL"

    override fun createTask(runConfiguration: RunConfiguration): StopReplBeforeRunTask? {
        if (runConfiguration is MicroPythonRunConfiguration) {
            return StopReplBeforeRunTask()
        }

        return null
    }

    override fun executeTask(
            context: DataContext,
            configuration: RunConfiguration,
            environment: ExecutionEnvironment,
            task: StopReplBeforeRunTask
    ): Boolean {
        if (configuration is MicroPythonRunConfiguration) {
            ApplicationManager.getApplication().invokeLater {
                configuration.module?.let {
                    ApplicationManager.getApplication().runWriteAction {
                        MicroPythonReplManager.getInstance(it).stopREPL()
                    }
                }
            }
        }

        return true
    }

}