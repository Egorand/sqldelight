/*
 * Copyright (C) 2017 Square, Inc.
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

package com.squareup.sqldelight.util

import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import com.squareup.sqldelight.SqliteFileCompiler
import com.squareup.sqldelight.model.relativePath
import org.junit.Assert.fail
import org.junit.rules.TemporaryFolder
import java.io.File

class TestFixturesRule : TemporaryFolder() {

  private val sqliteFileCompiler = SqliteFileCompiler()

  private lateinit var srcMainDir: File
  private lateinit var javaDir: File
  private lateinit var sqldelightDir: File

  override fun before() {
    super.before()
    createTestDirs()
  }

  private fun createTestDirs() {
    srcMainDir = File(newFolder("src"), "main").apply { mkdirs() }
    javaDir = File(srcMainDir, "java").apply { mkdirs() }
    sqldelightDir = File(srcMainDir, "sqldelight").apply { mkdirs() }
  }

  fun createTestSqFile(filePath: String, contents: String) =
      createTestFile(sqldelightDir, filePath, contents)

  fun createTestJavaFile(filePath: String, contents: String) =
      createTestFile(javaDir, filePath, contents)

  private fun createTestFile(dir: File, filePath: String, contents: String): File {
    val testFileDirPath = filePath.substring(0, filePath.lastIndexOf('/'))
    val testFileDir = File(dir, testFileDirPath).apply { mkdirs() }
    val testFileName = filePath.substring(filePath.lastIndexOf('/') + 1)
    return File(testFileDir, testFileName).apply {
      createNewFile()
      writeText(contents)
    }
  }

  fun checkCompilesTo(sqFile: File, output: String) {
    val inputs = listOf(sqFile)
    sqliteFileCompiler.compile(
        inputs = inputs,
        outOfDate = inputs,
        successCallback = { file, success ->
          assertThat(getCompiledFileContents(file, success.model)).isEqualTo(output)
        },
        errorCallback = { fail(it.joinToString(separator = "\n")) })
  }

  private fun getCompiledFileContents(file: File, model: TypeSpec) = buildString {
    JavaFile.builder(file.relativePackage(), model).build().writeTo(this)
  }

  private fun File.relativePackage() = absolutePath.relativePath(File.separatorChar).dropLast(1)
      .joinToString(".")
}
