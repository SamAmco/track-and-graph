package com.samco.trackandgraph.lua.community

import com.samco.trackandgraph.lua.LuaEngineImplTest
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.TimeBar
import com.samco.trackandgraph.lua.dto.TimeBarSegment
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.threeten.bp.ZonedDateTime

class CumulativeBarChartTest : LuaEngineImplTest() {
    private val scriptPath = "generated/lua-community/bar-charts/cumulative/script.lua"

    @Test
    fun `cumulative bar chart calculates cumulative sum of datapoints`() = testCommunityScript(
        filePath = scriptPath,
        configOverrides = mapOf(
            "totalling_period" to "core.PERIOD.DAY",
            "from_now" to "false"
        ),
        dataSources = mapOf(
            "source1" to sequenceOf(
                1 to 5.0,
                2 to 4.0,
                3 to 3.0,
                4 to 2.0,
                5 to 1.0,
            ).map {
                TestDP(
                    ZonedDateTime.now().minusDays(it.first.toLong()).toOffsetDateTime(),
                    it.second
                )
            }
        )
    ) {
        println(result)

        assert(result.data is LuaGraphResultData.TimeBarChartData)
        val data = result.data as LuaGraphResultData.TimeBarChartData

        assertEquals(
            listOf(15.0, 10.0, 6.0, 3.0, 1.0),
            data.bars.map { it.segments[0].value }
        )
    }

    @Test
    fun `cumulative bar chart with different labels works`() = testCommunityScript(
        filePath = scriptPath,
        configOverrides = mapOf(
            "totalling_period" to "core.PERIOD.DAY",
            "from_now" to "false"
        ),
        dataSources = mapOf(
            "source1" to sequenceOf(
                0 to TestDP(0, 11.0, "l2"),
                1 to TestDP(0, 10.0, "l1"),
                1 to TestDP(0, 8.0, "l2"),
                2 to TestDP(0, 5.0, "l1"),
                2 to TestDP(0, 3.0, "l1"),
                2 to TestDP(0, 1.0, "l2"),
                3 to TestDP(0, 2.0, "l2"),
                3 to TestDP(0, 1.0, "l2"),
            ).map { (days, dp) ->
                return@map TestDP(
                    ZonedDateTime.now().minusDays(days.toLong()).toOffsetDateTime(),
                    dp.value,
                    dp.label,
                )
            }
        )
    ) {
        println(result)

        assert(result.data is LuaGraphResultData.TimeBarChartData)
        val data = result.data as LuaGraphResultData.TimeBarChartData

        // The segments are sorted such that the largest segment at the end of the chart is always first
        assertEquals(
            listOf(
                TimeBar(
                    listOf(
                        TimeBarSegment(23.0, "l2"),
                        TimeBarSegment(18.0, "l1"),
                    )
                ),
                TimeBar(
                    listOf(
                        TimeBarSegment(12.0, "l2"),
                        TimeBarSegment(18.0, "l1"),
                    )
                ),
                TimeBar(
                    listOf(
                        TimeBarSegment(4.0, "l2"),
                        TimeBarSegment(8.0, "l1"),
                    )
                ),
                TimeBar(
                    listOf(
                        TimeBarSegment(3.0, "l2"),
                        TimeBarSegment(0.0, "l1"),
                    )
                ),
            ),
            data.bars
        )
    }
}