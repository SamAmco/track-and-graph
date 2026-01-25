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
import com.samco.trackandgraph.data.database.dto.LayoutItemType

val MIGRATION_58_59 = object : Migration(58, 59) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create the new layout_items_table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `layout_items_table` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `group_id` INTEGER NOT NULL,
                `type` INTEGER NOT NULL,
                `item_id` INTEGER NOT NULL,
                `display_index` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_layout_items_table_id` ON `layout_items_table` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_layout_items_table_group_id` ON `layout_items_table` (`group_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_layout_items_table_item_id_type` ON `layout_items_table` (`item_id`, `type`)")

        // Migrate trackers (features that have a corresponding entry in trackers_table)
        db.execSQL(
            """
            INSERT INTO layout_items_table (group_id, type, item_id, display_index)
            SELECT f.group_id, ${LayoutItemType.TRACKER.ordinal}, f.id, f.display_index
            FROM features_table f
            INNER JOIN trackers_table t ON t.feature_id = f.id
            """.trimIndent()
        )

        // Migrate functions (features that have a corresponding entry in functions_table)
        db.execSQL(
            """
            INSERT INTO layout_items_table (group_id, type, item_id, display_index)
            SELECT f.group_id, ${LayoutItemType.FUNCTION.ordinal}, f.id, f.display_index
            FROM features_table f
            INNER JOIN functions_table fn ON fn.feature_id = f.id
            """.trimIndent()
        )

        // Migrate groups (subgroups, not the root group)
        db.execSQL(
            """
            INSERT INTO layout_items_table (group_id, type, item_id, display_index)
            SELECT g.parent_group_id, ${LayoutItemType.GROUP.ordinal}, g.id, g.display_index
            FROM groups_table g
            WHERE g.parent_group_id IS NOT NULL
            """.trimIndent()
        )

        // Migrate graphs
        db.execSQL(
            """
            INSERT INTO layout_items_table (group_id, type, item_id, display_index)
            SELECT gs.group_id, ${LayoutItemType.GRAPH.ordinal}, gs.id, gs.display_index
            FROM graphs_and_stats_table2 gs
            """.trimIndent()
        )

        // Migrate reminders with group_id = -1 (they currently don't belong to groups)
        db.execSQL(
            """
            INSERT INTO layout_items_table (group_id, type, item_id, display_index)
            SELECT -1, ${LayoutItemType.REMINDER.ordinal}, r.id, r.display_index
            FROM reminders_table r
            """.trimIndent()
        )

        // Now remove display_index from features_table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `features_table_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `group_id` INTEGER NOT NULL,
                `feature_description` TEXT NOT NULL,
                FOREIGN KEY(`group_id`) REFERENCES `groups_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO features_table_new (id, name, group_id, feature_description)
            SELECT id, name, group_id, feature_description
            FROM features_table
            """.trimIndent()
        )
        db.execSQL("DROP TABLE features_table")
        db.execSQL("ALTER TABLE features_table_new RENAME TO features_table")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_features_table_id` ON `features_table` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_features_table_group_id` ON `features_table` (`group_id`)")

        // Remove display_index from groups_table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `groups_table_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `parent_group_id` INTEGER,
                `color_index` INTEGER NOT NULL,
                FOREIGN KEY(`parent_group_id`) REFERENCES `groups_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO groups_table_new (id, name, parent_group_id, color_index)
            SELECT id, name, parent_group_id, color_index
            FROM groups_table
            """.trimIndent()
        )
        db.execSQL("DROP TABLE groups_table")
        db.execSQL("ALTER TABLE groups_table_new RENAME TO groups_table")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_table_id` ON `groups_table` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_groups_table_parent_group_id` ON `groups_table` (`parent_group_id`)")

        // Remove display_index from graphs_and_stats_table2
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `graphs_and_stats_table2_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `group_id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `graph_stat_type` INTEGER NOT NULL,
                FOREIGN KEY(`group_id`) REFERENCES `groups_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO graphs_and_stats_table2_new (id, group_id, name, graph_stat_type)
            SELECT id, group_id, name, graph_stat_type
            FROM graphs_and_stats_table2
            """.trimIndent()
        )
        db.execSQL("DROP TABLE graphs_and_stats_table2")
        db.execSQL("ALTER TABLE graphs_and_stats_table2_new RENAME TO graphs_and_stats_table2")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_graphs_and_stats_table2_id` ON `graphs_and_stats_table2` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_graphs_and_stats_table2_group_id` ON `graphs_and_stats_table2` (`group_id`)")

        // Remove display_index from reminders_table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminders_table_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `group_id` INTEGER,
                `feature_id` INTEGER,
                `encoded_reminder_params` TEXT NOT NULL,
                FOREIGN KEY(`group_id`) REFERENCES `groups_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO reminders_table_new (id, name, group_id, feature_id, encoded_reminder_params)
            SELECT id, name, group_id, feature_id, encoded_reminder_params
            FROM reminders_table
            """.trimIndent()
        )
        db.execSQL("DROP TABLE reminders_table")
        db.execSQL("ALTER TABLE reminders_table_new RENAME TO reminders_table")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_table_id` ON `reminders_table` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_table_group_id` ON `reminders_table` (`group_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_table_feature_id` ON `reminders_table` (`feature_id`)")
    }
}
