package com.samco.trackandgraph.lua

import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.luaj.vm2.LuaError
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

class DataPointLuaGraphTests : LuaEngineImplTest() {

    @Test
    fun `Datapoint type gives data point with sensible defaults`() = testLuaEngine(
        emptyMap(),
        """
            local graph = require("tng.graph")
            return {
                 type = graph.GRAPH_TYPE.DATA_POINT,
                 datapoint = {
                     timestamp = 0 
                 }
             }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.DataPointData)
        val dataPoint = (result.data as LuaGraphResultData.DataPointData).dataPoint!!
        assertEquals(
            OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(0),
                ZoneOffset.ofTotalSeconds(0)
            ),
            dataPoint.timestamp
        )
        assertEquals(0, dataPoint.featureId)
        assertEquals(0.0, dataPoint.value)
        assertEquals("", dataPoint.label)
        assertEquals("", dataPoint.note)
    }

    @Test
    fun `Datapoint type respects all returned values`() = testLuaEngine(
        emptyMap(),
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.DATA_POINT,
                datapoint = { 
                    timestamp = 0,
                    offset = 1000,
                    featureId = 1,
                    value = 1.0,
                    label = "label",
                    note = "note"
                }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.DataPointData)
        val dataPoint = (result.data as LuaGraphResultData.DataPointData).dataPoint!!
        assertEquals(
            OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(0),
                ZoneOffset.ofTotalSeconds(1000)
            ),
            dataPoint.timestamp
        )
        assertEquals(1, dataPoint.featureId)
        assertEquals(1.0, dataPoint.value)
        assertEquals("label", dataPoint.label)
        assertEquals("note", dataPoint.note)
    }

    @Test
    fun `Datapoint type respects isDuration`() = testLuaEngine(
        emptyMap(),
        """
            local graph = require("tng.graph")
            return {
                type = graph.GRAPH_TYPE.DATA_POINT,
                datapoint = { timestamp = 0 },
                isduration = true
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.DataPointData)
        val data = result.data as LuaGraphResultData.DataPointData
        val dataPoint = data.dataPoint!!
        assertEquals(
            OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(0),
                ZoneOffset.UTC
            ),
            dataPoint.timestamp
        )
        assertEquals(0, dataPoint.featureId)
        assertEquals(0.0, dataPoint.value)
        assertEquals("", dataPoint.label)
        assertEquals("", dataPoint.note)
        assertEquals(true, data.isDuration)
    }

    @Test
    fun `Datapoint type with nil data is an error`() = testLuaEngine(
        emptyMap(),
        """
            local graph = require("tng.graph")
            return {
              type = graph.GRAPH_TYPE.DATA_POINT,
              datapoint = nil
            }
        """.trimIndent()
    ) {
        println(result)
        assertEquals(null, result.data)
        assert(result.error is LuaError)
    }

    @Test
    fun `Datapoint timestamp is in milliseconds and offset is in seconds`() = testLuaEngine(
        emptyMap(),
        """
            local graph = require("tng.graph")
            return {
              type = graph.GRAPH_TYPE.DATA_POINT,
              datapoint = {
                  timestamp = 123,
                  offset = 1234
              }
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.DataPointData)
        val data = result.data as LuaGraphResultData.DataPointData
        val dataPoint = data.dataPoint!!
        assertEquals(
            OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(123),
                ZoneOffset.ofTotalSeconds(1234)
            ),
            dataPoint.timestamp
        )
        assertEquals(0, dataPoint.featureId)
        assertEquals(0.0, dataPoint.value)
        assertEquals("", dataPoint.label)
        assertEquals("", dataPoint.note)
        assertEquals(false, data.isDuration)
    }

    @Test
    fun `Datapoint without data is an error`() = testLuaEngine(
        emptyMap(),
        """
            local graph = require("tng.graph")
            return {
              type = graph.GRAPH_TYPE.DATA_POINT,
            }
        """.trimIndent()
    ) {
        println(result)
        assertEquals(null, result.data)
        assert(result.error is LuaError)
    }
}
