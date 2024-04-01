package com.jetbrains.micropython.filesystem

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ui.SimpleTextAttributes

class MicroPythonDeviceFilesTree : TreeStructureProvider {
  override fun modify(parent: AbstractTreeNode<*>,
                      children: MutableCollection<AbstractTreeNode<*>>,
                      settings: ViewSettings?): MutableCollection<AbstractTreeNode<*>> {
    if(parent !is ProjectViewProjectNode) return children
    val project = parent.project ?: return children
    val mpRoot = object : AbstractTreeNode<String>(project, "My Files are here") {
      override fun getChildren(): Collection<out AbstractTreeNode<*>> {
        return emptyList()
      }

      override fun isAlwaysShowPlus(): Boolean = true

      override fun update(presentation: PresentationData) {
        presentation.addText("Here be my device files", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        presentation.addText("(disconnected)", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
      }
    }
    return mutableListOf<AbstractTreeNode<*>>(mpRoot).apply { addAll(children) }
  }
}