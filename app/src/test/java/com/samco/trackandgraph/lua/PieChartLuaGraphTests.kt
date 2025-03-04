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
package com.samco.trackandgraph.lua

import com.samco.trackandgraph.lua.dto.ColorSpec
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.PieChartSegment
import org.junit.Assert.assertEquals
import org.junit.Test
import org.luaj.vm2.LuaError

class PieChartLuaGraphTests : LuaEngineImplTest() {

    @Test
    fun `pie chart without segments is an error`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.PIE_CHART,
            }
        """.trimIndent()
    ) {
        println(result)
        assertEquals(null, result.data)
        assert(result.error is LuaError)
    }

    @Test
    fun `pie chart returns segments without colours`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.PIE_CHART,
                segments = {
                    { value = 10, label = "A" },
                    { value = 20, label = "B" }
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.PieChartData)
        val pieChartData = (result.data as LuaGraphResultData.PieChartData).segments!!
        assertEquals(
            listOf(
                PieChartSegment(10.0, "A", null),
                PieChartSegment(20.0, "B", null)
            ),
            pieChartData
        )
    }

    @Test
    fun `pie chart returns segments with colours`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.PIE_CHART,
                segments = {
                    { value = 10, label = "A", color = tng.COLOR.GREEN_DARK },
                    { value = 20, label = "B", color = "#00FF00" }
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.PieChartData)
        val pieChartData = (result.data as LuaGraphResultData.PieChartData).segments!!
        assertEquals(
            listOf(
                PieChartSegment(10.0, "A", ColorSpec.ColorIndex(11)),
                PieChartSegment(20.0, "B", ColorSpec.HexColor("#00FF00"))
            ),
            pieChartData
        )
    }

    @Test
    fun `pie chart returns invalid segment no value`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.PIE_CHART,
                segments = {
                    { value = 1, label = "A" }
                    { label = "A" }
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assertEquals(null, result.data)
        assert(result.error is LuaError)
    }

    @Test
    fun `pie chart returns invalid segment no label`() = testLuaEngine(
        """
            return {
                type = tng.GRAPH_TYPE.PIE_CHART,
                segments = {
                    { value = 10 }
                    { value = 10, label = "B" }
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assertEquals(null, result.data)
        assert(result.error is LuaError)
    }
}
