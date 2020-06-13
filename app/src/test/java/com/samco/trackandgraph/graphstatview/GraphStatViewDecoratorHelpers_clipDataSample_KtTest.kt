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
import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class GraphStatViewDecoratorHelpers_clipDataSample_KtTest {
    @Test
    fun clipDataSample_empty_sample() {
        //WHEN
        val answer = clipDataSample(DataSample(listOf()), null, null)

        //THEN
        assertEquals(0, answer.dataPoints.size)
    }

    @Test
    fun clipDataSample_no_end_time_or_duration() {
        //GIVEN
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            5.0 to 70L,
            0.0 to 50L,
            4.0 to 49L,
            2.0 to 48L,
            0.0 to 43L,
            4.0 to 41L,
            8.0 to 30L,
            7.0 to 20L,
            3.0 to 10L
        ).map { (value, hoursBefore) -> makedp(value, now.minusHours(hoursBefore)) }
        val dataSample = DataSample(dataPoints)

        //WHEN
        val answer = clipDataSample(dataSample, null, null)

        //THEN
        assertEquals(dataPoints, answer.dataPoints)
    }

    @Test
    fun clipDataSample_no_end_time_with_duration_inclusive() {
        //GIVEN
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            5.0 to 70L,
            0.0 to 50L,
            4.0 to 49L,
            2.0 to 48L,
            0.0 to 43L,
            4.0 to 41L,
            8.0 to 30L,
            7.0 to 20L,
            3.0 to 10L
        ).map { (value, hoursBefore) -> makedp(value, now.minusHours(hoursBefore)) }
        val dataSample = DataSample(dataPoints)
        val sampleDuration = Duration.ofHours(30)

        //WHEN
        val answer = clipDataSample(dataSample, null, sampleDuration)

        //THEN
        assertEquals(dataPoints.takeLast(3), answer.dataPoints)
    }

    @Test
    fun clipDataSample_no_end_time_with_duration_larger_than_data() {
        //GIVEN
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            5.0 to 70L,
            0.0 to 50L,
            4.0 to 49L,
            2.0 to 48L,
            0.0 to 43L,
            4.0 to 41L,
            8.0 to 30L,
            7.0 to 20L,
            3.0 to 10L
        ).map { (value, hoursBefore) -> makedp(value, now.minusHours(hoursBefore)) }
        val dataSample = DataSample(dataPoints)
        val sampleDuration = Duration.ofHours(100)

        //WHEN
        val answer = clipDataSample(dataSample, null, sampleDuration)

        //THEN
        assertEquals(dataPoints, answer.dataPoints)
    }

    @Test
    fun clipDataSample_no_end_time_with_duration_excluding_all() {
        //GIVEN
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            5.0 to 70L,
            0.0 to 50L,
            4.0 to 49L,
            2.0 to 48L,
            0.0 to 43L,
            4.0 to 41L,
            8.0 to 30L,
            7.0 to 20L,
            3.0 to 10L
        ).map { (value, hoursBefore) -> makedp(value, now.minusHours(hoursBefore)) }
        val dataSample = DataSample(dataPoints)
        val sampleDuration = Duration.ofHours(1)

        //WHEN
        val answer = clipDataSample(dataSample, null, sampleDuration)

        //THEN
        assertEquals(dataPoints.takeLast(1), answer.dataPoints)
    }

    @Test
    fun clipDataSample_no_end_time_all_data_in_future() {
        //GIVEN
        val future = OffsetDateTime.now().plusMonths(1)
        val dataPoints = listOf(
            5.0 to 70L,
            0.0 to 50L,
            4.0 to 49L,
            2.0 to 48L,
            0.0 to 43L,
            4.0 to 41L,
            8.0 to 30L,
            7.0 to 20L,
            3.0 to 10L
        ).map { (value, hoursBefore) -> makedp(value, future.minusHours(hoursBefore)) }
        val dataSample = DataSample(dataPoints)
        val sampleDuration = Duration.ofHours(35)

        //WHEN
        val answer = clipDataSample(dataSample, null, sampleDuration)

        //THEN
        assertEquals(dataPoints.takeLast(5), answer.dataPoints)
    }

    @Test
    fun clipDataSample_with_end_time_no_duration_inclusive() {
        //GIVEN
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            5.0 to 70L,
            0.0 to 50L,
            4.0 to 49L,
            2.0 to 48L,
            0.0 to 43L,
            4.0 to 41L,
            8.0 to 30L,
            7.0 to 20L,
            3.0 to 10L
        ).map { (value, hoursBefore) -> makedp(value, now.minusDays(hoursBefore)) }
        val dataSample = DataSample(dataPoints)
        val endTime = now.minusDays(20)

        //WHEN
        val answer = clipDataSample(dataSample, endTime, null)

        //THEN
        assertEquals(dataPoints.take(8), answer.dataPoints)
    }

    @Test
    fun clipDataSample_with_end_time_all_data_after() {
        //GIVEN
        val future = OffsetDateTime.now().plusYears(1)
        val dataPoints = listOf(
            5.0 to 70L,
            0.0 to 50L,
            4.0 to 49L,
            2.0 to 48L,
            0.0 to 43L,
            4.0 to 41L,
            8.0 to 30L,
            7.0 to 20L,
            3.0 to 10L
        ).map { (value, hoursBefore) -> makedp(value, future.minusDays(hoursBefore)) }
        val dataSample = DataSample(dataPoints)

        //WHEN
        val answer = clipDataSample(dataSample, OffsetDateTime.now(), null)

        //THEN
        assertEquals(emptyList<DataPoint>(), answer.dataPoints)
    }

    @Test
    fun clipDataSample_with_end_time_all_data_before() {
        //GIVEN
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            5.0 to 70L,
            0.0 to 50L,
            4.0 to 49L,
            2.0 to 48L,
            0.0 to 43L,
            4.0 to 41L,
            8.0 to 30L,
            7.0 to 20L,
            3.0 to 10L
        ).map { (value, hoursBefore) -> makedp(value, now.minusDays(hoursBefore)) }
        val dataSample = DataSample(dataPoints)

        //WHEN
        val answer = clipDataSample(dataSample, now, null)

        //THEN
        assertEquals(dataPoints, answer.dataPoints)
    }

    @Test
    fun clipDataSample_with_end_time_and_duration() {
        //GIVEN
        val now = OffsetDateTime.now()
        val future = now.plusMonths(1)
        val dataPoints = listOf(
            5.0 to 70L,
            0.0 to 50L,
            4.0 to 49L,
            2.0 to 48L,
            0.0 to 43L,
            4.0 to 41L,
            8.0 to 30L,
            7.0 to 20L,
            3.0 to 10L
        ).map { (value, hoursBefore) -> makedp(value, future.minusDays(hoursBefore)) }
        val dataSample = DataSample(dataPoints)
        val sampleDuration = Duration.ofDays(30)

        //WHEN
        val answer = clipDataSample(dataSample, now, sampleDuration)

        //THEN
        assertEquals(dataPoints.drop(1).take(6), answer.dataPoints)
    }

    @Test
    fun clipDataSample_with_end_time_and_duration_all_data_before() {
        //GIVEN
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            5.0 to 70L,
            0.0 to 50L,
            4.0 to 49L,
            2.0 to 48L,
            0.0 to 43L,
            4.0 to 41L,
            8.0 to 30L,
            7.0 to 20L,
            3.0 to 10L
        ).map { (value, hoursBefore) -> makedp(value, now.minusDays(hoursBefore)) }
        val dataSample = DataSample(dataPoints)
        val sampleDuration = Duration.ofDays(3)

        //WHEN
        val answer = clipDataSample(dataSample, now, sampleDuration)

        //THEN
        assertEquals(emptyList<DataPoint>(), answer.dataPoints)
    }

    @Test
    fun clipDataSample_with_end_time_and_duration_all_data_after() {
        //GIVEN
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            5.0 to 70L,
            0.0 to 50L,
            4.0 to 49L,
            2.0 to 48L,
            0.0 to 43L,
            4.0 to 41L,
            8.0 to 30L,
            7.0 to 20L,
            3.0 to 10L
        ).map { (value, hoursBefore) -> makedp(value, now.plusMonths(3).minusDays(hoursBefore)) }
        val dataSample = DataSample(dataPoints)
        val sampleDuration = Duration.ofDays(20)

        //WHEN
        val answer = clipDataSample(dataSample, now, sampleDuration)

        //THEN
        assertEquals(emptyList<DataPoint>(), answer.dataPoints)
    }

    private fun makedp(value: Double, timestamp: OffsetDateTime): DataPoint {
        return DataPoint(timestamp, 0L, value, "", "")
    }
}