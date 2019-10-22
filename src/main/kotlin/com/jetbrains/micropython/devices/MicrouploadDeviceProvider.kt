package com.jetbrains.micropython.devices

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.micropython.run.MicroPythonRunConfiguration
import com.jetbrains.micropython.settings.MicroPythonFacet
import com.jetbrains.micropython.settings.microPythonFacet
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.packaging.PyRequirement

abstract class MicrouploadDeviceProvider : MicroPythonDeviceProvider {
  override fun getPackageRequirements(sdk: Sdk): List<PyRequirement> {
    val manager = PyPackageManager.getInstance(sdk)
    return manager.parseRequirements("""|pyserial>=3.3,<4.0
                                          |docopt>=0.6.2,<0.7
                                          |adafruit-ampy>=1.0.5,<1.1""".trimMargin())
  }

  override fun getRunCommandLineState(configuration: MicroPythonRunConfiguration,
                                      environment: ExecutionEnvironment): CommandLineState? {
    val module = configuration.module ?: return null
    val facet = module.microPythonFacet ?: return null
    val pythonPath = facet.pythonPath ?: return null
    val devicePath = facet.devicePath ?: return null
    val rootPath = configuration.project.basePath ?: return null
    val rootDir = StandardFileSystems.local().findFileByPath(rootPath) ?: return null
    val file = StandardFileSystems.local().findFileByPath(configuration.path) ?: return null
    val excludeRoots = ModuleRootManager.getInstance(module).excludeRoots
    val excludes = excludeRoots
            .asSequence()
            .filter { VfsUtil.isAncestor(file, it, false) }
            .map { VfsUtilCore.getRelativePath(it, rootDir) }
            .filterNotNull()
            .map { listOf("-X", it) }
            .flatten()
            .toList()
    val command = listOf(pythonPath, "${MicroPythonFacet.scriptsPath}/microupload.py", "-C", rootPath) +
            excludes + listOf("-v", devicePath, configuration.path)

    return object : CommandLineState(environment) {
      override fun startProcess() =
              OSProcessHandler(GeneralCommandLine(command))
    }
  }
}