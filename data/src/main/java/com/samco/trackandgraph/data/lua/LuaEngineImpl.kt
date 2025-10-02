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

import com.samco.trackandgraph.data.lua.dto.LuaGraphResult
import com.samco.trackandgraph.data.lua.dto.LuaScriptException
import com.samco.trackandgraph.data.lua.functionadapters.LuaFunctionDataSourceAdapter
import com.samco.trackandgraph.data.lua.functionadapters.LuaFunctionMetadataAdapter
import com.samco.trackandgraph.data.lua.graphadapters.LuaGraphAdapter
import com.samco.trackandgraph.data.sampling.RawDataSample
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import org.luaj.vm2.LuaError
import javax.inject.Inject

internal class LuaEngineImpl @Inject constructor(
    private val luaGraphAdapter: LuaGraphAdapter,
    private val luaScriptResolver: LuaScriptResolver,
    private val luaFunctionDataSourceAdapter: LuaFunctionDataSourceAdapter,
    private val luaFunctionMetadataAdapter: LuaFunctionMetadataAdapter,
    private val luaVMProvider: LuaVMProvider,
) : LuaEngine {

    override suspend fun acquireVM(): LuaVMLock = luaVMProvider.acquire()

    override fun releaseVM(vmLock: LuaVMLock) = luaVMProvider.release(vmLock.asLease())

    override fun runLuaGraph(
        vmLock: LuaVMLock,
        script: String,
        params: LuaGraphEngineParams
    ): LuaGraphResult {
        return try {
            val resolvedScript = luaScriptResolver.resolveLuaScript(script, vmLock.asLease())
            luaGraphAdapter.process(resolvedScript, params)
        } catch (luaError: LuaError) {
            val luaScriptException = LuaScriptException(
                message = luaError.message ?: "",
                luaCauseStackTrace = luaError.luaCause?.stackTraceToString()
            )
            LuaGraphResult(error = luaScriptException)
        } catch (t: Throwable) {
            LuaGraphResult(error = t)
        }
    }

    override fun runLuaFunction(
        vmLock: LuaVMLock,
        script: String
    ): LuaFunctionMetadata {
        return try {
            val resolvedScript = luaScriptResolver.resolveLuaScript(script, vmLock.asLease())
            luaFunctionMetadataAdapter.process(resolvedScript, script)
        } catch (luaError: LuaError) {
            val luaScriptException = LuaScriptException(
                message = luaError.message ?: "",
                luaCauseStackTrace = luaError.luaCause?.stackTraceToString()
            )
            throw luaScriptException
        }
    }

    override fun runLuaFunctionGenerator(
        vmLock: LuaVMLock,
        script: String,
        dataSources: List<RawDataSample>,
        configuration: List<LuaScriptConfigurationValue>,
    ): Sequence<DataPoint> {
        return try {
            val vmLease = vmLock.asLease()
            val resolvedScript = luaScriptResolver.resolveLuaScript(script, vmLease)
            luaFunctionDataSourceAdapter.createDataPointSequence(
                vmLease = vmLease,
                resolvedScript = resolvedScript,
                dataSources = dataSources,
                configuration = configuration,
            )
        } catch (luaError: LuaError) {
            val luaScriptException = LuaScriptException(
                message = luaError.message ?: "",
                luaCauseStackTrace = luaError.luaCause?.stackTraceToString()
            )
            throw luaScriptException
        }
    }
}
