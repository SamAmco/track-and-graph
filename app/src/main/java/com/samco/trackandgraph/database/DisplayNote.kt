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

package com.samco.trackandgraph.database

import androidx.room.ColumnInfo
import org.threeten.bp.OffsetDateTime

enum class NoteType {
    DATA_POINT,
    GLOBAL_NOTE
}

data class DisplayNote(
    @ColumnInfo(name = "timestamp")
    val timestamp: OffsetDateTime = OffsetDateTime.now(),

    @ColumnInfo(name = "note_type")
    val noteType: NoteType,

    @ColumnInfo(name = "feature_id")
    val featureId: Long?,

    @ColumnInfo(name = "feature_name")
    val featureName: String?,

    @ColumnInfo(name = "track_group_name")
    val trackGroupName: String?,

    @ColumnInfo(name = "note")
    val note: String
)