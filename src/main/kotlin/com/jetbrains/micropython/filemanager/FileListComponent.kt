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
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.SystemProperties
import com.intellij.util.application
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.event.*
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Paths
import javax.swing.*

private const val GO_DOWN_ACTION = "GoDown"
private const val GO_OPPOSITE_ACTION = "GoToOppositeSide"
private const val GO_UP_ACTION = "GoUp"
val FILES_KEY = DataKey.create<List<URI>>("FILE_MANAGER_FILES")
val TARGET_DIR_KEY = DataKey.create<URI?>("FILE_MANAGER_TARGET_DIR")
val FILE_LIST_COMPONENT = DataKey.create<FileListComponent?>("FILE_MANAGER_LIST_COMPONENT")

internal val LOG = Logger.getInstance("#com.jetbrains.intellij.fileManager")

class FileListComponent(internal val project: Project, private val storageKey: String) {
    val panel: JComponent
    private val listModel: SortedListModel<FileItem>
    private val list: JBList<FileItem>
    private var oldSelection: Set<FileItem> = emptySet()
    private var current: URI? = null
    var active = false
    private val locationLabel = JBLabel()
    var oppositeSide: FileListComponent? = null

    init {
        val comparator = compareByDescending<FileItem> { it is FileItem.ParentDirectory }
            .thenByDescending(FileItem::isDirectory)
            .thenComparing({ it.name }, String.CASE_INSENSITIVE_ORDER)
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
                    value is FileItem.ParentDirectory -> AllIcons.Nodes.UpLevel
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
                FILES_KEY.name -> list.selectedValuesList.map { it.uri }
                TARGET_DIR_KEY.name -> oppositeSide?.current
                FILE_LIST_COMPONENT.name -> this
                else -> null
            }
        }
        val group = ActionManager.getInstance().getAction("FileManagerPopupGroup") as ActionGroup
        PopupHandler.installPopupMenu(list, group, ActionPlaces.UNKNOWN)
    }

    private fun getSelectedVirtualFiles() = list.selectedValuesList.mapNotNull {
        VirtualFileManager.getInstance().refreshAndFindFileByUrl(it.uri.toString()) //todo background
    }.toTypedArray()

    val preferredFocusableComponent: JComponent
        get() = list

    private fun goUp() {
        val up = listModel.items.firstOrNull() as? FileItem.ParentDirectory ?: return
        navigate(up)
    }

    private fun goDown() {
        val selected = list.selectedValuesList.firstOrNull() ?: return
        navigate(selected)
    }

    private fun navigate(item: FileItem) {
        if (item.isDirectory) {
            navigate(item.uri, (item as? FileItem.ParentDirectory)?.thisUri)
        } else if (FileTypeManager.getInstance().getFileTypeByFileName(item.name) == ArchiveFileType.INSTANCE) {
            if (item.uri.fromLocalFs()) {
                val archiveUri = usePath(item.uri) { path ->
                    FileSystems.newFileSystem(path).use {
                        it.rootDirectories.firstOrNull()?.toUri()
                    }
                }
                if (archiveUri != null) {
                    navigate(archiveUri)
                }
            }
        } else {
            application.executeOnPooledThread(
                {
                    try {
                        val file = VirtualFileManager.getInstance().refreshAndFindFileByUrl(item.uri.toString())


                        if (file != null) {
                            application.invokeLater(
                                { FileEditorManager.getInstance(project).openFile(file, true) },
                                { project.isDisposed })

                        }
                    } catch (e: Exception) {
                        LOG.info(e)
                    }
                }
            )
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

    fun navigateDefault(): Job {
        val uri = URI(PropertiesComponent.getInstance(project).getValue(storageKey, getDefaultURI()))
        return navigate(uri)
    }

    private fun getDefaultURI() = Paths.get(SystemProperties.getUserHome()).toUri().toString()

    private fun navigate(uri: URI, toSelect: URI? = null): Job {
        return GlobalScope.launch(Swing) {
            val children = runInBackground { computeChildren(uri) }
            current = uri
            locationLabel.text = uri.toPresentableForm()
            PropertiesComponent.getInstance(project).setValue(storageKey, uri.toString())
            listModel.setAll(children)
            if (toSelect != null) {
                selectItems { it.uri == toSelect }
            }
            if (list.selectionModel.isSelectionEmpty) {
                selectFirstItem()
            }
            active = true
        }
    }

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
            val toSelect = list.selectedValuesList.map { it.uri }.toSet()
            val children = runInBackground { computeChildren(current!!) }
            listModel.setAll(children)
            if (active) {
                selectItems { it.uri in toSelect }
                if (list.selectionModel.isSelectionEmpty) {
                    selectFirstItem()
                }
            }
        }

    }
}