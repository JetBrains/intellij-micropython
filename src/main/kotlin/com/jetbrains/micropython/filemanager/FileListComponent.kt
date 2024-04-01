package com.jetbrains.micropython.filemanager

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.SystemProperties
import com.intellij.util.asSafely
import com.intellij.util.ui.UIUtil
import com.jetbrains.intellij.fileManager.Swing
import com.jetbrains.intellij.fileManager.runInBackground
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.event.*
import java.net.URI
import java.nio.file.Paths
import java.util.function.Function
import javax.swing.*
import kotlin.io.path.toPath

private const val GO_DOWN_ACTION = "GoDown"
private const val GO_OPPOSITE_ACTION = "GoToOppositeSide"
private const val GO_UP_ACTION = "GoUp"
val FILES_KEY = DataKey.create<List<FileLocator>>("FILE_MANAGER_FILES")
val TARGET_DIR_KEY = DataKey.create<FileLocator?>("FILE_MANAGER_TARGET_DIR")
val FILE_LIST_COMPONENT = DataKey.create<FileListComponent?>("FILE_MANAGER_LIST_COMPONENT")

internal val LOG = Logger.getInstance("#com.jetbrains.intellij.fileManager")


abstract class FileListComponent(internal val project: Project) {
    val panel: JComponent
    private val listModel: SortedListModel<FileItem>
    private val list: JBList<FileItem>
    private var oldSelection: Set<FileItem> = emptySet()
    private var current: FileLocator? = null
    var active = false
    private val locationLabel = JBLabel()
    var oppositeSide: FileListComponent? = null

    init {
        val comparator = compareByDescending<FileItem> { it.isParentDirectory }.thenByDescending(
            FileItem::isDirectory
        ).thenComparing(Function { it.name }, String.CASE_INSENSITIVE_ORDER)
        listModel = SortedListModel(comparator)
        list = JBList(listModel)
        list.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        list.cellRenderer = object : SimpleListCellRenderer<FileItem>() {
            override fun customize(
                list: JList<out FileItem>,
                value: FileItem?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                if (value == null) return
                text = value.name
                icon = when {
                    value.isParentDirectory -> AllIcons.Nodes.UpLevel
                    value.isDirectory -> AllIcons.Nodes.Folder
                    else -> FileTypeManager.getInstance().getFileTypeByFileName(value.name).icon
                }
            }
        }
        panel = JPanel(BorderLayout())
        panel.add(BorderLayout.NORTH, locationLabel)
        panel.add(BorderLayout.CENTER, ScrollPaneFactory.createScrollPane(list))
        TreeUIHelper.getInstance().installListSpeedSearch(list) { it.name }
        ScrollingUtil.installActions(list)

        val ctrlMask = if (SystemInfo.isMac) InputEvent.META_MASK else InputEvent.CTRL_MASK
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), GO_DOWN_ACTION)
        list.getInputMap(JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, ctrlMask), GO_DOWN_ACTION)
        list.actionMap.put(GO_DOWN_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                goDown()
            }
        })
        object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent): Boolean {
                goDown()
                return true
            }
        }.installOn(list)

        list.getInputMap(JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, ctrlMask), GO_UP_ACTION)
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), GO_UP_ACTION)
        list.actionMap.put(GO_UP_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                goUp()
            }
        })
        list.addFocusListener(object : FocusListener {
            override fun focusLost(e: FocusEvent?) {
                deactivate()
            }

            override fun focusGained(e: FocusEvent?) {
                activate()
            }
        })

        DataManager.registerDataProvider(list) { key ->
            when (key) {
                CommonDataKeys.VIRTUAL_FILE_ARRAY.name -> getSelectedVirtualFiles()
                FILES_KEY.name -> list.selectedValuesList.map { it.fileLocator }
                TARGET_DIR_KEY.name -> oppositeSide?.current
                FILE_LIST_COMPONENT.name -> this
                else -> null
            }
        }
        val group = ActionManager.getInstance().getAction("FileManagerPopupGroup") as ActionGroup
        PopupHandler.installPopupMenu(list, group, ActionPlaces.UNKNOWN)
    }

    private fun getSelectedVirtualFiles() =
        list.selectedValuesList.mapNotNull { it.fileLocator.toVirtualFile() }.toTypedArray()

    val preferredFocusableComponent: JComponent
        get() = list

    private fun goUp() {
        val first = listModel.items.firstOrNull()
        if (first?.isParentDirectory == true) {
            navigate(first.fileLocator)
        }
    }

    private fun goDown() {
        val selected = list.selectedValuesList.firstOrNull() ?: return
        navigate(selected)
    }

    private fun navigate(item: FileItem) {
        if (item.isDirectory) {
            navigate(item.fileLocator, if (item.isParentDirectory) item.fileLocator else null)
        } else if (FileTypeManager.getInstance().getFileTypeByFileName(item.name) == ArchiveFileType.INSTANCE) {
            val fileUri = item.fileLocator.fileUri
            if (fileUri?.scheme == "file") {
                val archiveUri = item.fileLocator.runUnderFileSystem {
                  locator ->
                    locator.fileSystem?.use {
                        it.rootDirectories.firstOrNull()?.toCorrectUri()
                    }
                }
                if (archiveUri != null) {
                    navigate(RealFileLocator(archiveUri.toPath()))
                }
            }
        } else {
            try {
                val file = item.fileLocator.toVirtualFile()
                if (file != null) {
                    FileEditorManager.getInstance(project).openFile(file, true)
                }
            } catch (e: Exception) {
                LOG.info(e)
            }
        }
    }

    fun deactivate() {
        if (!active) return

        if (list.selectedValuesList.isNotEmpty()) {
            oldSelection = list.selectedValuesList.toSet()
        }
        list.selectionModel.clearSelection()
        active = false
    }

    fun activate() {
        if (active) return

        if (oldSelection.isEmpty()) {
            selectFirstItem()
        } else {
            selectItems { it in oldSelection }
        }
        active = true
    }

    private fun selectItems(filter: (FileItem) -> Boolean) {
        list.selectionModel.clearSelection()
        listModel.items.withIndex().filter { filter(it.value) }.forEach {
            list.selectionModel.addSelectionInterval(it.index, it.index)
        }
        UIUtil.scrollListToVisibleIfNeeded(list)
    }

    private fun selectFirstItem() {
        list.selectionModel.clearSelection()
        if (listModel.size > 0) {
            list.selectionModel.setSelectionInterval(0, 0)
        }
        list.scrollRectToVisible(Rectangle(0, 0, 1, 1))
    }

    abstract fun navigateDefault(): Job

    protected fun navigate(locator: FileLocator, toSelect: FileLocator? = null): Job {
        return GlobalScope.launch(Swing) {
            val children = runInBackground { locator.children().map { FileItem(it, false) } }
            current = locator
            locationLabel.text = locator.toPresentableForm()
            saveNavigated(locator)
            listModel.setAll(children)
            if (toSelect != null) {
                selectItems { it.fileLocator == toSelect }
            }
            if (list.selectionModel.isSelectionEmpty) {
                selectFirstItem()
            }
            active = true
        }
    }

    abstract fun saveNavigated(fileLocator: FileLocator)

    fun attachToOppositeSide(side: FileListComponent) {
        list.focusTraversalKeysEnabled = false
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), GO_OPPOSITE_ACTION)
        list.actionMap.put(GO_OPPOSITE_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                deactivate()
                side.activate()
                IdeFocusManager.getInstance(project).requestFocus(side.preferredFocusableComponent, true)
            }
        })
        oppositeSide = side
    }

    fun refresh() {
        if (current == null) return
        GlobalScope.launch(Swing) {
            val toSelect = list.selectedValuesList.map { it.fileLocator }.toSet()
            val children = runInBackground { current!!.children().map { FileItem(it, false) } }
            listModel.setAll(children)
            if (active) {
                selectItems { it.fileLocator in toSelect }
                if (list.selectionModel.isSelectionEmpty) {
                    selectFirstItem()
                }
            }
        }

    }
}


class RealFileListComponent(
    project: Project,
    val storageKey: String = "file.manager.left.side.uri"
) : FileListComponent(project) {
    override fun navigateDefault(): Job {
        val defaultUri = Paths.get(SystemProperties.getUserHome()).toCorrectUri().toString();
        val uri = URI(PropertiesComponent.getInstance(project).getValue(storageKey, defaultUri))
        return navigate(RealFileLocator(uri.toPath()))
    }

    override fun saveNavigated(fileLocator: FileLocator) {
        val uri = fileLocator.asSafely<RealFileLocator>()?.path?.toUri()
        if (uri != null) {
            PropertiesComponent.getInstance(project).setValue(storageKey, uri.toString())
        }
    }

}

class MicroPythonFileListComponent(project: Project) : FileListComponent(project) {
    val fileSystem: Lazy<MicroPythonBoardFiles> = lazy {
        MicroPythonBoardFiles("com8").apply { load() }
    }

    override fun navigateDefault(): Job {
        return navigate(fileSystem.value.root)
    }

    override fun saveNavigated(fileLocator: FileLocator) {
    }
}