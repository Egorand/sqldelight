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

class CustomClassWorksFineFixture {

  @JvmField @Rule val testFixturesRule = TestFixturesRule()

  @Test fun checkCompiles() {
    testFixturesRule.createTestJavaFile("com/test/User.java", USER_JAVA)
    val testSqFile = testFixturesRule.createTestSqFile("com/test/User.sq", USER_SQL)
    testFixturesRule.checkCompilesTo(testSqFile, USER_MODEL_JAVA)
  }

  companion object {

    private val USER_SQL = """
      |CREATE TABLE user (
      |  balance TEXT AS com.test.User.Money NOT NULL,
      |  balance_nullable TEXT AS com.test.User.Money NULL
      |);
      |""".trimMargin()

    private val USER_JAVA = """
      |package com.test;
      |
      |import android.database.Cursor;
      |import java.lang.Override;
      |import com.squareup.sqldelight.ColumnAdapter;
      |
      |public class User implements UserModel {
      |  public static class Money {
      |    final int dollars;
      |    final int cents;
      |
      |    Money(int dollars, int cents) {
      |      this.dollars = dollars;
      |      this.cents = cents;
      |    }
      |  }
      |
      |  public static ColumnAdapter<Money, String> MONEY_ADAPTER = new ColumnAdapter<Money, String>() {
      |    @Override
      |    public Money decode(String databaseValue) {
      |      String[] money = databaseValue.split(".");
      |      return new Money(Integer.parseInt(money[0]), Integer.parseInt(money[1]));
      |    }
      |
      |    @Override
      |    public String encode(Money balance) {
      |      return balance.dollars + "." + balance.cents;
      |    }
      |  };
      |
      |  public static Factory<User> FACTORY = new Factory<>(new Creator() {
      |    @Override
      |    public User create(Money balance, Money balance_nullable) {
      |      return new User(balance, balance_nullable);
      |    }
      |  }, MONEY_ADAPTER, MONEY_ADAPTER);
      |
      |  private final Money balance;
      |  private final Money balance_nullable;
      |
      |  private User(Money balance, Money balance_nullable) {
      |    this.balance = balance;
      |    this.balance_nullable = balance_nullable;
      |  }
      |
      |  @Override
      |  public Money balance() {
      |    return balance;
      |  }
      |
      |  @Override
      |  public Money balance_nullable() {
      |    return balance_nullable;
      |  }
      |}
      |""".trimMargin()

    private val USER_MODEL_JAVA = """
      |package com.test;
      |
      |import android.database.Cursor;
      |import android.support.annotation.NonNull;
      |import android.support.annotation.Nullable;
      |import com.squareup.sqldelight.ColumnAdapter;
      |import com.squareup.sqldelight.RowMapper;
      |import java.lang.Override;
      |import java.lang.String;
      |
      |public interface UserModel {
      |  String TABLE_NAME = "user";
      |
      |  String BALANCE = "balance";
      |
      |  String BALANCE_NULLABLE = "balance_nullable";
      |
      |  String CREATE_TABLE = ""
      |      + "CREATE TABLE user (\n"
      |      + "  balance TEXT NOT NULL,\n"
      |      + "  balance_nullable TEXT NULL\n"
      |      + ")";
      |
      |  @NonNull
      |  User.Money balance();
      |
      |  @Nullable
      |  User.Money balance_nullable();
      |
      |  interface Creator<T extends UserModel> {
      |    T create(@NonNull User.Money balance, @Nullable User.Money balance_nullable);
      |  }
      |
      |  final class Mapper<T extends UserModel> implements RowMapper<T> {
      |    private final Factory<T> userModelFactory;
      |
      |    public Mapper(Factory<T> userModelFactory) {
      |      this.userModelFactory = userModelFactory;
      |    }
      |
      |    @Override
      |    public T map(@NonNull Cursor cursor) {
      |      return userModelFactory.creator.create(
      |          userModelFactory.balanceAdapter.decode(cursor.getString(0)),
      |          cursor.isNull(1) ? null : userModelFactory.balance_nullableAdapter.decode(cursor.getString(1))
      |      );
      |    }
      |  }
      |
      |  final class Factory<T extends UserModel> {
      |    public final Creator<T> creator;
      |
      |    public final ColumnAdapter<User.Money, String> balanceAdapter;
      |
      |    public final ColumnAdapter<User.Money, String> balance_nullableAdapter;
      |
      |    public Factory(Creator<T> creator, ColumnAdapter<User.Money, String> balanceAdapter,
      |        ColumnAdapter<User.Money, String> balance_nullableAdapter) {
      |      this.creator = creator;
      |      this.balanceAdapter = balanceAdapter;
      |      this.balance_nullableAdapter = balance_nullableAdapter;
      |    }
      |  }
      |}
      |""".trimMargin()
  }
}
