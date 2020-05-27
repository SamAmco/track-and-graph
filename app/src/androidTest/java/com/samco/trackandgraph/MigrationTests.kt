/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph

import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import androidx.room.testing.MigrationTestHelper
import com.samco.trackandgraph.database.*
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    companion object {
        private const val FIRST_DATABASE_RELEASE_VERSION = 29
        private const val TEST_DB = "migration-test"
    }

    @Rule @JvmField
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TrackAndGraphDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        helper.createDatabase(TEST_DB, FIRST_DATABASE_RELEASE_VERSION).apply {
            close()
        }
        tryOpenDatabase()
    }

    @Test
    @Throws(IOException::class)
    fun testMigrateLineGraphFeatures_31_32() {
        helper.createDatabase(TEST_DB, 31).apply {
            this.execSQL("INSERT INTO features_table (id, name, track_group_id, type, discrete_values, display_index) VALUES ('4', 'Weather', '1', '0', '2:Meh||5:Great||3:Ok||4:Good||0:Very Poor||1:Poor', '0');")
            this.execSQL("INSERT INTO features_table (id, name, track_group_id, type, discrete_values, display_index) VALUES ('10', 'Anxiety', '1', '0', '0:None||1:Low||2:Medium||3:High', '0');")
            this.execSQL("INSERT INTO features_table (id, name, track_group_id, type, discrete_values, display_index) VALUES ('11', 'Stress', '1', '0', '0:None||1:Low||2:Medium||3:High', '0');")
            this.execSQL("INSERT INTO graphs_and_stats_table (id, graph_stat_group_id, name, graph_stat_type, display_index) VALUES ('1', '1', 'l1', '0', '1');")
            this.execSQL("INSERT INTO graphs_and_stats_table (id, graph_stat_group_id, name, graph_stat_type, display_index) VALUES ('2', '1', 'l2', '0', '0');")
            this.execSQL("INSERT INTO line_graphs_table (id, graph_stat_id, features, duration, y_range_type, y_from, y_to) VALUES ('2', '2', '10!!Anxiety!!4!!6!!3!!9.0!!1.01||11!!Stress!!7!!3!!0!!10.0!!0.255', 'PT744H', '1', '-10.0', '10.0');")
            this.execSQL("INSERT INTO line_graphs_table (id, graph_stat_id, features, duration, y_range_type, y_from, y_to) VALUES ('1', '1', '4!!Weather!!0!!0!!0!!0.0!!1.0', '', '0', '0.0', '1.0');")
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 32, true, *allMigrations)
        val featuresCursor = db.query("SELECT features FROM line_graphs_table")
        val featureStrings = mutableListOf<String>()
        while (featuresCursor.moveToNext()) {
          featureStrings.add(featuresCursor.getString(0))
        }
        assertTrue(featureStrings.contains("10!!Anxiety!!4!!6!!3!!0!!9.0!!1.01||11!!Stress!!7!!3!!0!!0!!10.0!!0.255"))
        assertTrue(featureStrings.contains("4!!Weather!!0!!0!!0!!0!!0.0!!1.0"))
    }

    @Test
    @Throws(IOException::class)
    fun testMigrateLineGraphFeatures_31_32_no_values() {
        helper.createDatabase(TEST_DB, 31).apply {
            this.execSQL("INSERT INTO features_table (id, name, track_group_id, type, discrete_values, display_index) VALUES ('4', 'Weather', '1', '0', '2:Meh||5:Great||3:Ok||4:Good||0:Very Poor||1:Poor', '0');")
            this.execSQL("INSERT INTO features_table (id, name, track_group_id, type, discrete_values, display_index) VALUES ('10', 'Anxiety', '1', '0', '0:None||1:Low||2:Medium||3:High', '0');")
            this.execSQL("INSERT INTO features_table (id, name, track_group_id, type, discrete_values, display_index) VALUES ('11', 'Stress', '1', '0', '0:None||1:Low||2:Medium||3:High', '0');")
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun testMigrateLineGraphFeatures_31_32_bad_names() {
        helper.createDatabase(TEST_DB, 31).apply {
            this.execSQL("INSERT INTO features_table (id, name, track_group_id, type, discrete_values, display_index) VALUES ('4', 'Weather''s', '1', '0', '2:Meh||5:Great||3:Ok||4:Good||0:Very Poor||1:Poor', '0');")
            this.execSQL("INSERT INTO features_table (id, name, track_group_id, type, discrete_values, display_index) VALUES ('10', 'Anxiety', '1', '0', '0:None||1:Low||2:Medium||3:High', '0');")
            this.execSQL("INSERT INTO features_table (id, name, track_group_id, type, discrete_values, display_index) VALUES ('11', 'Stress', '1', '0', '0:None||1:Low||2:Medium||3:High', '0');")
            this.execSQL("INSERT INTO graphs_and_stats_table (id, graph_stat_group_id, name, graph_stat_type, display_index) VALUES ('1', '1', 'l1', '0', '1');")
            this.execSQL("INSERT INTO graphs_and_stats_table (id, graph_stat_group_id, name, graph_stat_type, display_index) VALUES ('2', '1', 'l2', '0', '0');")
            this.execSQL("INSERT INTO line_graphs_table (id, graph_stat_id, features, duration, y_range_type, y_from, y_to) VALUES ('2', '2', '10!!Anxiety!!4!!6!!3!!9.0!!1.01||11!!Stress!!7!!3!!0!!10.0!!0.255', 'PT744H', '1', '-10.0', '10.0');")
            this.execSQL("INSERT INTO line_graphs_table (id, graph_stat_id, features, duration, y_range_type, y_from, y_to) VALUES ('1', '1', '4!!Weather''s!!0!!0!!0!!0.0!!1.0', '', '0', '0.0', '1.0');")
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 32, true, *allMigrations)
        val featuresCursor = db.query("SELECT features FROM line_graphs_table")
        val featureStrings = mutableListOf<String>()
        while (featuresCursor.moveToNext()) {
            featureStrings.add(featuresCursor.getString(0))
        }
        assertTrue(featureStrings.contains("10!!Anxiety!!4!!6!!3!!0!!9.0!!1.01||11!!Stress!!7!!3!!0!!0!!10.0!!0.255"))
        assertTrue(featureStrings.contains("4!!Weather's!!0!!0!!0!!0!!0.0!!1.0"))
    }

    @Test
    @Throws(IOException::class)
    fun testMigrateLineGraphFeatures_32_33() {
        helper.createDatabase(TEST_DB, 32).apply {
            this.execSQL("INSERT INTO features_table (id, name, track_group_id, type, discrete_values, display_index) VALUES ('4', 'Weather''s', '1', '0', '2:Meh||5:Great||3:Ok||4:Good||0:Very Poor||1:Poor', '0');")
            this.execSQL("INSERT INTO graphs_and_stats_table (id, graph_stat_group_id, name, graph_stat_type, display_index) VALUES ('1', '1', 'l1', '0', '1');")
            this.execSQL("INSERT INTO graphs_and_stats_table (id, graph_stat_group_id, name, graph_stat_type, display_index) VALUES ('2', '1', 'l2', '0', '0');")
            this.execSQL("INSERT INTO line_graphs_table (id, graph_stat_id, features, duration, y_range_type, y_from, y_to) VALUES ('2', '2', '4!!Weather''s!!4!!6!!4!!0!!9.0!!1.01||4!!Weather!!7!!3!!0!!1!!10.0!!0.255', 'PT744H', '1', '-10.0', '10.0');")
            this.execSQL("INSERT INTO line_graphs_table (id, graph_stat_id, features, duration, y_range_type, y_from, y_to) VALUES ('1', '1', '4!!Weather''s!!0!!0!!0!!0!!0.0!!1.0||4!!Weather!!7!!3!!1!!1!!10.0!!0.255', '', '0', '0.0', '1.0');")
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 33, true, *allMigrations)
        val featuresCursor = db.query("SELECT features FROM line_graphs_table")
        val featureStrings = mutableListOf<String>()
        while (featuresCursor.moveToNext()) {
            featureStrings.add(featuresCursor.getString(0))
        }
        assertTrue(featureStrings.contains("4!!Weather's!!4!!6!!5!!0!!9.0!!1.01||4!!Weather!!7!!3!!0!!1!!10.0!!0.255"))
        assertTrue(featureStrings.contains("4!!Weather's!!0!!0!!0!!0!!0.0!!1.0||4!!Weather!!7!!3!!2!!1!!10.0!!0.255"))
    }

    @Test
    @Throws(IOException::class)
    fun testMigrateLineGraphFeatures_34_35() {
        helper.createDatabase(TEST_DB, 34).apply {
            this.execSQL("INSERT INTO average_time_between_stat_table (id, graph_stat_id, feature_id, from_value, to_value, duration) VALUES (1, 2, 3, 0, 1, 'PT2232H');")
            this.execSQL("INSERT INTO time_since_last_stat_table (id, graph_stat_id, feature_id, from_value, to_value) VALUES (2, 3, 4, -5, 10);")
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 35, true, *allMigrations)
        val avCursor = db.query("SELECT * FROM average_time_between_stat_table")
        avCursor.moveToNext()
        assertEquals(avCursor.getLong(0), 1)
        assertEquals(avCursor.getLong(1), 2)
        assertEquals(avCursor.getLong(2), 3)
        assertEquals(avCursor.getString(3), "0")
        assertEquals(avCursor.getString(4), "1")
        assertEquals(avCursor.getString(5), "PT2232H")
        assertEquals(avCursor.getString(6), listOf(0, 1).joinToString(splitChars1) { i -> i.toString() })

        val tsCursor = db.query("SELECT * FROM time_since_last_stat_table")
        tsCursor.moveToNext()
        assertEquals(tsCursor.getLong(0), 2)
        assertEquals(tsCursor.getLong(1), 3)
        assertEquals(tsCursor.getLong(2), 4)
        assertEquals(tsCursor.getString(3), "-5")
        assertEquals(tsCursor.getString(4), "10")
        assertEquals(tsCursor.getString(5), listOf(-5, 10).joinToString(splitChars1) { i -> i.toString() })
    }

    private fun tryOpenDatabase() {
        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TrackAndGraphDatabase::class.java,
            TEST_DB
        ).addMigrations(*allMigrations).build().apply {
            openHelper.writableDatabase
            close()
        }
    }
}
