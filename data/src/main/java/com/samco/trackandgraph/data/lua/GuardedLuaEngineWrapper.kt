package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.lua.dto.LuaEngineDisabledException
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import com.samco.trackandgraph.data.lua.dto.LuaGraphResult
import com.samco.trackandgraph.data.sampling.RawDataSample
import javax.inject.Inject

class GuardedLuaEngineWrapper @Inject internal constructor(
    private val luaEngine: LuaEngineImpl,
    private val luaEngineSwitch: LuaEngineSwitch
) : LuaEngine {

    override suspend fun acquireVM(): LuaVMLock {
        if (luaEngineSwitch.enabled) return luaEngine.acquireVM()
        throw LuaEngineDisabledException()
    }

    override fun releaseVM(vmLock: LuaVMLock) {
        if (luaEngineSwitch.enabled) return luaEngine.releaseVM(vmLock)
        throw LuaEngineDisabledException()
    }

    override fun runLuaGraph(
        vmLock: LuaVMLock,
        script: String,
        params: LuaGraphEngineParams
    ): LuaGraphResult {
        return if (luaEngineSwitch.enabled) {
            luaEngine.runLuaGraph(vmLock, script, params)
        } else throw LuaEngineDisabledException()
    }

    override fun runLuaFunction(vmLock: LuaVMLock, script: String): LuaFunctionMetadata {
        return if (luaEngineSwitch.enabled) luaEngine.runLuaFunction(vmLock, script)
        else throw LuaEngineDisabledException()
    }

    override fun runLuaFunctionGenerator(
        vmLock: LuaVMLock,
        script: String,
        dataSources: List<RawDataSample>,
        configuration: List<LuaScriptConfigurationValue>
    ): Sequence<DataPoint> {
        return if (luaEngineSwitch.enabled) {
            luaEngine.runLuaFunctionGenerator(vmLock, script, dataSources, configuration)
        } else throw LuaEngineDisabledException()
    }

    override suspend fun runLuaCatalogue(
        vmLock: LuaVMLock,
        script: String
    ): List<LuaFunctionMetadata> {
        return if (luaEngineSwitch.enabled) {
            luaEngine.runLuaCatalogue(vmLock, script)
        } else throw LuaEngineDisabledException()
    }
}