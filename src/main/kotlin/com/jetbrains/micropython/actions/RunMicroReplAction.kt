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
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.micropython.run.DeviceCommsManager

class RunMicroReplAction : MicroPythonAction() {

  override fun actionPerformed(e: AnActionEvent) {
    e.project?.let { project ->
      DeviceCommsManager.getInstance(project).startREPL()
      ToolWindowManager.getInstance(project).getToolWindow("MicroPython")?.show()
    }
  }

}
