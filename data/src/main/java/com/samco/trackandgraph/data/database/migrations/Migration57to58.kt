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
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.ReminderParams
import kotlinx.serialization.json.Json
import org.threeten.bp.LocalTime

val MIGRATION_57_58 = object : Migration(57, 58) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val json = Json

        // Create new reminders table with foreign keys and encoded_reminder_params column
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `reminders_table_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `display_index` INTEGER NOT NULL, 
                `name` TEXT NOT NULL, 
                `group_id` INTEGER, 
                `feature_id` INTEGER, 
                `encoded_reminder_params` TEXT NOT NULL, 
                FOREIGN KEY(`group_id`) REFERENCES `groups_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent()
        )

        // Migrate existing data by reading each row and properly serializing the parameters
        val cursor =
            db.query("SELECT id, display_index, name, time, checked_days FROM reminders_table")

        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val displayIndex = cursor.getInt(1)
            val name = cursor.getString(2)
            val timeString = cursor.getString(3)
            val checkedDaysJson = cursor.getString(4)

            // Parse the existing data
            val time = LocalTime.parse(timeString)
            val checkedDays = json.decodeFromString<CheckedDays>(checkedDaysJson)

            // Create the proper ReminderParams structure
            val reminderParams = ReminderParams.WeekDayParams(
                time = time,
                checkedDays = checkedDays
            )

            // Serialize using strict Json serialization
            val encodedParams = json.encodeToString(ReminderParams.serializer(), reminderParams)

            // Insert into new table with null foreign keys for existing reminders
            db.execSQL(
                """
                    INSERT INTO reminders_table_new (id, display_index, name, group_id, feature_id, encoded_reminder_params)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                arrayOf<Any?>(id, displayIndex, name, null, null, encodedParams)
            )
        }

        cursor.close()

        // Drop old table
        db.execSQL("DROP TABLE reminders_table")

        // Rename new table to original name
        db.execSQL("ALTER TABLE reminders_table_new RENAME TO reminders_table")

        // Create indices for new table
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_table_id` ON `reminders_table` (`id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_table_group_id` ON `reminders_table` (`group_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_table_feature_id` ON `reminders_table` (`feature_id`)")
    }
}
