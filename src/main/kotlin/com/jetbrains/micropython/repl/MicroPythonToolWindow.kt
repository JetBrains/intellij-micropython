package com.jetbrains.micropython.repl

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
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
import com.intellij.ui.IconManager
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.asSafely
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jediterm.terminal.TerminalMode
import com.jediterm.terminal.ui.JediTermWidget
import com.jetbrains.micropython.fs.FILE_SYSTEM_WIDGET_KEY
import com.jetbrains.micropython.fs.FileSystemWidget
import com.jetbrains.micropython.settings.MicroPythonFacetType
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
    val connector = WebSocketTtyConnector("ws://192.168.50.68:8266", "passwd") {
      it.printStackTrace()
    }
    Disposer.register(newDisposable, connector)

    val tabbedPane = JBTabbedPane()
    connector.connect()
    val jediTermWidget = jediTermWidget(connector)
    tabbedPane.addTab("REPL", jediTermWidget)
    val fileSystemWidget = FileSystemWidget(project, connector)
    tabbedPane.addTab("File System", fileSystemWidget)
    val content = ContentFactory.getInstance().createContent(tabbedPane, "WebRepl", true)
    FILE_SYSTEM_WIDGET_KEY.set(content, fileSystemWidget)
    content.setDisposer(newDisposable)
    toolWindow.contentManager.addContent(content)
    fileSystemWidget.refresh()
  }

  private fun jediTermWidget(connector: WebSocketTtyConnector): JComponent {
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
  abstract fun performAction(fileSystemWidget: FileSystemWidget, editor: FileEditor)
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = FileEditorManager
                   .getInstance(project)
                   .selectedEditor
                 ?: return
    val fileSystemWidget = ToolWindowManager
                             .getInstance(project)
                             .getToolWindow("com.jetbrains.micropython.repl.MicroPythonToolWindow")
                             ?.contentManager
                             ?.selectedContent
                             ?.getUserData(FILE_SYSTEM_WIDGET_KEY)
                           ?: return
    performAction(fileSystemWidget, editor)
  }
}

class Refresh : ReplAction("Refresh", AllIcons.Actions.Refresh) {
  override fun performAction(fileSystemWidget: FileSystemWidget, editor: FileEditor) {
    fileSystemWidget.refresh()
  }
}

class InstantRun : ReplAction("Instant Run", AllIcons.Actions.Rerun) {
  override fun performAction(fileSystemWidget: FileSystemWidget, editor: FileEditor) {
    val code = editor.asSafely<TextEditor>()
                 ?.editor
                 ?.document
                 ?.text ?: return

    fileSystemWidget.connector.instantRun(code)
  }
}

class DeleteFile : ReplAction("Delete File", AllIcons.General.Remove) {
  override fun performAction(fileSystemWidget: FileSystemWidget, editor: FileEditor) {
    fileSystemWidget.deleteCurrent()
  }
}


open class UploadFile(text: String = "Upload File", icon: Icon = AllIcons.Actions.Upload) : ReplAction(text, icon) {

  override fun performAction(fileSystemWidget: FileSystemWidget, editor: FileEditor) {
    val code = editor.asSafely<TextEditor>()
                 ?.editor
                 ?.document
                 ?.text ?: return
    val file = editor.file
    val fullName =
      ProjectRootManagerEx
        .getInstance(fileSystemWidget.project)
        .contentRoots.firstNotNullOfOrNull {
          root -> VfsUtil.getRelativePath(file, root)
        } ?: return

    fileSystemWidget.connector.upload(fullName, code)
    fileSystemWidget.refresh()
  }
}

class UploadFileAndReset : UploadFile("Upload File",
                                      IconManager.getInstance().createLayered(AllIcons.Actions.Upload, AllIcons.Actions.New)) {
  override fun performAction(fileSystemWidget: FileSystemWidget, editor: FileEditor) {
    super.performAction(fileSystemWidget, editor)
    fileSystemWidget.connector.write("\u0004")
  }
}
