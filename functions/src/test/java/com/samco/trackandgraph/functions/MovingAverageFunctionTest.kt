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
import com.samco.trackandgraph.functions.functions.MovingAverageFunction
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class MovingAverageFunctionTest {

    @Test
    fun limitedUpstreamSamples() {
        //Make sure that the moving average function only accesses it's upstream sequence once per item
        // in the sequence that is accessed

        runBlocking {
            //GIVEN
            val now = OffsetDateTime.now()
            val averagingDuration = Duration.ofHours(2)
            var count = 0
            val sequence = sequence {
                for (i in 1..10) {
                    count++
                    yield(i)
                }
            }.mapIndexed { hoursBefore, value ->
                makedp(
                    value.toDouble(),
                    now.minusHours(hoursBefore.toLong())
                )
            }

            //WHEN
            MovingAverageFunction(averagingDuration)
                .mapSample(fromSequence(sequence))
                .take(3)
                .toList()

            //THEN
            //Each data point will be compared with the two following it, so the first 3 will
            // require comparison with 5 data points
            assertEquals(5, count)
        }
    }

    @Test
    fun maxUpstreamSamples() {
        //Make sure that the moving average function only accesses it's upstream sequence once per item
        // in the sequence

        runBlocking {
            //GIVEN
            val now = OffsetDateTime.now()
            val averagingDuration = Duration.ofHours(10)
            var count = 0
            val sequence = sequence {
                for (i in 1..10) {
                    count++
                    yield(i)
                }
            }.mapIndexed { hoursBefore, value ->
                makedp(
                    value.toDouble(),
                    now.minusHours(hoursBefore.toLong())
                )
            }

            //WHEN
            MovingAverageFunction(averagingDuration)
                .mapSample(fromSequence(sequence))
                .toList()

            //THEN
            assertEquals(10, count)
        }
    }

    @Test
    fun calculateMovingAverages() {
        /* The test assumes that datapoints will only be averages when the time between is
           smaller than the given window, NOT smaller or equal, e.g. the first two points will not be
           averaged together since they are exactly 10 hours apart, not less.
         */
        runBlocking {
            //GIVEN
            val now = OffsetDateTime.now()
            val dataPoints = listOf(
                3.0 to 10L,
                7.0 to 20L,
                8.0 to 30L,
                4.0 to 41L,
                0.0 to 43L,
                2.0 to 48L,
                4.0 to 49L,
                0.0 to 50L,
                5.0 to 70L,
                3.0 to 75L
            ).map { (value, hoursBefore) -> makedp(value, now.minusHours(hoursBefore)) }
            val averagingDuration = Duration.ofHours(10)

            //WHEN
            val answer = MovingAverageFunction(averagingDuration)
                .mapSample(fromSequence(dataPoints.asSequence()))

            //THEN
            val expected = listOf(3.0, 7.0, 8.0, 2.0, 1.5, 2.0, 2.0, 0.0, 4.0, 3.0)
            val actual = answer.map { dp -> dp.value }.toList()
            assertEquals(expected, actual)
        }
    }

    @Test
    fun calculateMovingAverages_empty_data() {
        runBlocking {
            //GIVEN
            val dataPoints = listOf<IDataPoint>()
            val averagingDuration = Duration.ofHours(10)

            //WHEN
            val answer = MovingAverageFunction(averagingDuration)
                .mapSample(fromSequence(dataPoints.asSequence()))

            //THEN
            assertEquals(0, answer.toList().size)
        }
    }

    private fun makedp(value: Double, timestamp: OffsetDateTime) = object: IDataPoint() {
        override val timestamp = timestamp
        override val value = value
        override val label = ""
    }
}