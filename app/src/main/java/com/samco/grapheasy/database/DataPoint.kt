package com.samco.grapheasy.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "data_points_table",
        foreignKeys = [
            androidx.room.ForeignKey(
                entity = Feature::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("featureId"),
                onDelete = ForeignKey.CASCADE
            )
    ]
)
class DataPoint (
    @PrimaryKey(autoGenerate = true)
    var id: Long = -1L,

    @ColumnInfo(name = "featureId")
    val featureId: Long,

    @ColumnInfo(name = "value")
    val value: String = "",

    @ColumnInfo(name = "timestamp")
    val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    fun getDisplayTimestamp() = displayFormatter.format(timestamp)
}