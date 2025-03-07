package com.samco.trackandgraph.lua

import com.samco.trackandgraph.lua.dto.ColorSpec
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.TimeBar
import com.samco.trackandgraph.lua.dto.TimeBarSegment
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.threeten.bp.Duration
import org.threeten.bp.Period

class TimeBarchartLuaGraphTests : LuaEngineImplTest() {

    @Test
    fun `time bars support 3 variants`() = testLuaEngine(
        """
        local graph = require("tng.graph")
        local core = require("tng.core")
        return {
            type = graph.GRAPH_TYPE.TIME_BARCHART,
            bar_period = core.PERIOD.DAY,
            end_time = core.time(),
            bars = {
                5,
                {
                    value = 6,
                    color = "#FF0000",
                    label = "A",
                },
                {
                    {
                        label = "A",
                        value = 1,
                        color = "#FF0000",
                    },
                    {
                        label = "B",
                        value = 2,
                        color = "#00FF00",
                    },
                    {
                        label = "C",
                        value = 3,
                        color = "#0000FF",
                    },
                },
            },
        }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TimeBarChartData)
        val timeBarChartData = result.data as LuaGraphResultData.TimeBarChartData
        assertEquals(3, timeBarChartData.bars.size)

        val expectedBars = listOf(
            TimeBar(listOf(TimeBarSegment(5.0))),
            TimeBar(listOf(TimeBarSegment(6.0, "A", ColorSpec.HexColor("#FF0000")))),
            TimeBar(
                listOf(
                    TimeBarSegment(1.0, "A", ColorSpec.HexColor("#FF0000")),
                    TimeBarSegment(2.0, "B", ColorSpec.HexColor("#00FF00")),
                    TimeBarSegment(3.0, "C", ColorSpec.HexColor("#0000FF"))
                )
            )
        )

        assertEquals(expectedBars, timeBarChartData.bars)
        assertEquals(null, timeBarChartData.yMax)
        assertEquals(false, timeBarChartData.durationBasedRange)
    }

    @Test
    fun `bar duration takes priority over bar period`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            local core = require("tng.core")
            return {
              type = graph.GRAPH_TYPE.TIME_BARCHART,
              bar_duration = 86400000,
              bar_period = core.PERIOD.DAY,
              bar_period_multiple = 2,
              end_time = core.time(),
              bars = { 1, 2, 3 },
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TimeBarChartData)
        val timeBarChartData = result.data as LuaGraphResultData.TimeBarChartData
        assertEquals(3, timeBarChartData.bars.size)

        val expectedBars = listOf(
            TimeBar(listOf(TimeBarSegment(1.0))),
            TimeBar(listOf(TimeBarSegment(2.0))),
            TimeBar(listOf(TimeBarSegment(3.0)))
        )

        assertEquals(expectedBars, timeBarChartData.bars)

        assertTrue(timeBarChartData.barDuration is Duration)
        assertEquals(86400000, (timeBarChartData.barDuration as Duration).toMillis())
        assertEquals(null, timeBarChartData.yMax)
        assertEquals(false, timeBarChartData.durationBasedRange)
    }

    @Test
    fun `fall back to period if bar duration is not provided`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            local core = require("tng.core")
            return {
              type = graph.GRAPH_TYPE.TIME_BARCHART,
              bar_period = core.PERIOD.DAY,
              bar_period_multiple = 2,
              end_time = core.time(),
              bars = { 1, 2, 3 },
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TimeBarChartData)
        val timeBarChartData = result.data as LuaGraphResultData.TimeBarChartData
        assertEquals(3, timeBarChartData.bars.size)

        val expectedBars = listOf(
            TimeBar(listOf(TimeBarSegment(1.0))),
            TimeBar(listOf(TimeBarSegment(2.0))),
            TimeBar(listOf(TimeBarSegment(3.0)))
        )

        assertEquals(expectedBars, timeBarChartData.bars)

        assertTrue(timeBarChartData.barDuration is Period)
        assertEquals(timeBarChartData.barDuration, Period.ofDays(2))
        assertEquals(null, timeBarChartData.yMax)
        assertEquals(false, timeBarChartData.durationBasedRange)
    }

    @Test
    fun `end_time supports integers`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
              type = graph.GRAPH_TYPE.TIME_BARCHART,
              bar_duration = 86400000,
              end_time = 1630000000000,
              bars = { 1, 2, 3 },
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TimeBarChartData)
        val timeBarChartData = result.data as LuaGraphResultData.TimeBarChartData
        assertEquals(3, timeBarChartData.bars.size)

        val expectedBars = listOf(
            TimeBar(listOf(TimeBarSegment(1.0))),
            TimeBar(listOf(TimeBarSegment(2.0))),
            TimeBar(listOf(TimeBarSegment(3.0)))
        )

        assertEquals(expectedBars, timeBarChartData.bars)

        assertEquals(1630000000000, timeBarChartData.endTime.toInstant().toEpochMilli())
        assertEquals(null, timeBarChartData.yMax)
        assertEquals(false, timeBarChartData.durationBasedRange)
    }

    @Test
    fun `end_time supports zone and offset`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
              type = graph.GRAPH_TYPE.TIME_BARCHART,
              bar_duration = 86400000,
              end_time = {
                timestamp = 1630000000000,
                zone = "Europe/London",
                offset = "+01:00",
              },
              bars = { 1, 2, 3 },
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TimeBarChartData)
        val timeBarChartData = result.data as LuaGraphResultData.TimeBarChartData
        assertEquals(3, timeBarChartData.bars.size)

        val expectedBars = listOf(
            TimeBar(listOf(TimeBarSegment(1.0))),
            TimeBar(listOf(TimeBarSegment(2.0))),
            TimeBar(listOf(TimeBarSegment(3.0)))
        )

        assertEquals(expectedBars, timeBarChartData.bars)

        assertEquals(1630000000000, timeBarChartData.endTime.toInstant().toEpochMilli())
        assertEquals("Europe/London", timeBarChartData.endTime.zone.id)
        assertEquals("+01:00", timeBarChartData.endTime.offset.id)
        assertEquals(null, timeBarChartData.yMax)
        assertEquals(false, timeBarChartData.durationBasedRange)
    }

    @Test
    fun `y_max is parsed correctly`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
              type = graph.GRAPH_TYPE.TIME_BARCHART,
              bar_duration = 86400000,
              end_time = 1630000000000,
              bars = { 1, 2, 3 },
              y_max = 10,
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TimeBarChartData)
        val timeBarChartData = result.data as LuaGraphResultData.TimeBarChartData
        assertEquals(3, timeBarChartData.bars.size)

        val expectedBars = listOf(
            TimeBar(listOf(TimeBarSegment(1.0))),
            TimeBar(listOf(TimeBarSegment(2.0))),
            TimeBar(listOf(TimeBarSegment(3.0)))
        )

        assertEquals(expectedBars, timeBarChartData.bars)

        assertEquals(10.0, timeBarChartData.yMax)
        assertEquals(false, timeBarChartData.durationBasedRange)
    }

    @Test
    fun `duration_based_range is parsed correctly`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
              type = graph.GRAPH_TYPE.TIME_BARCHART,
              bar_duration = 86400000,
              end_time = 1630000000000,
              bars = { 1, 2, 3 },
              duration_based_range = true,
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TimeBarChartData)
        val timeBarChartData = result.data as LuaGraphResultData.TimeBarChartData
        assertEquals(3, timeBarChartData.bars.size)

        val expectedBars = listOf(
            TimeBar(listOf(TimeBarSegment(1.0))),
            TimeBar(listOf(TimeBarSegment(2.0))),
            TimeBar(listOf(TimeBarSegment(3.0)))
        )

        assertEquals(expectedBars, timeBarChartData.bars)

        assertEquals(true, timeBarChartData.durationBasedRange)
        assertEquals(null, timeBarChartData.yMax)
    }
}