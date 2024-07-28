package com.jetbrains.micropython.nova

import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
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
import com.intellij.ui.content.ContentFactory
import com.intellij.util.asSafely
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jediterm.terminal.TerminalMode
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.ui.JediTermWidget
import com.jetbrains.rd.util.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.terminal.JBTerminalSystemSettingsProvider
import java.io.IOException
import javax.swing.Icon
import javax.swing.JComponent
import kotlin.coroutines.cancellation.CancellationException

private const val NOTIFICATION_GROUP = "Micropython"

class MicroPythonToolWindow : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val newDisposable = Disposer.newDisposable(toolWindow.disposable, "Webrepl")
        val comm = WebSocketComm {
            it.printStackTrace()
        }
        Disposer.register(newDisposable, comm)

        val jediTermWidget = jediTermWidget(comm.ttyConnector)
        val termContent = ContentFactory.getInstance().createContent(jediTermWidget, "REPL", true)
        val fileSystemWidget = FileSystemWidget(project, comm)
        val fileSystemContent = ContentFactory.getInstance().createContent(fileSystemWidget, "File System", true)
        toolWindow.contentManager.addContent(fileSystemContent)
        toolWindow.contentManager.addContent(termContent)
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

    abstract val actionDescription: String

    protected fun isFileEditorActive(e: AnActionEvent): Boolean {
        return e.project?.let { FileEditorManager.getInstance(it).selectedEditor } != null
    }

    @Throws(IOException::class, TimeoutCancellationException::class, CancellationException::class)
    abstract suspend fun performAction(fileSystemWidget: FileSystemWidget)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val fileSystemWidget = fileSystemWidget(project) ?: return
        fileSystemWidget.isVisible = false
        try {
        runWithModalProgressBlocking(project, "Board data exchange...") {
            var error: String? = null
            try {
                performAction(fileSystemWidget)
            } catch (e: TimeoutCancellationException) {
                error = "$actionDescription timed out"
            } catch (e: CancellationException) {
                error = "$actionDescription cancelled"
            } catch (e: IOException) {
                error = "$actionDescription I/O error - ${e.localizedMessage ?: e.message ?: "No message"}"
            } catch (e: Exception) {
                error = e.localizedMessage ?: e.message
                error = if (error.isNullOrBlank()) "$actionDescription error - ${e::class.simpleName}"
                else "$actionDescription error - ${e::class.simpleName}: $error"
            }
            if (!error.isNullOrBlank()) {
                Notifications.Bus.notify(Notification(NOTIFICATION_GROUP, error, NotificationType.ERROR), project)
            }
        }
        } finally {
            fileSystemWidget.isVisible = true
        }
    }

    private fun fileSystemWidget(project: Project?): FileSystemWidget? {
        return ToolWindowManager.getInstance(project ?: return null)
            .getToolWindow("com.jetbrains.micropython.nova.MicroPythonToolWindow")
            ?.contentManager
            ?.contents
            ?.firstNotNullOfOrNull { it.component.asSafely<FileSystemWidget>() }
    }

    protected  fun fileSystemWidget(e: AnActionEvent): FileSystemWidget? = fileSystemWidget(e.project)

}

class Refresh : ReplAction("Refresh", AllIcons.Actions.Refresh) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override val actionDescription: String = "Refresh"

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) = fileSystemWidget.refresh()
}

open class UploadFile(text: String = "Upload File", icon: Icon = AllIcons.Actions.Upload) : ReplAction(text, icon) {
//todo move out of MPY toolwindow
    override val actionDescription: String = "Upload"

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) {
        val file = withContext(Dispatchers.EDT) {
            FileDocumentManager.getInstance().saveAllDocuments()
            FileEditorManager.getInstance(fileSystemWidget.project).selectedEditor
                ?.file
        } ?: return
        val relativeName = readAction<@NonNls String?> {
            ProjectRootManagerEx.getInstance(fileSystemWidget.project)
                .contentRoots
                .firstNotNullOfOrNull {
                    VfsUtil.getRelativePath(file, it)
                }
        } ?: return
        fileSystemWidget.comm.upload(relativeName, file.contentsToByteArray())
        fileSystemWidget.refresh()
    }
}
