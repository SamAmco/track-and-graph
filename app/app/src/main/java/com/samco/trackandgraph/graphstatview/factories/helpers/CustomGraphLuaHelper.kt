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
import com.samco.trackandgraph.data.lua.dto.LuaCustomGraphRenderer
import com.samco.trackandgraph.data.lua.dto.LuaGraphContext
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
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
            override val renderData = customData.renderData
            override val renderer = LuaCustomGraphRenderer { renderData, context ->
                render(customData.renderFunction, renderData, context)
            }
            override val state = IGraphStatViewData.State.READY
            override val graphOrStat = graphOrStat
        }
        override val state = IGraphStatViewData.State.READY
        override val graphOrStat = graphOrStat
    }

    private suspend fun render(
        renderFunction: LuaValueReference,
        renderData: LuaValueReference,
        context: LuaGraphContext,
    ): LuaValueAccessor {
        val vmLock = luaEngine.acquireVM(renderFunction.affinity)
        return try {
            luaEngine.renderLuaGraph(vmLock, renderFunction, renderData, context)
        } finally {
            luaEngine.releaseVM(vmLock)
        }
    }
}
