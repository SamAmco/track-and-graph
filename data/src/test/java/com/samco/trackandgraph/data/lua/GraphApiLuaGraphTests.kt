package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.luaj.vm2.LuaError
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class GraphApiLuaGraphTests : LuaEngineImplTest() {

    @Test
    fun `Data source not found gives error`() = testLuaEngine(
        mapOf(),
        """
            return function(sources)
                local graph = require("tng.graph")
                return {
                    type = graph.GRAPH_TYPE.TEXT,
                    data = sources["source1"].dp().value
                }
            end
        """.trimIndent()
    ) {
        println(result)
        assertEquals(null, result.data)
        assert(result.error != null)
        assert(result.error is LuaError)
    }

    @Test
    fun `Sources returns full list of sources`() = testLuaEngine(
        mapOf(
            "source1" to sequenceOf(
                TestDP(timestamp = OffsetDateTime.now(), value = 1.0),
                TestDP(timestamp = OffsetDateTime.now(), value = 2.0),
            ),
            "source2" to emptySequence(),
        ),
        """
            function getTableKeys(tab)
              local keyset = {}
              for k,v in pairs(tab) do
                keyset[#keyset + 1] = k
              end
              return keyset
            end
            
            return function(sources) 
                local graph = require("tng.graph")
                return {
                    type = graph.GRAPH_TYPE.TEXT,
                    text = table.concat(getTableKeys(sources), ", ")
                }
            end
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        assertEquals("source1, source2", (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `dp returns next data point`() = testLuaEngine(
        mapOf(
            "source1" to sequenceOf(
                TestDP(timestamp = OffsetDateTime.now(), value = 1.0),
                TestDP(timestamp = OffsetDateTime.now(), value = 2.0),
            ),
        ),
        """
            return function(sources) 
                local graph = require("tng.graph")
                return {
                    type = graph.GRAPH_TYPE.TEXT,
                    text = sources["source1"].dp().value
                }
            end
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        assertEquals("1", (result.data as LuaGraphResultData.TextData).text)
        assertEquals(1, sampledData["source1"]?.size)
    }

    @Test
    fun `dpbatch returns n data points`() = testLuaEngine(
        mapOf(
            "source1" to sequenceOf(
                TestDP(timestamp = OffsetDateTime.now(), value = 1.0),
                TestDP(timestamp = OffsetDateTime.now(), value = 2.0),
                TestDP(timestamp = OffsetDateTime.now(), value = 3.0),
                TestDP(timestamp = OffsetDateTime.now(), value = 4.0),
                TestDP(timestamp = OffsetDateTime.now(), value = 5.0),
            ),
        ),
        """
            return function(sources) 
                local datapoints = sources["source1"].dpbatch(3)
                for k, v in pairs(datapoints) do
                    datapoints[k] = v.value
                end
            
                local graph = require("tng.graph")
                return {
                    type = graph.GRAPH_TYPE.TEXT,
                    text = table.concat(datapoints, ", ")
                 }
            end
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        assertEquals("1, 2, 3", (result.data as LuaGraphResultData.TextData).text)
        assertEquals(3, sampledData["source1"]?.size)
    }

    @Test
    fun `dpall returns all data points`() = testLuaEngine(
        mapOf(
            "source1" to sequenceOf(
                TestDP(timestamp = OffsetDateTime.now(), value = 1.0),
                TestDP(timestamp = OffsetDateTime.now(), value = 2.0),
                TestDP(timestamp = OffsetDateTime.now(), value = 3.0),
                TestDP(timestamp = OffsetDateTime.now(), value = 4.0),
                TestDP(timestamp = OffsetDateTime.now(), value = 5.0),
            ),
        ),
        """
            return function(sources) 
                local datapoints = sources["source1"].dpall()
                for k, v in pairs(datapoints) do
                    datapoints[k] = v.value
                end
            
                local graph = require("tng.graph")
                return {
                    type = graph.GRAPH_TYPE.TEXT,
                    text = table.concat(datapoints, ", ")
                 }
            end
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        assertEquals("1, 2, 3, 4, 5", (result.data as LuaGraphResultData.TextData).text)
        assertEquals(5, sampledData["source1"]?.size)
    }

    @Test
    fun `dpafter returns all data points after timestamp`() = testLuaEngine(
        mapOf(
            "source1" to sequenceOf(
                TestDP(
                    timestamp = OffsetDateTime.of(
                        2021, 3, 2, 0, 0, 0, 0, ZoneOffset.UTC
                    ), value = 1.0
                ),
                TestDP(
                    timestamp = OffsetDateTime.of(
                        2021, 3, 1, 8, 9, 7, 3, ZoneOffset.UTC
                    ), value = 2.0
                ),
                TestDP(
                    timestamp = OffsetDateTime.of(
                        2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
                    ), value = 3.0
                ),
                TestDP(
                    timestamp = OffsetDateTime.of(
                        2020, 12, 29, 0, 0, 0, 0, ZoneOffset.UTC
                    ), value = 3.0
                ),
            ),
        ),
        """
            return function(sources) 
                local graph = require("tng.graph")
                local core = require("tng.core")
                local cutoff = core.time({year=2021, month=1, day=1, hour=0, min=0, sec=0, zone="UTC"})
                local datapoints = sources["source1"].dpafter(cutoff)
                for k, v in pairs(datapoints) do
                    datapoints[k] = v.value
                end
            
                return {
                    type = graph.GRAPH_TYPE.TEXT,
                    text = table.concat(datapoints, ", ")
                 }
            end
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        assertEquals("1, 2", (result.data as LuaGraphResultData.TextData).text)
        //We had to get the third data point to check the cutoff even though it wasn't returned
        assertEquals(3, sampledData["source1"]?.size)
    }

    @Test
    fun `dpafter returns all datapoints after a date`() = testLuaEngine(
        mapOf(
            "source1" to sequenceOf(
                TestDP(
                    timestamp = OffsetDateTime.of(
                        2021, 3, 2, 0, 0, 0, 0, ZoneOffset.UTC
                    ), value = 1.0
                ),
                TestDP(
                    timestamp = OffsetDateTime.of(
                        2021, 3, 1, 8, 9, 7, 3, ZoneOffset.UTC
                    ), value = 2.0
                ),
                TestDP(
                    timestamp = OffsetDateTime.of(
                        2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
                    ), value = 3.0
                ),
                TestDP(
                    timestamp = OffsetDateTime.of(
                        2020, 12, 29, 0, 0, 0, 0, ZoneOffset.UTC
                    ), value = 3.0
                ),
            ),
        ),
        """
            return function(sources) 
                local graph = require("tng.graph")
                local core = require("tng.core")
                local cutoff = core.time({year=2021, month=1, day=1, hour=0, min=0, sec=0, zone="UTC"})
                local datapoints = sources["source1"].dpafter(cutoff)
                for k, v in pairs(datapoints) do
                    datapoints[k] = v.value
                end
            
                return {
                    type = graph.GRAPH_TYPE.TEXT,
                    text = table.concat(datapoints, ", ")
                 }
            end
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        assertEquals("1, 2", (result.data as LuaGraphResultData.TextData).text)
        assertEquals(3, sampledData["source1"]?.size)
    }

    @Test
    fun `apply_moving_averaging works correctly`() = testLuaEngine(
        mapOf(
            "source" to sequenceOf(
                TestDP(100 - 10, 3.0),
                TestDP(100 - 20, 7.0),
                TestDP(100 - 30, 8.0),
                TestDP(100 - 41, 4.0),
                TestDP(100 - 43, 0.0),
                TestDP(100 - 48, 2.0),
                TestDP(100 - 49, 4.0),
                TestDP(100 - 50, 0.0),
                TestDP(100 - 70, 5.0),
                TestDP(100 - 75, 3.0),
            )
        ),
        """
            local graph = require("tng.graph")
            local graphext = require("tng.graphext")
            return function(sources) 
                datapoints = sources["source"].dpall()
                graphext.apply_moving_averaging(datapoints, 10)
                return {
                    type = graph.GRAPH_TYPE.LINE_GRAPH,
                    lines = { { line_points = datapoints } },
                }
            end
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.LineGraphData)
        val lineGraphData = result.data as LuaGraphResultData.LineGraphData

        val expected = listOf(3.0, 7.0, 8.0, 2.0, 1.5, 2.0, 2.0, 0.0, 4.0, 3.0)

        assertEquals(expected, lineGraphData.lines?.get(0)?.linePoints?.map { it.value })
    }

    @Test
    fun `calculate_period_totals dates work correctly`() = testLuaEngine(
        mapOf(
            "source" to sequenceOf(
                "2021-11-01T00:13:13.949Z",
                "2021-10-25T10:44:18.040Z",
                "2021-10-18T00:16:16.137Z",
                "2021-10-11T08:08:16.310Z",
                "2021-10-04T00:20:00.197Z"
            )
                .map { DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(it, OffsetDateTime::from) }
                .map { TestDP(timestamp = it, value = 1.0) }
        ),
        """
            local graph = require("tng.graph")
            local graphext = require("tng.graphext")
            local core = require("tng.core")
            return function(sources) 
                datapoints = sources["source"].dpall()
                period_totals = graphext.calculate_period_totals(datapoints, core.PERIOD.WEEK)
                return {
                    type = graph.GRAPH_TYPE.LINE_GRAPH,
                    lines = { { line_points = period_totals } },
                }
            end
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.LineGraphData)
        val lineGraphData = result.data as LuaGraphResultData.LineGraphData

        val expectedValues = listOf(1.0, 1.0, 1.0, 1.0, 1.0)

        assertEquals(expectedValues, lineGraphData.lines?.get(0)?.linePoints?.map { it.value })

        val expectedTimes = listOf(
            Pair(11, 8), Pair(11, 1), Pair(10, 25), Pair(10, 18), Pair(10, 11)
            //"2021-10-10T23:59:59.999999999",
            //"2021-10-17T23:59:59.999999999",
            //"2021-10-24T23:59:59.999999999+01:00",
            //"2021-10-31T23:59:59.999999999+01:00",
            //"2021-11-07T23:59:59.999999999+01:00"
        ).map {
            ZonedDateTime.of(2021, it.first, it.second, 0, 0, 0, 0, ZoneId.systemDefault())
                .minusNanos(1_000_000)
                .toOffsetDateTime()
        }

        assertEquals(expectedTimes, lineGraphData.lines?.get(0)?.linePoints?.map { it.timestamp })
    }

    @Test
    fun `calculate_period_totals value sums work correctly`() = testLuaEngine(
        mapOf(
            "source" to sequenceOf(
                "2025-03-09T00:13:13.949Z",
                "2025-03-09T00:13:13.949Z",
                "2025-03-08T00:13:13.949Z",
                "2025-03-07T00:13:13.949Z",
                "2025-03-07T00:13:13.949Z",
                "2025-03-07T00:13:13.949Z",
                "2025-03-06T00:13:13.949Z",
                "2025-03-05T00:13:13.949Z",
                "2025-03-04T00:13:13.949Z",
                "2025-03-04T00:13:13.949Z",
                "2025-03-04T00:13:13.949Z",
                "2025-03-03T00:13:13.949Z",
                "2025-03-03T00:13:13.949Z",
                "2025-03-01T00:13:13.949Z",
                "2025-03-01T00:13:13.949Z",
            )
                .map { DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(it, OffsetDateTime::from) }
                .map { TestDP(timestamp = it, value = 1.0) }
        ),
        """
            local graph = require("tng.graph")
            local graphext = require("tng.graphext")
            local core = require("tng.core")
            return function(sources) 
                datapoints = sources["source"].dpall()
                period_totals = graphext.calculate_period_totals(datapoints, core.PERIOD.DAY)
                return {
                    type = graph.GRAPH_TYPE.LINE_GRAPH,
                    lines = { { line_points = period_totals } },
                }
            end
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.LineGraphData)
        val lineGraphData = result.data as LuaGraphResultData.LineGraphData

        val expectedValues = listOf(2.0, 1.0, 3.0, 1.0, 1.0, 3.0, 2.0, 0.0, 2.0)

        assertEquals(expectedValues, lineGraphData.lines?.get(0)?.linePoints?.map { it.value })
    }
}