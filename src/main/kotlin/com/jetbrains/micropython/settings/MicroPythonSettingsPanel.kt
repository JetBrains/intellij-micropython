/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.micropython.settings

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.CheckBox
import com.intellij.util.text.nullize
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import com.jetbrains.micropython.devices.MicroPythonDeviceProvider
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel

/**
 * @author vlan
 */
class MicroPythonSettingsPanel(private val module: Module) : JPanel() {
  private val deviceTypeCombo = ComboBox(MicroPythonDeviceProvider.providers.toTypedArray())
  private var docsHyperlink = object : ActionLink() {
    var url = ""

    init {
      addActionListener {
        BrowserUtil.browse(url)
      }
    }
  }
  private val devicePath = TextFieldWithBrowseButton()
  private val autoDetectDevicePath = CheckBox("Auto-detect device path").apply {
    addActionListener {
      update()
    }
  }

  private val devicePathPanel: JPanel by lazy {
    FormBuilder.createFormBuilder()
        .addLabeledComponent("Device path:", JPanel(BorderLayout()).apply {
          add(devicePath, BorderLayout.CENTER)
          add(JButton("Detect").apply {
            addActionListener {
              devicePath.text = module.microPythonFacet?.detectDevicePathSynchronously(selectedProvider) ?: ""
            }
          }, BorderLayout.EAST)
        })
        .panel
  }

  init {
    layout = BorderLayout()
    border = IdeBorderFactory.createEmptyBorder(UIUtil.PANEL_SMALL_INSETS)

    val deviceContentPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent("Device type:", deviceTypeCombo)
        .addComponent(autoDetectDevicePath)
        .addComponent(devicePathPanel)
        .addComponent(docsHyperlink)
        .panel

    add(deviceContentPanel, BorderLayout.NORTH)

    deviceTypeCombo.apply {
      renderer = object: SimpleListCellRenderer<MicroPythonDeviceProvider>() {
        override fun customize(list: JList<out MicroPythonDeviceProvider>, value: MicroPythonDeviceProvider?,
                               index: Int, selected: Boolean, hasFocus: Boolean) {
          text = value?.presentableName ?: return
        }
      }
      addActionListener {
        docsHyperlink.apply {
          url = selectedProvider.documentationURL
          text = "Learn more about setting up ${selectedProvider.presentableName} devices"
          repaint()
        }
      }
    }

    devicePath.apply {
      val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
      addBrowseFolderListener("My Title", null, module.project, descriptor)
    }

    update()
  }

  fun isModified(configuration: MicroPythonFacetConfiguration, facet: MicroPythonFacet): Boolean =
      deviceTypeCombo.selectedItem != configuration.deviceProvider
          || devicePath.text.nullize(true) != facet.devicePath
          || autoDetectDevicePath.isSelected != facet.autoDetectDevicePath

  fun getDisplayName(): String = "MicroPython"

  fun apply(configuration: MicroPythonFacetConfiguration, facet: MicroPythonFacet) {
    configuration.deviceProvider = selectedProvider
    facet.devicePath = devicePath.text.nullize(true)
    facet.autoDetectDevicePath = autoDetectDevicePath.isSelected
  }

  fun reset(configuration: MicroPythonFacetConfiguration, facet: MicroPythonFacet) {
    deviceTypeCombo.selectedItem = configuration.deviceProvider
    devicePath.text = facet.devicePath ?: ""
    autoDetectDevicePath.isSelected = facet.autoDetectDevicePath
    update()
  }

  private fun update() {
    UIUtil.setEnabled(devicePathPanel, !autoDetectDevicePath.isSelected, true)
  }

  private val selectedProvider: MicroPythonDeviceProvider
    get() = deviceTypeCombo.selectedItem as MicroPythonDeviceProvider
}