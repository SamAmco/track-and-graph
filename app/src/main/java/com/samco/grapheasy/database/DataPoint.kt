package com.samco.grapheasy.database

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

    //TODO should we convert this to a number?
    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "label")
    val label: String

) {
    fun getDisplayTimestamp(): String = displayFeatureDateFormat.format(timestamp)
    fun getDisplayValue(): String {
        var ans = value
        if (label.isNotEmpty()) ans += " : $label"
        return ans
    }
}