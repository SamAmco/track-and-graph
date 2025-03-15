package com.samco.trackandgraph.lua.community

import com.samco.trackandgraph.lua.LuaEngineImplTest
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.threeten.bp.ZonedDateTime

class DifferenceLineTest : LuaEngineImplTest() {
    private val scriptPath = "generated/lua-community/line-graphs/difference/script.lua"

    @Test
    fun `difference line calculates difference of datapoints`() = testCommunityScript(
        filePath = scriptPath,
        configOverrides = mapOf("from_now" to "false"),
        dataSources = mapOf(
            "source1" to sequenceOf(
                TestDP(5, 5.0),
                TestDP(4, 4.0),
                TestDP(3, 3.0),
                TestDP(2, 2.0),
                TestDP(1, 1.0),
            )
        )
    ) {
        println(result)

        assert(result.data is LuaGraphResultData.LineGraphData)
        val data = result.data as LuaGraphResultData.LineGraphData

        assertEquals(listOf(1.0, 1.0, 1.0, 1.0), data.lines!![0].linePoints.map { it.value })
    }

    @Test
    fun `difference line works with totalling period`() = testCommunityScript(
        filePath = scriptPath,
        configOverrides = mapOf(
            "from_now" to "false",
            "totalling_period" to "core.PERIOD.DAY"
        ),
        dataSources = mapOf(
            "source1" to sequenceOf(
                1 to 0,
                2 to 0,
                3 to 1,
                4 to 2,
                5 to 2,
                1 to 3,
                2 to 3,
                3 to 3,
            ).map {
                TestDP(
                    ZonedDateTime.now().minusDays(it.second.toLong()).toEpochSecond() * 1000L,
                    it.first.toDouble()
                )
            }
        ),
    ) {
        println(result)

        assert(result.data is LuaGraphResultData.LineGraphData)
        val data = result.data as LuaGraphResultData.LineGraphData

        assertEquals(listOf(0.0, -6.0, 3.0), data.lines!![0].linePoints.map { it.value })
    }

    @Test
    fun `difference works with multiple datasources`() = testCommunityScript(
        filePath = scriptPath,
        configOverrides = mapOf("from_now" to "false"),
        dataSources = mapOf(
            "source1" to sequenceOf(
                TestDP(5, 5.0),
                TestDP(4, 4.0),
                TestDP(3, 1.0),
                TestDP(2, 2.0),
                TestDP(1, 5.0),
            ),
            "source2" to sequenceOf(
                TestDP(5, 1.0),
                TestDP(4, -9.0),
                TestDP(3, 20.0),
                TestDP(2, 7.0),
                TestDP(1, 8.0),
            )
        )
    ) {
        println(result)

        assert(result.data is LuaGraphResultData.LineGraphData)
        val data = result.data as LuaGraphResultData.LineGraphData

        assertEquals(listOf(1.0, 3.0, -1.0, -3.0), data.lines!![0].linePoints.map { it.value })
        assertEquals(listOf(10.0, -29.0, 13.0, -1.0), data.lines!![1].linePoints.map { it.value })
    }
}