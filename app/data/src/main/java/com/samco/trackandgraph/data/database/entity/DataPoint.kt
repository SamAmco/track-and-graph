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
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.samco.trackandgraph.data.database.dto.DataPoint
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

@Entity(
    tableName = "data_points_table",
    primaryKeys = ["epoch_milli", "feature_id"],
    foreignKeys = [
        ForeignKey(
            entity = Feature::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("feature_id"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
internal data class DataPoint(
    @ColumnInfo(name = "epoch_milli", index = true)
    val epochMilli: Long,

    @ColumnInfo(name = "feature_id", index = true)
    val featureId: Long,

    @ColumnInfo(name = "utc_offset_sec")
    val utcOffsetSec: Int,

    @ColumnInfo(name = "value")
    val value: Double,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "note")
    val note: String
) {
    fun toDto() = DataPoint(
        OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(epochMilli),
            ZoneOffset.ofTotalSeconds(utcOffsetSec)
        ),
        featureId,
        value,
        label,
        note
    )
}