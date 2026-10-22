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
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaUiRenderResult
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
import com.samco.trackandgraph.data.lua.dto.LuaValueData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaCustomGraphViewData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CustomGraphLuaHelperTest {

    private val luaEngine: LuaEngine = mock()
    private val vmLock = TestLuaVMFixtures.createTestLuaVMLock()
    private val renderData: LuaValueReference = mock()
    private val renderState: LuaValueReference = mock()
    private val renderFunction: LuaValueReference = mock()
    private val result: LuaValueAccessor = mock()
    private val stateSnapshot = LuaValueData.TableValue(emptyMap())
    private val renderResult = LuaUiRenderResult(result, stateSnapshot, renderState)
    private val context: LuaUiContext = mock()

    @Test
    fun `renderer reacquires the VM that owns the retained render function`() = runTest {
        whenever(renderFunction.affinity).thenReturn(vmLock.affinity)
        whenever(luaEngine.acquireVM(vmLock.affinity)).thenReturn(vmLock)
        whenever(luaEngine.renderLuaUi(vmLock, renderFunction, renderState, context, renderData))
            .thenReturn(renderResult)

        val viewData = CustomGraphLuaHelper(luaEngine)(
            customData = LuaGraphResultData.CustomData(renderData, renderFunction, renderState),
            graphOrStat = GraphOrStat(1, "custom", GraphStatType.LUA_SCRIPT, true),
        ).wrapped as ILuaCustomGraphViewData

        assertSame(renderResult, viewData.content.render(context))
        verify(luaEngine).acquireVM(vmLock.affinity)
        verify(luaEngine).renderLuaUi(vmLock, renderFunction, renderState, context, renderData)
        verify(luaEngine).releaseVM(vmLock)
    }

    @Test
    fun `callback reacquires the VM that owns the retained function`() = runTest {
        val callback: LuaValueReference = mock()
        val argument = LuaValueData.NumberValue(4.0)
        whenever(callback.affinity).thenReturn(vmLock.affinity)
        whenever(luaEngine.acquireVM(vmLock.affinity)).thenReturn(vmLock)
        whenever(luaEngine.callLuaFunction(vmLock, callback, argument)).thenReturn(result)

        val viewData = CustomGraphLuaHelper(luaEngine)(
            customData = LuaGraphResultData.CustomData(renderData, renderFunction, renderState),
            graphOrStat = GraphOrStat(1, "custom", GraphStatType.LUA_SCRIPT, true),
        ).wrapped as ILuaCustomGraphViewData

        assertSame(result, viewData.content.call(callback, argument))
        verify(luaEngine).acquireVM(vmLock.affinity)
        verify(luaEngine).callLuaFunction(vmLock, callback, argument)
        verify(luaEngine).releaseVM(vmLock)
    }

    @Test
    fun `interaction calls callback and rerenders before releasing one VM lock`() = runTest {
        val callback: LuaValueReference = mock()
        whenever(renderFunction.affinity).thenReturn(vmLock.affinity)
        whenever(luaEngine.acquireVM(vmLock.affinity)).thenReturn(vmLock)
        whenever(luaEngine.renderLuaUi(vmLock, renderFunction, renderState, context, renderData))
            .thenReturn(renderResult)

        val content = (CustomGraphLuaHelper(luaEngine)(
            customData = LuaGraphResultData.CustomData(renderData, renderFunction, renderState),
            graphOrStat = GraphOrStat(1, "custom", GraphStatType.LUA_SCRIPT, true),
        ).wrapped as ILuaCustomGraphViewData).content

        assertSame(renderResult, content.interact(context, callback))

        inOrder(luaEngine) {
            verify(luaEngine).acquireVM(vmLock.affinity)
            verify(luaEngine).callLuaFunction(vmLock, callback)
            verify(luaEngine).renderLuaUi(
                vmLock,
                renderFunction,
                renderState,
                context,
                renderData,
            )
            verify(luaEngine).releaseVM(vmLock)
        }
    }
}
