/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.data.database.migrations


import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


val MIGRATION_43_44 = object : Migration(43, 44) {
    private var subGroupIndex = 1L
    private var trackGroupMap = mutableMapOf<Long, Long>()
    private var graphGroupMap = mutableMapOf<Long, Long>()
    private val groupInsertStatement =
        """
                INSERT INTO groups_table(
                    id, name, display_index, parent_group_id, color_index
                ) VALUES (?,?,?,?,?)
            """.trimIndent()

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graph_features_table2_feature_id` ON `line_graph_features_table2` (`feature_id`)")
        createDefaultGroup(database)
        createTrackGroups(database)
        createGraphGroups(database)
        createNewFeaturesTable(database)
        copyFeaturesToNewTable(database)
        createNewGraphsTable(database)
        copyGraphsToNewTable(database)
    }

    private fun createDefaultGroup(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `groups_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL, 
                `display_index` INTEGER NOT NULL, 
                `parent_group_id` INTEGER, 
                `color_index` INTEGER NOT NULL, 
                FOREIGN KEY(`parent_group_id`) REFERENCES `groups_table`(`id`) 
                ON UPDATE NO ACTION ON DELETE CASCADE)
            """.trimMargin()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_table_id` ON `groups_table` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_table_parent_id` ON `groups_table` (`parent_group_id`)")
        database.execSQL(
            """
            INSERT INTO 
            groups_table(id, name, display_index, parent_group_id, color_index) 
            VALUES(0, '', 0, NULL, 0)
            """.trimMargin()
        )
    }

    private fun createTrackGroups(database: SupportSQLiteDatabase) {
        val trackGroupsCursor = database.query("SELECT * FROM track_groups_table")
        val inserts = mutableListOf<List<String>>()
        while (trackGroupsCursor.moveToNext()) {
            val id = trackGroupsCursor.getLong(0)
            val newId = subGroupIndex++
            trackGroupMap[id] = newId
            val name = trackGroupsCursor.getString(1)
            val displayIndex = trackGroupsCursor.getString(2)
            inserts.add(listOf(newId.toString(), name, displayIndex, "0", "7"))
        }
        if (inserts.size > 0) inserts.forEach {
            database.execSQL(groupInsertStatement, it.toTypedArray())
        }
        database.execSQL("DROP TABLE IF EXISTS `track_groups_table`")
    }

    private fun createGraphGroups(database: SupportSQLiteDatabase) {
        val graphGroupCursor = database.query("SELECT * FROM graph_stat_groups_table")
        val inserts = mutableListOf<List<String>>()
        while (graphGroupCursor.moveToNext()) {
            val id = graphGroupCursor.getLong(0)
            val newId = subGroupIndex++
            graphGroupMap[id] = newId
            val name = graphGroupCursor.getString(1)
            val displayIndex = graphGroupCursor.getString(2)
            inserts.add(listOf(newId.toString(), name, displayIndex, "0", "2"))
        }
        if (inserts.size > 0) inserts.forEach {
            database.execSQL(groupInsertStatement, it.toTypedArray())
        }
        database.execSQL("DROP TABLE IF EXISTS `graph_stat_groups_table`")
    }

    private fun createNewFeaturesTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `features_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL, 
                `group_id` INTEGER NOT NULL, 
                `type` INTEGER NOT NULL, 
                `discrete_values` TEXT NOT NULL, 
                `display_index` INTEGER NOT NULL, 
                `has_default_value` INTEGER NOT NULL, 
                `default_value` REAL NOT NULL, 
                `feature_description` TEXT NOT NULL, 
                FOREIGN KEY(`group_id`) REFERENCES `groups_table`(`id`) 
                ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )
    }

    private fun copyFeaturesToNewTable(database: SupportSQLiteDatabase) {
        val inserts = mutableListOf<List<String>>()
        val query =
            """
                INSERT INTO features_table2(
                    id, name, group_id, type, discrete_values, display_index, 
                    has_default_value, default_value, feature_description
                ) VALUES (?,?,?,?,?,?,?,?,?)
            """.trimIndent()
        val featureCursor = database.query("SELECT * FROM features_table")
        while (featureCursor.moveToNext()) {
            val id = featureCursor.getString(0)
            val name = featureCursor.getString(1)
            val type = featureCursor.getString(3)
            val discreteValues = featureCursor.getString(4)
            val displayIndex = featureCursor.getString(5)
            val hasDefaultValue = featureCursor.getString(6)
            val defaultValue = featureCursor.getString(7)
            val featureDescription = featureCursor.getString(8)

            val trackGroupId = featureCursor.getLong(2)
            val newGroupId = trackGroupMap[trackGroupId] ?: 0
            inserts.add(
                listOf(
                    id,
                    name,
                    newGroupId.toString(),
                    type,
                    discreteValues,
                    displayIndex,
                    hasDefaultValue,
                    defaultValue,
                    featureDescription,
                )
            )
        }
        if (inserts.size > 0) inserts.forEach {
            database.execSQL(query, it.toTypedArray())
        }
        database.execSQL("DROP TABLE IF EXISTS `features_table`")
        database.execSQL("ALTER TABLE features_table2 RENAME TO features_table")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_features_table_id` ON `features_table` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_features_table_group_id` ON `features_table` (`group_id`)")
    }

    private fun createNewGraphsTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `graphs_and_stats_table3` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `group_id` INTEGER NOT NULL, 
            `name` TEXT NOT NULL, 
            `graph_stat_type` INTEGER NOT NULL, 
            `display_index` INTEGER NOT NULL, 
            FOREIGN KEY(`group_id`) REFERENCES `groups_table`(`id`) 
            ON UPDATE NO ACTION ON DELETE CASCADE)
            """.trimMargin()
        )
    }

    private fun copyGraphsToNewTable(database: SupportSQLiteDatabase) {
        val inserts = mutableListOf<List<String>>()
        val query =
            """
                INSERT INTO graphs_and_stats_table3(
                    id, group_id, name, graph_stat_type, display_index
                ) VALUES (?,?,?,?,?)
            """.trimIndent()
        val graphsCursor = database.query("SELECT * FROM graphs_and_stats_table2")
        while (graphsCursor.moveToNext()) {
            val id = graphsCursor.getString(0)
            val name = graphsCursor.getString(2)
            val graphStatType = graphsCursor.getString(3)
            val displayIndex = graphsCursor.getString(4)

            val graphGroupId = graphsCursor.getLong(1)
            val newGroupId = graphGroupMap[graphGroupId] ?: 0
            inserts.add(
                listOf(id, newGroupId.toString(), name, graphStatType, displayIndex)
            )
        }
        if (inserts.size > 0) inserts.forEach {
            database.execSQL(query, it.toTypedArray())
        }
        database.execSQL("DROP TABLE IF EXISTS `graphs_and_stats_table2`")
        database.execSQL("ALTER TABLE graphs_and_stats_table3 RENAME TO graphs_and_stats_table2")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_graphs_and_stats_table2_id` ON `graphs_and_stats_table2` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_graphs_and_stats_table2_group_id` ON `graphs_and_stats_table2` (`group_id`)")
    }
}
