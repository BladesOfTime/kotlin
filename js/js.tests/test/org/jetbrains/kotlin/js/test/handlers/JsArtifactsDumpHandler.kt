/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

/**
 * Copy JS artifacts from the temporary directory to the `build/out` firectory.
 */
class JsArtifactsDumpHandler(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {}

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()

        val outputDir = getJsArtifactsBuildOutputDir(originalFile, testServices, TranslationMode.FULL)
        val dceOutputDir = getJsArtifactsBuildOutputDir(originalFile, testServices, TranslationMode.FULL_DCE)
        val perModuleOutputDir = getJsArtifactsBuildOutputDir(originalFile, testServices, TranslationMode.PER_MODULE)
        val perModuleDceOutputDir = getJsArtifactsBuildOutputDir(originalFile, testServices, TranslationMode.PER_MODULE_DCE)
        val minOutputDir = File(dceOutputDir, originalFile.nameWithoutExtension)

        copy(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, TranslationMode.FULL), outputDir)
        copy(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, TranslationMode.FULL_DCE), dceOutputDir)
        copy(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, TranslationMode.PER_MODULE), perModuleOutputDir)
        copy(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, TranslationMode.PER_MODULE_DCE), perModuleDceOutputDir)
        copy(JsEnvironmentConfigurator.getMinificationJsArtifactsOutputDir(testServices), minOutputDir)
    }
}

private fun copy(from: File, into: File) {
    if (from.listFiles()?.size == 0) return
    from.copyRecursively(into, overwrite = true)
}

private fun testGroupOutputDirFor(allDirectives: RegisteredDirectives, translationMode: TranslationMode): File {
    val pathToRootOutputDir = allDirectives[JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR].first()
    val testGroupOutputDirPrefix = allDirectives[JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX].first()
    val subdir = when (translationMode) {
        TranslationMode.FULL -> "out/"
        TranslationMode.FULL_DCE -> "out-min/"
        TranslationMode.PER_MODULE -> "out-per-module/"
        TranslationMode.PER_MODULE_DCE -> "out-per-module-min/"
    }
    return File(pathToRootOutputDir + subdir + testGroupOutputDirPrefix)
}

fun getJsArtifactsBuildOutputDir(originalTestDataFile: File, testServices: TestServices, translationMode: TranslationMode): File {
    val allDirectives = testServices.moduleStructure.allDirectives
    val stopFile = File(allDirectives[JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR].first())
    return generateSequence(originalTestDataFile.parentFile) { it.parentFile }
        .takeWhile { it != stopFile }
        .map { it.name }
        .toList().asReversed()
        .fold(testGroupOutputDirFor(allDirectives, translationMode), ::File)
}
