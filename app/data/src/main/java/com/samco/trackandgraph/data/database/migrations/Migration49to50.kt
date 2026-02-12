package com.samco.trackandgraph.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_49_50 = object : Migration(49, 50) {
    override fun migrate(db: SupportSQLiteDatabase) {
        //migrate table pie_charts_table2 to add column sum_by_count

        //alter table to old
        db.execSQL("ALTER TABLE pie_charts_table2 RENAME TO pie_charts_table2_old")

        //create new table
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `pie_charts_table2` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `graph_stat_id` INTEGER NOT NULL,
                    `feature_id` INTEGER NOT NULL, 
                    `duration` TEXT, 
                    `end_date` TEXT, 
                    `sum_by_count` INTEGER NOT NULL, 
                    FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent()
        )

        //copy data from old table to new table
        db.execSQL(
            """
                INSERT INTO pie_charts_table2
                SELECT 
                    id, 
                    graph_stat_id, 
                    feature_id, 
                    duration, 
                    end_date, 
                    1 as sum_by_count
                FROM pie_charts_table2_old
            """.trimIndent()
        )

        //drop old table
        db.execSQL("DROP TABLE pie_charts_table2_old")

        //Create index's
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pie_charts_table2_id` ON `pie_charts_table2` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pie_charts_table2_graph_stat_id` ON `pie_charts_table2` (`graph_stat_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pie_charts_table2_feature_id` ON `pie_charts_table2` (`feature_id`)")
    }
}
