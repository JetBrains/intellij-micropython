package com.jetbrains.micropython.filemanager

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.asSafely
import java.io.Closeable
import java.io.IOException
import java.net.URI
import java.nio.file.FileSystem

class MicroPythonBoardFiles(val portName: String) : Closeable {
    val root = MicroPythonDirectory("", null)
    override fun close() {
        TODO("Not yet implemented")
    }

    fun load() {
        val output = runBoardCommand(
            "run",
            "C:\\Users\\elmot\\PycharmProjects\\pythonProject3\\dir-r.py" //todo normal resource access
        )
        output.lines()
            .filter { it.isNotBlank() }
            .forEach { line ->
                val fields = line.split(',')
                val pathComponents = fields[0].split('/')
                var filePtr: MicroPythonDirectory? = root
                for (s in pathComponents.dropLast(1)) {
                    val oldParent = filePtr!!
                    filePtr = oldParent.findChild(s)
                    if (filePtr == null) {
                        filePtr = MicroPythonDirectory(s, oldParent)
                        oldParent.files.add(filePtr)
                    }
                }
                if (fields[1] == "D") {
                    filePtr!!.files.add(MicroPythonDirectory(pathComponents.last(), filePtr))
                } else {
                    filePtr!!.files.add(MicroPythonFile(pathComponents.last(), filePtr))
                }
            }
    }

    fun runBoardCommand(vararg parameters: String): String {
        val cmd = GeneralCommandLine()
            .withExePath("C:\\Users\\elmot\\PycharmProjects\\pythonProject3\\.venv\\Scripts\\python.exe")
            .withParameters(
                "-m",
                "mpremote",
                "connect",
                portName
            )
            .withParameters(*parameters)
        val output = CapturingProcessHandler(cmd).runProcess(100000)
        if (output.isTimeout || output.exitCode != 0) throw IOException("Python error: ${output.exitCode},${output.stderr}")
        return output.stdout
    }

    companion object {
        val SCHEME: String = "mpfs"
    }
}

abstract class MicroPythonFSElement(override val name: String, override val parent: MicroPythonDirectory?) :
    FileLocator {
    override val fileSystem: FileSystem? = null
    abstract val path: String
    override fun toCorrectUri(): URI {
        return URI(MicroPythonBoardFiles.SCHEME, path, "")
    }

    override fun toVirtualFile(): VirtualFile? = null
    override fun toPresentableForm(): String = "DEVICE$path"
    override fun <T> runUnderFileSystem(action: (FileLocator) -> T): T = action(this) //todo
    override val fileUri: URI? = null
}

class MicroPythonDirectory(name: String, parent: MicroPythonDirectory?) : MicroPythonFSElement(name, parent) {
    val files: MutableList<MicroPythonFSElement> = mutableListOf()
    fun findChild(name: String): MicroPythonDirectory? =
        files.firstOrNull { it.name == name }.asSafely<MicroPythonDirectory>()

    override val isDirectory: Boolean = true
    override fun children(): List<FileLocator> = files.toList()
    override val path: String
        get() = if (parent == null) "" else "${parent.path}/${name}"

}

class MicroPythonFile(name: String, parent: MicroPythonDirectory?) : MicroPythonFSElement(name, parent) {
    override val isDirectory: Boolean = false
    override fun children(): List<FileLocator> = emptyList()
    override val path: String
        get() = (parent?.path ?: "") + name
}