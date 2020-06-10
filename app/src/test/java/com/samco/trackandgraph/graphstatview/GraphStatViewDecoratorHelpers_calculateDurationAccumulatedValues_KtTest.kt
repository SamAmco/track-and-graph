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

package com.samco.trackandgraph.graphstatview

import com.samco.trackandgraph.database.DataPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAmount

class GraphStatViewDecoratorHelpers_calculateDurationAccumulatedValues_KtTest {

    @Test
    fun calculateDurationAccumulatedValues_hourly_plot_totals() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
            val plotTotalTime = Duration.ofHours(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData: RawDataSample = RawDataSample(dataPoints, 3)

            //WHEN
            val answer = calculateDurationAccumulatedValues(rawData, 0L, endTime, plotTotalTime)

            //THEN
            assertEquals(4, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_weekly_plot_totals() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData: RawDataSample = RawDataSample(dataPoints, 3)

            //WHEN
            val answer = calculateDurationAccumulatedValues(rawData, 0L, endTime, plotTotalTime)

            //THEN
            assertEquals(4, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_weekly_plot_totals_() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData: RawDataSample = RawDataSample(dataPoints, 3)

            //WHEN
            val answer = calculateDurationAccumulatedValues(rawData, 0L, endTime, plotTotalTime)

            //THEN
            assertEquals(4, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
        }
    }

    private fun generateDataPoints(
        endTime: OffsetDateTime,
        totalingPeriod: TemporalAmount,
        clusters: List<Int>
    ): List<DataPoint> {
        val dataPoints = mutableListOf<DataPoint>()
        var currentTime = endTime
        for (element in clusters.reversed()) {
            for (y in 0 until element) {
                val dataPointTime = currentTime.minusSeconds(y + 1L)
                dataPoints.add(0, DataPoint(dataPointTime, 0L, 1.0, "", ""))
            }
            currentTime = currentTime.minus(totalingPeriod)
        }
        return dataPoints
    }
}
