package com.jetbrains.micropython.filemanager

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.net.URI
import java.nio.file.*
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.toPath

interface FileLocator {
    val isDirectory: Boolean
    val name: String
    val parent: FileLocator?
    val fileUri:URI?
    fun toCorrectUri(): URI
    val fileSystem: FileSystem?
    fun children(): List<FileLocator>
    fun toVirtualFile(): VirtualFile?
    fun toPresentableForm(): String
    fun <T> runUnderFileSystem(action: (FileLocator) -> T): T
}

class RealFileLocator(val path: Path) : FileLocator {
    override val isDirectory: Boolean
        get() = path.isDirectory()
    override val parent: FileLocator?
        get() = path.parent?.let { RealFileLocator(it) }

    override val name: String = path.name
    override fun toCorrectUri(): URI =
        path.toCorrectUri()

    override val fileSystem: FileSystem?
        get() = path.fileSystem

    override val fileUri: URI? = path.toUri()

    override fun children(): List<FileLocator> =
        runUnderFileSystem {
            val result = ArrayList<FileLocator>()
            val uri = path.toUri()
            val localFs = uri.scheme.equals("file", ignoreCase = true)
            val parentUri = path.parent?.toCorrectUri() ?: (if (!localFs) {
                Paths.get(URI(path.toUri().rawSchemeSpecificPart.substringBefore("!/"))).parent.toCorrectUri()
            } else null)
            if (parentUri != null) {
                result.add(RealFileLocator(parentUri.toPath()))
            }
            if (path.isDirectory()) {
                Files.newDirectoryStream(path).use { it.map { RealFileLocator(it) }.toCollection(result) }
            }
            result

        }

    override fun toVirtualFile(): VirtualFile? {
        return VfsUtil.findFileByIoFile(path.toFile(), true)
    }

    override fun toPresentableForm(): String {
        return path.pathString
    }

    override fun <T> runUnderFileSystem(action: (FileLocator) -> T): T {
        val uri = path.toUri()
        if (uri.scheme == "file") {
            FileSystems.newFileSystem(uri, emptyMap<String, Any>()).use {
                return action(this)
            }
        } else {
            return action(this)
        }
    }
}
