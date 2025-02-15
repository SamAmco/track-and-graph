package com.samco.trackandgraph.lua

import com.samco.trackandgraph.lua.dto.ColorSpec
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.PieChartSegment
import org.junit.Assert.assertEquals
import org.junit.Test
import org.luaj.vm2.LuaError

class PieChartLuaGraphTests : LuaEngineImplTest() {

    @Test
    fun `pie chart type returns NIL`() = testLuaEngine(
        emptyMap(),
        """
            return {
                type = tng.graph.PIECHART,
                data = NIL
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.PieChartData)
        val pieChartData = result.data as LuaGraphResultData.PieChartData
        assertEquals(null, pieChartData.segments)
    }

    @Test
    fun `pie chart returns segments without colours`() = testLuaEngine(
        emptyMap(),
        """
            return {
                type = tng.graph.PIECHART,
                data = {
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
        emptyMap(),
        """
            return {
                type = tng.graph.PIECHART,
                data = {
                    { value = 10, label = "A", color = tng.graph.color12 },
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
                PieChartSegment(10.0, "A", ColorSpec.ColorIndex(12)),
                PieChartSegment(20.0, "B", ColorSpec.HexColor("#00FF00"))
            ),
            pieChartData
        )
    }

    @Test
    fun `pie chart returns invalid segment no value`() = testLuaEngine(
        emptyMap(),
        """
            return {
                type = tng.graph.PIECHART,
                data = {
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
        emptyMap(),
        """
            return {
                type = tng.graph.PIECHART,
                data = {
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