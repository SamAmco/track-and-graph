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
import com.squareup.moshi.Types


val MIGRATION_42_43 = object : Migration(42, 43) {
    private val helper = MigrationMoshiHelper.getMigrationMoshiHelper()

    override fun migrate(database: SupportSQLiteDatabase) {
        updateDiscreteValues(database)
        updateCheckedDays(database)
    }

    private fun updateCheckedDays(database: SupportSQLiteDatabase) {
        val remindersCursor = database.query("SELECT * FROM reminders_table")
        val updates = mutableListOf<List<String>>()
        while (remindersCursor.moveToNext()) {
            val id = remindersCursor.getString(0)
            val checkedDays = CheckedDays.fromList(
                remindersCursor.getString(4)
                    .split("||")
                    .map { it.toBoolean() }
            )

            val jsonString = helper.moshi.adapter(CheckedDays::class.java).toJson(checkedDays) ?: ""
            updates.add(listOf(jsonString, id))
        }
        if (updates.size > 0) updates.forEach {
            val featureUpdateStatement =
                """UPDATE reminders_table 
                    SET checked_days = ?
                    WHERE id = ?
            """.trimIndent()
            database.execSQL(featureUpdateStatement, it.toTypedArray())
        }
    }

    private fun updateDiscreteValues(database: SupportSQLiteDatabase) {
        val featureCursor = database.query("SELECT * FROM features_table WHERE type = 0")
        val updates = mutableListOf<List<String>>()
        val deletes = mutableListOf<String>()
        while (featureCursor.moveToNext()) {
            val id = featureCursor.getString(0)
            val discreteValuesString = featureCursor.getString(4)
            val discreteValues = try {
                discreteValuesString
                    .split("||")
                    .map { MigrationMoshiHelper.DiscreteValue.fromString(it) }
            } catch (e: Exception) {
                deletes.add(id)
                continue
            }
            val listType = Types.newParameterizedType(
                List::class.java,
                MigrationMoshiHelper.DiscreteValue::class.java
            )
            val jsonString =
                helper.moshi.adapter<List<MigrationMoshiHelper.DiscreteValue>>(listType)
                    .toJson(discreteValues) ?: ""
            updates.add(listOf(jsonString, id))
        }
        if (deletes.size > 0) deletes.forEach {
            database.execSQL("""DELETE FROM features_table WHERE id = ?""", arrayOf(it))
        }
        if (updates.size > 0) updates.forEach {
            val featureUpdateStatement =
                """UPDATE features_table 
                    SET discrete_values = ?
                    WHERE id = ?
            """.trimIndent()
            database.execSQL(featureUpdateStatement, it.toTypedArray())
        }
    }
}
