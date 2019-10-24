package com.samco.grapheasy.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.threeten.bp.Duration

enum class LineGraphFeatureMode {
    TRACKED_VALUES,
    DAILY_MOVING_AVERAGE,
    THREE_DAY_MOVING_AVERAGE,
    WEEKLY_MOVING_AVERAGE,
    MONTHLY_MOVING_AVERAGE,
    THREE_MONTH_MOVING_AVERAGE,
    SIX_MONTH_MOVING_AVERAGE,
    YEARLY_MOVING_AVERAGE
}


val movingAverageDurations = mapOf(
    LineGraphFeatureMode.TRACKED_VALUES to null,
    LineGraphFeatureMode.DAILY_MOVING_AVERAGE to Duration.ofDays(1),
    LineGraphFeatureMode.THREE_DAY_MOVING_AVERAGE to Duration.ofDays(3),
    LineGraphFeatureMode.WEEKLY_MOVING_AVERAGE to Duration.ofDays(7),
    LineGraphFeatureMode.MONTHLY_MOVING_AVERAGE to Duration.ofDays(31),
    LineGraphFeatureMode.THREE_MONTH_MOVING_AVERAGE to Duration.ofDays(93),
    LineGraphFeatureMode.SIX_MONTH_MOVING_AVERAGE to Duration.ofDays(183),
    LineGraphFeatureMode.YEARLY_MOVING_AVERAGE to Duration.ofDays(365)
)

class LineGraphFeature(var featureId: Long, var colorId: Int, var mode: LineGraphFeatureMode, var offset: Double, var scale: Double)

@Entity(tableName = "line_graphs_table",
    foreignKeys = [ForeignKey(entity = GraphOrStat::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("graph_stat_id"),
        onDelete = ForeignKey.CASCADE)]
)
data class LineGraph(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "graph_stat_id")
    val graphStatId: Long,

    @ColumnInfo(name = "features")
    val features: List<LineGraphFeature>,

    @ColumnInfo(name = "duration")
    val duration: Duration?
)
