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

class LuaEngineImpl @Inject constructor(
    private val dataPointLuaGraphAdapter: DataPointLuaGraphAdapter
) : LuaEngine {

    // Create the Tng and graph tables
    private val tngTable = tableOf()
    private val graphTable = tableOf()

    private val globals by lazy {
        val globals = JsePlatform.standardGlobals()
        tngTable[LuaEngine.GRAPH] = graphTable
        globals[LuaEngine.TNG] = tngTable
        globals
    }

    override fun runLuaGraphScript(
        script: String,
        next: (String, Int) -> List<DataPoint>
    ): LuaGraphResult {
        try {
            graphTable[LuaEngine.NEXT] = getNextLuaFunction(next)
            graphTable[LuaEngine.NEXT_BATCH] = getNextBatchLuaFunction(next)
            return processLuaGraph(globals.load(script).call())
        } catch (t: Throwable) {
            return LuaGraphResult(error = t)
        }
    }

    private fun processLuaGraph(scriptResult: LuaValue): LuaGraphResult {
        val type = scriptResult[LuaEngine.TYPE].checkjstring()
            ?: throw IllegalArgumentException("No valid type found")

        val data = when (type) {
            LuaEngine.DATAPOINT -> dataPointLuaGraphAdapter.process(scriptResult)
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
