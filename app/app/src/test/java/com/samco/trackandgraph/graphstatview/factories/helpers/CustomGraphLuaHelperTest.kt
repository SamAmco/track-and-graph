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
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaCustomGraphViewData
import com.samco.trackandgraph.lua.ui.LuaUiAction
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
    private val renderData: LuaValueHandle = mock()
    private val renderState: LuaValueHandle = mock()
    private val renderFunction: LuaValueHandle = mock()
    private val result: LuaValueHandle = mock()
    private val context: LuaUiContext = mock()

    @Test
    fun `renderer reacquires the VM that owns the retained render function`() = runTest {
        whenever(renderFunction.affinity).thenReturn(vmLock.affinity)
        whenever(luaEngine.acquireVM(vmLock.affinity)).thenReturn(vmLock)
        whenever(luaEngine.renderLuaUi(vmLock, renderFunction, renderState, context, mapOf("data" to renderData)))
            .thenReturn(result)

        val viewData = CustomGraphLuaHelper(luaEngine)(
            customData = LuaGraphResultData.CustomData(renderData, renderFunction, renderState),
            graphOrStat = GraphOrStat(1, "custom", GraphStatType.LUA_SCRIPT, true),
        ).wrapped as ILuaCustomGraphViewData

        assertSame(result, viewData.content.render(context))
        verify(luaEngine).acquireVM(vmLock.affinity)
        verify(luaEngine).renderLuaUi(vmLock, renderFunction, renderState, context, mapOf("data" to renderData))
        verify(luaEngine).releaseVM(vmLock)
    }

    @Test
    fun `callback reacquires the VM that owns the retained function`() = runTest {
        val callback: LuaValueHandle = mock()
        val callbackLock = TestLuaVMFixtures.createTestLuaVMLock()
        val argument = LuaValueSnapshot.NumberValue(4.0)
        whenever(renderFunction.affinity).thenReturn(vmLock.affinity)
        whenever(callback.affinity).thenReturn(callbackLock.affinity)
        whenever(luaEngine.acquireVM(callbackLock.affinity)).thenReturn(callbackLock)
        whenever(luaEngine.callLuaFunction(callbackLock, callback, argument)).thenReturn(result)

        val viewData = CustomGraphLuaHelper(luaEngine)(
            customData = LuaGraphResultData.CustomData(renderData, renderFunction, renderState),
            graphOrStat = GraphOrStat(1, "custom", GraphStatType.LUA_SCRIPT, true),
        ).wrapped as ILuaCustomGraphViewData

        assertSame(result, viewData.content.call(callback, argument))
        verify(luaEngine).acquireVM(callbackLock.affinity)
        verify(luaEngine).callLuaFunction(callbackLock, callback, argument)
        verify(luaEngine).releaseVM(callbackLock)
    }

    @Test
    fun `callback releases its VM lock when invocation fails`() = runTest {
        val callback: LuaValueHandle = mock()
        val failure = IllegalStateException("callback failed")
        whenever(callback.affinity).thenReturn(vmLock.affinity)
        whenever(luaEngine.acquireVM(vmLock.affinity)).thenReturn(vmLock)
        whenever(luaEngine.callLuaFunction(vmLock, callback)).thenThrow(failure)

        val content = (CustomGraphLuaHelper(luaEngine)(
            customData = LuaGraphResultData.CustomData(renderData, renderFunction, renderState),
            graphOrStat = GraphOrStat(1, "custom", GraphStatType.LUA_SCRIPT, true),
        ).wrapped as ILuaCustomGraphViewData).content

        assertSame(failure, runCatching { content.call(callback) }.exceptionOrNull())
        verify(luaEngine).releaseVM(vmLock)
    }

    @Test
    fun `interaction calls callback and rerenders before releasing one VM lock`() = runTest {
        val callback: LuaValueHandle = mock()
        whenever(renderFunction.affinity).thenReturn(vmLock.affinity)
        whenever(luaEngine.acquireVM(vmLock.affinity)).thenReturn(vmLock)
        whenever(luaEngine.renderLuaUi(vmLock, renderFunction, renderState, context, mapOf("data" to renderData)))
            .thenReturn(result)

        val content = (CustomGraphLuaHelper(luaEngine)(
            customData = LuaGraphResultData.CustomData(renderData, renderFunction, renderState),
            graphOrStat = GraphOrStat(1, "custom", GraphStatType.LUA_SCRIPT, true),
        ).wrapped as ILuaCustomGraphViewData).content

        assertSame(result, content.interact(context, listOf(LuaUiAction(callback))))

        inOrder(luaEngine) {
            verify(luaEngine).acquireVM(vmLock.affinity)
            verify(luaEngine).callLuaFunction(vmLock, callback)
            verify(luaEngine).renderLuaUi(
                vmLock,
                renderFunction,
                renderState,
                context,
                mapOf("data" to renderData),
            )
            verify(luaEngine).releaseVM(vmLock)
        }
    }

    @Test
    fun `caller additional params merge with data on render and interaction without resetting state`() = runTest {
        val callback: LuaValueHandle = mock()
        val nextState: LuaValueHandle = mock()
        val listParams = mapOf("view_mode" to LuaValueSnapshot.StringValue("LIST"))
        val fullParams = mapOf("view_mode" to LuaValueSnapshot.StringValue("FULL_SCREEN"))
        val dataParams = mapOf("data" to renderData)
        whenever(renderFunction.affinity).thenReturn(vmLock.affinity)
        whenever(luaEngine.acquireVM(vmLock.affinity)).thenReturn(vmLock)
        whenever(luaEngine.renderLuaUi(vmLock, renderFunction, renderState, context, dataParams + listParams))
            .thenReturn(result)
        whenever(luaEngine.renderLuaUi(vmLock, renderFunction, nextState, context, dataParams + fullParams))
            .thenReturn(result)
        whenever(result["state"]).thenReturn(nextState)

        val content = (CustomGraphLuaHelper(luaEngine)(
            customData = LuaGraphResultData.CustomData(renderData, renderFunction, renderState),
            graphOrStat = GraphOrStat(1, "custom", GraphStatType.LUA_SCRIPT, true),
        ).wrapped as ILuaCustomGraphViewData).content

        assertSame(result, content.render(context, listParams))
        assertSame(nextState, content.state)
        assertSame(result, content.interact(context, listOf(LuaUiAction(callback)), fullParams))
        verify(luaEngine).callLuaFunction(vmLock, callback)
        verify(luaEngine).renderLuaUi(vmLock, renderFunction, nextState, context, dataParams + fullParams)
        assertSame(nextState, content.state)
    }

    @Test
    fun `content retains state from the complete renderer response`() = runTest {
        val nextState: LuaValueHandle = mock()
        whenever(renderFunction.affinity).thenReturn(vmLock.affinity)
        whenever(luaEngine.acquireVM(vmLock.affinity)).thenReturn(vmLock)
        whenever(luaEngine.renderLuaUi(vmLock, renderFunction, renderState, context, mapOf("data" to renderData)))
            .thenReturn(result)
        whenever(result["state"]).thenReturn(nextState)

        val content = (CustomGraphLuaHelper(luaEngine)(
            customData = LuaGraphResultData.CustomData(renderData, renderFunction, renderState),
            graphOrStat = GraphOrStat(1, "custom", GraphStatType.LUA_SCRIPT, true),
        ).wrapped as ILuaCustomGraphViewData).content

        assertSame(result, content.render(context))
        assertSame(nextState, content.state)
    }
}
