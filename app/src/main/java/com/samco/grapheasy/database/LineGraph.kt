package com.samco.grapheasy.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.threeten.bp.Duration
import org.threeten.bp.Period

enum class LineGraphAveraginModes {
    NO_AVERAGING,
    DAILY_MOVING_AVERAGE,
    THREE_DAY_MOVING_AVERAGE,
    WEEKLY_MOVING_AVERAGE,
    MONTHLY_MOVING_AVERAGE,
    THREE_MONTH_MOVING_AVERAGE,
    SIX_MONTH_MOVING_AVERAGE,
    YEARLY_MOVING_AVERAGE
}

enum class LineGraphPlottingModes {
    WHEN_TRACKED,
    GENERATE_DAILY_TOTALS,
    GENERATE_WEEKLY_TOTALS,
    GENERATE_MONTHLY_TOTALS,
    GENERATE_YEARLY_TOTALS
}

val plottingModePeriods = mapOf(
    LineGraphPlottingModes.WHEN_TRACKED to null,
    LineGraphPlottingModes.GENERATE_DAILY_TOTALS to Period.ofDays(1),
    LineGraphPlottingModes.GENERATE_WEEKLY_TOTALS to Period.ofWeeks(1),
    LineGraphPlottingModes.GENERATE_MONTHLY_TOTALS to Period.ofMonths(1),
    LineGraphPlottingModes.GENERATE_YEARLY_TOTALS to Period.ofYears(1)
)

val movingAverageDurations = mapOf(
    LineGraphAveraginModes.NO_AVERAGING to null,
    LineGraphAveraginModes.DAILY_MOVING_AVERAGE to Duration.ofDays(1),
    LineGraphAveraginModes.THREE_DAY_MOVING_AVERAGE to Duration.ofDays(3),
    LineGraphAveraginModes.WEEKLY_MOVING_AVERAGE to Duration.ofDays(7),
    LineGraphAveraginModes.MONTHLY_MOVING_AVERAGE to Duration.ofDays(31),
    LineGraphAveraginModes.THREE_MONTH_MOVING_AVERAGE to Duration.ofDays(93),
    LineGraphAveraginModes.SIX_MONTH_MOVING_AVERAGE to Duration.ofDays(183),
    LineGraphAveraginModes.YEARLY_MOVING_AVERAGE to Duration.ofDays(365)
)

class LineGraphFeature(
    var featureId: Long,
    var name: String,
    var colorId: Int,
    var averagingMode : LineGraphAveraginModes,
    var plottingMode: LineGraphPlottingModes,
    var offset: Double,
    var scale: Double
)

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

    @ColumnInfo(name = "graph_stat_id", index = true)
    val graphStatId: Long,

    @ColumnInfo(name = "features")
    val features: List<LineGraphFeature>,

    @ColumnInfo(name = "duration")
    val duration: Duration?
)
