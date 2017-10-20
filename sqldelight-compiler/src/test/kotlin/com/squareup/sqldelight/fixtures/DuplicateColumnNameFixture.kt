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

package com.squareup.sqldelight.fixtures

import com.squareup.sqldelight.util.TestFixturesRule
import org.junit.Rule
import org.junit.Test

class DuplicateColumnNameFixture {

  @JvmField @Rule val testFixturesRule = TestFixturesRule()

  @Test fun duplicateColumnForbidden() {
    val testSqFile = testFixturesRule.createTestSqFile("Table.sq", """
      |CREATE TABLE test (
      |  column_1 INTEGER,
      |  column_1 INTEGER
      |);
      |""".trimMargin())
    testFixturesRule.checkCompilationErrorMessageContains(testSqFile, """
      |Table.sq line 3:2 - Duplicate column name
      |1    CREATE TABLE test (
      |2      column_1 INTEGER,
      |3      column_1 INTEGER
      |       ^^^^^^^^
      |4    )
      |""".trimMargin())
  }

  @Test fun escapingWithBackticksCountsAsDuplicate() {
    val testSqFile = testFixturesRule.createTestSqFile("IdentifierTable.sq", SQL_IDENTIFIER_TABLE)
    testFixturesRule.checkCompilationErrorMessageContains(testSqFile, """
      |IdentifierTable.sq line 3:2 - Duplicate column name
      |1    CREATE TABLE identifier_table (
      |2      'DESC' TEXT,
      |3      `DESC` TEXT,
      |       ^^^^^^
      |4      "DESC" TEXT,
      |5      sup TEXT,
      |6      [sup] TEXT
      |7    )
      |""".trimMargin())
  }

  @Test fun escapingWithDoubleQuotesCountsAsDuplicate() {
    val testSqFile = testFixturesRule.createTestSqFile("IdentifierTable.sq", SQL_IDENTIFIER_TABLE)
    testFixturesRule.checkCompilationErrorMessageContains(testSqFile, """
      |IdentifierTable.sq line 4:2 - Duplicate column name
      |1    CREATE TABLE identifier_table (
      |2      'DESC' TEXT,
      |3      `DESC` TEXT,
      |4      "DESC" TEXT,
      |       ^^^^^^
      |5      sup TEXT,
      |6      [sup] TEXT
      |7    )
      |""".trimMargin())
  }

  @Test fun escapingWithSquareBracketsCountsAsDuplicate() {
    val testSqFile = testFixturesRule.createTestSqFile("IdentifierTable.sq", SQL_IDENTIFIER_TABLE)
    testFixturesRule.checkCompilationErrorMessageContains(testSqFile, """
      |IdentifierTable.sq line 6:2 - Duplicate column name
      |1    CREATE TABLE identifier_table (
      |2      'DESC' TEXT,
      |3      `DESC` TEXT,
      |4      "DESC" TEXT,
      |5      sup TEXT,
      |6      [sup] TEXT
      |       ^^^^^
      |7    )
      |""".trimMargin())
  }

  companion object {

    private val SQL_IDENTIFIER_TABLE = """
      |CREATE TABLE identifier_table (
      |  'DESC' TEXT,
      |  `DESC` TEXT,
      |  "DESC" TEXT,
      |  sup TEXT,
      |  [sup] TEXT
      |);
      |""".trimMargin()
  }
}
