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


val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE average_time_between_stat_table ADD discrete_values TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE time_since_last_stat_table ADD discrete_values TEXT NOT NULL DEFAULT ''")
        val updates = mutableListOf<Pair<String, List<String>>>()
        val avTimeCursor = db.query("SELECT * FROM average_time_between_stat_table")
        while (avTimeCursor.moveToNext()) {
            val id = avTimeCursor.getLong(0)
            val from = avTimeCursor.getString(3)
            val to = avTimeCursor.getString(4)
            val discreteValues = from + "||" + to
            val query = "UPDATE average_time_between_stat_table SET discrete_values=? WHERE id=?"
            val args = listOf(discreteValues, id.toString())
            updates.add(Pair(query, args))
        }
        val timeSinceCursor = db.query("SELECT * FROM time_since_last_stat_table")
        while (timeSinceCursor.moveToNext()) {
            val id = timeSinceCursor.getLong(0)
            val from = timeSinceCursor.getString(3)
            val to = timeSinceCursor.getString(4)
            val discreteValues = from + "||" + to
            val query = "UPDATE time_since_last_stat_table SET discrete_values=? WHERE id=?"
            val args = listOf(discreteValues, id.toString())
            updates.add(Pair(query, args))
        }
        if (updates.size > 0) updates.forEach {
            db.execSQL(
                it.first,
                it.second.toTypedArray()
            )
        }
    }
}
