package com.jetbrains.micropython.devices

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.micropython.run.MicroPythonRunConfiguration
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.MicroPythonTypeHints
import com.jetbrains.micropython.settings.MicroPythonUsbId
import com.jetbrains.micropython.settings.microPythonFacet
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.packaging.PyRequirement

/**
 * @author vlan
 */
class Esp8266DeviceProvider : MicroPythonDeviceProvider {
  override val persistentName: String
    get() = "ESP8266"

  override val documentationURL: String
    get() = "https://github.com/vlasovskikh/intellij-micropython/wiki/ESP8266"

  override val usbIds: List<MicroPythonUsbId>
    get() = listOf(MicroPythonUsbId(0x1A86, 0x7523),
                   MicroPythonUsbId(0x10C4, 0xEA60))

  override val typeHints: MicroPythonTypeHints by lazy {
    MicroPythonTypeHints(listOf("stdlib", "micropython", "esp8266"))
  }

  override fun getPackageRequirements(sdk: Sdk): List<PyRequirement> {
    val manager = PyPackageManager.getInstance(sdk)
    return manager.parseRequirements("""|pyserial>=3.3,<4.0
                                        |docopt>=0.6.2,<0.7
                                        |adafruit-ampy>=1.0.5,<1.1""".trimMargin())
  }

  override fun getRunCommandLineState(configuration: MicroPythonRunConfiguration,
                                      environment: ExecutionEnvironment): CommandLineState? {
    val module = configuration.module ?: return null
    val command = getMicroUploadCommand(configuration.path, module) ?: return null

    return object : CommandLineState(environment) {
      override fun startProcess() =
          OSProcessHandler(GeneralCommandLine(command))
    }
  }

  companion object {
    private fun getMicroUploadCommand(path: String, module: Module): List<String>? {
      val facet = module.microPythonFacet ?: return null
      val pythonPath = facet.pythonPath ?: return null
      val devicePath = facet.devicePath ?: return null
      val file = StandardFileSystems.local().findFileByPath(path) ?: return null
      val rootDir = getClosestRoot(file, module) ?: return null
      val excludeRoots = ModuleRootManager.getInstance(module).excludeRoots
      val excludes = excludeRoots
          .asSequence()
          .filter { VfsUtil.isAncestor(file, it, false) }
          .map { VfsUtilCore.getRelativePath(it, rootDir) }
          .filterNotNull()
          .map { listOf("-X", it) }
          .flatten()
          .toList()
      return listOf(pythonPath, "${MicroPythonFacet.scriptsPath}/microupload.py", "-C", rootDir.path) +
          excludes +
          listOf("-v", devicePath, path)
    }

    private fun getClosestRoot(file: VirtualFile, module: Module): VirtualFile? {
      val roots = mutableSetOf<VirtualFile>().apply {
        val rootManager = module.rootManager
        addAll(rootManager.contentRoots)
        addAll(rootManager.sourceRoots)
      }
      var parent: VirtualFile? = file
      while (parent != null) {
        if (parent in roots) {
          break
        }
        parent = parent.parent
      }
      return parent ?: module.guessModuleDir()
    }
  }
}