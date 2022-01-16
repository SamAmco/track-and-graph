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

package com.samco.trackandgraph.graphstatview.factories

import com.samco.trackandgraph.database.dto.IDataPoint
import com.samco.trackandgraph.database.entity.DataType
import org.junit.Assert
import org.junit.Test
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class AverageTimeBetweenDataFactoryTest {

    @Test
    fun test_returns_null_for_less_than_two_data_points_and_no_duration() {
        //PREPARE
        val dataPoints1 = listOf(
            unitDataPoint(OffsetDateTime.now())
        )

        val dataPoints2 = emptyList<IDataPoint>()

        //EXECUTE
        val ans1 = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            OffsetDateTime.now(),
            null,
            null,
            dataPoints1
        )
        val ans2 = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            OffsetDateTime.now(),
            null,
            null,
            dataPoints2
        )

        //VERIFY
        Assert.assertNull(ans1)
        Assert.assertNull(ans2)
    }

    @Test
    fun test_returns_duration_if_only_two_data_points() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusSeconds(1)),
            unitDataPoint(now)
        )

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            null,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(Duration.ofSeconds(1).toMillis().toDouble(), ans)
    }

    @Test
    fun test_returns_duration_if_only_one_data_point() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now)
        )

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            Duration.ofSeconds(1),
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(Duration.ofSeconds(1).toMillis().toDouble(), ans)
    }

    @Test
    fun test_2_data_points_firstStart_secondMiddle_endDate() {
        //PREPARE
        val now = OffsetDateTime.now()
        val endDate = now.minusSeconds(10)
        val dataPoints = listOf(
            unitDataPoint(now.minusSeconds(20)),
            unitDataPoint(now.minusSeconds(11))
        )

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            endDate,
            null,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                dataPoints.first().timestamp,
                endDate
            ).toMillis().toDouble() / 2.0,
            ans
        )
    }

    @Test
    fun test_2_data_points_firstMiddle_secondEnd_noEndDate() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusSeconds(20)),
            unitDataPoint(now)
        )
        val duration = Duration.ofSeconds(30)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(duration.toMillis().toDouble() / 2.0, ans)
    }

    @Test
    fun test_2_data_points_firstStart_secondEnd_noEndDate() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusSeconds(20)),
            unitDataPoint(now)
        )

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            null,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                dataPoints.first().timestamp,
                dataPoints.last().timestamp
            ).toMillis().toDouble(), ans
        )
    }

    @Test
    fun test_2_data_points_firstClipped_secondMiddle_endDate() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusSeconds(20)),
            unitDataPoint(now.minusSeconds(10))
        )
        val duration = Duration.ofSeconds(10)
        val endDate = now.minusSeconds(5)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            endDate,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(duration.toMillis().toDouble() / 2.0, ans)
    }

    @Test
    fun test_2_data_points_firstMiddle_secondClipped_endDate() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusSeconds(5)),
            unitDataPoint(now.minusSeconds(1))
        )
        val duration = Duration.ofSeconds(10)
        val endDate = now.minusSeconds(5)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            endDate,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(duration.toMillis().toDouble() / 2.0, ans)
    }

    @Test
    fun test_2_data_points_firstMiddle_secondMiddle_endDate() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusSeconds(5)),
            unitDataPoint(now.minusSeconds(2))
        )
        val duration = Duration.ofSeconds(10)
        val endDate = now.minusSeconds(1)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            endDate,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(duration.toMillis().toDouble() / 3.0, ans)
    }

    @Test
    fun test_2_data_points_firstStart_secondClipped_endDate() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusSeconds(5)),
            unitDataPoint(now.minusSeconds(1))
        )
        val endDate = now.minusSeconds(3)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            endDate,
            null,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                dataPoints.first().timestamp,
                endDate
            ).toMillis().toDouble(),
            ans
        )
    }

    @Test
    fun test_2_data_points_firstClipped_secondEnd_noEndDate() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusSeconds(11)),
            unitDataPoint(now)
        )
        val duration = Duration.ofSeconds(10)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            duration.toMillis().toDouble(),
            ans
        )
    }

    @Test
    fun test_3_data_points_months_uniform_noDuration_lastIsNow() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusMonths(2)),
            unitDataPoint(now.minusMonths(1)),
            unitDataPoint(now)
        )

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            null,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                dataPoints.first().timestamp,
                dataPoints.last().timestamp
            ).toMillis().toDouble() / 2.0,
            ans
        )
    }

    @Test
    fun test_3_data_points_months_nonUniform_noDuration_lastIsNow() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusMonths(2)),
            unitDataPoint(now.minusMonths(1).minusDays(15)),
            unitDataPoint(now)
        )

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            null,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                dataPoints.first().timestamp,
                dataPoints.last().timestamp
            ).toMillis().toDouble() / 2.0,
            ans
        )
    }

    @Test
    fun test_6_data_points_months_nonUniform_noDuration_lastIsNow() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusMonths(2)),
            unitDataPoint(now.minusMonths(1).minusDays(15)),
            unitDataPoint(now.minusMonths(1).minusDays(16)),
            unitDataPoint(now.minusMonths(1).minusDays(17)),
            unitDataPoint(now.minusMonths(1).minusDays(18)),
            unitDataPoint(now)
        )

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            null,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                dataPoints.first().timestamp,
                dataPoints.last().timestamp
            ).toMillis().toDouble() / 5.0,
            ans
        )
    }

    @Test
    fun test_3_data_points_months_nonUniform_noDuration_lastBeforeNow() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusMonths(2)),
            unitDataPoint(now.minusMonths(1).minusDays(18)),
            unitDataPoint(now.minusMonths(1))
        )

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            null,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                dataPoints.first().timestamp,
                now
            ).toMillis().toDouble() / 3.0,
            ans
        )
    }

    @Test
    fun test_3_data_points_months_nonUniform_withDuration_lastBeforeNow() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusMonths(2)),
            unitDataPoint(now.minusMonths(1).minusDays(18)),
            unitDataPoint(now.minusMonths(1))
        )
        val duration = Duration.ofDays(365 / 4)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                now.minus(duration),
                now
            ).toMillis().toDouble() / 4.0,
            ans
        )
    }

    @Test
    fun test_3_data_points_minutes_nonUniform_withDuration_lastBeforeNow() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusMinutes(2)),
            unitDataPoint(now.minusMinutes(1).minusSeconds(18)),
            unitDataPoint(now.minusSeconds(6))
        )
        val duration = Duration.ofDays(365 / 4)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                now.minus(duration),
                now
            ).toMillis().toDouble() / 4.0,
            ans
        )
    }

    @Test
    fun test_3_data_points_minutes_nonUniform_withDuration_lastAfterNow() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusMinutes(2)),
            unitDataPoint(now.minusMinutes(1).minusSeconds(18)),
            unitDataPoint(now.plusSeconds(6))
        )
        val duration = Duration.ofHours(1)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                dataPoints.last().timestamp.minus(duration),
                dataPoints.last().timestamp
            ).toMillis().toDouble() / 3.0,
            ans
        )
    }

    @Test
    fun test_3_data_points_minutes_nonUniform_withDuration_lastAfterNow_firstBeforeDuration() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now.minusHours(2)),
            unitDataPoint(now.minusMinutes(2)),
            unitDataPoint(now.minusMinutes(1).minusSeconds(18)),
            unitDataPoint(now.plusSeconds(6))
        )
        val duration = Duration.ofHours(1)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            null,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                dataPoints.last().timestamp.minus(duration),
                dataPoints.last().timestamp
            ).toMillis().toDouble() / 3.0,
            ans
        )
    }

    @Test
    fun test_4_data_points_minutes_nonUniform_withDuration_withEndDate_lastBeforeEndDate() {
        //PREPARE
        val now = OffsetDateTime.now()
        val endDate = now.minusSeconds(5)
        val dataPoints = listOf(
            unitDataPoint(now.minusHours(2)),
            unitDataPoint(now.minusMinutes(2)),
            unitDataPoint(now.minusMinutes(1).minusSeconds(18)),
            unitDataPoint(now.minusSeconds(6))
        )
        val duration = Duration.ofHours(3)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            endDate,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                endDate.minus(duration),
                endDate
            ).toMillis().toDouble() / 5.0,
            ans
        )
    }

    @Test
    fun test_4_data_points_minutes_nonUniform_withDuration_withEndDate_lastBeforeEndDate_firstBeforeDuration() {
        //PREPARE
        val now = OffsetDateTime.now()
        val endDate = now.minusSeconds(5)
        val dataPoints = listOf(
            unitDataPoint(now.minusHours(2).minusSeconds(10)),
            unitDataPoint(now.minusMinutes(2)),
            unitDataPoint(now.minusMinutes(1).minusSeconds(18)),
            unitDataPoint(now.minusSeconds(6))
        )
        val duration = Duration.ofHours(2)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            endDate,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                endDate.minus(duration),
                endDate
            ).toMillis().toDouble() / 4.0,
            ans
        )
    }

    @Test
    fun test_4_data_points_minutes_nonUniform_withDuration_withEndDate_lastAfterEndDate() {
        //PREPARE
        val now = OffsetDateTime.now()
        val endDate = now.minusSeconds(5)
        val dataPoints = listOf(
            unitDataPoint(now.minusHours(2)),
            unitDataPoint(now.minusMinutes(2)),
            unitDataPoint(now.minusMinutes(1).minusSeconds(18)),
            unitDataPoint(now.plusSeconds(6))
        )
        val duration = Duration.ofHours(2)

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetweenOrNull(
            now,
            endDate,
            duration,
            dataPoints
        )

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                endDate.minus(duration),
                endDate
            ).toMillis().toDouble() / 4.0,
            ans
        )
    }

    private fun unitDataPoint(timestamp: OffsetDateTime) = object : IDataPoint() {
        override val timestamp = timestamp
        override val dataType = DataType.CONTINUOUS
        override val value = 1.0
        override val label = ""
    }
}
