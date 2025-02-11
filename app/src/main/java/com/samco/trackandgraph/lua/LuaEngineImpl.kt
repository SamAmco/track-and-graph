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

import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.lua.dto.LuaGraphResult
import com.samco.trackandgraph.lua.graphadapters.DataPointLuaGraphAdapter
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.Companion.tableOf
import org.luaj.vm2.LuaValue.Companion.valueOf
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import javax.inject.Inject
import com.samco.trackandgraph.assetreader.AssetReader

class LuaEngineImpl @Inject constructor(
    private val dataPointLuaGraphAdapter: DataPointLuaGraphAdapter,
    private val assetReader: AssetReader,
) : LuaEngine {

    private val globals by lazy {
        val globals = JsePlatform.standardGlobals()
        val tngScript = assetReader.readAssetToString("generated/lua/tng.lua")
        globals[LuaEngine.TNG] = globals.load(tngScript).call()
        globals
    }

    private val tngTable: LuaValue by lazy {
        globals[LuaEngine.TNG].opttable(tableOf()) ?: tableOf()
    }

    private val graphTable: LuaValue by lazy {
        tngTable[LuaEngine.GRAPH].opttable(tableOf()) ?: tableOf()
    }

    override fun runLuaGraphScript(
        script: String,
        next: (String, Int) -> List<DataPoint>
    ): LuaGraphResult {
        try {
            graphTable[LuaEngine.NEXT_DP] = getNextLuaFunction(next)
            graphTable[LuaEngine.NEXT_DP_BATCH] = getNextBatchLuaFunction(next)
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
            else -> throw IllegalArgumentException("Unknown lua graph type: $type")
        }

        return LuaGraphResult(data = data)
    }

    private fun getNextBatchLuaFunction(next: (String, Int) -> List<DataPoint>): LuaValue {
        return object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val name = arg1.checkjstring()
                    ?: throw IllegalArgumentException("Name must be provided and be a string")
                val count = arg2.optint(1)
                val dataPoints = next(name, count)

                // Convert the List<DataPoint> to a Lua table
                val luaTable = tableOf()
                dataPoints.forEachIndexed { index, dataPoint ->
                    val luaDataPoint = tableOf()
                    val epochMilli = dataPoint.timestamp.toInstant().toEpochMilli().toDouble()
                    luaDataPoint[LuaEngine.TIMESTAMP] = valueOf(epochMilli)
                    luaDataPoint[LuaEngine.FEATURE_ID] = valueOf(dataPoint.featureId.toString())
                    luaDataPoint[LuaEngine.VALUE] = valueOf(dataPoint.value)
                    luaDataPoint[LuaEngine.LABEL] = valueOf(dataPoint.label)
                    luaDataPoint[LuaEngine.NOTE] = valueOf(dataPoint.note)
                    luaTable[index + 1] = luaDataPoint
                }
                return luaTable
            }
        }
    }

    private fun getNextLuaFunction(next: (String, Int) -> List<DataPoint>): LuaValue {
        return object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val name = arg.checkjstring()
                    ?: throw IllegalArgumentException("Name must be provided and be a string")
                return next(name, 1).firstOrNull()?.toLuaValue() ?: NIL
            }
        }
    }

    private fun DataPoint.toLuaValue(): LuaValue {
        val luaDataPoint = tableOf()
        val epochMilli = timestamp.toInstant().toEpochMilli().toDouble()
        luaDataPoint[LuaEngine.TIMESTAMP] = valueOf(epochMilli)
        luaDataPoint[LuaEngine.FEATURE_ID] = valueOf(featureId.toString())
        luaDataPoint[LuaEngine.VALUE] = valueOf(value)
        luaDataPoint[LuaEngine.LABEL] = valueOf(label)
        luaDataPoint[LuaEngine.NOTE] = valueOf(note)
        return luaDataPoint
    }
}
