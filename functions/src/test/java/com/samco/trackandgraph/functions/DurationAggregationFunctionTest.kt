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

package com.samco.trackandgraph.functions

import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.functions.aggregation.AggregationPreferences
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.functions.functions.DurationAggregationFunction
import com.samco.trackandgraph.functions.helpers.TimeHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.TemporalAmount

class DurationAggregationFunctionTest {
    private val timeHelper = TimeHelper(
        object : AggregationPreferences {
            override val firstDayOfWeek = DayOfWeek.MONDAY
            override val startTimeOfDay = Duration.ofSeconds(0)
        }
    )

    @Test
    fun calculateDurationAccumulatedValues_DateTimeOffset_test() {
        //A data point with the time stamp:2021-10-04T00:20:00.197+01:00 should appear in the week
        // 10-04, (not the previous week). Assuming the user has a current time zone offset of +01:00

        runBlocking {
            //GIVEN
            val plotTotalTime = Period.ofWeeks(1)
            val toODT = { s: String ->
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(s, OffsetDateTime::from)
            }
            val dataPoints = listOf(
                "2021-11-01T00:13:13.949Z",
                "2021-10-25T10:44:18.040+01:00",
                "2021-10-18T00:16:16.137+01:00",
                "2021-10-11T08:08:16.310+01:00",
                "2021-10-04T00:20:00.197+01:00"
            ).map { iDataPoint(toODT(it), 1.0, "") }
            val rawData = fromSequence(dataPoints.asSequence())

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                plotTotalTime
            ).mapSample(rawData)
                .toList()

            //THEN
            val expectedTimes = listOf(
                Pair(11, 8), Pair(11, 1), Pair(10, 25), Pair(10, 18), Pair(10, 11)
                //"2021-10-10T23:59:59.999999999+01:00",
                //"2021-10-17T23:59:59.999999999+01:00",
                //"2021-10-24T23:59:59.999999999+01:00",
                //"2021-10-31T23:59:59.999999999+01:00",
                //"2021-11-07T23:59:59.999999999+01:00"
            ).map {
                ZonedDateTime.of(2021, it.first, it.second, 0, 0, 0, 0, ZoneId.systemDefault())
                    .minusNanos(1)
                    .toOffsetDateTime()
            }

            assertEquals(expectedTimes, answer.map { it.timestamp })

            val expectedValues = listOf(1.0, 1.0, 1.0, 1.0, 1.0)

            assertEquals(expectedValues, answer.map { it.value })
        }
    }

    private fun iDataPoint(time: OffsetDateTime, value: Double, label: String) =
        object : IDataPoint() {
            override val timestamp = time
            override val value = value
            override val label = label
        }

    @Test
    fun calculateDurationAccumulatedValues_hourly_plot_totals() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
            val plotTotalTime = Duration.ofHours(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData = fromSequence(dataPoints)

            //WHEN
            val answer =
                DurationAggregationFunction(
                    timeHelper,
                    plotTotalTime
                ).mapSample(rawData)

            //THEN
            assertEquals(4, answer.toList().size)
            val answerTotals = answer.map { dp -> dp.value.toInt() }.toList()
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
            val rawData = fromSequence(dataPoints)

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                plotTotalTime
            ).mapSample(rawData)

            //THEN
            assertEquals(4, answer.toList().size)
            val answerTotals = answer.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_yearly_plot_totals() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
            val plotTotalTime: TemporalAmount = Period.ofYears(1)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData = fromSequence(dataPoints)

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                plotTotalTime
            ).mapSample(rawData)

            //THEN
            assertEquals(4, answer.toList().size)
            val answerTotals = answer.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_plot_totals_no_data() {
        runBlocking {
            //GIVEN
            val plotTotalTime: TemporalAmount = Period.ofMonths(1)
            val dataPoints = emptyList<IDataPoint>()
            val rawData = fromSequence(dataPoints.asSequence())

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                plotTotalTime
            ).mapSample(rawData)

            //THEN
            assertEquals(0, answer.toList().size)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_data_before_now_3month_period() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
                .withYear(2020)
                .withMonth(5)
                .withDayOfMonth(15)
            val plotTotalTime: TemporalAmount = Period.ofMonths(3)
            val plotTotals = listOf(8, 3, 1, 7)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData = fromSequence(dataPoints)

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                plotTotalTime
            ).mapSample(rawData).toList()

            //THEN
            assertEquals(4, answer.size)
            val answerTotals = answer.map { dp -> dp.value.toInt() }
            assertEquals(plotTotals, answerTotals)
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_with_data_in_future() {
        runBlocking {
            //GIVEN
            val endTime = OffsetDateTime.now()
                .plusMonths(1)
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val plotTotals = listOf(8, 3, 1)
            val dataPoints = generateDataPoints(endTime, plotTotalTime, plotTotals)
            val rawData = fromSequence(dataPoints)

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                plotTotalTime
            ).mapSample(rawData)

            //THEN
            assertEquals(3, answer.toList().size)
            val answerTotals = answer.map { dp -> dp.value.toInt() }.toList()
            assertEquals(plotTotals, answerTotals)
            assertTrue(answer.all { dp -> dp.timestamp > OffsetDateTime.now() })
        }
    }

    @Test
    fun calculateDurationAccumulatedValues_weekly_plot_totals_aggregation_prefs() {
        runBlocking {
            //GIVEN
            val plotTotalTime: TemporalAmount = Period.ofWeeks(1)
            val endTime = OffsetDateTime.now()
            val dataPoints = generateDataPoints2(
                endTime,
                listOf(
                    Triple(DayOfWeek.THURSDAY, 14 * 60 + 30, 1.0),
                    Triple(DayOfWeek.WEDNESDAY, 13 * 60 + 30, 1.0),
                    // this is now the previous week
                    Triple(DayOfWeek.WEDNESDAY, 3 * 60 + 30, 1.0),
                    Triple(DayOfWeek.MONDAY, 3 * 60 + 30, 1.0),
                    Triple(DayOfWeek.SUNDAY, 2 * 60 + 30, 1.0),
                )
            )
            val rawData = fromSequence(dataPoints)

            val timeHelper = TimeHelper(
                object : AggregationPreferences {
                    override val firstDayOfWeek = DayOfWeek.WEDNESDAY
                    override val startTimeOfDay = Duration.ofHours(4)
                }
            )

            //WHEN
            val answer = DurationAggregationFunction(
                timeHelper,
                plotTotalTime,
            ).mapSample(rawData)
                .toList()

            //THEN
            assertEquals(answer.map { it.value }, listOf(2.0, 3.0))
            assertEquals(
                answer.map { it.timestamp }, listOf(
                    endTime.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
                        .withHour(4)
                        .withSecond(0)
                        .withMinute(0)
                        .withNano(0),
                    endTime.with(TemporalAdjusters.previous(DayOfWeek.WEDNESDAY))
                        .withHour(4)
                        .withSecond(0)
                        .withMinute(0)
                        .withNano(0)
                ).map { it.minusNanos(1) }
            )
        }
    }


    private fun generateDataPoints(
        endTime: OffsetDateTime,
        totalingPeriod: TemporalAmount,
        clusters: List<Int>
    ): Sequence<IDataPoint> {
        val dataPoints = mutableListOf<IDataPoint>()
        var currentTime = endTime
        for (element in clusters) {
            for (y in 0 until element) {
                val dataPointTime = currentTime.minusSeconds(y + 1L)
                dataPoints.add(
                    iDataPoint(dataPointTime, 1.0, "")
                )
            }
            currentTime = currentTime.minus(totalingPeriod)
        }
        return dataPoints.asSequence()
    }

    private fun generateDataPoints2(
        endTime: OffsetDateTime,
        points: List<Triple<DayOfWeek, Int, Double>>
    ): Sequence<IDataPoint> {
        var currentDay = endTime.withHour(0).withMinute(0).withSecond(0).withNano(0)

        val output = mutableListOf<IDataPoint>()

        for (pointData in points) {
            val (dayOfWeek, timeInMinutes, value) = pointData

            currentDay = currentDay.with(TemporalAdjusters.previousOrSame(dayOfWeek))
            val timestamp = currentDay + Duration.ofMinutes(timeInMinutes.toLong())

            output.add(iDataPoint(timestamp, value, "Hi"))
        }

        return output.asSequence()
    }

    private fun parseOffsetDateTime(dateTime: String) =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(dateTime, OffsetDateTime::from)

}
