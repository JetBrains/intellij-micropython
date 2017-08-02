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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ListCellRendererWrapper
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.SwingHelper
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
  private val deviceTypeCombo = ComboBox(MicroPythonDeviceProvider.providers)
  private val label = SwingHelper.createWebHyperlink("")
  private val devicePath = TextFieldWithBrowseButton()

  init {
    layout = BorderLayout()
    border = IdeBorderFactory.createEmptyBorder(UIUtil.PANEL_SMALL_INSETS)

    val contentPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent("Device type:", deviceTypeCombo)
        .addLabeledComponent("Device path:", JPanel(BorderLayout()).apply {
          add(devicePath, BorderLayout.CENTER)
          add(JButton("Detect").apply {
            addActionListener {
              val detectPath = {
                val facet = MicroPythonFacet.getInstance(module)
                val detected = facet?.findSerialPorts(selectedProvider)?.firstOrNull()
                ApplicationManager.getApplication().invokeLater {
                  if (detected == null) {
                    Messages.showErrorDialog(this,
                                             "No devices detected. Specify the device path manually.")
                  }
                  devicePath.text = detected ?: ""
                }
              }
              val progress = ProgressManager.getInstance()
              progress.runProcessWithProgressSynchronously(detectPath, "Detecting Serial Ports",
                                                           false, module.project, this)
            }
          }, BorderLayout.EAST)
        })
        .addComponent(label)
        .panel

    add(contentPanel, BorderLayout.NORTH)

    deviceTypeCombo.apply {
      renderer = object: ListCellRendererWrapper<MicroPythonDeviceProvider>() {
        override fun customize(list: JList<*>, value: MicroPythonDeviceProvider, index: Int, selected: Boolean,
                               hasFocus: Boolean) {
          setText(value.presentableName)
        }
      }
      addActionListener {
        label.apply {
          setHyperlinkTarget(selectedProvider.documentationURL)
          setHyperlinkText("Learn more about setting up ${selectedProvider.presentableName} devices")
          repaint()
        }
      }
    }

    devicePath.apply {
      val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
      addBrowseFolderListener("My Title", null, module.project, descriptor)
    }
  }

  fun isModified(configuration: MicroPythonFacetConfiguration): Boolean =
      deviceTypeCombo.selectedItem != configuration.deviceProvider

  fun getDisplayName(): String = "MicroPython"

  fun apply(configuration: MicroPythonFacetConfiguration) {
    configuration.deviceProvider = selectedProvider
  }

  fun reset(configuration: MicroPythonFacetConfiguration) {
    deviceTypeCombo.selectedItem = configuration.deviceProvider
  }

  private val selectedProvider: MicroPythonDeviceProvider
    get() = deviceTypeCombo.selectedItem as MicroPythonDeviceProvider
}