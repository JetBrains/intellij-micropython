package com.jetbrains.micropython.filesystem

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.scratch.ScratchTreeStructureProvider
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.components.services
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.containers.JBIterable
import com.jetbrains.micropython.settings.firstMicroPythonFacet
import org.jetbrains.annotations.Nls

class MicroPythonDeviceFilesTree(val project: Project) : TreeStructureProvider, Disposable {
  override fun dispose() {
  }

  private val projectTreeApplier = object : AsyncFileListener.ChangeApplier {
    override fun afterVfsChange() {
      if (project.isDisposed()) return
      ProjectView.getInstance(project)
        .getProjectViewPaneById(ProjectViewPane.ID)
        ?.updateFromRoot(true)
    }
  }
  override fun getData(selected: Collection<AbstractTreeNode<*>>, dataId: String): Any? {
    if (PlatformCoreDataKeys.BGT_DATA_PROVIDER.`is`(dataId)) {
      return DataProvider { slowId: String -> getSlowData(slowId, selected) }
    }
    return null
  }

  private fun getSlowData(dataId: String, selected: Collection<AbstractTreeNode<*>>): Any? {
    if (LangDataKeys.PASTE_TARGET_PSI_ELEMENT.`is`(dataId)) {
      val singleSelected = selected.firstOrNull()
      if (singleSelected is MPFilesDirectoryNode) {
        val file = singleSelected.virtualFile
        val project = singleSelected.getProject()
        return project?.service<PsiManager>()?.findDirectory(file ?: return null)
      }
    }
    return null
  }

  override fun modify(
    parent: AbstractTreeNode<*>,
    children: MutableCollection<AbstractTreeNode<*>>,
    settings: ViewSettings
  ): MutableCollection<AbstractTreeNode<*>> {
    if (parent !is ProjectViewProjectNode) return children
    val project = parent.project ?: return children
    val facet = project.firstMicroPythonFacet
    val pythonPath = facet?.pythonPath ?: return children
    val devicePath = facet.getOrDetectDevicePathSynchronously() ?: return children
    var mpRoot: AbstractTreeNode<*>? = null
    try {
      val fileSystem = MicroPythonVFS.readFromBoard(project, pythonPath, devicePath)
      val fsRoot = PsiManager.getInstance(project).findDirectory(fileSystem.root)
      if (fsRoot != null) {
        mpRoot = MPDeviceRoot(project, fsRoot, settings)
        VirtualFileManager.getInstance().addAsyncFileListener(
          { events ->
            val needsUpdate = events.any { it.fileSystem is MicroPythonVFS }
            if (needsUpdate) projectTreeApplier else null
          },
          this
        )
      }
    }
    catch (e: Throwable) {
      mpRoot = ReadFailNode(project, e.message ?: "Read error") //todo beaty messages
    }
    return if (mpRoot == null) children else children.toMutableList().apply { add(1, mpRoot) }
  }

  private class ReadFailNode(project: Project,@Nls val description: String) : AbstractTreeNode<Unit>(project, Unit) {
    override fun getChildren(): List<AbstractTreeNode<*>> = emptyList()

    override fun update(presentation: PresentationData) {
      val shortText = StringUtil.shortenTextWithEllipsis(description, 50, 10)
      presentation.addText(shortText, SimpleTextAttributes.ERROR_ATTRIBUTES)
      presentation.tooltip = description
      presentation.setIcon(AllIcons.General.BalloonWarning)
    }
  }

  private open class MPFilesDirectoryNode(project: Project, value: PsiDirectory, viewSettings: ViewSettings) : PsiDirectoryNode(project,
                                                                                                                                value,
                                                                                                                                viewSettings) {
    override fun getChildrenImpl(): List<AbstractTreeNode<*>> {
      val psiManager = PsiManager.getInstance(project)
      val result =
        (value.virtualFile as MicroPythonVFS.MicroPythonDirectory)
          .files
          .mapNotNull { file ->
            if (file.isDirectory) {
              val psiDirectory = psiManager.findDirectory(file)
              if (psiDirectory != null) MPFilesDirectoryNode(project, psiDirectory, settings) else null
            }
            else {
              val psiFile = psiManager.findFile(file)
              if (psiFile != null) PsiFileNode(project, psiFile, settings) else null
            }
          }
          .toList()
      return result
    }
  }

  private class MPDeviceRoot(project: Project, value: PsiDirectory, viewSettings: ViewSettings) :
    MPFilesDirectoryNode(project, value, viewSettings) {
    override fun update(presentation: PresentationData) {
      presentation.addText("Here be my device files", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
      presentation.addText("(disconnected)", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
    }
  }
}