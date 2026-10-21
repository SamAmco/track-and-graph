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
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.dto.LuaUiContent
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaUiRenderResult
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
import com.samco.trackandgraph.data.lua.dto.LuaValueData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaCustomGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import javax.inject.Inject

class CustomGraphLuaHelper @Inject constructor(
    private val luaEngine: LuaEngine,
) {
    operator fun invoke(
        customData: LuaGraphResultData.CustomData,
        graphOrStat: GraphOrStat,
    ): ILuaGraphViewData = object : ILuaGraphViewData {
        override val wrapped = object : ILuaCustomGraphViewData {
            override val content = object : LuaUiContent {
                private var state = customData.renderState

                override suspend fun render(context: LuaUiContext): LuaUiRenderResult =
                    render(customData.renderFunction, state, customData.renderData, context)
                        .also { state = it.state }

                override suspend fun interact(
                    context: LuaUiContext,
                    function: LuaValueReference,
                ): LuaUiRenderResult {
                    val vmLock = luaEngine.acquireVM(customData.renderFunction.affinity)
                    return try {
                        luaEngine.callLuaFunction(vmLock, function)
                        luaEngine.renderLuaGraph(
                            vmLock,
                            customData.renderFunction,
                            state,
                            customData.renderData,
                            context,
                        ).also { state = it.state }
                    } finally {
                        luaEngine.releaseVM(vmLock)
                    }
                }

                override suspend fun call(function: LuaValueReference, argument: LuaValueData) =
                    callFunction(function, argument)
            }
            override val state = IGraphStatViewData.State.READY
            override val graphOrStat = graphOrStat
        }
        override val state = IGraphStatViewData.State.READY
        override val graphOrStat = graphOrStat
    }

    private suspend fun render(
        renderFunction: LuaValueReference,
        renderState: LuaValueReference,
        renderData: LuaValueReference,
        context: LuaUiContext,
    ): LuaUiRenderResult {
        val vmLock = luaEngine.acquireVM(renderFunction.affinity)
        return try {
            luaEngine.renderLuaGraph(vmLock, renderFunction, renderState, renderData, context)
        } finally {
            luaEngine.releaseVM(vmLock)
        }
    }

    private suspend fun callFunction(
        function: LuaValueReference,
        argument: LuaValueData,
    ): LuaValueAccessor {
        val vmLock = luaEngine.acquireVM(function.affinity)
        return try {
            luaEngine.callLuaFunction(vmLock, function, argument)
        } finally {
            luaEngine.releaseVM(vmLock)
        }
    }
}
