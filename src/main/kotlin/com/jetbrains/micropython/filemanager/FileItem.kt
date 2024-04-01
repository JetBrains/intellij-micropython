package com.jetbrains.micropython.filemanager

import com.intellij.util.asSafely
import com.intellij.util.io.URLUtil
import java.net.URI
import java.nio.file.Path

class FileItem(val fileLocator: FileLocator, val isParentDirectory: Boolean) {
    val isDirectory: Boolean
        get() = fileLocator.isDirectory
    val name: String
        get() = fileLocator.name

    override fun equals(other: Any?): Boolean {
        return fileLocator == other.asSafely<FileItem>()?.fileLocator
    }

    override fun hashCode() = fileLocator.hashCode()
}

fun Path.toCorrectUri(): URI {
    val uri = toUri()
    if (uri.scheme != "jar") return uri
    val fullString = uri.toString()
    return URI(
        URLUtil.unescapePercentSequences(fullString.substringBefore("!/")) + "!/" + fullString.substringAfter(
            "!/",
            ""
        )
    )
}
