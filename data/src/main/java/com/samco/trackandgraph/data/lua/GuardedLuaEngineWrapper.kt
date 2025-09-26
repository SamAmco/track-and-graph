package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.lua.dto.LuaEngineDisabledException
import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import com.samco.trackandgraph.data.lua.dto.LuaGraphResult
import com.samco.trackandgraph.data.sampling.RawDataSample
import javax.inject.Inject

class GuardedLuaEngineWrapper @Inject internal constructor(
    private val luaEngine: LuaEngineImpl,
    private val luaEngineSwitch: LuaEngineSwitch
) : LuaEngine {
    override fun runLuaGraphScript(script: String, params: LuaGraphEngineParams): LuaGraphResult {
        return if (luaEngineSwitch.enabled) luaEngine.runLuaGraphScript(script, params)
        else throw LuaEngineDisabledException()
    }

    override fun runLuaFunctionGenerator(
        script: String,
        dataSources: List<RawDataSample>
    ): Sequence<DataPoint> {
        return if (luaEngineSwitch.enabled) luaEngine.runLuaFunctionGenerator(script, dataSources)
        else throw LuaEngineDisabledException()
    }
}