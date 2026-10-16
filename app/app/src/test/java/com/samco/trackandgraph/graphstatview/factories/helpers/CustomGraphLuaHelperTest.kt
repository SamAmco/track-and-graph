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
package com.samco.trackandgraph.graphstatview.factories.helpers

import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.TestLuaVMFixtures
import com.samco.trackandgraph.data.lua.dto.LuaGraphContext
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaCustomGraphViewData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CustomGraphLuaHelperTest {

    private val luaEngine: LuaEngine = mock()
    private val vmLock = TestLuaVMFixtures.createTestLuaVMLock()
    private val renderData: LuaValueReference = mock()
    private val renderFunction: LuaValueReference = mock()
    private val result: LuaValueAccessor = mock()
    private val context: LuaGraphContext = mock()

    @Test
    fun `renderer reacquires the VM that owns the retained render function`() = runTest {
        whenever(renderFunction.affinity).thenReturn(vmLock.affinity)
        whenever(luaEngine.acquireVM(vmLock.affinity)).thenReturn(vmLock)
        whenever(luaEngine.renderLuaGraph(vmLock, renderFunction, renderData, context))
            .thenReturn(result)

        val viewData = CustomGraphLuaHelper(luaEngine)(
            customData = LuaGraphResultData.CustomData(renderData, renderFunction),
            graphOrStat = GraphOrStat(1, "custom", GraphStatType.LUA_SCRIPT, true),
        ).wrapped as ILuaCustomGraphViewData

        assertSame(result, viewData.renderer.render(viewData.renderData, context))
        verify(luaEngine).acquireVM(vmLock.affinity)
        verify(luaEngine).renderLuaGraph(vmLock, renderFunction, renderData, context)
        verify(luaEngine).releaseVM(vmLock)
    }
}
