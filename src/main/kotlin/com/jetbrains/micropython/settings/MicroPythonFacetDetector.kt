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

import com.intellij.framework.detection.FacetBasedFrameworkDetector
import com.intellij.framework.detection.FileContentPattern
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.patterns.PatternCondition
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileContent
import com.jetbrains.micropython.devices.MicroPythonDeviceProvider
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFromImportStatement
import com.jetbrains.python.psi.PyImportStatement

/**
 * @author Mikhail Golubev
 */
class MicroPythonFacetDetector : FacetBasedFrameworkDetector<MicroPythonFacet, MicroPythonFacetConfiguration>("MicroPython") {
  override fun getFacetType() = MicroPythonFacetType.getInstance()

  override fun createSuitableFilePattern() =
      FileContentPattern.fileContent().with(object : PatternCondition<FileContent>("Contains MicroPython imports") {
        override fun accepts(fileContent: FileContent, context: ProcessingContext?): Boolean {
          val fileIndex = ProjectRootManager.getInstance(fileContent.project).fileIndex
          if (!fileIndex.isInContent(fileContent.file) || fileIndex.isInLibraryClasses(fileContent.file)) {
            return false
          }
          val detected = MicroPythonDeviceProvider.providers
              .asSequence()
              .flatMap { it.detectedModuleNames.asSequence() }
              .toSet()
          val psiFile = fileContent.psiFile
          return when (psiFile) {
            is PyFile -> {
              return psiFile.importBlock.any {
                when (it) {
                  is PyFromImportStatement -> it.importSourceQName?.firstComponent in detected
                  is PyImportStatement -> it.importElements.any { it.importedQName?.firstComponent in detected }
                  else -> false
                }
              }
            }
            else -> false
          }
        }
      })

  override fun getFileType(): FileType = PythonFileType.INSTANCE
}