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


val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `line_graphs_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                 `graph_stat_id` INTEGER NOT NULL, 
                `duration` TEXT, 
                `y_range_type` INTEGER NOT NULL, 
                `y_from` REAL NOT NULL, 
                `y_to` REAL NOT NULL, 
                FOREIGN KEY(`graph_stat_id`) 
                REFERENCES `graphs_and_stats_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimMargin()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graphs_table2_id` ON `line_graphs_table2` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graphs_table2_graph_stat_id` ON `line_graphs_table2` (`graph_stat_id`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `line_graph_features_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `line_graph_id` INTEGER NOT NULL,
                `feature_id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `color_index` INTEGER NOT NULL,
                `averaging_mode` INTEGER NOT NULL,
                 `plotting_mode` INTEGER NOT NULL, 
                `point_style` INTEGER NOT NULL, 
                `offset` REAL NOT NULL, 
                `scale` REAL NOT NULL, 
                `duration_plotting_mode` INTEGER NOT NULL, 
                FOREIGN KEY(`line_graph_id`) REFERENCES `line_graphs_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimMargin()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graph_features_table_id` ON `line_graph_features_table` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graph_features_table_line_graph_id` ON `line_graph_features_table` (`line_graph_id`)")

        db.execSQL("INSERT INTO line_graphs_table2 SELECT id, graph_stat_id, duration, y_range_type, y_from, y_to FROM line_graphs_table")
        val lineGraphsCursor = db.query("SELECT * FROM line_graphs_table")
        val inserts = mutableListOf<Pair<String, List<String>>>()
        var index = 0L
        val lineGraphFeatureInsertStatement =
            """
                INSERT INTO line_graph_features_table(
                    id, line_graph_id, feature_id, name,
                    color_index, averaging_mode, plotting_mode, point_style,
                    offset, scale, duration_plotting_mode
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent()
        while (lineGraphsCursor.moveToNext()) {
            val id = lineGraphsCursor.getString(0)
            val features = lineGraphsCursor.getString(2).split("||")
            for (featureString in features) {
                val featureProperties = featureString.split("!!")
                if (featureProperties.size >= 8) {
                    val params = mutableListOf(index++.toString(), id)
                    params.addAll(featureProperties)
                    params.add(0.toString())
                    inserts.add(Pair(lineGraphFeatureInsertStatement, params))
                }
            }
        }
        if (inserts.size > 0) inserts.forEach {
            db.execSQL(
                it.first,
                it.second.toTypedArray()
            )
        }
        db.execSQL("DROP TABLE IF EXISTS `line_graphs_table`")
    }
}
