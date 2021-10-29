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

package com.samco.trackandgraph.functionslib

import com.samco.trackandgraph.database.entity.DataPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.*
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.TemporalAmount

class Statistics_calculateDurationAccumulatedValues_KtTest {
    private val timeHelper = TimeHelper(
        object : AggregationPreferences {
            override val firstDayOfWeek = DayOfWeek.MONDAY
            override val startTimeOfDay = Duration.ofSeconds(0)
        }
    )

    @Test
    fun calculateDurationAccumulatedValues_hourly_plot_totals() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
            val plotTotalTime = Duration.ofHours(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData =
                DataSample(
                    dataPoints
                )

            //WHEN
            val answer =
                DurationAggregationFunction(
                    timeHelper,
                    0L,
                    null,
                    null,
                    plotTotalTime
                ).execute(rawData)

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
            val rawData =
                DataSample(
                    dataPoints
                )

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                null,
                null,
                plotTotalTime
            ).execute(rawData)

            //THEN
            assertEquals(4, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_plot_totals_with_larger_duration_containing_data() {
        runBlocking {
            //GIVEN
            val sampleDuration = Duration.ofHours(3)
            val endTime = OffsetDateTime.now()
            val plotTotalTime: TemporalAmount = Duration.ofHours(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData =
                DataSample(
                    dataPoints
                )

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                sampleDuration,
                null,
                plotTotalTime
            ).execute(rawData)

            //THEN
            assertEquals(4, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_plot_totals_with_larger_duration_not_containing_data() {
        runBlocking {
            //GIVEN
            val sampleDuration = Duration.ofHours(3)
            val plotTotalTime: TemporalAmount = Duration.ofHours(1)
            val dataPoints = emptyList<DataPoint>()
            val rawData =
                DataSample(
                    dataPoints
                )

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                sampleDuration,
                null,
                plotTotalTime
            ).execute(rawData)

            //THEN
            assertEquals(4, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(listOf(0, 0, 0, 0), answerTotals)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_plot_totals_with_smaller_duration_containing_data() {
        runBlocking {
            //GIVEN
            val sampleDuration = Duration.ofHours(1)
            val endTime = OffsetDateTime.now()
            val plotTotalTime: TemporalAmount = Duration.ofHours(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData = DataSample(dataPoints)

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                sampleDuration,
                null,
                plotTotalTime
            ).execute(rawData)

            //THEN
            assertEquals(4, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_plot_totals_with_smaller_duration_not_containing_data() {
        runBlocking {
            //GIVEN
            val sampleDuration = Duration.ofHours(1)
            val plotTotalTime: TemporalAmount = Period.ofDays(1)
            val dataPoints = emptyList<DataPoint>()
            val rawData = DataSample(dataPoints)

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                sampleDuration,
                null,
                plotTotalTime
            ).execute(rawData)

            //THEN
            assertEquals(1, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(listOf(0), answerTotals)
            val timestamp = answer.dataPoints[0].timestamp
            //it should be right at the end of the current day
            assertEquals(23, timestamp.hour)
            assertEquals(59, timestamp.minute)
            assertEquals(59, timestamp.second)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_plot_totals_no_duration_containing_data() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
            val plotTotalTime: TemporalAmount = Period.ofYears(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData =
                DataSample(
                    dataPoints
                )

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                null,
                null,
                plotTotalTime
            ).execute(rawData)

            //THEN
            assertEquals(4, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_plot_totals_no_duration_containing_no_data() {
        runBlocking {
            //GIVEN
            val plotTotalTime: TemporalAmount = Period.ofYears(1)
            val dataPoints = emptyList<DataPoint>()
            val rawData =
                DataSample(
                    dataPoints
                )

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                null,
                null,
                plotTotalTime
            ).execute(rawData)

            //THEN
            assertEquals(1, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(listOf(0), answerTotals)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_no_duration_with_end_time_in_past() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
                .withYear(2020)
                .withMonth(5)
                .withDayOfMonth(15)
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData = DataSample(dataPoints)

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                null,
                endTime,
                plotTotalTime,
            ).execute(rawData)

            //THEN
            assertEquals(4, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
            val last = answer.dataPoints.last().timestamp
            assertEquals(17, last.dayOfMonth)
            assertEquals(5, last.monthValue)
            assertEquals(2020, last.year)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_no_duration_with_end_time_data_before_end_time() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
                .withYear(2020)
                .withMonth(5)
                .withDayOfMonth(15)
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData = DataSample(dataPoints)

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                null,
                OffsetDateTime.now(),
                plotTotalTime
            ).execute(rawData)

            //THEN
            assertTrue(answer.dataPoints.size > 4)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals.subList(0, 4))
            assertTrue(answerTotals.drop(4).all { i -> i == 0 })
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_no_duration_no_end_time_data_before_now() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
                .withYear(2020)
                .withMonth(5)
                .withDayOfMonth(15)
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData = DataSample(dataPoints)

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                null,
                null,
                plotTotalTime
            ).execute(rawData)

            //THEN
            assertTrue(answer.dataPoints.size > 4)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals.subList(0, 4))
            assertTrue(answerTotals.drop(4).all { i -> i == 0 })
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_with_duration_with_end_time_data_before_end_time() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
                .withYear(2020)
                .withMonth(5)
                .withDayOfMonth(15)
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData =
                DataSample(
                    dataPoints
                )
            val sampleDuration = Duration.ofDays(7 * 4)

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                sampleDuration,
                OffsetDateTime.now(),
                plotTotalTime
            ).execute(rawData)

            //THEN
            // (the clipping isn't done by this algorithm it totals everything passed
            //  to it regardless of sampleDuration and endTime)
            assertTrue(answer.dataPoints.size > 4)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals.subList(0, 4))
            assertTrue(answerTotals.drop(4).all { i -> i == 0 })
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_with_data_after_end_time() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
                .plusMonths(1)
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val plotTotals = listOf(8, 3, 1)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData =
                DataSample(
                    dataPoints
                )

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                null,
                OffsetDateTime.now(),
                plotTotalTime
            ).execute(rawData)

            //THEN
            // (the clipping isn't done by this algorithm it totals everything passed
            //  to it regardless of sampleDuration and endTime)
            assertEquals(3, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
            assertTrue(answer.dataPoints.all { dp -> dp.timestamp > OffsetDateTime.now() })
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_with_data_after_end_time_with_duration() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now().plusWeeks(4)
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val plotTotals = listOf(8, 3, 1)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData = DataSample(dataPoints)
            val sampleDuration = Duration.ofDays(7 * 4)

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                sampleDuration,
                OffsetDateTime.now(),
                plotTotalTime
            ).execute(rawData)

            //THEN we should go back to the beginning of endTime-sampleDuration
            assertEquals(9, answer.dataPoints.size)
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals.takeLast(3))
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_with_data_in_future_no_end_time_or_duration() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
                .plusMonths(1)
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val plotTotals = listOf(8, 3, 1, 7, 6, 4, 2, 4, 2, 3, 1, 9)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData =
                DataSample(
                    dataPoints
                )

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                null,
                null,
                plotTotalTime
            ).execute(rawData)

            //THEN we should go back to the beginning of endTime-sampleDuration
            val answerTotals = answer.dataPoints.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
            assertTrue(
                answer.dataPoints.takeLast(3).all { dp -> dp.timestamp > OffsetDateTime.now() })
            assertTrue(answer.dataPoints.take(3).all { dp -> dp.timestamp < OffsetDateTime.now() })
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_weekly_plot_totals_StartTime() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val plotTotals = listOf(1, 3)
            val dataPoints = generateDataPoints2(
                listOf(
                    Triple(DayOfWeek.MONDAY, 3 * 60 + 30, 1.0), // before 4AM cutoff -> own bin
                    Triple(DayOfWeek.MONDAY, 13 * 60 + 30, 1.0), // after  4AM cutoff -> new bin
                    Triple(DayOfWeek.SUNDAY, 13 * 60 + 30, 1.0), // filler to fill week
                    // this is now next monday
                    Triple(
                        DayOfWeek.MONDAY,
                        3 * 60 + 30,
                        1.0
                    ), // before 4AM cutoff on monday, should be same bin
                )
            )
            val rawData =
                DataSample(
                    dataPoints
                )

            val timeHelper = TimeHelper(
                object : AggregationPreferences {
                    override val firstDayOfWeek = DayOfWeek.MONDAY
                    override val startTimeOfDay = Duration.ofHours(4)
                }
            )

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                0L,
                null,
                null,
                plotTotalTime,
            ).execute(rawData)

            //THEN
            assertEquals(2, answer.dataPoints.size)
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
                dataPoints.add(
                    0,
                    DataPoint(
                        dataPointTime,
                        0L,
                        1.0,
                        "",
                        ""
                    )
                )
            }
            currentTime = currentTime.minus(totalingPeriod)
        }
        return dataPoints
    }

    private fun generateDataPoints2(
        points: List<Triple<DayOfWeek, Int, Double>>
    ): List<DataPoint> {
        var currentDay = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)

        val output = mutableListOf<DataPoint>()

        for (pointData in points) {
            val (dayOfWeek, timeInMinutes, value) = pointData

            currentDay = currentDay.with(TemporalAdjusters.nextOrSame(dayOfWeek))
            val timestamp = currentDay + Duration.ofMinutes(timeInMinutes.toLong())

            output.add(DataPoint(timestamp, 0L, value, "", ""))
        }

        return output
    }

}
