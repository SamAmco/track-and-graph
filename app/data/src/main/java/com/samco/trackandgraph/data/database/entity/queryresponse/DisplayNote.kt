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

package com.samco.trackandgraph.data.database.entity.queryresponse

import androidx.room.ColumnInfo
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

internal data class DisplayNote(
    @ColumnInfo(name = "epoch_milli")
    val epochMilli: Long,

    @ColumnInfo(name = "utc_offset_sec")
    val utcOffsetSec: Int,

    @ColumnInfo(name = "tracker_id")
    val trackerId: Long?,

    @ColumnInfo(name = "feature_id")
    val featureId: Long?,

    @ColumnInfo(name = "feature_name")
    val featureName: String?,

    @ColumnInfo(name = "group_id")
    val groupId: Long?,

    @ColumnInfo(name = "note")
    val note: String
) {
    fun toDto() = com.samco.trackandgraph.data.database.dto.DisplayNote(
        timestamp = OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(epochMilli),
            ZoneOffset.ofTotalSeconds(utcOffsetSec)
        ),
        trackerId = trackerId,
        featureId = featureId,
        featureName = featureName,
        groupId = groupId,
        note = note
    )
}