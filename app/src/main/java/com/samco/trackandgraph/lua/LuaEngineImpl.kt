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

import com.samco.trackandgraph.assetreader.AssetReader
import com.samco.trackandgraph.lua.apiimpl.GraphApiImpl
import com.samco.trackandgraph.lua.apiimpl.TimeApiImpl
import com.samco.trackandgraph.lua.dto.LuaGraphResult
import com.samco.trackandgraph.lua.graphadapters.DataPointLuaGraphAdapter
import com.samco.trackandgraph.lua.graphadapters.TextLuaGraphAdapter
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import javax.inject.Inject

class LuaEngineImpl @Inject constructor(
    private val assetReader: AssetReader,
    private val dataPointLuaGraphAdapter: DataPointLuaGraphAdapter,
    private val textLuaGraphAdapter: TextLuaGraphAdapter,
    private val graphApiImpl: GraphApiImpl,
    private val timeApiImpl: TimeApiImpl,
) : LuaEngine {

    private val globals by lazy {
        val globals = JsePlatform.standardGlobals()
        val tngScript = assetReader.readAssetToString("generated/lua/tng.lua")
        globals[LuaEngine.TNG] = globals.load(tngScript).call()
        globals
    }

    private val tngTable: LuaTable by lazy {
        //Fail hard if this is not defined to make sure tests fail
        globals[LuaEngine.TNG].checktable()!!
    }

    override fun runLuaGraphScript(
        script: String,
        params: LuaEngine.LuaGraphEngineParams
    ): LuaGraphResult {
        try {
            graphApiImpl.installIn(tngTable, params)
            timeApiImpl.installIn(tngTable)
            return processLuaGraph(globals.load(script).call())
        } catch (t: Throwable) {
            return LuaGraphResult(error = t)
        }
    }

    private fun processLuaGraph(scriptResult: LuaValue): LuaGraphResult {
        val type = scriptResult[LuaEngine.TYPE].checkjstring()
            ?: throw IllegalArgumentException("No valid type found")
        val dataLua = scriptResult[LuaEngine.DATA]

        val data = when (type) {
            LuaEngine.DATAPOINT -> dataPointLuaGraphAdapter.process(dataLua)
            LuaEngine.TEXT -> textLuaGraphAdapter.process(dataLua)
            else -> throw IllegalArgumentException("Unknown lua graph type: $type")
        }

        return LuaGraphResult(data = data)
    }
}
