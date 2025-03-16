package com.samco.trackandgraph.lua.community

import com.samco.trackandgraph.lua.LuaEngineImplTest
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.TimeBar
import com.samco.trackandgraph.lua.dto.TimeBarSegment
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.threeten.bp.ZonedDateTime

class MergeBarsTest : LuaEngineImplTest() {
    private val scriptPath = "generated/lua-community/bar-charts/merge-inputs/script.lua"

    @Test
    fun `merge bars from multiple sources`() = testCommunityScript(
        filePath = scriptPath,
        configOverrides = mapOf(
            "totalling_period" to "core.PERIOD.DAY",
            "totalling_period_multiplier" to "2",
        ),
        dataSources = mapOf(
            "source1" to sequenceOf(
                TestDP(2, 5.0, "d"),
                TestDP(4, 4.0, "a"),
                TestDP(6, 3.0, "c"),
                TestDP(9, 2.0, "b"),
                TestDP(10, 1.0, "a"),
            ).mapToNowMinusDays(),
            "source2" to sequenceOf(
                TestDP(1, 5.0, "d"),
                TestDP(3, 4.0, "a"),
                TestDP(5, 3.0, "c"),
                TestDP(7, 2.0, "b"),
                TestDP(8, 1.0, "f"),
            ).mapToNowMinusDays(),
        )
    ) {
        println(result)

        assert(result.data is LuaGraphResultData.TimeBarChartData)
        val data = result.data as LuaGraphResultData.TimeBarChartData

        assertEquals(
            listOf(
                TimeBar(
                    listOf(
                        TimeBarSegment(
                            value = 10.0,
                            label = "d",
                        ),
                    )
                ),
                TimeBar(
                    listOf(
                        TimeBarSegment(
                            value = 8.0,
                            label = "a",
                        ),
                    )
                ),
                TimeBar(
                    listOf(
                        TimeBarSegment(
                            value = 6.0,
                            label = "c",
                        ),
                    )
                ),
                TimeBar(
                    listOf(
                        TimeBarSegment(
                            value = 2.0,
                            label = "b",
                        ),
                        TimeBarSegment(
                            value = 1.0,
                            label = "f",
                        ),
                    )
                ),
                TimeBar(
                    listOf(
                        TimeBarSegment(
                            value = 1.0,
                            label = "a",
                        ),
                        TimeBarSegment(
                            value = 2.0,
                            label = "b",
                        ),
                    )
                ),
            ),
            data.bars,
        )
    }

    private fun Sequence<TestDP>.mapToNowMinusDays() = map {
        TestDP(
            ZonedDateTime.now().minusDays(it.timestamp.nano / 1_000_000L).toOffsetDateTime(),
            it.value,
            it.label
        )
    }
}