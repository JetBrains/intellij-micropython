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

import com.intellij.facet.Facet
import com.intellij.facet.FacetManager
import com.intellij.facet.FacetType
import com.intellij.facet.ui.FacetConfigurationQuickFix
import com.intellij.facet.ui.ValidationResult
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.module.Module
import com.jetbrains.python.facet.FacetLibraryConfigurator
import com.jetbrains.python.facet.LibraryContributingFacet
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.packaging.PyPackageManagerUI
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PythonSdkType
import javax.swing.JComponent

/**
 * @author vlan
 */
class MicroPythonFacet(facetType: FacetType<out Facet<*>, *>, module: Module, name: String,
                       configuration: MicroPythonFacetConfiguration, underlyingFacet: Facet<*>?)
  : LibraryContributingFacet<MicroPythonFacetConfiguration>(facetType, module, name, configuration, underlyingFacet) {

  companion object {
    val PLUGIN_ID = "intellij-micropython"

    fun getPluginDescriptor(): IdeaPluginDescriptor {
      val plugin = PluginManager.getPlugin(PluginId.getId(PLUGIN_ID))
      plugin ?: throw RuntimeException("The plugin cannot find itself")
      return plugin
    }

    fun getInstance(module: Module) = FacetManager.getInstance(module).getFacetByType(MicroPythonFacetType.ID)
  }

  override fun initFacet() {
    updateLibrary()
  }

  override fun updateLibrary() {
    val typeHints = configuration.deviceProvider.typeHints ?: return
    val plugin = getPluginDescriptor()
    FacetLibraryConfigurator.attachPythonLibrary(module, null, typeHints.libraryName,
                                                 listOf("${plugin.path}/typehints/${typeHints.path}"))
  }

  override fun removeLibrary() {
    val typeHints = configuration.deviceProvider.typeHints ?: return
    FacetLibraryConfigurator.detachPythonLibrary(module, typeHints.libraryName)
  }

  fun checkValid(): ValidationResult {
    val sdk = PythonSdkType.findPythonSdk(module)
    if (sdk == null || PythonSdkType.isInvalid(sdk) || PythonSdkType.getLanguageLevelForSdk(sdk).isOlderThan(LanguageLevel.PYTHON35)) {
      return ValidationResult("MicroPython support requires valid Python 3.5+ SDK")
    }
    val packageManager = PyPackageManager.getInstance(sdk)
    val packages = packageManager.packages ?: return ValidationResult.OK
    val requirements = configuration.deviceProvider.packageRequirements
    if (requirements.any { it.match(packages) == null }) {
      return ValidationResult("Packages required for MicroPython support not found",
                              object : FacetConfigurationQuickFix("Install requirements") {
        override fun run(place: JComponent?) {
          PyPackageManagerUI(module.project, sdk, null).install(requirements, listOf("-U"))
        }
      })
    }
    return ValidationResult.OK
  }
}
