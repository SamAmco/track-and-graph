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

/**
 * Adds support for functions with several outputs. Every existing function keeps both new
 * columns null, which means "the primary output of my own graph", so no data needs rewriting.
 */
val MIGRATION_62_63 = object : Migration(62, 63) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `functions_table` ADD COLUMN `parent_function_id` INTEGER " +
                "REFERENCES `functions_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
        )
        db.execSQL("ALTER TABLE `functions_table` ADD COLUMN `output_node_id` INTEGER")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_functions_table_parent_function_id` " +
                "ON `functions_table` (`parent_function_id`)"
        )
    }
}
