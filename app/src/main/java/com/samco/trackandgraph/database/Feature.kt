package com.samco.trackandgraph.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.lang.Exception

@Entity(tableName = "features_table",
    foreignKeys = [ForeignKey(entity = TrackGroup::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("track_group_id"),
        onDelete = ForeignKey.CASCADE)]
)
data class Feature (
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
    val displayIndex: Int
) {
    companion object {
        fun create(id: Long, name: String, trackGroupId: Long, featureType: FeatureType,
                   discreteValues: List<DiscreteValue>, displayIndex: Int): Feature {
            discreteValues.forEach { dv -> validateDiscreteValue(dv) }
            val validName = name
                .take(MAX_FEATURE_NAME_LENGTH)
                .replace(splitChars1, " ")
                .replace(splitChars2, " ")
            return Feature(id, validName, trackGroupId, featureType, discreteValues, displayIndex)
        }
    }
}

enum class FeatureType { DISCRETE, CONTINUOUS }

data class DiscreteValue (val index: Int, val label: String) {
    override fun toString() = "$index:$label"
    companion object {
        fun fromString(value: String): DiscreteValue {
            if (!value.contains(':')) throw Exception("value did not contain a colon")
            val label = value.substring(value.indexOf(':')+1).trim()
            val index = value.substring(0, value.indexOf(':')).trim().toInt()
            val discreteValue = DiscreteValue(index, label)
            validateDiscreteValue(discreteValue)
            return discreteValue
        }
        fun fromDataPoint(dataPoint: DataPoint) = DiscreteValue(dataPoint.value.toInt(), dataPoint.label)
    }
}

fun validateDiscreteValue(discreteValue: DiscreteValue) {
    if (discreteValue.label.contains(splitChars1) || discreteValue.label.contains(splitChars2))
        throw Exception("Illegal discrete value name")
    if (discreteValue.label.length > MAX_LABEL_LENGTH)
        throw Exception("label size exceeded the maximum size allowed")
}