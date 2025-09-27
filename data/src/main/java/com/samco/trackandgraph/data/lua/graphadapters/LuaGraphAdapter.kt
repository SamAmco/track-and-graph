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

import com.samco.trackandgraph.data.lua.apiimpl.LuaDataSourceProviderImpl
import com.samco.trackandgraph.data.lua.dto.LuaGraphResult
import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class LuaGraphAdapter @Inject constructor(
    private val luaDataSourceProviderImpl: LuaDataSourceProviderImpl,
    private val dataPointLuaGraphAdapter: DataPointLuaGraphAdapter,
    private val textLuaGraphAdapter: TextLuaGraphAdapter,
    private val pieChartLuaGraphAdapter: PieChartLuaGraphAdapter,
    private val lineGraphLuaGraphAdapter: LineGraphLuaGraphAdapter,
    private val timeBarChartLuaGraphAdapter: TimeBarChartLuaGraphAdapter,
) {

    fun process(resolvedScript: LuaValue, params: LuaGraphEngineParams): LuaGraphResult {
        val dataSources = luaDataSourceProviderImpl.createDataSourceTable(params.dataSources)
        val scriptResult = executeScript(resolvedScript, dataSources)
        return processLuaGraphResult(scriptResult)
    }

    fun executeScript(resolvedScript: LuaValue, dataSources: LuaValue): LuaValue {
        return when {
            resolvedScript.isfunction() -> resolvedScript.checkfunction()!!.call(dataSources)
            resolvedScript.istable() -> resolvedScript
            else -> throw IllegalArgumentException("Invalid lua graph script result. Must be a function or table")
        }
    }

    private fun processLuaGraphResult(resolvedResult: LuaValue): LuaGraphResult {
        if (resolvedResult.isnil()) return LuaGraphResult(data = null)

        val type = resolvedResult[TYPE].checkjstring()
            ?: throw IllegalArgumentException("No valid type found for lua graph script result")

        val data = when (type) {
            DATA_POINT -> dataPointLuaGraphAdapter.process(resolvedResult)
            TEXT -> textLuaGraphAdapter.process(resolvedResult)
            PIE_CHART -> pieChartLuaGraphAdapter.process(resolvedResult)
            LINE_GRAPH -> lineGraphLuaGraphAdapter.process(resolvedResult)
            TIME_BARCHART -> timeBarChartLuaGraphAdapter.process(resolvedResult)
            else -> throw IllegalArgumentException("Unknown lua graph type: $type")
        }

        return LuaGraphResult(data = data, error = null)
    }

    companion object {
        const val TEXT = "TEXT"
        const val PIE_CHART = "PIE_CHART"
        const val LINE_GRAPH = "LINE_GRAPH"
        const val TIME_BARCHART = "TIME_BARCHART"
        const val TYPE = "type"
        const val DATA_POINT = "DATA_POINT"
    }
}
