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


val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(db: SupportSQLiteDatabase) {
        //UPDATE graphs_and_stats_table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `graphs_and_stats_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_group_id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `graph_stat_type` INTEGER NOT NULL,
                `display_index` INTEGER NOT NULL,
                FOREIGN KEY(`graph_stat_group_id`) REFERENCES `graph_stat_groups_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimMargin()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_graphs_and_stats_table2_id` ON `graphs_and_stats_table2` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_graphs_and_stats_table2_graph_stat_group_id` ON `graphs_and_stats_table2` (`graph_stat_group_id`)")
        db.execSQL("INSERT INTO graphs_and_stats_table2 SELECT id, graph_stat_group_id, name, graph_stat_type, display_index FROM graphs_and_stats_table")

        //UPDATE time_since_last_stat_table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `time_since_last_stat_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_id` INTEGER NOT NULL,
                `feature_id` INTEGER NOT NULL, 
                `from_value` TEXT NOT NULL, 
                `to_value` TEXT NOT NULL, 
                `discrete_values` TEXT NOT NULL, 
                FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table2_id` ON `time_since_last_stat_table2` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table2_graph_stat_id` ON `time_since_last_stat_table2` (`graph_stat_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_time_since_last_stat_table2_feature_id` ON `time_since_last_stat_table2` (`feature_id`)")
        db.execSQL("INSERT INTO time_since_last_stat_table2 SELECT id, graph_stat_id, feature_id, from_value, to_value, discrete_values FROM time_since_last_stat_table")

        //UPDATE pie_charts_table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pie_charts_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_id` INTEGER NOT NULL,
                `feature_id` INTEGER NOT NULL, 
                `duration` TEXT, 
                `end_date` TEXT, 
                FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pie_charts_table2_id` ON `pie_charts_table2` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pie_charts_table2_graph_stat_id` ON `pie_charts_table2` (`graph_stat_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pie_charts_table2_feature_id` ON `pie_charts_table2` (`feature_id`)")
        db.execSQL("INSERT INTO pie_charts_table2 SELECT id, graph_stat_id, feature_id, duration, NULL FROM pie_chart_table")
        db.execSQL(
            """UPDATE pie_charts_table2 SET end_date= (
                SELECT end_date FROM graphs_and_stats_table 
                WHERE id=pie_charts_table2.graph_stat_id
            )""".trimMargin()
        )

        //UPDATE line_graphs_table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `line_graphs_table3` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_id` INTEGER NOT NULL,
                `duration` TEXT,
                `y_range_type` INTEGER NOT NULL,
                `y_from` REAL NOT NULL,
                `y_to` REAL NOT NULL,
                `end_date` TEXT,
                FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graphs_table3_id` ON `line_graphs_table3` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graphs_table3_graph_stat_id` ON `line_graphs_table3` (`graph_stat_id`)")
        db.execSQL("INSERT INTO line_graphs_table3 SELECT id, graph_stat_id, duration, y_range_type, y_from, y_to, NULL FROM line_graphs_table2")
        db.execSQL(
            """UPDATE line_graphs_table3 SET end_date= (
                SELECT end_date FROM graphs_and_stats_table 
                WHERE id=line_graphs_table3.graph_stat_id
            )""".trimMargin()
        )

        //UPDATE average_time_between_stat_table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `average_time_between_stat_table2` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_id` INTEGER NOT NULL,
                `feature_id` INTEGER NOT NULL, 
                `from_value` TEXT NOT NULL,
                `to_value` TEXT NOT NULL,
                `duration` TEXT,
                `discrete_values` TEXT NOT NULL,
                `end_date` TEXT,
                FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
        )""".trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table2_id` ON `average_time_between_stat_table2` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table2_graph_stat_id` ON `average_time_between_stat_table2` (`graph_stat_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_average_time_between_stat_table2_feature_id` ON `average_time_between_stat_table2` (`feature_id`)")
        db.execSQL("INSERT INTO average_time_between_stat_table2 SELECT id, graph_stat_id, feature_id, from_value, to_value, duration, discrete_values, NULL FROM average_time_between_stat_table")
        db.execSQL(
            """UPDATE average_time_between_stat_table2 SET end_date= (
                SELECT end_date FROM graphs_and_stats_table 
                WHERE id=average_time_between_stat_table2.graph_stat_id
            )""".trimMargin()
        )

        //UPDATE line_graph_features_table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `line_graph_features_table2` (
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
                FOREIGN KEY(`line_graph_id`) REFERENCES `line_graphs_table3`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graph_features_table2_id` ON `line_graph_features_table2` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_line_graph_features_table2_line_graph_id` ON `line_graph_features_table2` (`line_graph_id`)")
        db.execSQL("INSERT INTO line_graph_features_table2 SELECT id, line_graph_id, feature_id, name, color_index, averaging_mode, plotting_mode, point_style, offset, scale, duration_plotting_mode FROM line_graph_features_table")


        //DROP UNUSED TABLES
        db.execSQL("DROP TABLE IF EXISTS `graphs_and_stats_table`")
        db.execSQL("DROP TABLE IF EXISTS `time_since_last_stat_table`")
        db.execSQL("DROP TABLE IF EXISTS `pie_chart_table`")
        db.execSQL("DROP TABLE IF EXISTS `line_graphs_table2`")
        db.execSQL("DROP TABLE IF EXISTS `average_time_between_stat_table`")
        db.execSQL("DROP TABLE IF EXISTS `line_graph_features_table`")
    }
}
