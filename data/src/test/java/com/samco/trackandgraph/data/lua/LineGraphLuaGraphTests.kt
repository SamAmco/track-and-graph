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
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.ColorSpec
import com.samco.trackandgraph.data.lua.dto.Line
import com.samco.trackandgraph.data.lua.dto.LinePoint
import com.samco.trackandgraph.data.lua.dto.LinePointStyle
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.luaj.vm2.LuaError
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

class LineGraphLuaGraphTests : LuaEngineImplTest() {

    @Test
    fun `line graph without lines is an error`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.LINE_GRAPH,
            }
        """.trimIndent()
    ) {
        println(result)
        assertEquals(null, result.data)
        assert(result.error is LuaError)
    }

    @Test
    fun `line graph returns lines without colors`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.LINE_GRAPH,
                lines = {
                    { 
                        line_points = { 
                            { timestamp = 2, value = 10 }, 
                            { timestamp = 1, value = 20 } 
                        } 
                    }
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.LineGraphData)
        val lineGraphData = (result.data as LuaGraphResultData.LineGraphData)
        assertEquals(false, lineGraphData.durationBasedRange)
        assertEquals(
            listOf(
                Line(
                    lineColor = null,
                    pointStyle = null,
                    label = null,
                    linePoints = listOf(
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(2), ZoneOffset.UTC), 10.0),
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(1), ZoneOffset.UTC), 20.0)
                    )
                )
            ),
            lineGraphData.lines
        )
    }

    @Test
    fun `line graph returns lines with colors`() = testLuaEngine(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.LINE_GRAPH,
                lines = {
                    { 
                        line_color = core.COLOR.RED, 
                        line_points = { 
                            { timestamp = 2, value = 10 }, 
                            { timestamp = 1, value = 20 } 
                        } 
                    }
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.LineGraphData)
        val lineGraphData = (result.data as LuaGraphResultData.LineGraphData).lines!!
        assertEquals(
            listOf(
                Line(
                    lineColor = ColorSpec.ColorIndex(1),
                    pointStyle = null,
                    label = null,
                    linePoints = listOf(
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(2), ZoneOffset.UTC), 10.0),
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(1), ZoneOffset.UTC), 20.0)
                    )
                )
            ),
            lineGraphData
        )
    }

    @Test
    fun `line graph returns invalid line no points`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.LINE_GRAPH,
                lines = {
                    { line_color = tng.COLOR.RED }
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assertEquals(null, result.data)
        assert(result.error is LuaError)
    }

    @Test
    fun `line graph returns invalid line no timestamp`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.LINE_GRAPH,
                lines = {
                    { 
                        line_points = { { value = 10 } } 
                    }
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assertEquals(null, result.data)
        assertNotNull(result.error)
    }

    @Test
    fun `range_bounds are parsed correctly`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.LINE_GRAPH,
                lines = {
                    { 
                        line_points = { 
                            { timestamp = 2, value = 10 }, 
                            { timestamp = 1, value = 20 } 
                        } 
                    }
                },
                range_bounds = { min = 1, max = 2 },
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.LineGraphData)
        val lineGraphData = result.data as LuaGraphResultData.LineGraphData
        assertEquals(1.0, lineGraphData.yMin)
        assertEquals(2.0, lineGraphData.yMax)
        assertEquals(false, lineGraphData.durationBasedRange)
    }

    @Test
    fun `range bounds only provides one value`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.LINE_GRAPH,
                lines = {
                    { 
                        line_points = { 
                            { timestamp = 2, value = 10 }, 
                            { timestamp = 1, value = 20 } 
                        } 
                    }
                },
                range_bounds = { min = 1 },
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.LineGraphData)
        val lineGraphData = result.data as LuaGraphResultData.LineGraphData
        assertEquals(1.0, lineGraphData.yMin)
        assertEquals(null, lineGraphData.yMax)
        assertEquals(false, lineGraphData.durationBasedRange)
    }

    @Test
    fun `range bounds invalid`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.LINE_GRAPH,
                lines = {
                    { 
                        line_points = { 
                            { timestamp = 2, value = 10 }, 
                            { timestamp = 1, value = 20 } 
                        } 
                    }
                },
                range_bounds = 5,
            }
        """.trimIndent()
    ) {
        println(result)
        assertEquals(null, result.data)
        assertNotNull(result.error)
    }

    @Test
    fun `duration_based_range is parsed correctly`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.LINE_GRAPH,
                lines = {
                    { 
                        line_points = { 
                            { timestamp = 1, value = 20 } 
                        } 
                    }
                },
                duration_based_range = true,
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.LineGraphData)
        val lineGraphData = result.data as LuaGraphResultData.LineGraphData
        assertEquals(true, lineGraphData.durationBasedRange)
    }

    @Test
    fun `lines with colours and point styles`() = testLuaEngine(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.LINE_GRAPH,
                lines = {
                    { 
                        line_color = core.COLOR.RED, 
                        point_style = graph.LINE_POINT_STYLE.CIRCLE,
                        line_points = { 
                            { timestamp = 2, value = 10 }, 
                            { timestamp = 1, value = 20 } 
                        } 
                    },
                    { 
                        line_color = "#00FF00",
                        point_style = graph.LINE_POINT_STYLE.CIRCLE_VALUE,
                        line_points = { 
                            { timestamp = 3, value = 30 }, 
                            { timestamp = 1, value = 40 } 
                        } 
                    },
                    {
                        line_color = core.COLOR.BLUE,
                        point_style = graph.LINE_POINT_STYLE.NONE,
                        line_points = {
                            { timestamp = 4, value = 50 },
                            { timestamp = 2, value = 60 }
                        }
                    },
                    {
                        line_color = "#FFFF00",
                        point_style = graph.LINE_POINT_STYLE.CIRCLE_ONLY,
                        line_points = {
                            { timestamp = 5, value = 70 },
                            { timestamp = 3, value = 80 }
                        }
                    }
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.LineGraphData)
        val lineGraphData = (result.data as LuaGraphResultData.LineGraphData).lines!!
        assertEquals(
            listOf(
                Line(
                    lineColor = ColorSpec.ColorIndex(1),
                    pointStyle = LinePointStyle.CIRCLE,
                    label = null,
                    linePoints = listOf(
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(2), ZoneOffset.UTC), 10.0),
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(1), ZoneOffset.UTC), 20.0)
                    )
                ),
                Line(
                    lineColor = ColorSpec.HexColor("#00FF00"),
                    pointStyle = LinePointStyle.CIRCLE_VALUE,
                    label = null,
                    linePoints = listOf(
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(3), ZoneOffset.UTC), 30.0),
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(1), ZoneOffset.UTC), 40.0)
                    )
                ),
                Line(
                    lineColor = ColorSpec.ColorIndex(7),
                    pointStyle = LinePointStyle.NONE,
                    label = null,
                    linePoints = listOf(
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(4), ZoneOffset.UTC), 50.0),
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(2), ZoneOffset.UTC), 60.0)
                    )
                ),
                Line(
                    lineColor = ColorSpec.HexColor("#FFFF00"),
                    pointStyle = LinePointStyle.CIRCLES_ONLY,
                    label = null,
                    linePoints = listOf(
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(5), ZoneOffset.UTC), 70.0),
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(3), ZoneOffset.UTC), 80.0)
                    )
                )
            ),
            lineGraphData
        )
    }

    @Test
    fun `line with label parses correctly`() = testLuaEngine(
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.LINE_GRAPH,
                lines = {
                    { 
                        line_points = { 
                            { timestamp = 1, value = 20 } 
                        },
                        label = "label"
                    }
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.LineGraphData)
        val lineGraphData = (result.data as LuaGraphResultData.LineGraphData).lines!!
        assertEquals(
            listOf(
                Line(
                    lineColor = null,
                    pointStyle = null,
                    label = "label",
                    linePoints = listOf(
                        LinePoint(OffsetDateTime.ofInstant(Instant.ofEpochMilli(1), ZoneOffset.UTC), 20.0)
                    )
                )
            ),
            lineGraphData
        )
    }
}
