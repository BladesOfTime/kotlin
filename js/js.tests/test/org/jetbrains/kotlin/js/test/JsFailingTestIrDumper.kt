/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.test.handlers.getJsArtifactsBuildOutputDir
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

/**
 * Copy phase dumps from the temporary directory to the `build/out` directory in case we don't make it to
 * [org.jetbrains.kotlin.js.test.handlers.JsArtifactsDumpHandler] (for example, if the compiler crashes, producing no artifacts).
 */
class JsFailingTestIrDumper(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun check(failedAssertions: List<WrappedException>) {
        if (failedAssertions.isEmpty()) return
        val moduleStructure = testServices.moduleStructure
        val module = moduleStructure.modules.first()
        val originalFile = moduleStructure.originalTestDataFiles.first()
        for (translationMode in TranslationMode.values()) {
            val dumpDir = JsEnvironmentConfigurator.getJsPhaseDumpDir(testServices, module.name, translationMode)
            if (!dumpDir.exists()) continue
            val outputDir = getJsArtifactsBuildOutputDir(originalFile, testServices, translationMode)
            dumpDir.copyRecursively(File(outputDir, dumpDir.name), overwrite = true)
        }
    }
}
