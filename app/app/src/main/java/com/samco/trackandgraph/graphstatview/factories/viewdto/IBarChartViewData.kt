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
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.graphstatview.factories.viewdto

import org.threeten.bp.Period
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.TemporalAmount

data class BarChartSeries(
    val label: String,
    val values: List<Double>,
    val color: ColorSpec,
)

internal fun calculateStackedBarYExtents(series: List<BarChartSeries>): Pair<Double, Double> {
    val bucketCount = series.firstOrNull()?.values?.size ?: return 0.0 to 0.0
    var minimum = 0.0
    var maximum = 0.0
    repeat(bucketCount) { bucket ->
        var negativeTotal = 0.0
        var positiveTotal = 0.0
        series.forEach { bar ->
            val value = bar.values[bucket]
            if (value < 0.0) negativeTotal += value else positiveTotal += value
        }
        minimum = minOf(minimum, negativeTotal)
        maximum = maxOf(maximum, positiveTotal)
    }
    return minimum to maximum
}

interface IBarChartViewData : IGraphStatViewData {
    /**
     * One x date for every bar in the list. Sorted from oldest to newest. You don't necessarily draw all of them on the x axis.
     * Each series in [bars] should contain the same number of values.
     */
    val xDates: List<ZonedDateTime>
        get() = emptyList()

    /**
     * One series for each label in the data set. Each series contains one value per xDates entry.
     */
    val bars: List<BarChartSeries>
        get() = emptyList()

    /**
     * Whether the y values should be interpreted as a number of seconds or just a number
     */
    val durationBasedRange: Boolean
        get() = false

    /**
     * The end time of the graph
     */
    val endTime: ZonedDateTime
        get() = ZonedDateTime.now()

    val yMin: Double
        get() = 0.0

    val yMax: Double
        get() = 1.0

    val yAxisSubdivides: Int
        get() = 11

    /**
     * The period/duration of a single bar
     */
    val barPeriod: TemporalAmount
        get() = Period.ofDays(1)
}
