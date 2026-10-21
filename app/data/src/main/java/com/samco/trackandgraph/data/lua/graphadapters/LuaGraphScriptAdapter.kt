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
package com.samco.trackandgraph.data.lua.graphadapters

import com.samco.trackandgraph.data.lua.LuaVMAffinity
import com.samco.trackandgraph.data.lua.LuaValueReferenceImpl
import com.samco.trackandgraph.data.lua.LuaStateJsonCodec
import com.samco.trackandgraph.data.lua.apiimpl.LuaDataSourceProviderImpl
import com.samco.trackandgraph.data.lua.dto.LuaGraphResult
import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class LuaGraphScriptAdapter @Inject constructor(
    private val luaDataSourceProviderImpl: LuaDataSourceProviderImpl,
    private val dataPointLuaGraphAdapter: DataPointLuaGraphAdapter,
    private val textLuaGraphAdapter: TextLuaGraphAdapter,
    private val pieChartLuaGraphAdapter: PieChartLuaGraphAdapter,
    private val lineGraphLuaGraphAdapter: LineGraphLuaGraphAdapter,
    private val timeBarChartLuaGraphAdapter: TimeBarChartLuaGraphAdapter,
) {

    fun process(
        resolvedScript: LuaValue,
        params: LuaGraphEngineParams,
        affinity: LuaVMAffinity,
    ): LuaGraphResult {
        val dataSources = luaDataSourceProviderImpl.createDataSourceTable(params.dataSources)
        val scriptResult = executeScript(resolvedScript, dataSources)
        return processLuaGraphResult(scriptResult, dataSources, params.configuration, affinity)
    }

    fun executeScript(resolvedScript: LuaValue, dataSources: LuaValue): LuaValue {
        return when {
            resolvedScript.isfunction() -> resolvedScript.checkfunction()!!.call(dataSources)
            resolvedScript.istable() -> resolvedScript
            else -> throw IllegalArgumentException("Invalid lua graph script result. Must be a function or table")
        }
    }

    private fun processLuaGraphResult(
        resolvedResult: LuaValue,
        dataSources: LuaValue,
        configuration: String?,
        affinity: LuaVMAffinity,
    ): LuaGraphResult {
        if (resolvedResult.isnil()) return LuaGraphResult(data = null)

        val type = resolvedResult[TYPE].checkjstring()
            ?: throw IllegalArgumentException("No valid type found for lua graph script result")

        val data = when (type) {
            DATA_POINT -> dataPointLuaGraphAdapter.process(resolvedResult)
            TEXT -> textLuaGraphAdapter.process(resolvedResult)
            PIE_CHART -> pieChartLuaGraphAdapter.process(resolvedResult)
            LINE_GRAPH -> lineGraphLuaGraphAdapter.process(resolvedResult)
            TIME_BARCHART -> timeBarChartLuaGraphAdapter.process(resolvedResult)
            CUSTOM -> processCustomGraph(resolvedResult, dataSources, configuration, affinity)
            else -> throw IllegalArgumentException("Unknown lua graph type: $type")
        }

        return LuaGraphResult(data = data, error = null)
    }

    private fun processCustomGraph(
        descriptor: LuaValue,
        dataSources: LuaValue,
        configuration: String?,
        affinity: LuaVMAffinity,
    ): LuaGraphResultData.CustomData {
        val renderState = LuaStateJsonCodec.decode(configuration)
        val renderData = descriptor[PREPARE_DATA].checkfunction()!!.call(dataSources, renderState)
        val renderFunction = descriptor[RENDER_GRAPH].checkfunction()!!
        return LuaGraphResultData.CustomData(
            renderData = LuaValueReferenceImpl(renderData, affinity),
            renderFunction = LuaValueReferenceImpl(renderFunction, affinity),
            renderState = LuaValueReferenceImpl(renderState, affinity),
        )
    }

    companion object {
        const val TEXT = "TEXT"
        const val PIE_CHART = "PIE_CHART"
        const val LINE_GRAPH = "LINE_GRAPH"
        const val TIME_BARCHART = "TIME_BARCHART"
        const val TYPE = "type"
        const val DATA_POINT = "DATA_POINT"
        const val CUSTOM = "CUSTOM"
        const val PREPARE_DATA = "prepare_data"
        const val RENDER_GRAPH = "render_graph"
    }
}
