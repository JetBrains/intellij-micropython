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

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.jetbrains.micropython.settings.MicroPythonFacetType
import com.jetbrains.python.run.PythonConfigurationFactoryBase
import javax.swing.Icon

/**
 * @author Mikhail Golubev
 */
class MicroPythonConfigurationType : ConfigurationType {
  companion object {
    fun getInstance(): MicroPythonConfigurationType =
        ConfigurationTypeUtil.findConfigurationType(MicroPythonConfigurationType::class.java)
  }
  
  internal val factory = object : PythonConfigurationFactoryBase(this) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration = MicroPythonRunConfiguration(project, this)
  }

  override fun getIcon(): Icon = MicroPythonFacetType.LOGO

  override fun getConfigurationTypeDescription(): String = "MicroPython run configuration"

  override fun getId(): String = "MicroPythonConfigurationType"

  override fun getDisplayName(): String = "MicroPython"

  override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(factory)
}