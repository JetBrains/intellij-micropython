package com.jetbrains.micropython.nova

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.util.Key
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.asSafely
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import org.jetbrains.annotations.NonNls
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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

private fun fileReadCommand(name:String) = """
with open('$name','rb') as f:
    while 1:
          b=f.read(50)
          if not b:break
          print(b.hex())
"""

class FileSystemWidget(val project: Project, val connector: WebSocketTtyConnector) : BorderLayoutPanel() {
  private val tree: Tree  = Tree(DefaultTreeModel(DirNode("/", "/"), true))
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
    tree.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if(e.button == MouseEvent.BUTTON1 && e.clickCount==2) {
          tree.getClosestPathForLocation(e.x, e.y)?.lastPathComponent.asSafely<FileNode>()
          ?.let { fileNode ->
            val hex = connector.blindExecute(fileReadCommand(fileNode.fullName))
            val text = hex
              .filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
              .chunked(2)
              .map { it.toInt(16).toByte() }
              .toByteArray()
              .toString(StandardCharsets.UTF_8)
            val fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(fileNode.name)
            val infoFile = LightVirtualFile("micropython: ${fileNode.fullName}", fileType, text)
            FileEditorManager.getInstance(project).openFile(infoFile, false)
          }
      }
    }
  })
    val actions = ActionManager.getInstance().getAction("micropython.repl.FSToolbar") as ActionGroup
    //PopupHandler.installFollowingSelectionTreePopup(this, action as ActionGroup, ActionPlaces.UNKNOWN)
    addToCenter(tree)
    val actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actions, true)
    actionToolbar.targetComponent = tree

    addToTop(actionToolbar.component)
  }

  fun refresh() {
    (tree.model.root as DirNode).removeAllChildren()
    val dirList: String = connector.blindExecute(MPY_FS_SCAN)
    dirList
      .lines()
      .filter { it.isNotBlank() }
      .forEach { line ->
        line.split(" ").let { fields ->
          val flags = fields[0].toInt()
          val len = fields[1].toInt()
          val fullName = fields[2]
          val names = fullName.split('/')
          val folders = if (flags and 0x4000 == 0) names.dropLast(1) else names
          var currentFolder = tree.model.root as DirNode
          folders
            .filter { it.isNotBlank() }
            .forEach { name ->
              val child = currentFolder.children().asSequence().find { (it as FileSystemNode).name == name }
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
    val expandedPaths = TreeUtil.collectExpandedPaths(tree)
    val selectedPath = tree.selectionPath
    (tree.model as DefaultTreeModel).reload()
    TreeUtil.restoreExpandedPaths(tree, expandedPaths)
    TreeUtil.selectPath(tree, selectedPath)
  }

  fun deleteCurrent() {
    val fileName = tree.selectionPath?.lastPathComponent.asSafely<FileSystemNode>()?.fullName ?: return
    val sure = MessageDialogBuilder
      .yesNo(fileName, "Are you sure to delete $fileName?\n\r The operation is not reversible!")
      .icon(AllIcons.General.Warning).ask(project)
    if (sure) {
      connector.blindExecute("import os\nos.remove('${fileName}')")
      refresh()
    }
  }

}

internal sealed class FileSystemNode(@NonNls val fullName: String, @NonNls val name: String) : DefaultMutableTreeNode() {

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

internal class FileNode(fullName: String, name: String, val size: Int) : FileSystemNode(fullName, name) {
  override fun getAllowsChildren(): Boolean = false
  override fun isLeaf(): Boolean = true
}

internal class DirNode(fullName: String, name: String) : FileSystemNode(fullName, name) {
  override fun getAllowsChildren(): Boolean = true
}

