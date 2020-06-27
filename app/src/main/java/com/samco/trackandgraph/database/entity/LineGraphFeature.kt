/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.samco.trackandgraph.R
import org.threeten.bp.Duration
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAmount

enum class DurationPlottingMode {
    NONE,
    DURATION_IF_POSSIBLE,
    HOURS,
    MINUTES,
    SECONDS
}

enum class LineGraphPlottingModes {
    WHEN_TRACKED,
    GENERATE_HOURLY_TOTALS,
    GENERATE_DAILY_TOTALS,
    GENERATE_WEEKLY_TOTALS,
    GENERATE_MONTHLY_TOTALS,
    GENERATE_YEARLY_TOTALS
}

enum class LineGraphPointStyle {
    NONE,
    CIRCLES,
    CIRCLES_AND_NUMBERS
}

val plottingModePeriods: Map<LineGraphPlottingModes, TemporalAmount?> = mapOf(
    LineGraphPlottingModes.WHEN_TRACKED to null,
    LineGraphPlottingModes.GENERATE_HOURLY_TOTALS to Duration.ofHours(1),
    LineGraphPlottingModes.GENERATE_DAILY_TOTALS to Period.ofDays(1),
    LineGraphPlottingModes.GENERATE_WEEKLY_TOTALS to Period.ofWeeks(1),
    LineGraphPlottingModes.GENERATE_MONTHLY_TOTALS to Period.ofMonths(1),
    LineGraphPlottingModes.GENERATE_YEARLY_TOTALS to Period.ofYears(1)
)

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

val pointStyleDrawableResources = listOf(
    R.drawable.point_style_none_icon,
    R.drawable.point_style_circles_icon,
    R.drawable.point_style_circles_and_numbers_icon
)

@Entity(
    tableName = "line_graph_features_table2",
    foreignKeys = [ForeignKey(
        entity = LineGraph::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("line_graph_id"),
        onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
        entity = Feature::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("feature_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class LineGraphFeature(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "line_graph_id", index = true)
    val lineGraphId: Long,

    @ColumnInfo(name = "feature_id")
    val featureId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color_index")
    val colorIndex: Int,

    @ColumnInfo(name = "averaging_mode")
    val averagingMode: LineGraphAveraginModes,

    @ColumnInfo(name = "plotting_mode")
    val plottingMode: LineGraphPlottingModes,

    @ColumnInfo(name = "point_style")
    val pointStyle: LineGraphPointStyle,

    @ColumnInfo(name = "offset")
    val offset: Double,

    @ColumnInfo(name = "scale")
    val scale: Double,

    @ColumnInfo(name = "duration_plotting_mode")
    val durationPlottingMode: DurationPlottingMode
)
