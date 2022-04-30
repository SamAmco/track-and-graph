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

import com.samco.trackandgraph.base.database.dto.IDataPoint
import org.junit.Assert
import org.junit.Test
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class AverageTimeBetweenDataFactoryTest {

    @Test(expected = Exception::class)
    fun `test throws exception if called with less than 2 data points`() {
        AverageTimeBetweenDataFactory.calculateAverageTimeBetween(
            listOf(unitDataPoint(OffsetDateTime.now()))
        )
    }

    @Test(expected = Exception::class)
    fun `test throws exception if called with no data points`() {
        AverageTimeBetweenDataFactory.calculateAverageTimeBetween(emptyList())
    }

    @Test
    fun test_returns_duration_if_only_two_data_points() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now),
            unitDataPoint(now.minusSeconds(1))
        )

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetween(dataPoints)

        //VERIFY
        Assert.assertEquals(Duration.ofSeconds(1).toMillis().toDouble(), ans, 0.0001)
    }

    @Test
    fun test_6_data_points_months_nonUniform_noDuration_lastIsNow() {
        //PREPARE
        val now = OffsetDateTime.now()
        val dataPoints = listOf(
            unitDataPoint(now),
            unitDataPoint(now.minusMonths(1).minusDays(15)),
            unitDataPoint(now.minusMonths(1).minusDays(16)),
            unitDataPoint(now.minusMonths(1).minusDays(17)),
            unitDataPoint(now.minusMonths(1).minusDays(18)),
            unitDataPoint(now.minusMonths(2))
        )

        //EXECUTE
        val ans = AverageTimeBetweenDataFactory.calculateAverageTimeBetween(dataPoints)

        //VERIFY
        Assert.assertEquals(
            Duration.between(
                dataPoints.last().timestamp,
                dataPoints.first().timestamp
            ).toMillis().toDouble() / 5.0,
            ans,
            0.0001
        )
    }

    private fun unitDataPoint(timestamp: OffsetDateTime) = object : IDataPoint() {
        override val timestamp = timestamp
        override val value = 1.0
        override val label = ""
    }
}
