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
* along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.database

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

enum class YRangeType {
    DYNAMIC,
    FIXED
}

data class LineGraphFeature(
    var featureId: Long,
    var name: String,
    var colorIndex: Int,
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
    val duration: Duration?,

    @ColumnInfo(name = "y_range_type")
    val yRangeType: YRangeType,

    @ColumnInfo(name = "y_from")
    val yFrom: Double,

    @ColumnInfo(name = "y_to")
    val yTo: Double
) {
    companion object {
        fun create(id: Long, graphStatId: Long, features: List<LineGraphFeature>, duration: Duration?,
                   yRangeType: YRangeType, yFrom: Double, yTo: Double): LineGraph {
            val validFeatures = features
                .take(MAX_LINE_GRAPH_FEATURES)
                .map { f -> validateLineGraphFeature(f) }
            return LineGraph(id, graphStatId, validFeatures, duration, yRangeType, yFrom, yTo)
        }

        private fun validateLineGraphFeature(f: LineGraphFeature): LineGraphFeature {
            return f.copy(name = f.name
                .take(MAX_LINE_GRAPH_FEATURE_NAME_LENGTH)
                .replace(splitChars1, " ")
                .replace(splitChars2, " ")
            )
        }
    }
}
