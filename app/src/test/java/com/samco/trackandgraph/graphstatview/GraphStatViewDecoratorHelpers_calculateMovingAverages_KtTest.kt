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

import com.samco.trackandgraph.database.entity.DataPoint
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class GraphStatViewDecoratorHelpers_calculateMovingAverages_KtTest {

    @Test
    fun calculateMovingAverages() {
        runBlocking {
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
            val averagingDuration = Duration.ofHours(10)

            //WHEN
            val answer = calculateMovingAverages(DataSample(dataPoints), averagingDuration)

            //THEN
            val expected = listOf(5.0, 0.0, 2.0, 2.0, 1.5, 2.0, 8.0, 7.5, 5.0)
            val actual = answer.dataPoints.map { dp -> dp.value }
            assertEquals(expected, actual)
        }
    }
    @Test

    fun calculateMovingAverages_empty_data() {
        runBlocking {
            //GIVEN
            val dataPoints = listOf<DataPoint>()
            val averagingDuration = Duration.ofHours(10)

            //WHEN
            val answer = calculateMovingAverages(DataSample(dataPoints), averagingDuration)

            //THEN
            assertEquals(0, answer.dataPoints.size)
        }
    }

    private fun makedp(value: Double, timestamp: OffsetDateTime): DataPoint {
        return DataPoint(
            timestamp,
            0L,
            value,
            "",
            ""
        )
    }
}