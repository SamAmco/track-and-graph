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
import kotlin.math.abs
import kotlin.math.floor

val MIGRATION_48_49 = object : Migration(48, 49) {
    override fun migrate(database: SupportSQLiteDatabase) {
        createFeatureTimerIndex(database)

        createNewTrackersTable(database)
        copyTrackersToNewTable(database)
        deleteOldTrackersTableAndCreateIndexes(database)
    }

    private fun deleteOldTrackersTableAndCreateIndexes(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS `trackers_table_old`")
        val tableName = "trackers_table"
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_trackers_table_id` ON `${tableName}` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_trackers_table_feature_id` ON `${tableName}` (`feature_id`)")
    }

    private fun copyTrackersToNewTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                INSERT INTO trackers_table
                SELECT 
                    id, 
                    feature_id, 
                    case type when 0 then 0 when 1 then 0 when 2 then 1 end as type ,
                    has_default_value,
                    default_value,
                    "" as default_label,
                    case trackers_table_old.type when 0 then "VALUE_AND_LABEL" when 1 then "NONE" when 2 then "NONE" end as suggestion_type,
                    "VALUE_ASCENDING" as suggestion_order
                FROM trackers_table_old
            """.trimIndent()
        )
        val cursor = database.query("SELECT * FROM trackers_table_old WHERE has_default_value = 1")
        val moshi = MigrationMoshiHelper.getMigrationMoshiHelper()
        val updates = mutableListOf<Pair<Long, String>>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val discreteValues = moshi.stringToListOfDiscreteValues(cursor.getString(3))
            val defaultValue = cursor.getDouble(5)
            val index = floor(defaultValue).toInt()
            if (abs(defaultValue - index) < 0.0001 && index in discreteValues.indices) {
                updates.add(Pair(id, discreteValues[index].label))
            }
        }
        for (update in updates) {
            database.execSQL(
                "UPDATE trackers_table SET default_label=? WHERE id=?",
                arrayOf(update.second, update.first)
            )
        }
    }

    private fun createNewTrackersTable(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE trackers_table RENAME TO trackers_table_old")
        val tableName = "trackers_table"
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `${tableName}` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `feature_id` INTEGER NOT NULL, 
                    `type` INTEGER NOT NULL, 
                    `has_default_value` INTEGER NOT NULL, 
                    `default_value` REAL NOT NULL, 
                    `default_label` TEXT NOT NULL, 
                    `suggestion_type` TEXT NOT NULL, 
                    `suggestion_order` TEXT NOT NULL, 
                    FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimMargin()
        )

    }

    private fun createFeatureTimerIndex(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_feature_timers_table_feature_id` ON `feature_timers_table` (`feature_id`)")
    }
}
