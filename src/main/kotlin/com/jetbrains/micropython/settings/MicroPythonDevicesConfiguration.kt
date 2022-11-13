package com.jetbrains.micropython.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute

/**
 * @author vlan
 */
@State(name = "MicroPythonDevices", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class MicroPythonDevicesConfiguration : PersistentStateComponent<MicroPythonDevicesConfiguration> {

  companion object {
    fun getInstance(project: Project): MicroPythonDevicesConfiguration =
      project.getService(MicroPythonDevicesConfiguration::class.java)
  }

  // Currently, the device path is stored per project, not per module
  @Attribute var devicePath: String = ""

  @Attribute var autoDetectDevicePath: Boolean = true

  @Attribute var clearReplOnLaunch: Boolean = true

  override fun getState() = this

  override fun loadState(state: MicroPythonDevicesConfiguration) {
    XmlSerializerUtil.copyBean(state, this)
  }
}
