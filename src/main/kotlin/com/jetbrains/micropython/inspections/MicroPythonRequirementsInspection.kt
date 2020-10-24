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

package com.jetbrains.micropython.inspections

import com.intellij.codeInspection.*
import com.intellij.facet.ui.FacetConfigurationQuickFix
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.micropython.settings.microPythonFacet

/**
 * @author vlan
 */
class MicroPythonRequirementsInspection : LocalInspectionTool() {
  override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
    val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return null
    val facet = module.microPythonFacet ?: return null
    val result = facet.checkValid()
    if (result.isOk) return null
    val facetFix: FacetConfigurationQuickFix? = result.quickFix
    val fix = if (facetFix != null) object : LocalQuickFix {
      override fun getFamilyName() = "Missing required MicroPython packages"

      override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        facetFix.run(null)
      }
    } else null
    val fixes = if (fix != null) arrayOf(fix) else emptyArray()
    return arrayOf(manager.createProblemDescriptor(file, result.errorMessage, true, fixes,
                                                   ProblemHighlightType.GENERIC_ERROR_OR_WARNING))
  }
}