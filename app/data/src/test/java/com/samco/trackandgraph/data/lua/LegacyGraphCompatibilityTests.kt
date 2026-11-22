/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.time.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime

/** Run unchanged published scripts through the real inline engine and native result parsers. */
internal class LegacyGraphCompatibilityTests : LuaEngineImplTest() {
    private val now = ZonedDateTime.parse("2024-07-17T12:00:00Z")
    override val timeProvider = object : TimeProvider {
        override fun now() = now
        override fun epochMilli() = now.toInstant().toEpochMilli()
        override fun defaultZone() = ZoneOffset.UTC
    }

    private fun script(path: String): String = requireNotNull(
        javaClass.classLoader!!.getResourceAsStream("generated/legacy-lua-graphs/$path/script.lua")
    ) { "Missing published legacy graph: $path" }.bufferedReader().use { it.readText() }

    private fun inputs() = mapOf(
        "first" to sequenceOf(
            TestDP(now.minusHours(1).toOffsetDateTime(), value = 5.0, label = "A"),
            TestDP(now.minusHours(3).toOffsetDateTime(), value = 2.0, label = "B"),
        ),
        "second" to sequenceOf(
            TestDP(now.minusHours(2).toOffsetDateTime(), value = 3.0, label = "A"),
            TestDP(now.minusDays(9).toOffsetDateTime(), value = 7.0, label = "C"),
        ),
    )

    @Test
    fun `published text graph retains named sources and rolling date filtering`() = testLuaGraph(
        mapOf("first" to (inputs().getValue("first") + inputs().getValue("second").drop(1))),
        script("text/total-in-last-duration"),
    ) {
        assertNull(result.error)
        assertEquals("7", (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `published line graph still merges sources in reverse chronological order`() = testLuaGraph(
        inputs(), script("line-graphs/merge-inputs"),
    ) {
        assertNull(result.error)
        val line = (result.data as LuaGraphResultData.LineGraphData).lines!!.single()
        assertEquals(listOf(5.0, 3.0, 2.0, 7.0), line.linePoints.map { it.value })
    }

    @Test
    fun `published bar graph retains period grouping and label aggregation`() = testLuaGraph(
        inputs(), script("bar-charts/merge-inputs"),
    ) {
        assertNull(result.error)
        val bars = (result.data as LuaGraphResultData.TimeBarChartData).bars
        assertEquals(mapOf("A" to 8.0, "B" to 2.0), bars.first().segments.associate { it.label to it.value })
        assertEquals(7.0, bars.last().segments.single().value, 0.0)
    }

    @Test
    fun `published pie graph retains merged label totals`() = testLuaGraph(
        inputs(), script("pie-charts/merged-pie-chart"),
    ) {
        assertNull(result.error)
        val segments = (result.data as LuaGraphResultData.PieChartData).segments!!
        assertEquals(mapOf("A" to 8.0, "B" to 2.0, "C" to 7.0), segments.associate { it.label to it.value })
    }

    @Test
    fun `published data point graph retains latest matching source selection`() = testLuaGraph(
        inputs(), script("datapoint/last-value-above-threshold"),
    ) {
        assertNull(result.error)
        assertEquals(5.0, (result.data as LuaGraphResultData.DataPointData).dataPoint!!.value, 0.0)
    }

    @Test
    fun `published data point graph can still produce no result`() = testLuaGraph(
        script("datapoint/last-value-above-threshold"),
    ) {
        assertNull(result.error)
        assertNull(result.data)
    }
}
