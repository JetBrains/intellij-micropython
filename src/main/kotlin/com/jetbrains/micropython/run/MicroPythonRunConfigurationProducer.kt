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

package com.jetbrains.micropython.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.facet.FacetManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.micropython.settings.MicroPythonFacetType
import com.jetbrains.python.run.AbstractPythonRunConfiguration

/**
 * @author Mikhail Golubev
 */
class MicroPythonRunConfigurationProducer : 
  RunConfigurationProducer<MicroPythonRunConfiguration>(MicroPythonConfigurationType.getInstance()) {
  
  override fun isConfigurationFromContext(configuration: MicroPythonRunConfiguration, context: ConfigurationContext): Boolean {
    val location = context.location ?: return false
    val script = location.psiElement.containingFile ?: return false
    if (!facetEnabledForElement(script)) return false
    val virtualFile = script.virtualFile ?: return false
    if (virtualFile is LightVirtualFile) return false
    return configuration.scriptPath == virtualFile.path
  }

  override fun setupConfigurationFromContext(configuration: MicroPythonRunConfiguration,
                                             context: ConfigurationContext,
                                             sourceElement: Ref<PsiElement>): Boolean {
    val location = context.location ?: return false
    val script = location.psiElement.containingFile ?: return false
    if (!facetEnabledForElement(script)) return false
    val vFile = script.virtualFile
    configuration.scriptPath = vFile.path
    configuration.setGeneratedName()
    configuration.setModule(ModuleUtilCore.findModuleForFile(vFile, context.project))
    return true
  }

  private fun facetEnabledForElement(elem: PsiElement): Boolean {
    val module = ModuleUtilCore.findModuleForFile(elem.containingFile.virtualFile, elem.project) ?: return false
    return FacetManager.getInstance(module)?.getFacetByType(MicroPythonFacetType.ID) != null
  }

  override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
    return other.configuration is AbstractPythonRunConfiguration<*>
  }
}
