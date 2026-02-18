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
 *    - type (FEATURE, GRAPH, GROUP, REMINDER)
 *    - created_at (epoch millis, 0 for migrated legacy data)
 * 2. Migrates existing groupId and displayIndex data from:
 *    - features_table -> type=FEATURE
 *    - graphs_and_stats_table2 -> type=GRAPH
 *    - groups_table (parentGroupId) -> type=GROUP
 *    - reminders_table -> type=REMINDER
 *    All migrated entries get created_at=0 to indicate legacy data.
 * 3. Removes groupId/parentGroupId and displayIndex columns from existing tables
 *
 * TODO: Implement the actual migration logic
 */
val MIGRATION_58_59 = object : Migration(58, 59) {
    override fun migrate(db: SupportSQLiteDatabase) {
        throw NotImplementedError("Migration 58 to 59 not yet implemented")
    }
}
