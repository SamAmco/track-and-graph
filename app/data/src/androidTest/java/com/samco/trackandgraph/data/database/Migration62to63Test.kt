/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.samco.trackandgraph.data.database.migrations.MIGRATION_62_63
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration62to63Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TrackAndGraphDatabase::class.java,
    )

    private fun createVersion62WithTwoFunctions() {
        helper.createDatabase(TEST_DATABASE, 62).apply {
            execSQL("INSERT INTO features_table (id, name, feature_description) VALUES (1, 'First', '')")
            execSQL("INSERT INTO features_table (id, name, feature_description) VALUES (2, 'Second', '')")
            execSQL("INSERT INTO functions_table (id, feature_id, function_graph) VALUES (7, 1, '$FIRST_GRAPH')")
            execSQL("INSERT INTO functions_table (id, feature_id, function_graph) VALUES (8, 2, '$SECOND_GRAPH')")
            execSQL("INSERT INTO function_input_features_table (id, function_id, feature_id) VALUES (1, 7, 2)")
            close()
        }
    }

    private fun migrate(): SupportSQLiteDatabase = helper.runMigrationsAndValidate(
        TEST_DATABASE,
        63,
        true,
        MIGRATION_62_63,
    )

    @Test
    fun existingFunctionsKeepTheirDataAndAreTheirOwnPrimaryOutput() {
        createVersion62WithTwoFunctions()

        val migrated = migrate()

        migrated.query(
            "SELECT id, feature_id, function_graph, parent_function_id, output_node_id " +
                "FROM functions_table ORDER BY id"
        ).use { cursor ->
            assertEquals(2, cursor.count)

            cursor.moveToFirst()
            assertEquals(7, cursor.getLong(0))
            assertEquals(1, cursor.getLong(1))
            assertEquals(FIRST_GRAPH, cursor.getString(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))

            cursor.moveToNext()
            assertEquals(8, cursor.getLong(0))
            assertEquals(SECOND_GRAPH, cursor.getString(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))
        }
        migrated.query("SELECT function_id, feature_id FROM function_input_features_table").use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals(7, it.getLong(0))
            assertEquals(2, it.getLong(1))
        }
        migrated.close()
    }

    @Test
    fun migratedDatabaseAcceptsAdditionalOutputs() {
        createVersion62WithTwoFunctions()
        val migrated = migrate()
        migrated.execSQL("INSERT INTO features_table (id, name, feature_description) VALUES (3, 'Extra', '')")

        migrated.execSQL(
            "INSERT INTO functions_table (id, feature_id, function_graph, parent_function_id, output_node_id) " +
                "VALUES (9, 3, '', 7, 4)"
        )

        migrated.query("SELECT parent_function_id, output_node_id FROM functions_table WHERE id = 9").use {
            it.moveToFirst()
            assertEquals(7, it.getLong(0))
            assertEquals(4, it.getInt(1))
        }
        migrated.close()
    }

    @Test
    fun deletingTheOwnerCascadesToItsAdditionalOutputs() {
        createVersion62WithTwoFunctions()
        val migrated = migrate()
        migrated.execSQL("PRAGMA foreign_keys = ON")
        migrated.execSQL("INSERT INTO features_table (id, name, feature_description) VALUES (3, 'Extra', '')")
        migrated.execSQL(
            "INSERT INTO functions_table (id, feature_id, function_graph, parent_function_id, output_node_id) " +
                "VALUES (9, 3, '', 7, 4)"
        )

        migrated.execSQL("DELETE FROM functions_table WHERE id = 7")

        migrated.query("SELECT id FROM functions_table ORDER BY id").use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals(8, it.getLong(0))
        }
        migrated.close()
    }

    @Test
    fun parentFunctionIndexExists() {
        createVersion62WithTwoFunctions()
        val migrated = migrate()

        migrated.query("PRAGMA index_list(`functions_table`)").use { cursor ->
            val names = generateSequence { if (cursor.moveToNext()) cursor.getString(1) else null }.toList()
            assertTrue(names.contains("index_functions_table_parent_function_id"))
        }
        migrated.close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-62-63"
        const val FIRST_GRAPH =
            """{"nodes":[],"outputNode":{"x":1.0,"y":2.0,"id":1,"dependencies":[]},"isDuration":false}"""
        const val SECOND_GRAPH =
            """{"nodes":[],"outputNode":{"x":3.0,"y":4.0,"id":2,"dependencies":[]},"isDuration":true}"""
    }
}
