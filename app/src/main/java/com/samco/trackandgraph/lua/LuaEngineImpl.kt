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
package com.samco.trackandgraph.lua

import com.samco.trackandgraph.base.database.sampling.RawDataSample
import com.samco.trackandgraph.lua.apiimpl.LuaDataSourceProviderImpl
import com.samco.trackandgraph.lua.apiimpl.RequireApiImpl
import com.samco.trackandgraph.lua.dto.LuaGraphResult
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.graphadapters.DataPointLuaGraphAdapter
import com.samco.trackandgraph.lua.graphadapters.LineGraphLuaGraphAdapter
import com.samco.trackandgraph.lua.graphadapters.PieChartLuaGraphAdapter
import com.samco.trackandgraph.lua.graphadapters.TextLuaGraphAdapter
import com.samco.trackandgraph.lua.graphadapters.TimeBarChartLuaGraphAdapter
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.CoroutineLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.jse.JseBaseLib
import org.luaj.vm2.lib.jse.JseMathLib
import javax.inject.Inject

class LuaEngineImpl @Inject constructor(
    private val dataPointLuaGraphAdapter: DataPointLuaGraphAdapter,
    private val textLuaGraphAdapter: TextLuaGraphAdapter,
    private val pieChartLuaGraphAdapter: PieChartLuaGraphAdapter,
    private val lineGraphLuaGraphAdapter: LineGraphLuaGraphAdapter,
    private val timeBarChartLuaGraphAdapter: TimeBarChartLuaGraphAdapter,
    private val requireApi: RequireApiImpl,
    private val luaDataSourceProviderImpl: LuaDataSourceProviderImpl,
) : LuaEngine {

    private val globals by lazy {
        val globals = Globals()
        // Only install libraries that are useful and not dangerous
        globals.load(JseBaseLib())
        globals.load(PackageLib())
        globals.load(Bit32Lib())
        globals.load(TableLib())
        globals.load(StringLib())
        globals.load(CoroutineLib())
        globals.load(JseMathLib())
        // Remove potentially dangerous functions from BaseLib
        globals["dofile"] = LuaValue.NIL
        globals["loadfile"] = LuaValue.NIL
        globals["pcall"] = LuaValue.NIL
        globals["xpcall"] = LuaValue.NIL
        globals["package"] = LuaValue.NIL
        // Print isn't dangerous but it won't work so I would rather throw than
        // fail silently as it may be confusing
        globals["print"] = LuaValue.NIL
        LoadState.install(globals)
        LuaC.install(globals)
        requireApi.installIn(globals)
        globals
    }

    override fun runLuaGraphScript(
        script: String,
        params: LuaEngine.LuaGraphEngineParams
    ): LuaGraphResult {
        try {
            val cleanedScript = script.cleanLuaScript()
            return processLuaGraph(
                scriptResult = globals.load(cleanedScript).call(),
                dataSources = params.dataSources
            )
        } catch (t: Throwable) {
            return LuaGraphResult(error = t)
        }
    }

    private fun processLuaGraph(
        scriptResult: LuaValue,
        dataSources: Map<String, RawDataSample>
    ): LuaGraphResult {
        val resolvedResult = when {
            scriptResult.isfunction() -> scriptResult.checkfunction()!!
                .call(luaDataSourceProviderImpl.createDataSourceTable(dataSources))

            scriptResult.istable() -> scriptResult
            else -> throw IllegalArgumentException("Invalid lua graph script result. Must be a function or table")
        }

        if (resolvedResult.isnil()) return LuaGraphResult(data = null)

        return LuaGraphResult(data = parseResolvedGraphScriptResult(resolvedResult))
    }

    private fun parseResolvedGraphScriptResult(resolvedResult: LuaValue): LuaGraphResultData {
        val type = resolvedResult[TYPE].checkjstring()
            ?: throw IllegalArgumentException("No valid type found for lua graph script result")

        return when (type) {
            DATA_POINT -> dataPointLuaGraphAdapter.process(resolvedResult)
            TEXT -> textLuaGraphAdapter.process(resolvedResult)
            PIE_CHART -> pieChartLuaGraphAdapter.process(resolvedResult)
            LINE_GRAPH -> lineGraphLuaGraphAdapter.process(resolvedResult)
            TIME_BARCHART -> timeBarChartLuaGraphAdapter.process(resolvedResult)
            else -> throw IllegalArgumentException("Unknown lua graph type: $type")
        }
    }

    private fun String.cleanLuaScript(): String {
        return this
            // Replace NBSP with space
            .replace(Regex("\\u00A0"), " ")
            // Remove zero-width space and BOM
            .replace(Regex("[\\u200B\\uFEFF]"), "")
            // Replace all new lines with the same newline character
            .replace(Regex("[\\r\\n]+"), "\n")
            .trim()
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
