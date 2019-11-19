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