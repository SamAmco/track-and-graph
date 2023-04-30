package com.samco.trackandgraph.base.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_50_51 = object : Migration(50, 51) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `last_value_stats_table` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `graph_stat_id` INTEGER NOT NULL, 
                    `feature_id` INTEGER NOT NULL, 
                    `end_date` TEXT, 
                    `from_value` REAL NOT NULL, 
                    `to_value` REAL NOT NULL, 
                    `labels` TEXT NOT NULL, 
                    `filter_by_range` INTEGER NOT NULL, 
                    `filter_by_labels` INTEGER NOT NULL, 
                     FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                     FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                 )
            """.trimIndent()
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_last_value_stats_table_id` ON `last_value_stats_table` (`id`)"
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_last_value_stats_table_graph_stat_id` ON `last_value_stats_table` (`graph_stat_id`)"
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_last_value_stats_table_feature_id` ON `last_value_stats_table` (`feature_id`)"
        )
    }
}
