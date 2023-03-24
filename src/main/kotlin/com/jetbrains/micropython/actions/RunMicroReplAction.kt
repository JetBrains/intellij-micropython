/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.micropython.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.micropython.repl.MicroPythonReplManager
import com.jetbrains.micropython.settings.firstMicroPythonFacet

class RunMicroReplAction : MicroPythonAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = FileEditorManagerEx.getInstanceEx(project).selectedTextEditor ?: return

    /*
    Here we make best effort to find out module which is relevant to the current event.
    There are two cases to consider:
    1) There is an open file present.
    2) No files are opened.
    */

    val virtualFile: VirtualFile? = FileDocumentManager.getInstance().getFile(editor.document)
    val module: Module? = if (virtualFile != null) {
      ModuleUtil.findModuleForFile(virtualFile, project)
    } else {
      project.firstMicroPythonFacet?.module
    }

    if (module != null) {
      MicroPythonReplManager.getInstance(module).startREPL()
      ToolWindowManager.getInstance(project).getToolWindow("MicroPython")?.show()
    }
  }
}
