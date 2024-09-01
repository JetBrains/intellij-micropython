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
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.ActionLink
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import com.jetbrains.micropython.devices.MicroPythonDeviceProvider
import com.jetbrains.micropython.nova.ConnectCredentials
import com.jetbrains.micropython.nova.uriOrMessageUrl
import java.awt.BorderLayout
import javax.swing.JList
import javax.swing.JPanel

/**
 * @author vlan
 */
class MicroPythonSettingsPanel(private val facet: MicroPythonFacet, disposable: Disposable) : JPanel(BorderLayout()) {
  private val deviceTypeCombo = ComboBox(MicroPythonDeviceProvider.providers.toTypedArray())

  var deviceProviderUrl = ""
  private var docsHyperlink = ActionLink("") { BrowserUtil.browse(deviceProviderUrl) }

  private var url: String = facet.configuration.webReplUrl
  private var password: String = ""
  val connectionPanel:DialogPanel

  init {
    border = IdeBorderFactory.createEmptyBorder(UIUtil.PANEL_SMALL_INSETS)
    runWithModalProgressBlocking(facet.module.project, "Save password") {
      password = facet.module.project.service<ConnectCredentials>().retrievePassword(url)
    }
    val deviceContentPanel = FormBuilder.createFormBuilder()
      .addLabeledComponent("Device type:", deviceTypeCombo)
      .addComponent(docsHyperlink)
      .panel

    connectionPanel = panel {
      row {
        textField().bindText({ url }, { url = it })
          .label("URL: ").columns(40)
          .validationInfo { field ->
            val (_, msg) = uriOrMessageUrl(field.text)
            msg?.let { error(it).withOKEnabled() }
          }

      }.layout(RowLayout.LABEL_ALIGNED)
      row {
        passwordField()
          .bindText({ password }, { password = it })
          .label("Password (4..9 symbols): ").columns(40)
          .validationInfo { field ->
            if (field.password.size !in PASSWORD_LENGHT) error("Allowed password length is $PASSWORD_LENGHT").withOKEnabled() else null
          }
      }.layout(RowLayout.LABEL_ALIGNED)

    }.apply {
      registerValidators(disposable)
      validateAll()
    }
    add(deviceContentPanel, BorderLayout.NORTH)
    add(connectionPanel, BorderLayout.CENTER)

    deviceTypeCombo.apply {
      renderer = object: SimpleListCellRenderer<MicroPythonDeviceProvider>() {
        override fun customize(list: JList<out MicroPythonDeviceProvider>, value: MicroPythonDeviceProvider?,
                               index: Int, selected: Boolean, hasFocus: Boolean) {
          text = value?.presentableName ?: return
        }
      }
      addActionListener {
        docsHyperlink.apply {
          deviceProviderUrl = selectedProvider.documentationURL
          text = "Learn more about setting up ${selectedProvider.presentableName} devices"
          repaint()
        }
      }
    }
  }

  fun isModified(configuration: MicroPythonFacetConfiguration, facet: MicroPythonFacet): Boolean {
    return deviceTypeCombo.selectedItem != configuration.deviceProvider || connectionPanel.isModified()
  }

  fun getDisplayName(): String = "MicroPython"

  fun apply(configuration: MicroPythonFacetConfiguration, facet: MicroPythonFacet) {
    configuration.deviceProvider = selectedProvider
    connectionPanel.apply()
    configuration.webReplUrl = url
    runWithModalProgressBlocking(facet.module.project, "Save password") {
      facet.module.project.service<ConnectCredentials>().savePassword(url, password)
    }
  }

  fun reset(configuration: MicroPythonFacetConfiguration, facet: MicroPythonFacet) {
    deviceTypeCombo.selectedItem = configuration.deviceProvider
    connectionPanel.reset()
  }

  private val selectedProvider: MicroPythonDeviceProvider
    get() = deviceTypeCombo.selectedItem as MicroPythonDeviceProvider
}
private val PASSWORD_LENGHT = 4..9
