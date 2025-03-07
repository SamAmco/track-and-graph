package com.samco.trackandgraph.lua

import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.luaj.vm2.LuaError
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

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
}