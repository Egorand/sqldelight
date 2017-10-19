/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.gradle

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteFileCompiler
import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.model.relativePath
import com.squareup.sqldelight.validation.SqlDelightValidator
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

open class SqlDelightTask : SourceTask() {
  private val sqldelightValidator = SqlDelightValidator()
  private val sqliteFileCompiler = SqliteFileCompiler(sqldelightValidator)

  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input fun pluginVersion() = VERSION

  @get:OutputDirectory var outputDirectory: File? = null

  var buildDirectory: File? = null
    set(value) {
      field = value
      outputDirectory = SqliteCompiler.OUTPUT_DIRECTORY.fold(buildDirectory, ::File)
    }

  @TaskAction
  fun execute(inputs: IncrementalTaskInputs) {
    val outOfDateFiles = collectOutOfDateFiles(inputs)
    sqliteFileCompiler.compile(
        getInputs().files,
        outOfDateFiles,
        { ErrorListener(it) },
        { file, status -> writeModel(file.relativePackage(), status.model) },
        { logErrors(it) })
  }

  private fun collectOutOfDateFiles(inputs: IncrementalTaskInputs) = mutableListOf<File>().apply {
    inputs.outOfDate { add(it.file) }
  }

  private fun writeModel(packageName: String, model: TypeSpec) {
    JavaFile.builder(packageName, model).build().writeTo(outputDirectory)
  }

  private fun File.relativePackage() = absolutePath.relativePath(File.separatorChar).dropLast(1)
      .joinToString(".")

  private fun logErrors(errors: Collection<String>) {
    logger.log(LogLevel.ERROR, "")
    errors.forEach { logger.log(LogLevel.ERROR, it.replace("\n", "\n  ").trimEnd(' ')) }
    val errorString = if (errors.size != 1) "errors" else "error"
    logger.log(LogLevel.ERROR, "${errors.size} $errorString")
    throw SqlDelightException(
        "Generation failed; see the generator error output for details.")
  }
}
