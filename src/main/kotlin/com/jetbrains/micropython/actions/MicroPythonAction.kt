package com.jetbrains.micropython.actions

import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.micropython.settings.firstMicroPythonFacet

abstract class MicroPythonAction : AnAction() {
    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val facet = project.firstMicroPythonFacet
        if (facet != null) {
            e.presentation.isEnabled = facet.checkValid() == ValidationResult.OK
        } else {
            e.presentation.isVisible = false
            e.presentation.isEnabled = false
        }
    }
}
