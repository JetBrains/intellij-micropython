package com.jetbrains.micropython.nova

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ActionPlaces.TOOLWINDOW_CONTENT
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.PopupHandler
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.asSafely
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NonNls
import java.nio.charset.StandardCharsets
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

val FILE_SYSTEM_WIDGET_KEY = Key<FileSystemWidget>(FileSystemWidget::class.java.name)

private const val MPY_FS_SCAN = """
import os
class ___FSScan(object):

    def fld(self, name):
        for r in os.ilistdir(name):
            print(r[1],r[3] if len(r) > 3 else -1,name + r[0])
            if r[1] & 0x4000:
                self.fld(name + r[0]+ "/")

___FSScan().fld("/")
del ___FSScan
gc.collect()
  
"""

private fun fileReadCommand(name: String) = """
with open('$name','rb') as f:
    while 1:
          b=f.read(50)
          if not b:break
          print(b.hex())
"""

class FileSystemWidget(val project: Project, val comm: WebSocketComm) : BorderLayoutPanel() {
    private val tree: Tree = Tree(newTreeModel())

    private fun newTreeModel() = DefaultTreeModel(DirNode("/", "/"), true)

    init {
        tree.setCellRenderer(object : ColoredTreeCellRenderer() {

            override fun customizeCellRenderer(
                tree: JTree, value: Any?, selected: Boolean, expanded: Boolean,
                leaf: Boolean, row: Int, hasFocus: Boolean,
            ) {
                value as FileSystemNode
                icon = when (value) {
                    is DirNode -> AllIcons.Nodes.Folder
                    is FileNode -> FileTypeRegistry.getInstance().getFileTypeByFileName(value.name).icon
                }
                append(value.name)
                if (value is FileNode) {
                    append("  ${value.size} bytes", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
                }

            }
        })
        val actionManager = ActionManager.getInstance()
        EditSourceOnDoubleClickHandler.install(tree) {
            val action = actionManager.getAction("micropython.repl.OpenFile")
            actionManager.tryToExecute(action, null, tree, TOOLWINDOW_CONTENT, true)
        }
        val popupActions = actionManager.getAction("micropython.repl.FSContextMenu") as ActionGroup
        PopupHandler.installFollowingSelectionTreePopup(tree, popupActions, ActionPlaces.UNKNOWN)
        TreeUtil.installActions(tree)
        addToCenter(tree)

        val actions = actionManager.getAction("micropython.repl.FSToolbar") as ActionGroup
        val actionToolbar = actionManager.createActionToolbar(ActionPlaces.TOOLBAR, actions, true)
        actionToolbar.targetComponent = tree

        addToTop(actionToolbar.component)
    }

    suspend fun refresh() {
        if (!comm.isConnected()) {
            comm.reconnect()
        }
        val newModel = newTreeModel()
        val dirList = comm.blindExecute(MPY_FS_SCAN).extractSingleResponse()
        dirList.lines().filter { it.isNotBlank() }.forEach { line ->
                line.split(" ").let { fields ->
                    val flags = fields[0].toInt()
                    val len = fields[1].toInt()
                    val fullName = fields[2]
                    val names = fullName.split('/')
                    val folders = if (flags and 0x4000 == 0) names.dropLast(1) else names
                    var currentFolder = newModel.root as DirNode
                    folders.filter { it.isNotBlank() }.forEach { name ->
                            val child =
                                currentFolder.children().asSequence().find { (it as FileSystemNode).name == name }
                            when (child) {
                                is DirNode -> currentFolder = child
                                is FileNode -> Unit
                                null -> currentFolder = DirNode(fullName, name).also { currentFolder.add(it) }
                            }
                        }
                    if (flags and 0x4000 == 0) {
                        currentFolder.add(FileNode(fullName, names.last(), len))
                    }
                }
            }
        withContext(Dispatchers.EDT) {
            val expandedPaths = TreeUtil.collectExpandedPaths(tree)
            val selectedPath = tree.selectionPath
            tree.model = newModel
            TreeUtil.restoreExpandedPaths(tree, expandedPaths)
            TreeUtil.selectPath(tree, selectedPath)
        }
    }

    suspend fun deleteCurrent() {
        val confirmedFileSystemNode = withContext(Dispatchers.EDT) {
            val fileSystemNode =
                tree.selectionPath
                    ?.lastPathComponent
                    .asSafely<FileSystemNode>() ?: return@withContext null
            val childCount = fileSystemNode.asSafely<DirNode>()?.childCount ?: 0
            val fileName = fileSystemNode.fullName
            if (childCount > 0) {
                Messages.showErrorDialog(project, "Can not delete", "Folder $fileName is not empty")
                return@withContext null
            }
            val sure = MessageDialogBuilder.yesNo(
                        fileName,
                        "Are you sure to delete $fileName?\n\r The operation is not reversible!"
                    ).icon(AllIcons.General.Warning).ask(project)
            if (sure) fileSystemNode else null
        }
        if (confirmedFileSystemNode != null) {
            val cmd = if(confirmedFileSystemNode is DirNode) "rmdir" else "remove"
            comm.blindExecute("import os\nos.$cmd('${confirmedFileSystemNode.fullName}')")
                .extractSingleResponse()
            refresh()
        }
    }

    fun selectedFile(): FileSystemNode? {
        return tree.selectionPath?.lastPathComponent.asSafely<FileSystemNode>()
    }

}

sealed class FileSystemNode(@NonNls val fullName: String, @NonNls val name: String) : DefaultMutableTreeNode() {

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return fullName == (other as FileSystemNode).fullName
    }

    override fun hashCode(): Int {
        return fullName.hashCode()
    }

}

class FileNode(fullName: String, name: String, val size: Int) : FileSystemNode(fullName, name) {
    override fun getAllowsChildren(): Boolean = false
    override fun isLeaf(): Boolean = true
}

class DirNode(fullName: String, name: String) : FileSystemNode(fullName, name) {
    override fun getAllowsChildren(): Boolean = true
}

class DeleteFile : ReplAction("Delete File", AllIcons.General.Remove) {

    override val actionDescription: String = "Delete"

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) {
        fileSystemWidget.deleteCurrent()
    }

    override fun update(e: AnActionEvent) {
        val selectedFile = fileSystemWidget(e)?.selectedFile()
        e.presentation.isEnabled = selectedFile?.fullName !in listOf("/", null)
        e.presentation.text = if (selectedFile is DirNode) "Delete folder" else "Delete file"
    }
}

class InstantRun : ReplAction("Instant Run", AllIcons.Actions.Rerun) {

    override val actionDescription: String = "Run code"

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = isFileEditorActive(e)
    }

    override suspend fun performAction(fileSystemWidget: FileSystemWidget) {
        val code = withContext(Dispatchers.EDT) {
            FileEditorManager.getInstance(fileSystemWidget.project).selectedEditor.asSafely<TextEditor>()?.editor?.document?.text
        }
        if (code != null) {
            fileSystemWidget.comm.instantRun(code)
        }
    }
}

class OpenMpyFile : ReplAction("Open file", AllIcons.Actions.MenuOpen) {

    override val actionDescription: String = "Open file"

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = fileSystemWidget(e)?.selectedFile() is FileNode
    }
    override suspend fun performAction(fileSystemWidget: FileSystemWidget) {
        val selectedFile = withContext(Dispatchers.EDT) {
            fileSystemWidget.selectedFile()
        }
        if (selectedFile !is FileNode) return
        val result = fileSystemWidget.comm.blindExecute(fileReadCommand(selectedFile.fullName)).extractSingleResponse()
        val text =
            result.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.chunked(2).map { it.toInt(16).toByte() }
                .toByteArray().toString(StandardCharsets.UTF_8)
        withContext(Dispatchers.EDT) {
            val fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(selectedFile.name)
            val infoFile = LightVirtualFile("micropython: ${selectedFile.fullName}", fileType, text)
            infoFile.isWritable = false
            FileEditorManager.getInstance(fileSystemWidget.project).openFile(infoFile, false)
        }
    }
}
