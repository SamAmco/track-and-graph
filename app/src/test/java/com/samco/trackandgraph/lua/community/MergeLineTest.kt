package com.samco.trackandgraph.lua.community

import com.samco.trackandgraph.lua.LuaEngineImplTest
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import junit.framework.TestCase.assertEquals
import org.junit.Test

class MergeLineTest : LuaEngineImplTest() {
    @Test
    fun `merge line merges datapoints from multiple sources in time order`() = testCommunityScript(
        filePath = "generated/lua-community/line-graphs/merge-inputs/script.lua",
        configOverrides = mapOf("from_now" to "false"),
        dataSources = mapOf(
            "source1" to sequenceOf(
                TestDP(5, 1.0),
                TestDP(4, 4.0),
                TestDP(1, 5.0),
            ),
            "source2" to sequenceOf(
                TestDP(6, 2.0),
                TestDP(3, 3.0),
                TestDP(2, 6.0),
            ),
        )
    ) {
        println(result)

        assert(result.data is LuaGraphResultData.LineGraphData)
        val data = result.data as LuaGraphResultData.LineGraphData

        assertEquals(listOf(2.0, 1.0, 4.0, 3.0, 6.0, 5.0), data.lines!![0].linePoints.map { it.value })
    }
}