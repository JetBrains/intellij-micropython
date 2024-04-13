package com.jetbrains.micropython.filemanager

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBSplitter
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.update.UiNotifyConnector
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class FileManagerComponent(project: Project) {
    val mainComponent: JComponent
    private val leftSide = FileListComponent(project, "file.manager.left.side.uri")
    private val rightSide = FileListComponent(project, "file.manager.right.side.uri")
    private val splitter = JBSplitter(0.5f)

    init {
        mainComponent = JPanel(BorderLayout())
        splitter.firstComponent = leftSide.panel
        splitter.secondComponent = rightSide.panel
        splitter.setAndLoadSplitterProportionKey("file-manager.splitter")
        leftSide.attachToOppositeSide(rightSide)
        rightSide.attachToOppositeSide(leftSide)
        mainComponent.add(BorderLayout.CENTER, splitter)
        UiNotifyConnector.doWhenFirstShown(mainComponent) {
            leftSide.navigateDefault().invokeOnCompletion { leftSide.activate() }
            rightSide.navigateDefault().invokeOnCompletion { rightSide.deactivate() }
        }
    }


    val activeSide: FileListComponent
      get() = if (rightSide.active) rightSide else leftSide
}

class FileManagerToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun init(toolWindow: ToolWindow) {
        toolWindow.stripeTitle = "File Manager"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val fileManagerComponent = FileManagerComponent(project)
        val content = ContentFactory.getInstance().createContent(fileManagerComponent.mainComponent, "", false)
        content.setPreferredFocusedComponent { fileManagerComponent.activeSide.preferredFocusableComponent }
        toolWindow.contentManager.addContent(content)
    }
}