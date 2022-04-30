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
import com.samco.trackandgraph.functions.functions.CompositeFunction
import com.samco.trackandgraph.functions.sampling.DataSample
import com.samco.trackandgraph.functions.functions.FilterLabelFunction
import com.samco.trackandgraph.functions.functions.FilterValueFunction
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.threeten.bp.OffsetDateTime

class FilterValueFunctionTest {

    data class DpInput(
        val offset: Int,
        val label: String,
        val value: Double = 1.0
    )

    @Test
    fun filter_continuous() {
        runBlocking {
            //GIVEN
            val now = OffsetDateTime.now()
            val inputPoints = listOf(
                DpInput(1, "", 500.0),
                DpInput(4, "", 200.0),
                DpInput(5, "", 100.0),
                DpInput(10, "", 300.0)
            ).map { makedp(now, it) }
            val dataSample = DataSample.fromSequence(inputPoints.asSequence())

            //WHEN
            val answer = FilterValueFunction(150.0, 350.0)
                .mapSample(dataSample)
                .toList()

            //THEN
            assertEquals(
                listOf(
                    DpInput(4, "", 200.0),
                    DpInput(10, "", 300.0)
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
                DpInput(1, "A", 1.0),
                DpInput(4, "B", 2.0),
                DpInput(5, "A", 1.0),
                DpInput(10, "B", 2.0)
            ).map { makedp(now, it) }
            val dataSample = DataSample.fromSequence(inputPoints.asSequence())

            //WHEN
            val answer = FilterLabelFunction(setOf("A"))
                .mapSample(dataSample)
                .toList()

            //THEN
            assertEquals(
                listOf(
                    DpInput(1, "A", 1.0),
                    DpInput(5, "A", 1.0)
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
                DpInput(1, "A", 1.0),
                DpInput(4, "B", 2.0),
                DpInput(5, "A", 100.0),
                DpInput(10, "B", 2.0)
            ).map { makedp(now, it) }
            val dataSample = DataSample.fromSequence(inputPoints.asSequence())

            //WHEN
            val answer = CompositeFunction(
                FilterValueFunction(-50.0, 350.0),
                FilterLabelFunction(setOf("A"))
            )
                .mapSample(dataSample)
                .toList()

            //THEN
            assertEquals(
                listOf(
                    DpInput(1, "A", 1.0),
                    DpInput(5, "A", 100.0),
                ).map { makedp(now, it) },
                answer
            )
        }
    }

    private fun makedp(now: OffsetDateTime, dpInput: DpInput) = object : IDataPoint() {
        override val timestamp = now.minusHours(dpInput.offset.toLong())
        override val value = dpInput.value
        override val label = dpInput.label
    }
}