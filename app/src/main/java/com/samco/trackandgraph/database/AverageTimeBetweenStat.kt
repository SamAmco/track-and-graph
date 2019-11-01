package com.samco.trackandgraph.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.threeten.bp.Duration

@Entity(tableName = "average_time_between_stat_table",
    foreignKeys = [
        ForeignKey(
            entity = GraphOrStat::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("graph_stat_id"),
            onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = Feature::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("feature_id"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AverageTimeBetweenStat(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "graph_stat_id", index = true)
    val graphStatId: Long,

    @ColumnInfo(name = "feature_id", index = true)
    val featureId: Long,

    @ColumnInfo(name = "from_value")
    val fromValue: String,

    @ColumnInfo(name = "to_value")
    val toValue: String,

    @ColumnInfo(name = "duration")
    val duration: Duration?
)

