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

package com.samco.trackandgraph.base.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_51_52 = object : Migration(51, 52) {
    override fun migrate(database: SupportSQLiteDatabase) {
        //add the bar charts table
        val tableName = "bar_charts_table"
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `$tableName` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `graph_stat_id` INTEGER NOT NULL, 
                    `feature_id` INTEGER NOT NULL, 
                    `end_date` TEXT, 
                    `duration` TEXT, 
                    `y_range_type` INTEGER NOT NULL,
                    `y_from` REAL NOT NULL,
                    `y_to` REAL NOT NULL,
                    `bar_period` INTEGER NOT NULL, 
                    `sum_by_count` INTEGER NOT NULL, 
                     FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                     FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                 )
            """.trimIndent()
        )

        //create indexes
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_bar_charts_table_id` ON `$tableName` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_bar_charts_table_graph_stat_id` ON `$tableName` (`graph_stat_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_bar_charts_table_feature_id` ON `$tableName` (`feature_id`)")
    }
}