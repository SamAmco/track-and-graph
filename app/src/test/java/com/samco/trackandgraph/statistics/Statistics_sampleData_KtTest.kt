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

package com.samco.trackandgraph.statistics

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.`when`

import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.TemporalAmount

class Statistics_sampleData_KtTest {
    @Test
    fun sampleData_no_duration_or_endTime() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val sampleDuration: Duration? = null
            val endDate: OffsetDateTime? = null
            val averagingDuration: Duration? = null
            val plotTotalTime: TemporalAmount? = null

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureAscSync(featureId)
        }
    }

    @Test
    fun sampleData_no_duration_with_endTime() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val sampleDuration: Duration? = null
            val endDate: OffsetDateTime = OffsetDateTime.now().minusDays(100)
            val averagingDuration: Duration? = null
            val plotTotalTime: TemporalAmount? = null

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureBetweenAscSync(
                eq(featureId),
                eq(OffsetDateTime.MIN),
                eq(endDate)
            )
        }
    }

    @Test
    fun sampleData_with_duration_no_endTime() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val sampleDuration: Duration? = Duration.ofDays(10)
            val endDate: OffsetDateTime? = null
            val averagingDuration: Duration? = null
            val plotTotalTime: TemporalAmount? = null

            val startTimeCaptor = argumentCaptor<OffsetDateTime>()
            val endTimeCaptor = argumentCaptor<OffsetDateTime>()

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureBetweenAscSync(
                eq(featureId),
                startTimeCaptor.capture(),
                endTimeCaptor.capture()
            )

            val captureStartTimeDiff = Duration.between(
                startTimeCaptor.firstValue,
                OffsetDateTime.now().minus(sampleDuration)
            )

            val capturedEndTimeDiff = Duration.between(
                endTimeCaptor.firstValue,
                OffsetDateTime.now()
            )

            assertTrue(kotlin.math.abs(captureStartTimeDiff.seconds.toDouble()) < 1.0)
            assertTrue(kotlin.math.abs(capturedEndTimeDiff.seconds.toDouble()) < 1.0)
        }
    }

    @Test
    fun sampleData_with_duration_and_endTime() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val sampleDuration: Duration = Duration.ofDays(10)
            val endDate: OffsetDateTime = OffsetDateTime.now().minusDays(100)
            val averagingDuration: Duration? = null
            val plotTotalTime: TemporalAmount? = null

            val startTimeCaptor = argumentCaptor<OffsetDateTime>()
            val endTimeCaptor = argumentCaptor<OffsetDateTime>()

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureBetweenAscSync(
                eq(featureId),
                startTimeCaptor.capture(),
                endTimeCaptor.capture()
            )

            val captureStartTimeDiff = Duration.between(
                startTimeCaptor.firstValue,
                endDate.minus(sampleDuration)
            )

            val capturedEndTimeDiff = Duration.between(
                endTimeCaptor.firstValue,
                endDate
            )

            assertTrue(kotlin.math.abs(captureStartTimeDiff.seconds.toDouble()) < 1.0)
            assertTrue(kotlin.math.abs(capturedEndTimeDiff.seconds.toDouble()) < 1.0)
        }
    }

    @Test
    fun sampleData_with_endTime_and_data_past_endTime() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val lastDataPoint =
                DataPoint(
                    OffsetDateTime.now().plusDays(10), 0, 0.0, "", ""
                )
            `when`(dataSource.getLastDataPointForFeatureSync(0)).thenReturn(listOf(lastDataPoint))
            val sampleDuration: Duration = Duration.ofDays(10)
            val endDate: OffsetDateTime = OffsetDateTime.now().minusDays(100)
            val averagingDuration: Duration? = null
            val plotTotalTime: TemporalAmount? = null

            val startTimeCaptor = argumentCaptor<OffsetDateTime>()
            val endTimeCaptor = argumentCaptor<OffsetDateTime>()

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureBetweenAscSync(
                eq(featureId),
                startTimeCaptor.capture(),
                endTimeCaptor.capture()
            )

            val captureStartTimeDiff = Duration.between(
                startTimeCaptor.firstValue,
                endDate.minus(sampleDuration)
            )

            val capturedEndTimeDiff = Duration.between(
                endTimeCaptor.firstValue,
                endDate
            )

            assertTrue(kotlin.math.abs(captureStartTimeDiff.seconds.toDouble()) < 1.0)
            assertTrue(kotlin.math.abs(capturedEndTimeDiff.seconds.toDouble()) < 1.0)
        }
    }

    @Test
    fun sampleData_with_no_endTime_and_data_past_now() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val lastDataPointTime = OffsetDateTime.now().plusDays(10)
            val lastDataPoint =
                DataPoint(
                    lastDataPointTime,
                    0,
                    0.0,
                    "",
                    ""
                )
            `when`(dataSource.getLastDataPointForFeatureSync(0)).thenReturn(listOf(lastDataPoint))
            val sampleDuration: Duration = Duration.ofDays(10)
            val endDate: OffsetDateTime? = null
            val averagingDuration: Duration? = null
            val plotTotalTime: TemporalAmount? = null

            val startTimeCaptor = argumentCaptor<OffsetDateTime>()
            val endTimeCaptor = argumentCaptor<OffsetDateTime>()

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureBetweenAscSync(
                eq(featureId),
                startTimeCaptor.capture(),
                endTimeCaptor.capture()
            )

            val captureStartTimeDiff = Duration.between(
                startTimeCaptor.firstValue,
                lastDataPointTime.minus(sampleDuration)
            )

            val capturedEndTimeDiff = Duration.between(
                endTimeCaptor.firstValue,
                lastDataPointTime
            )

            assertTrue(kotlin.math.abs(captureStartTimeDiff.seconds.toDouble()) < 2.0)
            assertTrue(kotlin.math.abs(capturedEndTimeDiff.seconds.toDouble()) < 2.0)
        }
    }

    @Test
    fun sampleData_with_averaging_and_plot_periods_no_duration() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val sampleDuration: Duration? = null
            val endDate: OffsetDateTime? = null
            val averagingDuration: Duration? = Duration.ofDays(100)
            val plotTotalTime: TemporalAmount? = Duration.ofDays(200)

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureAscSync(featureId)
        }
    }

    @Test
    fun sampleData_with_averaging_and_plot_periods_no_endTime_plot_period_longer() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val sampleDuration: Duration = Duration.ofDays(10)
            val endDate: OffsetDateTime? = null
            val averagingDuration: Duration = Duration.ofDays(100)
            val plotTotalTime: Duration = Duration.ofDays(112)

            val startTimeCaptor = argumentCaptor<OffsetDateTime>()
            val endTimeCaptor = argumentCaptor<OffsetDateTime>()

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureBetweenAscSync(
                eq(featureId),
                startTimeCaptor.capture(),
                endTimeCaptor.capture()
            )

            val captureStartTimeDiff = Duration.between(
                startTimeCaptor.firstValue,
                OffsetDateTime.now().minus(sampleDuration.plus(plotTotalTime))
            )

            val capturedEndTimeDiff = Duration.between(
                endTimeCaptor.firstValue,
                OffsetDateTime.now()
            )

            assertTrue(kotlin.math.abs(captureStartTimeDiff.seconds.toDouble()) < 2.0)
            assertTrue(kotlin.math.abs(capturedEndTimeDiff.seconds.toDouble()) < 2.0)
        }
    }

    @Test
    fun sampleData_with_averaging_and_plot_periods_no_endTime_averaging_duration_longer() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val sampleDuration: Duration = Duration.ofDays(10)
            val endDate: OffsetDateTime? = null
            val averagingDuration: Duration = Duration.ofDays(112)
            val plotTotalTime: Duration = Duration.ofDays(100)

            val startTimeCaptor = argumentCaptor<OffsetDateTime>()
            val endTimeCaptor = argumentCaptor<OffsetDateTime>()

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureBetweenAscSync(
                eq(featureId),
                startTimeCaptor.capture(),
                endTimeCaptor.capture()
            )

            val captureStartTimeDiff = Duration.between(
                startTimeCaptor.firstValue,
                OffsetDateTime.now().minus(sampleDuration.plus(averagingDuration))
            )

            val capturedEndTimeDiff = Duration.between(
                endTimeCaptor.firstValue,
                OffsetDateTime.now()
            )

            assertTrue(kotlin.math.abs(captureStartTimeDiff.seconds.toDouble()) < 2.0)
            assertTrue(kotlin.math.abs(capturedEndTimeDiff.seconds.toDouble()) < 2.0)
        }
    }

    @Test
    fun sampleData_with_averaging_no_plot_periods_no_endTime() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val sampleDuration: Duration = Duration.ofDays(10)
            val endDate: OffsetDateTime? = null
            val averagingDuration: Duration = Duration.ofDays(112)
            val plotTotalTime: Duration? = null

            val startTimeCaptor = argumentCaptor<OffsetDateTime>()
            val endTimeCaptor = argumentCaptor<OffsetDateTime>()

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureBetweenAscSync(
                eq(featureId),
                startTimeCaptor.capture(),
                endTimeCaptor.capture()
            )

            val captureStartTimeDiff = Duration.between(
                startTimeCaptor.firstValue,
                OffsetDateTime.now().minus(sampleDuration.plus(averagingDuration))
            )

            val capturedEndTimeDiff = Duration.between(
                endTimeCaptor.firstValue,
                OffsetDateTime.now()
            )

            assertTrue(kotlin.math.abs(captureStartTimeDiff.seconds.toDouble()) < 2.0)
            assertTrue(kotlin.math.abs(capturedEndTimeDiff.seconds.toDouble()) < 2.0)
        }
    }

    @Test
    fun sampleData_with_plot_period_no_averaging_duration_no_endTime() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val sampleDuration: Duration = Duration.ofDays(10)
            val endDate: OffsetDateTime? = null
            val averagingDuration: Duration? = null
            val plotTotalTime: Duration = Duration.ofDays(112)

            val startTimeCaptor = argumentCaptor<OffsetDateTime>()
            val endTimeCaptor = argumentCaptor<OffsetDateTime>()

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureBetweenAscSync(
                eq(featureId),
                startTimeCaptor.capture(),
                endTimeCaptor.capture()
            )

            val captureStartTimeDiff = Duration.between(
                startTimeCaptor.firstValue,
                OffsetDateTime.now().minus(sampleDuration.plus(plotTotalTime))
            )

            val capturedEndTimeDiff = Duration.between(
                endTimeCaptor.firstValue,
                OffsetDateTime.now()
            )

            assertTrue(kotlin.math.abs(captureStartTimeDiff.seconds.toDouble()) < 2.0)
            assertTrue(kotlin.math.abs(capturedEndTimeDiff.seconds.toDouble()) < 2.0)
        }
    }

    @Test
    fun sampleData_with_plot_period_and_duration_and_endTime() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val sampleDuration: Duration = Duration.ofDays(10)
            val endDate: OffsetDateTime = OffsetDateTime.now().minusDays(100)
            val averagingDuration: Duration? = null
            val plotTotalTime: Duration = Duration.ofDays(112)

            val startTimeCaptor = argumentCaptor<OffsetDateTime>()
            val endTimeCaptor = argumentCaptor<OffsetDateTime>()

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureBetweenAscSync(
                eq(featureId),
                startTimeCaptor.capture(),
                endTimeCaptor.capture()
            )

            val captureStartTimeDiff = Duration.between(
                startTimeCaptor.firstValue,
                endDate.minus(sampleDuration.plus(plotTotalTime))
            )

            val capturedEndTimeDiff = Duration.between(
                endTimeCaptor.firstValue,
                endDate
            )

            assertTrue(kotlin.math.abs(captureStartTimeDiff.seconds.toDouble()) < 2.0)
            assertTrue(kotlin.math.abs(capturedEndTimeDiff.seconds.toDouble()) < 2.0)
        }
    }

    @Test
    fun sampleData_with_plot_period_no_duration_and_endTime() {
        runBlocking {
            //GIVEN
            val dataSource = mock<TrackAndGraphDatabaseDao>()
            val featureId = 0L
            val sampleDuration: Duration? = null
            val endDate: OffsetDateTime = OffsetDateTime.now().minusDays(100)
            val averagingDuration: Duration? = null
            val plotTotalTime: Duration = Duration.ofDays(112)

            val startTimeCaptor = argumentCaptor<OffsetDateTime>()
            val endTimeCaptor = argumentCaptor<OffsetDateTime>()

            //WHEN
            sampleData(
                dataSource, featureId, sampleDuration, endDate,
                averagingDuration, plotTotalTime
            )

            //THEN
            verify(dataSource).getDataPointsForFeatureBetweenAscSync(
                eq(featureId),
                startTimeCaptor.capture(),
                endTimeCaptor.capture()
            )

            val captureStartTimeDiff = Duration.between(
                startTimeCaptor.firstValue,
                OffsetDateTime.MIN
            )

            val capturedEndTimeDiff = Duration.between(
                endTimeCaptor.firstValue,
                endDate
            )

            assertTrue(kotlin.math.abs(captureStartTimeDiff.seconds.toDouble()) < 2.0)
            assertTrue(kotlin.math.abs(capturedEndTimeDiff.seconds.toDouble()) < 2.0)
        }
    }
}