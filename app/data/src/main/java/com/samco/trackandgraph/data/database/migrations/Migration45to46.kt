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


val MIGRATION_45_46 = object : Migration(45, 46) {
    override fun migrate(db: SupportSQLiteDatabase) {
        updateAverageTimeBetween(db)
        updateTimeSinceLast(db)
    }

    private fun updateAverageTimeBetween(db: SupportSQLiteDatabase) {
        createAverageTimeBetweenTable(db)
        insertAverageTimeBetweenData(db)
        db.execSQL("DROP TABLE IF EXISTS `average_time_between_stat_table3`")
    }

    private fun insertAverageTimeBetweenData(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                INSERT INTO average_time_between_stat_table4 
                    SELECT id, graph_stat_id, feature_id, from_value, to_value, duration, labels, end_date, 0, 0
                    FROM average_time_between_stat_table3
            """.trimIndent()
        )
        val updates = mutableListOf<NTuple3<Int, Int, Long>>()
        val graphsCursor = db.query("SELECT * FROM average_time_between_stat_table4")
        while (graphsCursor.moveToNext()) {
            try {
                val id = graphsCursor.getLong(0)
                val labels = graphsCursor.getString(6)
                val filterByRange = labels.equals("[]")
                val filterByLabels = !filterByRange
                updates.add(NTuple3(if (filterByRange) 1 else 0, if (filterByLabels) 1 else 0, id))
            } catch (throwable: Throwable) {
                continue
            }
        }
        val query =
            """
                UPDATE average_time_between_stat_table4 SET filter_by_range=?, filter_by_labels=? WHERE id=?
            """.trimIndent()
        if (updates.size > 0) updates.forEach { args ->
            db.execSQL(query, args.toList().map { it.toString() }.toTypedArray())
        }
    }

    private fun createAverageTimeBetweenTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `average_time_between_stat_table4` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `graph_stat_id` INTEGER NOT NULL, 
                    `feature_id` INTEGER NOT NULL, 
                    `from_value` REAL NOT NULL, 
                    `to_value` REAL NOT NULL, 
                    `duration` TEXT, 
                    `labels` TEXT NOT NULL, 
                    `end_date` TEXT, 
                    `filter_by_range` INTEGER NOT NULL, 
                    `filter_by_labels` INTEGER NOT NULL, 
                    FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimMargin()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table4_id` ON `average_time_between_stat_table4` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table4_graph_stat_id` ON `average_time_between_stat_table4` (`graph_stat_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table4_feature_id` ON `average_time_between_stat_table4` (`feature_id`)")
    }

    private fun updateTimeSinceLast(db: SupportSQLiteDatabase) {
        createTimeSinceLastTable(db)
        insertTimeSinceLastData(db)
        db.execSQL("DROP TABLE IF EXISTS `time_since_last_stat_table3`")
    }

    private fun insertTimeSinceLastData(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                INSERT INTO time_since_last_stat_table4 
                SELECT id, graph_stat_id, feature_id, from_value, to_value, labels, 0, 0
                FROM time_since_last_stat_table3
            """.trimIndent()
        )
        val updates = mutableListOf<NTuple3<Int, Int, Long>>()
        val graphsCursor = db.query("SELECT * FROM time_since_last_stat_table4")
        while (graphsCursor.moveToNext()) {
            try {
                val id = graphsCursor.getLong(0)
                val labels = graphsCursor.getString(5)
                val filterByRange = labels.equals("[]")
                val filterByLabels = !filterByRange
                updates.add(NTuple3(if (filterByRange) 1 else 0, if (filterByLabels) 1 else 0, id))
            } catch (throwable: Throwable) {
                continue
            }
        }
        val query =
            """
                UPDATE time_since_last_stat_table4 SET filter_by_range=?, filter_by_labels=? WHERE id=?
            """.trimIndent()
        if (updates.size > 0) updates.forEach { args ->
            db.execSQL(query, args.toList().map { it.toString() }.toTypedArray())
        }
    }

    private fun createTimeSinceLastTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `time_since_last_stat_table4` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `graph_stat_id` INTEGER NOT NULL, 
                    `feature_id` INTEGER NOT NULL, 
                    `from_value` REAL NOT NULL, 
                    `to_value` REAL NOT NULL, 
                    `labels` TEXT NOT NULL, 
                    `filter_by_range` INTEGER NOT NULL, 
                    `filter_by_labels` INTEGER NOT NULL, 
                    FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimMargin()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table4_id` ON `time_since_last_stat_table4` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table4_graph_stat_id` ON `time_since_last_stat_table4` (`graph_stat_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table4_feature_id` ON `time_since_last_stat_table4` (`feature_id`)")
    }
}
