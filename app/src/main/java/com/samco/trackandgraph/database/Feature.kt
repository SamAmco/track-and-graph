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
package com.samco.trackandgraph.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.lang.Exception

@Entity(
    tableName = "features_table",
    foreignKeys = [ForeignKey(
        entity = TrackGroup::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("track_group_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Feature(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "track_group_id", index = true)
    val trackGroupId: Long,

    @ColumnInfo(name = "type")
    val featureType: FeatureType,

    @ColumnInfo(name = "discrete_values")
    val discreteValues: List<DiscreteValue>,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "has_default_value")
    val hasDefaultValue: Boolean,

    @ColumnInfo(name = "default_value")
    val defaultValue: Double,

    @ColumnInfo(name = "feature_description")
    val description: String
) {
    companion object {
        fun create(
            id: Long, name: String, trackGroupId: Long, featureType: FeatureType,
            discreteValues: List<DiscreteValue>, hasDefaultValue: Boolean, defaultValue: Double,
            displayIndex: Int, description: String
        ): Feature {
            discreteValues.forEach { dv -> validateDiscreteValue(dv) }
            val validName = name
                .take(MAX_FEATURE_NAME_LENGTH)
                .replace(splitChars1, " ")
                .replace(splitChars2, " ")
            return Feature(
                id, validName, trackGroupId, featureType, discreteValues,
                displayIndex, hasDefaultValue, defaultValue, description
            )
        }
    }
}

enum class FeatureType { DISCRETE, CONTINUOUS }

data class DiscreteValue(val index: Int, val label: String) {
    override fun toString() = "$index:$label"

    companion object {
        fun fromString(value: String): DiscreteValue {
            if (!value.contains(':')) throw Exception("value did not contain a colon")
            val label = value.substring(value.indexOf(':') + 1).trim()
            val index = value.substring(0, value.indexOf(':')).trim().toInt()
            val discreteValue = DiscreteValue(index, label)
            validateDiscreteValue(discreteValue)
            return discreteValue
        }

        fun fromDataPoint(dataPoint: DataPoint) =
            DiscreteValue(dataPoint.value.toInt(), dataPoint.label)
    }
}

fun validateDiscreteValue(discreteValue: DiscreteValue) {
    if (discreteValue.label.contains(splitChars1) || discreteValue.label.contains(splitChars2))
        throw Exception("Illegal discrete value name")
    if (discreteValue.label.length > MAX_LABEL_LENGTH)
        throw Exception("label size exceeded the maximum size allowed")
}