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

package com.samco.trackandgraph.base.database.entity.queryresponse

import androidx.room.ColumnInfo
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.DisplayTracker
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime

internal data class DisplayTracker(
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "feature_id")
    var featureId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "type")
    val featureType: DataType = DataType.CONTINUOUS,

    @ColumnInfo(name = "has_default_value")
    val hasDefaultValue: Boolean,

    @ColumnInfo(name = "default_value")
    val defaultValue: Double,

    @ColumnInfo(name = "default_label")
    val defaultLabel: String,

    @ColumnInfo(name = "last_timestamp")
    val timestamp: OffsetDateTime?,

    @ColumnInfo(name = "num_data_points")
    val numDataPoints: Long?,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "feature_description")
    val description: String,

    @ColumnInfo(name = "start_instant")
    val timerStartInstant: Instant?
) {
    fun toDto() = DisplayTracker(
        id = id,
        featureId = featureId,
        name = name,
        groupId = groupId,
        dataType = featureType,
        hasDefaultValue = hasDefaultValue,
        defaultValue = defaultValue,
        defaultLabel = defaultLabel,
        timestamp = timestamp,
        numDataPoints = numDataPoints,
        displayIndex = displayIndex,
        description = description,
        timerStartInstant = timerStartInstant
    )
}