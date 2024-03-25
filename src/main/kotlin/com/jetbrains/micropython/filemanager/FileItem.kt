package com.jetbrains.micropython.filemanager

import com.intellij.util.io.URLUtil
import com.intellij.util.io.isDirectory
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

sealed class FileItem {
    abstract val isDirectory: Boolean
    abstract val name: String
    abstract val uri: URI

    override fun equals(other: Any?) = javaClass == other?.javaClass && uri == (other as? FileItem)?.uri

    override fun hashCode() = uri.hashCode()

    class ParentDirectory(private val parentUri: URI, val thisUri: URI) : FileItem() {
        override val isDirectory = true
        override val name = ".."
        override val uri: URI
            get() = parentUri
    }

    class Regular(override val uri: URI, override val isDirectory: Boolean, override val name: String) : FileItem()
}

fun computeChildren(uri: URI): List<FileItem> {
    return usePath(uri) { path ->
        val result = ArrayList<FileItem>()
        val localFs = uri.scheme.equals("file", ignoreCase = true)
        val parentUri = path.parent?.toCorrectUri() ?: (if (!localFs) {
            Paths.get(URI(uri.rawSchemeSpecificPart.substringBefore("!/"))).parent.toCorrectUri()
        }
        else null)
        if (parentUri != null) {
            result.add(FileItem.ParentDirectory(parentUri, uri))
        }
        if (path.isDirectory()) {
            Files.newDirectoryStream(path).use { it.map(::createFileItem).toCollection(result) }
        }
        result
    }
}

fun createFileItem(path: Path) = FileItem.Regular(path.toCorrectUri(), path.isDirectory(), path.fileName.toString())

fun URI.fromLocalFs() = "file".equals(scheme, ignoreCase = true)

fun URI.toPresentableForm(): String =
        if (fromLocalFs()) path else toString()

fun Path.toCorrectUri(): URI {
    val uri = toUri()
    if (uri.scheme != "jar") return uri
    val fullString = uri.toString()
    return URI(URLUtil.unescapePercentSequences(fullString.substringBefore("!/")) + "!/" + fullString.substringAfter("!/", ""))
}

fun <T> usePath(uri: URI, action: (Path) -> T): T {
    val fileSystem = if (!uri.fromLocalFs()) {
        FileSystems.newFileSystem(uri, emptyMap<String, Any>())
    } else {
        null
    }
    fileSystem.use {
        return action(Paths.get(uri))
    }
}