package com.jetbrains.micropython.filemanager

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem
import com.intellij.util.asSafely
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class MicroPythonVFS(val portName: String) : NewVirtualFileSystem() {

  fun main() {

  }


  val root: MicroPythonDirectory = MicroPythonDirectory("", null, 0)

  companion object {
    fun readFs(portName: String): MicroPythonVFS {
      val result = MicroPythonVFS(portName)
      val cmd = GeneralCommandLine()
        .withExePath("C:\\Users\\elmot\\PycharmProjects\\pythonProject3\\.venv\\Scripts\\python.exe")
        .withParameters(
          "-m",
          "mpremote",
          "connect",
          portName,
          "run",
          "C:\\Users\\elmot\\PycharmProjects\\pythonProject3\\dir-r.py"
        )
      val output = CapturingProcessHandler(cmd).runProcess(100000)
      if (output.isTimeout || output.exitCode != 0) throw IOException("Python error: ${output.exitCode},${output.stderr}")
      output.stdout.lines()
        .filter { it.isNotBlank() }
        .forEach { line ->
          val fields = line.split(',')
          val pathComponents = fields[0].split('/')
          var filePtr: MicroPythonDirectory? = result.root
          for (s in pathComponents.dropLast(1)) {
            val oldParent = filePtr!!
            filePtr = oldParent.findChild(s) as MicroPythonDirectory?
            if (filePtr == null) {
              filePtr = result.MicroPythonDirectory(s, oldParent, 0/*todo*/)
              oldParent.files.add(filePtr)
            }
          }
          if (fields[1] == "D") {
            filePtr!!.files.add(result.MicroPythonDirectory(pathComponents.last(), filePtr, 0/*todo*/))
          }
          else {
            filePtr!!.files.add(result.MicroPythonFile(pathComponents.last(), filePtr, 0/*todo*/, fields[2].toLong()))
          }
        }
      return result
    }
  }

  override fun exists(file: VirtualFile): Boolean = file.exists()

  override fun getProtocol(): String = "mpfs"

  override fun findFileByPath(path: String): VirtualFile? {
    TODO("Not yet implemented")
  }

  override fun refresh(asynchronous: Boolean) {
    TODO("Not yet implemented")
  }

  override fun refreshAndFindFileByPath(path: String): VirtualFile? {
    refresh(false)
    return findFileByPath(path)
  }

  override fun getAttributes(file: VirtualFile): FileAttributes? {
    return if (file.exists()) (file as MicroPythonFileBase).attributes else null
  }

  override fun deleteFile(requestor: Any?, file: VirtualFile) {
    TODO("Not yet implemented")
  }

  override fun moveFile(requestor: Any?, file: VirtualFile, newParent: VirtualFile) {
    TODO("Not yet implemented")
  }

  override fun renameFile(requestor: Any?, file: VirtualFile, newName: String) {
    TODO("Not yet implemented")
  }

  override fun createChildFile(requestor: Any?, parent: VirtualFile, file: String): VirtualFile {
    TODO("Not yet implemented")
  }

  override fun createChildDirectory(requestor: Any?, parent: VirtualFile, dir: String): VirtualFile {
    TODO("Not yet implemented")
  }

  override fun copyFile(requestor: Any?, file: VirtualFile, newParent: VirtualFile, copyName: String): VirtualFile {
    TODO("Not yet implemented")
  }

  override fun list(file: VirtualFile): Array<String> =
    file.children.filter { it.exists() }.map { it.name }.toTypedArray()


  override fun isDirectory(file: VirtualFile): Boolean = file.isDirectory

  override fun getTimeStamp(file: VirtualFile): Long = file.timeStamp

  override fun setTimeStamp(file: VirtualFile, timeStamp: Long) {
    (file as MicroPythonFileBase).myTimeStamp = timeStamp
  }

  override fun isWritable(file: VirtualFile): Boolean = true

  override fun setWritable(file: VirtualFile, writableFlag: Boolean) {
  }

  override fun contentsToByteArray(file: VirtualFile): ByteArray {
    return file.asSafely<MicroPythonFileBase>()?.contentsToByteArray()
           ?: throw IOException("Cannot read content of ${file.path}")
  }

  override fun getInputStream(file: VirtualFile): InputStream {
    return file.asSafely<MicroPythonFileBase>()?.inputStream
           ?: throw IOException("Cannot read content of ${file.path}")
  }

  override fun getOutputStream(file: VirtualFile, requestor: Any?, modStamp: Long, timeStamp: Long): OutputStream {
    return file.asSafely<MicroPythonFileBase>()?.getOutputStream(requestor, modStamp, timeStamp)
           ?: throw IOException("Cannot write content of ${file.path}")
  }

  override fun getLength(file: VirtualFile): Long {
    return if (file.exists()) (file as MicroPythonFileBase).length else throw IOException("Cannot stat ${file.path}")
  }

  override fun extractRootPath(normalizedPath: String): String = ""

  override fun findFileByPathIfCached(path: String): VirtualFile? {
    TODO("Not yet implemented")
  }

  override fun getRank(): Int = 1

  abstract inner class MicroPythonFileBase(
    protected var myName: String,
    protected val parent: MicroPythonDirectory?,
    var myTimeStamp: Long
  ) : VirtualFile() {

    protected var exists: Boolean = true

    override fun getFileSystem(): NewVirtualFileSystem = this@MicroPythonVFS

    override fun getParent(): VirtualFile? = parent

    override fun getCanonicalFile(): VirtualFile = this

    override fun setWritable(writable: Boolean) {
    }

    override fun getName(): String = myName

    override fun getPath(): String {
      return if (parent != null) "${parent.path}/${myName}"
      else myName
    }

    override fun isWritable(): Boolean = true

    override fun getTimeStamp(): Long = myTimeStamp
    override fun isValid(): Boolean = exists

    abstract val attributes: FileAttributes
  }

  inner class MicroPythonDirectory(name: String, parent: MicroPythonDirectory?, timeStamp: Long) :
    MicroPythonFileBase(name, parent, timeStamp) {
    internal val files: MutableList<MicroPythonFileBase> = mutableListOf()
    override fun findChild(name: String): VirtualFile? = files.firstOrNull { it.getName() == name }
    override fun contentsToByteArray(): ByteArray {
      TODO("Not yet implemented")
    }

    override fun isDirectory(): Boolean = true

    override fun getNameSequence(): CharSequence = "mpfsdir"
    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
      throw IOException("Not supported")

    override fun getInputStream(): InputStream = throw IOException("Not supported")

    override fun getChildren(): Array<VirtualFile> = files.toTypedArray()

    override fun getLength(): Long = 0

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
      // TODO("Not yet implemented")
    }

    override val attributes: FileAttributes
      get() = FileAttributes(true, false, false, false, 0, timeStamp, isWritable)
  }

  inner class MicroPythonFile(name: String, parent: MicroPythonDirectory?, timeStamp: Long, var initialLength: Long) :
    MicroPythonFileBase(name, parent, timeStamp) {
    private var bytes: MicroPythonByteStream? = null

    override fun findChild(name: String): NewVirtualFile? = null

    override fun getNameSequence(): CharSequence = "mpfsfile"
    override fun isDirectory(): Boolean = false
    override fun getChildren(): Array<VirtualFile> = emptyArray()
    override fun getLength(): Long = bytes?.size()?.toLong() ?: initialLength
    fun loadFromDevice(forced: Boolean): MicroPythonByteStream {
      if (!exists) throw IOException("Does not exists")
      TODO()
    }

    fun saveToDevice() {
      TODO()
    }

    override fun contentsToByteArray(): ByteArray {
      loadFromDevice(false)
      return bytes!!.toByteArray()
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
      if (!exists()) throw IOException("Does not exists")
      bytes = MicroPythonByteStream()
      myTimeStamp = newTimeStamp
      return bytes!!
    }

    inner class MicroPythonByteStream() : ByteArrayOutputStream() {
      override fun close() {
        saveToDevice()
      }
    }

    override fun getInputStream(): InputStream {
      val result = loadFromDevice(false)
      return ByteArrayInputStream(result.toByteArray())
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
      //todo
    }

    override val attributes: FileAttributes
      get() = FileAttributes(false, false, false, false, getLength(), timeStamp, isWritable)
  }

}

