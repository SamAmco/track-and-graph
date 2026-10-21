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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.samco.trackandgraph.data.database.migrations.MIGRATION_61_62
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration61to62Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TrackAndGraphDatabase::class.java,
    )

    @Test
    fun addsNullableConfigurationWithoutChangingExistingGraphs() {
        helper.createDatabase(TEST_DATABASE, 61).apply {
            execSQL(
                "INSERT INTO graphs_and_stats_table2 (id, name, graph_stat_type) VALUES (42, 'Existing', 'LUA_SCRIPT')"
            )
            execSQL(
                "INSERT INTO lua_graphs_table (id, graph_stat_id, script) VALUES (7, 42, 'return function() end')"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            62,
            true,
            MIGRATION_61_62,
        )
        migrated.query(
            "SELECT script, configuration FROM lua_graphs_table WHERE id = 7"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("return function() end", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }
        migrated.close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-61-62"
    }
}
