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

import com.samco.trackandgraph.database.dto.IDataPoint
import com.samco.trackandgraph.database.entity.DataType
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.threeten.bp.OffsetDateTime

class FilterValueFunction_KtTest {

    data class DpInput(
        val offset: Int,
        val dataType: DataType,
        val label: String,
        val value: Double = 1.0
    )

    @Test
    fun filter_continuous() {
        runBlocking {
            //GIVEN
            val now = OffsetDateTime.now()
            val inputPoints = listOf(
                DpInput(1, DataType.CONTINUOUS, "", 500.0),
                DpInput(4, DataType.CONTINUOUS, "", 200.0),
                DpInput(5, DataType.CONTINUOUS, "", 100.0),
                DpInput(10, DataType.CONTINUOUS, "", 300.0)
            ).map { makedp(now, it) }
            val dataSample = DataSample.fromSequence(inputPoints.asSequence())

            //WHEN
            val answer = FilterValueFunction(150.0, 350.0, emptyList())
                .mapSample(dataSample)
                .toList()

            //THEN
            assertEquals(
                listOf(
                    DpInput(4, DataType.CONTINUOUS, "", 200.0),
                    DpInput(10, DataType.CONTINUOUS, "", 300.0)
                ).map { makedp(now, it) },
                answer
            )
        }
    }

    @Test
    fun filter_discrete() {
        runBlocking {
            //GIVEN
            val now = OffsetDateTime.now()
            val inputPoints = listOf(
                DpInput(1, DataType.DISCRETE, "A", 1.0),
                DpInput(4, DataType.DISCRETE, "B", 2.0),
                DpInput(5, DataType.DISCRETE, "A", 1.0),
                DpInput(10, DataType.DISCRETE, "B", 2.0)
            ).map { makedp(now, it) }
            val dataSample = DataSample.fromSequence(inputPoints.asSequence())

            //WHEN
            val answer = FilterValueFunction(150.0, 350.0, listOf(1))
                .mapSample(dataSample)
                .toList()

            //THEN
            assertEquals(
                listOf(
                    DpInput(1, DataType.DISCRETE, "A", 1.0),
                    DpInput(5, DataType.DISCRETE, "A", 1.0)
                ).map { makedp(now, it) },
                answer
            )
        }
    }
    @Test
    fun filter_mixed() {
        runBlocking {
            //GIVEN
            val now = OffsetDateTime.now()
            val inputPoints = listOf(
                DpInput(1, DataType.DISCRETE, "A", 1.0),
                DpInput(4, DataType.DISCRETE, "B", 2.0),
                DpInput(5, DataType.CONTINUOUS, "", 100.0),
                DpInput(10, DataType.DISCRETE, "B", 2.0)
            ).map { makedp(now, it) }
            val dataSample = DataSample.fromSequence(inputPoints.asSequence())

            //WHEN
            val answer = FilterValueFunction(-50.0, 350.0, listOf(1))
                .mapSample(dataSample)
                .toList()

            //THEN
            assertEquals(
                listOf(
                    DpInput(1, DataType.DISCRETE, "A", 1.0),
                    DpInput(5, DataType.CONTINUOUS, "", 100.0),
                ).map { makedp(now, it) },
                answer
            )
        }
    }

    private fun makedp(now: OffsetDateTime, dpInput: DpInput) = object : IDataPoint() {
        override val timestamp = now.minusHours(dpInput.offset.toLong())
        override val dataType = dpInput.dataType
        override val value = dpInput.value
        override val label = dpInput.label
    }
}