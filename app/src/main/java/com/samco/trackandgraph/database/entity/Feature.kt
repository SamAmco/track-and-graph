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
package com.samco.trackandgraph.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.samco.trackandgraph.database.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.lang.Exception

@Entity(
    tableName = "features_table",
    foreignKeys = [ForeignKey(
        entity = Group::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("group_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Feature(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "group_id", index = true)
    val groupId: Long?,

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
    fun getDefaultLabel(): String =
        if (featureType == FeatureType.DISCRETE)
            discreteValues.first { dv -> dv.index == defaultValue.toInt() }.label
        else ""

    companion object {
        fun create(
            id: Long, name: String, trackGroupId: Long, featureType: FeatureType,
            discreteValues: List<DiscreteValue>, hasDefaultValue: Boolean, defaultValue: Double,
            displayIndex: Int, description: String
        ): Feature {
            return Feature(
                id, name, trackGroupId, featureType, discreteValues,
                displayIndex, hasDefaultValue, defaultValue, description
            )
        }
    }
}

enum class FeatureType { DISCRETE, CONTINUOUS, DURATION }

@JsonClass(generateAdapter = true)
data class DiscreteValue(
    val index: Int,
    val label: String
) {

    //Ideally we wouldn't need fromString and toString here but they are still used by CSVReadWriter.
    override fun toString() = "$index:$label"

    companion object {
        fun fromString(value: String): DiscreteValue {
            if (!value.contains(':')) throw Exception("value did not contain a colon")
            val label = value.substring(value.indexOf(':') + 1).trim()
            val index = value.substring(0, value.indexOf(':')).trim().toInt()
            return DiscreteValue(index, label)
        }

        fun fromDataPoint(dataPoint: DataPoint) =
            DiscreteValue(
                dataPoint.value.toInt(),
                dataPoint.label
            )
    }
}