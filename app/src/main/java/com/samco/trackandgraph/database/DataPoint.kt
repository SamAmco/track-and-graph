/* 
* This file is part of Track & Graph
* 
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "data_points_table",
        primaryKeys = ["timestamp", "feature_id"],
        foreignKeys = [
            androidx.room.ForeignKey(
                entity = Feature::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("feature_id"),
                onDelete = ForeignKey.CASCADE
            )
    ]
)
data class DataPoint (
    @ColumnInfo(name = "timestamp")
    val timestamp: OffsetDateTime = OffsetDateTime.now(),

    @ColumnInfo(name = "feature_id", index = true)
    val featureId: Long,

    @ColumnInfo(name = "value")
    val value: Double,

    @ColumnInfo(name = "label")
    val label: String

) {
    fun getDisplayTimestamp(): String = displayFeatureDateFormat.format(timestamp)
    fun getDisplayValue(): String {
        return if (label.isNotEmpty()) doubleFormatter.format(value) + " : $label"
            else doubleFormatter.format(value)
    }
}