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

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ListCellRendererWrapper
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.micropython.devices.MicroPythonDeviceProvider
import java.awt.BorderLayout
import javax.swing.JList
import javax.swing.JPanel

/**
 * @author vlan
 */
class MicroPythonSettingsPanel : JPanel(BorderLayout()) {
  val deviceTypeCombo = ComboBox(MicroPythonDeviceProvider.providers, JBUI.scale(300))

  init {
    deviceTypeCombo.renderer = object: ListCellRendererWrapper<MicroPythonDeviceProvider>() {
      override fun customize(list: JList<*>, value: MicroPythonDeviceProvider, index: Int, selected: Boolean, hasFocus: Boolean) {
        setText(value.presentableName)
      }
    }
    val contentPanel = FormBuilder.createFormBuilder().addLabeledComponent("Device:", deviceTypeCombo).panel
    add(contentPanel, BorderLayout.NORTH)
    // +5px (scaled) from the left added by FacetEditor
    border = IdeBorderFactory.createEmptyBorder(UIUtil.PANEL_SMALL_INSETS)
  }

  fun isModified(configuration: MicroPythonFacetConfiguration): Boolean = deviceTypeCombo.selectedItem != configuration.deviceProvider

  fun getDisplayName(): String = "MicroPython"

  fun apply(configuration: MicroPythonFacetConfiguration) {
    configuration.deviceProvider = deviceTypeCombo.selectedItem as MicroPythonDeviceProvider
  }

  fun reset(configuration: MicroPythonFacetConfiguration) {
    deviceTypeCombo.selectedItem = configuration.deviceProvider
  }
}