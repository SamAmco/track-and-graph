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

import androidx.room.*
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.samco.trackandgraph.base.database.*
import com.samco.trackandgraph.base.database.migrations.allMigrations
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
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

    @Rule
    @JvmField
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
        assertEquals(
            avCursor.getString(6),
            listOf(0, 1).joinToString("||") { i -> i.toString() })

        val tsCursor = db.query("SELECT * FROM time_since_last_stat_table")
        tsCursor.moveToNext()
        assertEquals(tsCursor.getLong(0), 2)
        assertEquals(tsCursor.getLong(1), 3)
        assertEquals(tsCursor.getLong(2), 4)
        assertEquals(tsCursor.getString(3), "-5")
        assertEquals(tsCursor.getString(4), "10")
        assertEquals(
            tsCursor.getString(5),
            listOf(-5, 10).joinToString("||") { i -> i.toString() })
    }

    @Test
    @Throws(IOException::class)
    fun testMigrateLineGraphFeatures_40_41() {
        helper.createDatabase(TEST_DB, 40).apply {
            execSQL("INSERT INTO graphs_and_stats_table (id, graph_stat_group_id, name, graph_stat_type, display_index, end_date) VALUES (0, 0, 'graph1', 0, 0, '321')")
            execSQL("INSERT INTO graphs_and_stats_table (id, graph_stat_group_id, name, graph_stat_type, display_index, end_date) VALUES (1, 0, 'graph2', 1, 1, '21')")
            execSQL("INSERT INTO graphs_and_stats_table (id, graph_stat_group_id, name, graph_stat_type, display_index, end_date) VALUES (2, 0, 'graph3', 2, 2, '32')")
            execSQL("INSERT INTO graphs_and_stats_table (id, graph_stat_group_id, name, graph_stat_type, display_index, end_date) VALUES (3, 0, 'graph4', 3, 3, '31')")
            execSQL("INSERT INTO time_since_last_stat_table (id, graph_stat_id, feature_id, from_value, to_value, discrete_values) VALUES (0, 3, 0, '5.0', '6.0', '7,8')")
            execSQL("INSERT INTO pie_chart_table (id, graph_stat_id, feature_id, duration) VALUES (0, 1, 0, '123')")
            execSQL("INSERT INTO line_graphs_table2 (id, graph_stat_id, duration, y_range_type, y_from, y_to) VALUES (0, 0, '234', 3, '3.0', '4.0')")
            execSQL("INSERT INTO average_time_between_stat_table (id, graph_stat_id, feature_id, from_value, to_value, duration, discrete_values) VALUES (0, 2, 0, '2.0', '4.0', '545', '3,2,1')")
            execSQL("INSERT INTO line_graph_features_table (id, line_graph_id, feature_id, name, color_index, averaging_mode, plotting_mode, point_style, offset, scale, duration_plotting_mode) VALUES(0, 0, 0, 'some name', 3, 2, 6, 4, 4, 2, 3)")
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 41, true, *allMigrations)
        val graphStatCursor = db.query("SELECT * FROM graphs_and_stats_table2")
        var graphStatCursorCount = 0
        val expectedGraphStatValues = listOf(
            listOf(0L, 0L, "graph1", 0, 0),
            listOf(1L, 0L, "graph2", 1, 1),
            listOf(2L, 0L, "graph3", 2, 2),
            listOf(3L, 0L, "graph4", 3, 3)
        )
        while (graphStatCursor.moveToNext()) {
            assertEquals(
                expectedGraphStatValues[graphStatCursorCount][0],
                graphStatCursor.getLong(0)
            )
            assertEquals(
                expectedGraphStatValues[graphStatCursorCount][1],
                graphStatCursor.getLong(1)
            )
            assertEquals(
                expectedGraphStatValues[graphStatCursorCount][2],
                graphStatCursor.getString(2)
            )
            assertEquals(
                expectedGraphStatValues[graphStatCursorCount][3],
                graphStatCursor.getInt(3)
            )
            assertEquals(
                expectedGraphStatValues[graphStatCursorCount][4],
                graphStatCursor.getInt(4)
            )
            graphStatCursorCount++
        }
        assertEquals(4, graphStatCursorCount)

        val timeSinceLastCursor = db.query("SELECT * FROM time_since_last_stat_table2")
        timeSinceLastCursor.moveToNext()
        assertEquals(0L, timeSinceLastCursor.getLong(0))
        assertEquals(3L, timeSinceLastCursor.getLong(1))
        assertEquals(0L, timeSinceLastCursor.getLong(2))
        assertEquals("5.0", timeSinceLastCursor.getString(3))
        assertEquals("6.0", timeSinceLastCursor.getString(4))
        assertEquals("7,8", timeSinceLastCursor.getString(5))

        val pieChartCursor = db.query("SELECT * FROM pie_charts_table2")
        pieChartCursor.moveToNext()
        assertEquals(0L, pieChartCursor.getLong(0))
        assertEquals(1L, pieChartCursor.getLong(1))
        assertEquals(0L, pieChartCursor.getLong(2))
        assertEquals("123", pieChartCursor.getString(3))
        assertEquals("21", pieChartCursor.getString(4))

        val lineGraphCursor = db.query("SELECT * FROM line_graphs_table3")
        lineGraphCursor.moveToNext()
        assertEquals(0L, lineGraphCursor.getLong(0))
        assertEquals(0L, lineGraphCursor.getLong(1))
        assertEquals("234", lineGraphCursor.getString(2))
        assertEquals(3, lineGraphCursor.getInt(3))
        assertEquals(3.0, lineGraphCursor.getDouble(4))
        assertEquals(4.0, lineGraphCursor.getDouble(5))
        assertEquals("321", lineGraphCursor.getString(6))

        val avTimeBetweenCursor = db.query("SELECT * FROM average_time_between_stat_table2")
        avTimeBetweenCursor.moveToNext()
        assertEquals(0L, avTimeBetweenCursor.getLong(0))
        assertEquals(2L, avTimeBetweenCursor.getLong(1))
        assertEquals(0L, avTimeBetweenCursor.getLong(2))
        assertEquals("2.0", avTimeBetweenCursor.getString(3))
        assertEquals("4.0", avTimeBetweenCursor.getString(4))
        assertEquals("545", avTimeBetweenCursor.getString(5))
        assertEquals("3,2,1", avTimeBetweenCursor.getString(6))
        assertEquals("32", avTimeBetweenCursor.getString(7))

        val lineGraphFeatureCursor = db.query("SELECT * FROM line_graph_features_table2")
        lineGraphFeatureCursor.moveToNext()
        assertEquals(0L, lineGraphFeatureCursor.getLong(0))
        assertEquals(0L, lineGraphFeatureCursor.getLong(1))
        assertEquals(0L, lineGraphFeatureCursor.getLong(2))
        assertEquals("some name", lineGraphFeatureCursor.getString(3))
        assertEquals(3, lineGraphFeatureCursor.getInt(4))
        assertEquals(2, lineGraphFeatureCursor.getInt(5))
        assertEquals(6, lineGraphFeatureCursor.getInt(6))
        assertEquals(4, lineGraphFeatureCursor.getInt(7))
        assertEquals(4, lineGraphFeatureCursor.getInt(8))
        assertEquals(2, lineGraphFeatureCursor.getInt(9))
        assertEquals(3, lineGraphFeatureCursor.getInt(10))
    }

    @Test
    fun testMigrateLineGraphFeatures_47_49() {
        helper.createDatabase(TEST_DB, 47).apply {
            query("pragma foreign_keys")
            query("pragma foreign_keys=on")

            execSQL(
                """ INSERT OR REPLACE INTO 
                    groups_table(id, name, display_index, parent_group_id, color_index) 
                    VALUES(0, '', 0, NULL, 0)
                    """.trimMargin()
            )

            //Insert one feature
            execSQL(
                "INSERT INTO features_table " +
                        "(id, name, group_id, type, discrete_values, display_index, has_default_value, default_value, feature_description)" +
                        " VALUES (0, 'name', 0, 0, '[]', 0, 0, 0.0, 'description')"
            )
            //Insert one data point
            execSQL(
                "INSERT INTO data_points_table " +
                        "(timestamp, feature_id, value, label, note) " +
                        "VALUES (0, 0, 0.0, 'label', 'note')"
            )

            //Insert a graphstat
            execSQL(
                "INSERT INTO graphs_and_stats_table2 " +
                        "(id, group_id, name, graph_stat_type, display_index)" +
                        " VALUES (0, 0, 'name', 0, 0)"
            )

            //Insert an average time between stat
            execSQL(
                "INSERT INTO average_time_between_stat_table4 " +
                        "(id, graph_stat_id, feature_id, from_value, to_value, duration, labels, end_date, filter_by_range, filter_by_labels)" +
                        " VALUES (0, 0, 0, 0.0, 0.0, 0, '[]', 0, 0, 0)"
            )
        }
        helper.runMigrationsAndValidate(TEST_DB, 49, true, *allMigrations)
    }

    private fun tryOpenDatabase() {
        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TrackAndGraphDatabase::class.java,
            TEST_DB
        ).addMigrations(*allMigrations).build().apply {
            openHelper.writableDatabase.close()
        }
    }
}
