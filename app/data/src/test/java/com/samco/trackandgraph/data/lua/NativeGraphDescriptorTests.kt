/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import com.samco.trackandgraph.data.lua.dto.LuaGraphResult
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import com.samco.trackandgraph.data.sampling.RawDataSample
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class NativeGraphDescriptorTests : LuaEngineImplTest() {
    private fun runGraph(
        script: String,
        state: LuaValueSnapshot? = null,
        packaged: Boolean = false,
    ): LuaGraphResult = runBlocking {
        val engine = uut()
        val lock = engine.acquireVM()
        try {
            val points = listOf(TestDP(timestamp = 0L, value = 6.0).toDataPoint())
            var consumed = false
            val sources = mapOf("input" to RawDataSample.fromSequence(
                data = points.asSequence().onEach { consumed = true },
                getRawDataPoints = { if (consumed) points else emptyList() },
                onDispose = {},
            ))
            val params = LuaGraphEngineParams(sources, state)
            if (packaged) {
                val archive = ByteArrayOutputStream().also { output ->
                    ZipOutputStream(output).use { zip ->
                        zip.putNextEntry(ZipEntry("main.lua"))
                        zip.write(script.toByteArray())
                        zip.closeEntry()
                    }
                }.toByteArray()
                engine.runLuaGraph(lock, ZipLuaScriptAccessor(archive), params)
            } else {
                engine.runLuaGraph(lock, script, params)
            }
        } finally {
            engine.releaseVM(lock)
        }
    }

    @Test
    fun `every native type prepares its existing result shape for inline and package sources`() {
        val results = listOf(
            "graph.text({ text = params.sources.input.dp().value * params.state.multiplier })" to
                LuaGraphResultData.TextData::class.java,
            "graph.data_point({ datapoint = params.sources.input.dp() })" to
                LuaGraphResultData.DataPointData::class.java,
            "graph.line_graph({ lines = {{ line_points = { params.sources.input.dp() } }} })" to
                LuaGraphResultData.LineGraphData::class.java,
            "graph.pie_chart({ segments = {{ value = params.sources.input.dp().value, label = 'Input' }} })" to
                LuaGraphResultData.PieChartData::class.java,
            "graph.time_barchart({ bars = { params.sources.input.dp().value }, bar_period = core.PERIOD.DAY, end_time = core.time() })" to
                LuaGraphResultData.TimeBarChartData::class.java,
        )
        val types = listOf("TEXT", "DATA_POINT", "LINE_GRAPH", "PIE_CHART", "TIME_BARCHART")
        results.forEachIndexed { index, (expression, expectedClass) ->
            for (packaged in listOf(false, true)) {
                val result = runGraph("""
                    local graph = require("tng.graph")
                    local core = require("tng.core")
                    local calls = 0
                    return {
                        type = graph.GRAPH_TYPE.${types[index]},
                        prepare_data = function(params)
                            calls = calls + 1
                            assert(calls == 1)
                            assert(params.context == nil and params.view_mode == nil)
                            assert(params.state.multiplier == 3)
                            return $expression
                        end,
                        render_configuration = function() error("not called during graph preparation") end,
                    }
                """.trimIndent(), LuaStateJsonCodec.decode("{\"multiplier\":3}"), packaged)
                assertNull("${types[index]}, packaged=$packaged", result.error)
                assertTrue(expectedClass.isInstance(result.data))
                if (result.data is LuaGraphResultData.TextData) assertEquals("18", result.data.text)
            }
        }
    }

    @Test
    fun `native preparation receives nil state when no configuration was saved`() {
        val result = runGraph("""
            return { type = "TEXT", prepare_data = function(params)
                assert(params.state == nil)
                return { type = "TEXT", text = params.sources.input.dp().value }
            end }
        """.trimIndent())
        assertNull(result.error)
        assertEquals("6", (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `legacy functions still receive sources directly and plain result tables still work`() {
        for (packaged in listOf(false, true)) {
            for (script in listOf(
                "return function(sources) assert(sources.sources == nil); return { type = 'TEXT', text = sources.input.dp().value } end",
                "return { type = 'TEXT', text = '6' }",
            )) {
                val result = runGraph(script, packaged = packaged)
                assertNull(result.error)
                assertEquals("6", (result.data as LuaGraphResultData.TextData).text)
            }
        }
    }

    @Test
    fun `both legacy functions and native preparation may return nil for no graph`() {
        for (script in listOf(
            "return function(sources) return nil end",
            "return { type = 'TEXT', prepare_data = function(params) return nil end }",
        )) {
            val result = runGraph(script)
            assertNull(result.error)
            assertNull(result.data)
        }
    }

    @Test
    fun `invalid native preparation is reported as a graph error`() {
        for (preparation in listOf(
            "42",
            "function() error('broken preparation') end",
            "function() return 42 end",
            "function() return { type = 'PIE_CHART', segments = {} } end",
            "function() return {} end",
        )) {
            val result = runGraph("return { type = 'TEXT', prepare_data = $preparation }")
            assertNotNull(result.error)
            assertNull(result.data)
        }
    }

    @Test
    fun `prepared results are parsed once rather than executed as nested descriptors`() {
        val result = runGraph("""
            return { type = 'TEXT', prepare_data = function()
                return { type = 'TEXT', text = 'prepared',
                    prepare_data = function() error('must not recurse') end }
            end }
        """.trimIndent())
        assertNull(result.error)
        assertEquals("prepared", (result.data as LuaGraphResultData.TextData).text)
    }
}
