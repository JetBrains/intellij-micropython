package com.jetbrains.micropython.nova

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.readAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.asSafely
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jediterm.terminal.TerminalMode
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.ui.JediTermWidget
import com.jetbrains.micropython.settings.MicroPythonFacetType
import com.jetbrains.rd.util.URI
import kotlinx.coroutines.Dispatchers
import com.intellij.openapi.application.EDT
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.terminal.JBTerminalSystemSettingsProvider
import javax.swing.Icon
import javax.swing.JComponent

class ReplOpen : DumbAwareAction("Open REPL", null, MicroPythonFacetType.LOGO) {
  override fun actionPerformed(e: AnActionEvent) {
    ToolWindowManager.getInstance(e.project!!).getToolWindow("com.jetbrains.micropython.repl.MicroPythonToolWindow")?.apply {
      show()
      if (contentManager.contentCount == 0) {
        MicroPythonToolWindow().createToolWindowContent(e.project!!, this)
      }
    }

  }
}


class MicroPythonToolWindow : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val newDisposable = Disposer.newDisposable(toolWindow.disposable, "Webrepl")
    val comm = WebSocketComm {
      it.printStackTrace()
    }
    Disposer.register(newDisposable, comm)

    val tabbedPane = JBTabbedPane()
    val jediTermWidget = jediTermWidget(comm.ttyConnector)
    tabbedPane.addTab("REPL", jediTermWidget)
    val fileSystemWidget = FileSystemWidget(project, comm)
    tabbedPane.addTab("File System", fileSystemWidget)
    val content = ContentFactory.getInstance().createContent(tabbedPane, "WebRepl", true)
    FILE_SYSTEM_WIDGET_KEY.set(content, fileSystemWidget)
    content.setDisposer(newDisposable)
    toolWindow.contentManager.addContent(content)
    runWithModalProgressBlocking(project, "Initializing WebREPL") {
      comm.connect(URI("ws://192.168.50.68:8266"), "passwd")
      fileSystemWidget.refresh()
    }
  }

  private fun jediTermWidget(connector: TtyConnector): JComponent {
    val mySettingsProvider = JBTerminalSystemSettingsProvider()
    val terminal = JediTermWidget(mySettingsProvider)
    terminal.isEnabled = false
    with(terminal.terminal) {
      setModeEnabled(TerminalMode.ANSI, true)
      setModeEnabled(TerminalMode.AutoNewLine, true)
      setModeEnabled(TerminalMode.WideColumn, true)
    }
    terminal.ttyConnector = connector
    terminal.start()

    val widget = BorderLayoutPanel()
    widget.addToCenter(terminal)
    val actions = ActionManager.getInstance().getAction("micropython.repl.ReplToolbar") as ActionGroup
    val actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actions, true)
    actionToolbar.targetComponent = terminal
    widget.addToTop(actionToolbar.component)
    return widget

  }

}

abstract class ReplAction(text: String, icon: Icon) : DumbAwareAction(text, "", icon) {
  abstract suspend fun performAction(fileSystemWidget: FileSystemWidget, editor: FileEditor)
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = FileEditorManager
                   .getInstance(project)
                   .selectedEditor
                 ?: return
    val fileSystemWidget = ToolWindowManager
                             .getInstance(project)
      .getToolWindow("com.jetbrains.micropython.nova.MicroPythonToolWindow")
                             ?.contentManager
                             ?.selectedContent
                             ?.getUserData(FILE_SYSTEM_WIDGET_KEY)
                           ?: return
    runWithModalProgressBlocking(project, "Board data exchange...") {
      performAction(fileSystemWidget, editor)
    }
  }
}

class Refresh : ReplAction("Refresh", AllIcons.Actions.Refresh) {
  override suspend fun performAction(fileSystemWidget: FileSystemWidget, editor: FileEditor) {
    fileSystemWidget.refresh()
  }
}

class InstantRun : ReplAction("Instant Run", AllIcons.Actions.Rerun) {
  override suspend fun performAction(fileSystemWidget: FileSystemWidget, editor: FileEditor) {
    val code = withContext(Dispatchers.EDT) {
      editor.asSafely<TextEditor>()
        ?.editor
        ?.document
        ?.text
    }
    if (code != null) {
      fileSystemWidget.comm.instantRun(code)
    }
  }
}

class DeleteFile : ReplAction("Delete File", AllIcons.General.Remove) {
  override suspend fun performAction(fileSystemWidget: FileSystemWidget, editor: FileEditor) {
    fileSystemWidget.deleteCurrent()
  }
}


open class UploadFile(text: String = "Upload File", icon: Icon = AllIcons.Actions.Upload) : ReplAction(text, icon) {

  override suspend fun performAction(fileSystemWidget: FileSystemWidget, editor: FileEditor) {
    val code = editor.asSafely<TextEditor>()?.editor?.document?.text ?: return
    val file = editor.file
    val fullName = readAction {
      ProjectRootManagerEx.getInstance(fileSystemWidget.project).contentRoots.firstNotNullOfOrNull { root ->
        VfsUtil.getRelativePath(file, root)
      }
    }
    if (fullName != null) {
        fileSystemWidget.comm.upload(fullName, code)
        fileSystemWidget.refresh()
      }
  }
}
