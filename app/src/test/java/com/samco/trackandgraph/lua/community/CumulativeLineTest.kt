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

import com.samco.trackandgraph.lua.LuaEngineImplTest
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.threeten.bp.ZonedDateTime

class CumulativeLineTest : LuaEngineImplTest() {
    private val scriptPath = "generated/lua-community/line-graphs/cumulative/script.lua"

    @Test
    fun `cumulative line calculates cumulative sum of datapoints`() = testCommunityScript(
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

        assertEquals(listOf(15.0, 10.0, 6.0, 3.0, 1.0), data.lines!![0].linePoints.map { it.value })
    }

    @Test
    fun `cumulative line works with totalling period`() = testCommunityScript(
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

        assertEquals(listOf(21.0, 18.0, 15.0, 6.0), data.lines!![0].linePoints.map { it.value })
    }

    @Test
    fun `cumulative line supports multiple lines`() = testCommunityScript(
        filePath = scriptPath,
        configOverrides = mapOf("from_now" to "false"),
        dataSources = mapOf(
            "source1" to sequenceOf(
                TestDP(5, 5.0),
                TestDP(4, 4.0),
                TestDP(3, 3.0),
                TestDP(2, 2.0),
                TestDP(1, 1.0),
            ),
            "source2" to sequenceOf(
                TestDP(5, 10.0),
                TestDP(4, 9.0),
                TestDP(3, 8.0),
                TestDP(2, 7.0),
                TestDP(1, 6.0),
            )
        )
    ) {
        println(result)

        assert(result.data is LuaGraphResultData.LineGraphData)
        val data = result.data as LuaGraphResultData.LineGraphData

        assertEquals(listOf(15.0, 10.0, 6.0, 3.0, 1.0), data.lines!![0].linePoints.map { it.value })
        assertEquals(listOf(40.0, 30.0, 21.0, 13.0, 6.0), data.lines!![1].linePoints.map { it.value })
    }
}
