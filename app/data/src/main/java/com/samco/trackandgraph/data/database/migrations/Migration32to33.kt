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


val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val lineGraphsCursor = database.query("SELECT * FROM line_graphs_table")
        val updates = mutableListOf<Pair<String, List<String>>>()
        while (lineGraphsCursor.moveToNext()) {
            val id = lineGraphsCursor.getLong(0)
            val oldFeaturesString = lineGraphsCursor.getString(2)
            val oldFeatures = oldFeaturesString.split("||")
            val newFeatures = oldFeatures.map { f ->
                val params = f.split("!!").toMutableList()
                var plottingMode = params[4].toInt()
                if (plottingMode > 0) plottingMode += 1
                params[4] = plottingMode.toString()
                params.joinToString("!!")
            }
            val newFeaturesString = newFeatures.joinToString("||")
            updates.add(
                Pair(
                    "UPDATE line_graphs_table SET features=? WHERE id=?",
                    listOf(newFeaturesString, id.toString())
                )
            )
        }
        if (updates.size > 0) updates.forEach {
            database.execSQL(
                it.first,
                it.second.toTypedArray()
            )
        }
    }
}
