package com.samco.grapheasy.database

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
    var id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "track_group_id", index = true)
    val trackGroupId: Long,

    @ColumnInfo(name = "type")
    val featureType: FeatureType,

    @ColumnInfo(name = "discrete_values")
    val discreteValues: List<DiscreteValue>
)

enum class FeatureType { DISCRETE, CONTINUOUS }

const val MAX_FEATURE_NAME_LENGTH = 20
const val MAX_LABEL_LENGTH = 20
const val MAX_DISCRETE_VALUES_PER_FEATURE = 10
//TODO add a max length for a track group name
//TODO add a max length for a graph name

data class DiscreteValue (val index: Int, val label: String) {
    override fun toString() = "$index:$label"
    companion object {
        fun fromString(value: String): DiscreteValue {
            if (!value.contains(':')) throw Exception("value did not contain a colon")
            val label = value.substring(value.indexOf(':')+1).trim()
            if (label.length > MAX_LABEL_LENGTH) throw Exception("label size exceeded the maximum size allowed")
            val index = value.substring(0, value.indexOf(':')).trim().toIntOrNull()
                ?: throw Exception("could not get index from value")
            return DiscreteValue(index, label)
        }
        fun fromDataPoint(dataPoint: DataPoint) = DiscreteValue(dataPoint.value.toInt(), dataPoint.label)
    }
}