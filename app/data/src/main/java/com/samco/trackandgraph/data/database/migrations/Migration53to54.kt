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
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter

val MIGRATION_53_54 = object : Migration(53, 54) {
    override fun migrate(database: SupportSQLiteDatabase) {
        migrateDataPointsTable(database)
        migrateGlobalNotesTable(database)
    }

    private fun migrateDataPointsTable(database: SupportSQLiteDatabase) {
        //rename data_points_table to data_points_table_old
        database.execSQL("ALTER TABLE data_points_table RENAME TO data_points_table_old")

        //create new data_points_table
        database.execSQL(
            """
           CREATE TABLE IF NOT EXISTS `data_points_table` (
               `epoch_milli` INTEGER NOT NULL,
               `feature_id` INTEGER NOT NULL, 
               `utc_offset_sec` INTEGER NOT NULL, 
               `value` REAL NOT NULL, 
               `label` TEXT NOT NULL, 
               `note` TEXT NOT NULL, 
               PRIMARY KEY(`epoch_milli`, `feature_id`), 
               FOREIGN KEY(`feature_id`) REFERENCES `features_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
           )
        """.trimIndent()
        )

        //Copy data across
        val cursor = database.query("SELECT * FROM data_points_table_old")
        cursor.moveToFirst()
        val count = cursor.count

        for (i in 0 until count) {
            val odtString = cursor.getString(0)
            val featureId = cursor.getLong(1)
            val value = cursor.getDouble(2)
            val label = cursor.getString(3)
            val note = cursor.getString(4)

            val odt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(odtString, OffsetDateTime::from)
            val epochMilli = odt.toInstant().toEpochMilli()
            val utcOffsetSec = odt.offset.totalSeconds

            database.execSQL(
                """
                    INSERT INTO data_points_table (epoch_milli, feature_id, utc_offset_sec, value, label, note)
                    VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(epochMilli, featureId, utcOffsetSec, value, label, note)
            )

            cursor.moveToNext()
        }

        //drop the old table
        database.execSQL("DROP TABLE data_points_table_old")

        //re-create indexes
        database.execSQL(
            """
           CREATE INDEX IF NOT EXISTS `index_data_points_table_epoch_milli` ON `data_points_table` (`epoch_milli`)
        """.trimIndent()
        )

        database.execSQL(
            """
           CREATE INDEX IF NOT EXISTS `index_data_points_table_feature_id` ON `data_points_table` (`feature_id`)
        """.trimIndent()
        )
    }

    private fun migrateGlobalNotesTable(database: SupportSQLiteDatabase) {
        //rename notes_table to notes_table_old
        database.execSQL("ALTER TABLE notes_table RENAME TO notes_table_old")

        //create new notes_table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notes_table` (
                `epoch_milli` INTEGER NOT NULL,
                `utc_offset_sec` INTEGER NOT NULL, 
                `note` TEXT NOT NULL, 
                PRIMARY KEY(`epoch_milli`)
            )
        """.trimIndent()
        )

        //copy data across
        val cursor = database.query("SELECT * FROM notes_table_old")
        cursor.moveToFirst()
        val count = cursor.count

        for (i in 0 until count) {
            val odtString = cursor.getString(0)
            val note = cursor.getString(1)

            val odt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(odtString, OffsetDateTime::from)
            val epochMilli = odt.toInstant().toEpochMilli()
            val utcOffsetSec = odt.offset.totalSeconds

            database.execSQL(
                """
                    INSERT INTO notes_table (epoch_milli, utc_offset_sec, note)
                    VALUES (?, ?, ?)
                """.trimIndent(),
                arrayOf(epochMilli, utcOffsetSec, note)
            )

            cursor.moveToNext()
        }

        //drop old table
        database.execSQL("DROP TABLE notes_table_old")

        //create index
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_notes_table_epoch_milli` ON `notes_table` (`epoch_milli`)
        """.trimIndent()
        )
    }
}
