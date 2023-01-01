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

package com.samco.trackandgraph.base.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.samco.trackandgraph.base.database.dto.DataType

internal enum class TrackerSuggestionType {
    VALUE_AND_LABEL,
    VALUE_ONLY,
    LABEL_ONLY
}

internal enum class TrackerSuggestionOrder {
    VALUE_ASCENDING,
    VALUE_DESCENDING,
    LABEL_ASCENDING,
    LABEL_DESCENDING,
    LATEST,
    OLDEST,
}

@Entity(
    tableName = "trackers_table",
    foreignKeys = [ForeignKey(
        entity = Feature::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("feature_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
internal data class Tracker(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "feature_id", index = true)
    val featureId: Long,

    @ColumnInfo(name = "type")
    val dataType: DataType,

    @ColumnInfo(name = "has_default_value")
    val hasDefaultValue: Boolean,

    @ColumnInfo(name = "default_value")
    val defaultValue: Double,

    @ColumnInfo(name = "default_label")
    val defaultLabel: String,

    @ColumnInfo(name = "suggestion_type")
    val suggestionType: TrackerSuggestionType,

    @ColumnInfo(name = "suggestion_order")
    val suggestionOrder: TrackerSuggestionOrder
)