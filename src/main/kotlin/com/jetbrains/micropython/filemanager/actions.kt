package com.jetbrains.micropython.filemanager

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.IdeFocusManager
import com.jetbrains.intellij.fileManager.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URI
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

abstract class BaseFileAction(text: String) : DumbAwareAction(text) {
    override fun update(e: AnActionEvent) {
        val files = e.getData(FILES_KEY)
        if (files == null) {
            e.presentation.isEnabled = false
            return
        }
        e.presentation.isEnabled = files.isNotEmpty() && isEnabled(e.getData(TARGET_DIR_KEY)?.fileUri)
    }

    open fun isEnabled(target: URI?): Boolean = true

    override fun actionPerformed(e: AnActionEvent) {
        val selected = e.getData(FILES_KEY) ?: return
        val selectedUris = selected.mapNotNull{ it.fileUri}
        perform(selectedUris, e.getData(TARGET_DIR_KEY)?.fileUri, e.getData(FILE_LIST_COMPONENT))
    }

    abstract fun perform(selected: List<URI>, targetUri: URI?, component: FileListComponent?)
}

class CopyFilesAction : BaseFileProcessingAction("Copy", "copy", { from, to -> copyRecursively(from, to) })

private fun copyRecursively(from: Path, to: Path) {
    Files.walkFileTree(from, object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.copy(file, to.resolve(from.relativize(file)))
            return FileVisitResult.CONTINUE
        }

        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.createDirectory(to.resolve(from.relativize(dir)))
            return FileVisitResult.CONTINUE
        }
    })
}

private fun moveRecursively(from: Path, to: Path) {
    Files.walkFileTree(from, object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.move(file, to.resolve(from.relativize(file)))
            return FileVisitResult.CONTINUE
        }

        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.createDirectory(to.resolve(from.relativize(dir)))
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            Files.deleteIfExists(dir)
            return FileVisitResult.CONTINUE
        }
    })
}

private fun deleteRecursively(file: Path) {
    Files.walkFileTree(file, object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.deleteIfExists(file)
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            Files.deleteIfExists(dir)
            return FileVisitResult.CONTINUE
        }
    })
}

class MoveFilesAction : BaseFileProcessingAction("Move", "move", { from, to -> moveRecursively(from, to) })

open class BaseFileProcessingAction(
    text: String,
    private val actionDescription: String,
    private val action: (Path, Path) -> Unit
) : BaseFileAction(text) {
    override fun isEnabled(target: URI?) =
        target != null && target.scheme in listOf("file", MicroPythonBoardFiles.SCHEME)

    override fun perform(selected: List<URI>, targetUri: URI?, component: FileListComponent?) {
        if (targetUri == null) return
        GlobalScope.launch(Swing) {
            try {
                runInBackground {
                    val target = Paths.get(targetUri)
                    for (uri in selected) {
                        val source = Paths.get(uri)
                        action(source, target.resolve(source.fileName.toString()))
                    }
                }
            } catch (e: Exception) {
                LOG.info(e)
                Notifications.Bus.notify(
                    Notification(
                        "File Manager Errors",
                        "Failed to $actionDescription",
                        "Failed to $actionDescription: $e",
                        NotificationType.ERROR
                    )
                )
            }
            component?.refresh()
            component?.oppositeSide?.refresh()
        }
    }
}

class DeleteFilesAction : BaseFileAction("Delete...") {
    override fun perform(selected: List<URI>, targetUri: URI?, component: FileListComponent?) {
        GlobalScope.launch(Swing) {
            try {
                val filesText =
                    if (selected.size == 1) Paths.get(selected.first()).fileName.toString() else "${selected.size} files"
                if (Messages.showYesNoDialog(
                        component?.project,
                        "Do you want to delete $filesText? You won't be able to undo this operation.",
                        "Delete",
                        "Delete",
                        "Cancel",
                        null
                    ) == Messages.YES
                ) {
                    runInBackground {
                        for (uri in selected) {
                            deleteRecursively(Paths.get(uri))
                        }
                    }
                }
            } catch (e: Exception) {
                LOG.info(e)
                Notifications.Bus.notify(
                    Notification(
                        "File Manager Errors",
                        "Failed to delete",
                        "Failed to delete: $e",
                        NotificationType.ERROR
                    )
                )
            }
            if (component != null) {
                component.refresh()
                component.oppositeSide?.refresh()
                IdeFocusManager.getInstance(component.project).requestFocus(component.preferredFocusableComponent, true)
            }
        }
    }
}
