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
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.apiimpl.LuaDataSourceProviderImpl
import com.samco.trackandgraph.data.lua.dto.LuaGraphResult
import com.samco.trackandgraph.data.lua.dto.LuaScriptException
import com.samco.trackandgraph.data.lua.graphadapters.DataPointLuaGraphAdapter
import com.samco.trackandgraph.data.lua.graphadapters.LineGraphLuaGraphAdapter
import com.samco.trackandgraph.data.lua.graphadapters.PieChartLuaGraphAdapter
import com.samco.trackandgraph.data.lua.graphadapters.TextLuaGraphAdapter
import com.samco.trackandgraph.data.lua.graphadapters.TimeBarChartLuaGraphAdapter
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class LuaEngineImpl @Inject constructor(
    private val dataPointLuaGraphAdapter: DataPointLuaGraphAdapter,
    private val textLuaGraphAdapter: TextLuaGraphAdapter,
    private val pieChartLuaGraphAdapter: PieChartLuaGraphAdapter,
    private val lineGraphLuaGraphAdapter: LineGraphLuaGraphAdapter,
    private val timeBarChartLuaGraphAdapter: TimeBarChartLuaGraphAdapter,
    private val luaScriptResolver: LuaScriptResolver,
    private val luaDataSourceProviderImpl: LuaDataSourceProviderImpl,
) : LuaEngine {

    override fun runLuaGraphScript(
        script: String,
        params: LuaEngine.LuaGraphEngineParams
    ): LuaGraphResult {
        return try {
            val dataSources = luaDataSourceProviderImpl.createDataSourceTable(params.dataSources)
            processLuaGraphResult(
                luaScriptResolver.resolveLuaGraphScriptResult(
                    script = script,
                    dataSources = dataSources,
                )
            )
        } catch (luaError: LuaError) {
            // Wrap LuaError in our custom exception to avoid exposing luak library types to app module
            val luaScriptException = LuaScriptException(
                message = luaError.message ?: "Lua script error",
                luaCauseStackTrace = luaError.luaCause?.stackTraceToString()
            )
            LuaGraphResult(error = luaScriptException)
        } catch (t: Throwable) {
            LuaGraphResult(error = t)
        }
    }

    private fun processLuaGraphResult(
        resolvedResult: LuaValue
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
