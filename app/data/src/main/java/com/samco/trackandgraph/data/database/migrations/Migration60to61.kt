/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds normalized, versioned ZIP packages while retaining every legacy inline script. */
val MIGRATION_60_61 = object : Migration(60, 61) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lua_graph_packages_table` (
                `package_id` TEXT NOT NULL,
                `version` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `archive` BLOB NOT NULL,
                PRIMARY KEY(`package_id`, `version`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `lua_graphs_table_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `graph_stat_id` INTEGER NOT NULL,
                `script` TEXT,
                `package_id` TEXT,
                `package_version` TEXT,
                FOREIGN KEY(`graph_stat_id`) REFERENCES `graphs_and_stats_table2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`package_id`, `package_version`) REFERENCES `lua_graph_packages_table`(`package_id`, `version`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `lua_graphs_table_new` (`id`, `graph_stat_id`, `script`)
            SELECT `id`, `graph_stat_id`, `script` FROM `lua_graphs_table`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `lua_graphs_table`")
        db.execSQL("ALTER TABLE `lua_graphs_table_new` RENAME TO `lua_graphs_table`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_lua_graphs_table_id` ON `lua_graphs_table` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_lua_graphs_table_graph_stat_id` ON `lua_graphs_table` (`graph_stat_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_lua_graphs_table_package_id_package_version` ON `lua_graphs_table` (`package_id`, `package_version`)")
    }
}
