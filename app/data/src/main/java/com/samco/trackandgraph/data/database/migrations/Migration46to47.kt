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


val MIGRATION_46_47 = object : Migration(46, 47) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `feature_timers_table` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `feature_id` INTEGER NOT NULL,
                    `start_instant` TEXT NOT NULL, 
                    FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_feature_timers_table_id` ON `feature_timers_table` (`id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_feature_timers_table_start_instant` ON `feature_timers_table` (`start_instant`)")
    }
}
