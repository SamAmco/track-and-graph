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

package com.samco.trackandgraph.graphstatview.data_sample_functions

import com.samco.trackandgraph.data.database.dto.IDataPoint
import com.samco.trackandgraph.data.sampling.DataSampleProperties
import com.samco.trackandgraph.graphstatview.functions.aggregation.AggregationPreferences
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.DataPaddingFunction
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.DurationAggregationFunction
import com.samco.trackandgraph.graphstatview.functions.exceptions.InvalidRegularityException
import com.samco.trackandgraph.graphstatview.functions.helpers.TimeHelper
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.TemporalAmount

class DataPaddingFunctionTest {

    private val defaultTimeHelper = TimeHelper(
        object : AggregationPreferences {
            override val firstDayOfWeek = DayOfWeek.MONDAY
            override val startTimeOfDay = Duration.ofHours(0)
        }
    )

    @Test(expected = InvalidRegularityException::class)
    fun test_invalid_regularity() {
        runBlocking {
            //GIVEN
            val sample = fromSequence(emptySequence())

            //WHEN
            DataPaddingFunction(defaultTimeHelper, null, null as TemporalAmount?)
                .mapSample(sample)
                .toList()
        }
    }

    @Test
    fun test_generate_empty_data() {
        runBlocking {
            //GIVEN
            val dataSampleProperties = DataSampleProperties(
                regularity = Duration.ofHours(1)
            )
            val sample = fromSequence(emptySequence(), dataSampleProperties)

            //WHEN
            val dataPoints = DataPaddingFunction(defaultTimeHelper, null, null as TemporalAmount?)
                .mapSample(sample)
                .toList()

            //THEN
            assertEquals(emptyList<IDataPoint>(), dataPoints)
        }
    }

    @Test
    fun test_generate_from_empty_data_now_duration() {
        runBlocking {
            //GIVEN
            val dataSampleProperties = DataSampleProperties(
                regularity = Duration.ofHours(1)
            )
            val sample = fromSequence(emptySequence(), dataSampleProperties)

            //WHEN
            val dataPoints = DataPaddingFunction(defaultTimeHelper, null, Period.ofDays(1))
                .mapSample(sample)
                .toList()

            //THEN
            assertEquals(MutableList(24) { 0.0 }, dataPoints.map { it.value })
            assertTrue(
                Duration.between(
                    dataPoints.first().timestamp,
                    OffsetDateTime.now()
                ) < Duration.ofHours(1)
            )
        }
    }

    @Test
    fun test_generate_from_empty_data_null_duration() {
        runBlocking {
            //GIVEN
            val now = OffsetDateTime.now()
            val dataSampleProperties = DataSampleProperties(
                regularity = Duration.ofHours(1)
            )
            val sample = fromSequence(emptySequence(), dataSampleProperties)

            //WHEN
            val dataPoints = DataPaddingFunction(defaultTimeHelper, now, null as TemporalAmount?)
                .mapSample(sample)
                .toList()

            //THEN
            assertEquals(emptyList<IDataPoint>(), dataPoints)
        }
    }

    @Test
    fun test_generate_from_end_time_and_duration() {
        runBlocking {
            //GIVEN
            val now = OffsetDateTime.now()
            val endOfDay = defaultTimeHelper.toZonedDateTime(now)
                .plusDays(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toOffsetDateTime()
            val duration = Duration.ofHours(9)
            val dataSampleProperties = DataSampleProperties(
                regularity = Duration.ofHours(1)
            )
            val sequence =
                listOf(3, 5, 7).map { makedp(endOfDay.minusHours(it.toLong())) }.asSequence()
            val sample = fromSequence(sequence, dataSampleProperties)

            //WHEN
            val dataPoints = com.samco.trackandgraph.graphstatview.functions.data_sample_functions.CompositeFunction(
                DurationAggregationFunction(defaultTimeHelper, Duration.ofHours(1)),
                DataPaddingFunction(defaultTimeHelper, endOfDay, duration)
            ).mapSample(sample)
                .toList()

            //THEN
            assertEquals(9, dataPoints.size)
            assertTrue(
                dataPoints.take(8)
                    .mapIndexed { i, dp ->
                        Duration.between(
                            dp.timestamp,
                            dataPoints[i + 1].timestamp
                        )
                    }
                    .all { (it - Duration.ofHours(1)) < Duration.ofMillis(1) }
            )
            assertEquals(
                listOf(0, 0, 1, 0, 1, 0, 1, 0, 0).map { it.toDouble() },
                dataPoints.map { it.value }
            )
        }
    }

    @Test
    fun test_data_past_end() {
        runBlocking {
            //GIVEN
            val now = OffsetDateTime.now()
            val endOfDay = defaultTimeHelper.toZonedDateTime(now)
                .plusDays(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toOffsetDateTime()
            val duration = Duration.ofHours(9)
            val regularity = Duration.ofHours(1)
            val dataSampleProperties = DataSampleProperties(regularity = regularity)
            val sequence = listOf(7, 5, 3)
                .map { makedp(endOfDay.minusMinutes(30).plusHours(it.toLong())) }
                .asSequence()
            val sample = fromSequence(sequence, dataSampleProperties)

            //WHEN
            val dataPoints = com.samco.trackandgraph.graphstatview.functions.data_sample_functions.CompositeFunction(
                DurationAggregationFunction(defaultTimeHelper, regularity),
                DataPaddingFunction(defaultTimeHelper, endOfDay, duration)
            ).mapSample(sample)
                .toList()

            //THEN
            assertEquals(16, dataPoints.size)
            assertTrue(
                dataPoints.take(15)
                    .mapIndexed { i, dp ->
                        Duration.between(
                            dp.timestamp,
                            dataPoints[i + 1].timestamp
                        )
                    }
                    .all { (it - regularity) < Duration.ofMillis(1) }
            )
            assertEquals(
                listOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0).map { it.toDouble() },
                dataPoints.map { it.value }
            )
        }
    }

    @Test
    fun test_data_before_start() {
        runBlocking {
            //GIVEN
            val now = OffsetDateTime.now()
            val endOfDay = defaultTimeHelper.toZonedDateTime(now)
                .plusDays(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toOffsetDateTime()
            val endOfNextDay = defaultTimeHelper.toZonedDateTime(now)
                .plusDays(2)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toOffsetDateTime()
            val duration = Duration.ofHours(9)
            val regularity = Duration.ofHours(1)
            val dataSampleProperties = DataSampleProperties(regularity = regularity)
            val sequence = listOf(1, 3, 4)
                .map { makedp(endOfDay.plusMinutes(30).minusHours(it.toLong())) }
                .asSequence()
            val sample = fromSequence(sequence, dataSampleProperties)

            //WHEN
            val dataPoints = com.samco.trackandgraph.graphstatview.functions.data_sample_functions.CompositeFunction(
                DurationAggregationFunction(defaultTimeHelper, regularity),
                DataPaddingFunction(defaultTimeHelper, endOfNextDay, duration)
            ).mapSample(sample)
                .toList()

            //THEN
            assertEquals(28, dataPoints.size)
            assertTrue(
                dataPoints.take(27)
                    .mapIndexed { i, dp ->
                        Duration.between(
                            dp.timestamp,
                            dataPoints[i + 1].timestamp
                        )
                    }
                    .all { (it - regularity) < Duration.ofMillis(1) }
            )
            assertEquals(
                MutableList(24) { 0 }.plus(listOf(1, 0, 1, 1)).map { it.toDouble() },
                dataPoints.map { it.value }
            )
        }
    }

    @Test
    fun test_generate_from_end_time_and_duration_with_time_settings() {
        runBlocking {
            //GIVEN
            val defaultTimeHelper = TimeHelper(
                object : AggregationPreferences {
                    override val firstDayOfWeek = DayOfWeek.FRIDAY
                    override val startTimeOfDay = Duration.ofHours(16)
                }
            )
            val endDate = OffsetDateTime.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
                .withHour(16)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            val duration = Period.ofWeeks(3)
            val dataSampleProperties = DataSampleProperties(regularity = Period.ofWeeks(1))
            val sequence = listOf(1, 2)
                .map { makedp(endDate.minusSeconds(1).minusWeeks(it.toLong())) }
                .asSequence()
            val sample = fromSequence(sequence, dataSampleProperties)

            //WHEN
            val dataPoints = com.samco.trackandgraph.graphstatview.functions.data_sample_functions.CompositeFunction(
                DurationAggregationFunction(defaultTimeHelper, Period.ofWeeks(1)),
                DataPaddingFunction(defaultTimeHelper, endDate, duration)
            ).mapSample(sample)
                .toList()

            //THEN
            assertEquals(3, dataPoints.size)
            assertTrue(dataPoints.all {
                it.timestamp.dayOfWeek == DayOfWeek.FRIDAY
                        //15th hour because it's the very end of that hour at 15:59:59.9999...
                        && it.timestamp.hour == 15
            })
            assertEquals(
                listOf(0, 1, 1).map { it.toDouble() },
                dataPoints.map { it.value }
            )
        }
    }

    private fun makedp(timestamp: OffsetDateTime) = object : IDataPoint() {
        override val timestamp = timestamp
        override val value = 1.0
        override val label = ""
    }
}