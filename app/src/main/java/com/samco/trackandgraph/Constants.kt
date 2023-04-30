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

package com.samco.trackandgraph

import com.samco.trackandgraph.base.database.dto.LineGraphAveraginModes
import com.samco.trackandgraph.base.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.base.database.dto.TimeHistogramWindow
import org.threeten.bp.Duration
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAmount

data class TimeHistogramWindowData(
    val window: TimeHistogramWindow,
    val period: TemporalAmount,
    val numBins: Int
) {
    companion object {
        fun getWindowData(window: TimeHistogramWindow): TimeHistogramWindowData = when (window) {
            TimeHistogramWindow.HOUR -> TimeHistogramWindowData(
                window = TimeHistogramWindow.HOUR,
                period = Duration.ofHours(1),
                numBins = 60
            )
            TimeHistogramWindow.DAY -> TimeHistogramWindowData(
                window = TimeHistogramWindow.DAY,
                period = Period.ofDays(1),
                numBins = 24
            )
            TimeHistogramWindow.WEEK -> TimeHistogramWindowData(
                window = TimeHistogramWindow.WEEK,
                period = Period.ofWeeks(1),
                numBins = 7
            )
            TimeHistogramWindow.MONTH -> TimeHistogramWindowData(
                window = TimeHistogramWindow.MONTH,
                period = Period.ofMonths(1),
                numBins = 30
            )
            TimeHistogramWindow.THREE_MONTHS -> TimeHistogramWindowData(
                window = TimeHistogramWindow.THREE_MONTHS,
                period = Period.ofMonths(3),
                numBins = 13
            )
            TimeHistogramWindow.SIX_MONTHS -> TimeHistogramWindowData(
                window = TimeHistogramWindow.SIX_MONTHS,
                period = Period.ofMonths(6),
                numBins = 26
            )
            TimeHistogramWindow.YEAR -> TimeHistogramWindowData(
                window = TimeHistogramWindow.YEAR,
                period = Period.ofYears(1),
                numBins = 12
            )
        }
    }
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

val plottingModePeriods: Map<LineGraphPlottingModes, TemporalAmount?> = mapOf(
    LineGraphPlottingModes.WHEN_TRACKED to null,
    LineGraphPlottingModes.GENERATE_HOURLY_TOTALS to Duration.ofHours(1),
    LineGraphPlottingModes.GENERATE_DAILY_TOTALS to Period.ofDays(1),
    LineGraphPlottingModes.GENERATE_WEEKLY_TOTALS to Period.ofWeeks(1),
    LineGraphPlottingModes.GENERATE_MONTHLY_TOTALS to Period.ofMonths(1),
    LineGraphPlottingModes.GENERATE_YEARLY_TOTALS to Period.ofYears(1)
)
