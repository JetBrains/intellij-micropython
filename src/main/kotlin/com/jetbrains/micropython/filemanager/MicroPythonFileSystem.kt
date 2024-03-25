package com.jetbrains.micropython.filemanager

import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem
import java.io.InputStream
import java.io.OutputStream

class MicroPythonFileSystem: NewVirtualFileSystem() {
    private val listeners:MutableList<VirtualFileListener> = mutableListOf()

    override fun getProtocol(): String = "mpfs"

    override fun findFileByPath(p0: String): VirtualFile? {
        TODO("Not yet implemented")
    }

    override fun refresh(p0: Boolean) {
        TODO("Not yet implemented")
    }

    override fun refreshAndFindFileByPath(p0: String): VirtualFile? {
        TODO("Not yet implemented")
    }

    override fun deleteFile(p0: Any?, p1: VirtualFile) {
        TODO("Not yet implemented")
    }

    override fun moveFile(p0: Any?, p1: VirtualFile, p2: VirtualFile) {
        TODO("Not yet implemented")
    }

    override fun renameFile(p0: Any?, p1: VirtualFile, p2: String) {
        TODO("Not yet implemented")
    }

    override fun createChildFile(p0: Any?, p1: VirtualFile, p2: String): VirtualFile {
        TODO("Not yet implemented")
    }

    override fun createChildDirectory(p0: Any?, p1: VirtualFile, p2: String): VirtualFile {
        TODO("Not yet implemented")
    }

    override fun copyFile(p0: Any?, p1: VirtualFile, p2: VirtualFile, p3: String): VirtualFile {
        TODO("Not yet implemented")
    }

    override fun exists(p0: VirtualFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun list(p0: VirtualFile): Array<String> {
        TODO("Not yet implemented")
    }

    override fun isDirectory(p0: VirtualFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun getTimeStamp(p0: VirtualFile): Long {
        TODO("Not yet implemented")
    }

    override fun setTimeStamp(p0: VirtualFile, p1: Long) {
        TODO("Not yet implemented")
    }

    override fun isWritable(p0: VirtualFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun setWritable(p0: VirtualFile, p1: Boolean) {
        TODO("Not yet implemented")
    }

    override fun contentsToByteArray(p0: VirtualFile): ByteArray {
        TODO("Not yet implemented")
    }

    override fun getInputStream(p0: VirtualFile): InputStream {
        TODO("Not yet implemented")
    }

    override fun getOutputStream(p0: VirtualFile, p1: Any?, p2: Long, p3: Long): OutputStream {
        TODO("Not yet implemented")
    }

    override fun getLength(p0: VirtualFile): Long {
        TODO("Not yet implemented")
    }

    override fun extractRootPath(p0: String): String {
        TODO("Not yet implemented")
    }

    override fun findFileByPathIfCached(p0: String): VirtualFile? {
        TODO("Not yet implemented")
    }

    override fun getRank(): Int {
        TODO("Not yet implemented")
    }

    override fun getAttributes(p0: VirtualFile): FileAttributes? {
        TODO("Not yet implemented")
    }
}