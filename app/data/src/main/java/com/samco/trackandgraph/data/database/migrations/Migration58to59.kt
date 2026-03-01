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

/**
 * Migration 58 to 59: Introduce group_items_table for symlinks feature.
 *
 * This migration:
 * 1. Creates the new group_items_table to track item placement in groups with columns:
 *    - id (auto-generated primary key)
 *    - group_id (nullable, references groups_table)
 *    - display_index
 *    - child_id (the ID of the item in its own table)
 *    - type (TRACKER, FUNCTION, GRAPH, GROUP, REMINDER)
 *    - created_at (epoch millis, 0 for migrated legacy data)
 * 2. Migrates existing group_id and display_index data from:
 *    - features_table where id NOT IN functions_table.feature_id -> type=TRACKER
 *    - features_table where id IN functions_table.feature_id     -> type=FUNCTION
 *    - graphs_and_stats_table2                                   -> type=GRAPH
 *    - groups_table (parent_group_id)                            -> type=GROUP
 *    - reminders_table                                           -> type=REMINDER
 *    All migrated entries get created_at=0 to indicate legacy data.
 * 3. Removes group_id/parent_group_id and display_index columns from:
 *    features_table, graphs_and_stats_table2, groups_table, and reminders_table
 */
val MIGRATION_58_59 = object : Migration(58, 59) {
    override fun migrate(db: SupportSQLiteDatabase) {
        createGroupItemsTable(db)
        migrateGroupItems(db)
        recreateFeaturesTable(db)
        recreateGroupsTable(db)
        recreateGraphsTable(db)
        recreateRemindersTable(db)
    }

    private fun createGroupItemsTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `group_items_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `group_id` INTEGER,
                `display_index` INTEGER NOT NULL,
                `child_id` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                FOREIGN KEY(`group_id`) REFERENCES `groups_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_group_items_table_group_id` ON `group_items_table` (`group_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_group_items_table_child_id_type` ON `group_items_table` (`child_id`, `type`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_group_items_table_group_id_child_id_type` ON `group_items_table` (`group_id`, `child_id`, `type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_group_items_table_id` ON `group_items_table` (`id`)")
    }

    private fun migrateGroupItems(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO `group_items_table` (`group_id`, `display_index`, `child_id`, `type`, `created_at`)
            SELECT ft.`group_id`, ft.`display_index`, t.`id`, 'TRACKER', 0
            FROM `features_table` ft
            JOIN `trackers_table` t ON t.`feature_id` = ft.`id`
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `group_items_table` (`group_id`, `display_index`, `child_id`, `type`, `created_at`)
            SELECT ft.`group_id`, ft.`display_index`, f.`id`, 'FUNCTION', 0
            FROM `features_table` ft
            JOIN `functions_table` f ON f.`feature_id` = ft.`id`
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `group_items_table` (`group_id`, `display_index`, `child_id`, `type`, `created_at`)
            SELECT `group_id`, `display_index`, `id`, 'GRAPH', 0
            FROM `graphs_and_stats_table2`
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `group_items_table` (`group_id`, `display_index`, `child_id`, `type`, `created_at`)
            SELECT `parent_group_id`, `display_index`, `id`, 'GROUP', 0
            FROM `groups_table`
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `group_items_table` (`group_id`, `display_index`, `child_id`, `type`, `created_at`)
            SELECT `group_id`, `display_index`, `id`, 'REMINDER', 0
            FROM `reminders_table`
            """.trimIndent()
        )
    }

    private fun recreateFeaturesTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `features_table_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `feature_description` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `features_table_new` (`id`, `name`, `feature_description`)
            SELECT `id`, `name`, `feature_description`
            FROM `features_table`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `features_table`")
        db.execSQL("ALTER TABLE `features_table_new` RENAME TO `features_table`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_features_table_id` ON `features_table` (`id`)")
    }

    private fun recreateGroupsTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `groups_table_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `color_index` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `groups_table_new` (`id`, `name`, `color_index`)
            SELECT `id`, `name`, `color_index`
            FROM `groups_table`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `groups_table`")
        db.execSQL("ALTER TABLE `groups_table_new` RENAME TO `groups_table`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_table_id` ON `groups_table` (`id`)")
    }

    private fun recreateGraphsTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `graphs_and_stats_table2_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `graph_stat_type` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `graphs_and_stats_table2_new` (`id`, `name`, `graph_stat_type`)
            SELECT `id`, `name`, `graph_stat_type`
            FROM `graphs_and_stats_table2`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `graphs_and_stats_table2`")
        db.execSQL("ALTER TABLE `graphs_and_stats_table2_new` RENAME TO `graphs_and_stats_table2`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_graphs_and_stats_table2_id` ON `graphs_and_stats_table2` (`id`)")
    }

    private fun recreateRemindersTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminders_table_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `feature_id` INTEGER,
                `encoded_reminder_params` TEXT NOT NULL,
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `reminders_table_new` (`id`, `name`, `feature_id`, `encoded_reminder_params`)
            SELECT `id`, `name`, `feature_id`, `encoded_reminder_params`
            FROM `reminders_table`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `reminders_table`")
        db.execSQL("ALTER TABLE `reminders_table_new` RENAME TO `reminders_table`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_table_id` ON `reminders_table` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_table_feature_id` ON `reminders_table` (`feature_id`)")
    }
}
