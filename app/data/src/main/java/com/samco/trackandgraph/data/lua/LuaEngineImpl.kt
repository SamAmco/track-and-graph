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
import com.samco.trackandgraph.data.lua.graphadapters.LuaGraphScriptAdapter
import com.samco.trackandgraph.data.lua.LuaUiScriptAdapter
import com.samco.trackandgraph.data.sampling.RawDataSample
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.lua.dto.LocalizationsTable
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.LuaFunctionCatalogue
import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle
import com.samco.trackandgraph.data.lua.functionadapters.LuaFunctionCatalogueAdapter
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class LuaEngineImpl @Inject constructor(
    private val luaGraphScriptAdapter: LuaGraphScriptAdapter,
    private val luaUiScriptAdapter: LuaUiScriptAdapter,
    private val luaScriptResolver: LuaScriptResolver,
    private val luaFunctionDataSourceAdapter: LuaFunctionDataSourceAdapter,
    private val luaFunctionMetadataAdapter: LuaFunctionMetadataAdapter,
    private val luaVMProvider: LuaVMProvider,
    private val luaFunctionCatalogueAdapter: LuaFunctionCatalogueAdapter,
) : LuaEngine {

    override suspend fun acquireVM(affinity: LuaVMAffinity?): LuaVMLock =
        luaVMProvider.acquire(affinity)

    override fun releaseVM(vmLock: LuaVMLock) = luaVMProvider.release(vmLock.asLease())

    override fun runLuaGraph(
        vmLock: LuaVMLock,
        script: String,
        params: LuaGraphEngineParams,
    ): LuaGraphResult = runLuaGraph(
        vmLock = vmLock,
        scriptAccessor = InlineLuaGraphScriptAccessor(script),
        params = params,
    )

    override fun runLuaGraph(
        vmLock: LuaVMLock,
        scriptAccessor: LuaGraphScriptAccessor,
        params: LuaGraphEngineParams
    ): LuaGraphResult {
        val vmLease = vmLock.requireActiveLease()
        return try {
            val resolvedScript = luaScriptResolver.resolveLuaScript(scriptAccessor, vmLease)
            luaGraphScriptAdapter.process(resolvedScript, params, vmLock.affinity)
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

    override fun runLuaScript(
        vmLock: LuaVMLock,
        script: String,
    ): LuaValueHandle = runLuaScript(vmLock, InlineLuaGraphScriptAccessor(script))

    override fun runLuaScript(
        vmLock: LuaVMLock,
        scriptAccessor: LuaGraphScriptAccessor,
    ): LuaValueHandle {
        val vmLease = vmLock.requireActiveLease()
        return try {
            LuaValueHandleImpl(
                luaScriptResolver.resolveLuaScript(scriptAccessor, vmLease),
                vmLock.affinity,
            )
        } catch (luaError: LuaError) {
            throw luaError.toLuaScriptException()
        }
    }

    override fun renderLuaUi(
        vmLock: LuaVMLock,
        renderFunction: LuaValueHandle,
        renderState: LuaValueAccessor?,
        context: LuaUiContext,
        vararg arguments: LuaValueAccessor,
    ): LuaValueHandle {
        vmLock.requireActiveLease()
        return try {
            luaUiScriptAdapter.render(vmLock, renderFunction, renderState, context, *arguments)
        } catch (luaError: LuaError) {
            throw luaError.toLuaScriptException()
        }
    }

    override fun callLuaFunction(
        vmLock: LuaVMLock,
        function: LuaValueHandle,
        vararg arguments: LuaValueAccessor,
    ): LuaValueHandle {
        vmLock.requireActiveLease()
        return try {
            LuaValueHandleImpl(
                function.valueFor(vmLock)
                    .invoke(LuaValue.varargsOf(arguments.map { it.valueFor(vmLock) }.toTypedArray()))
                    .arg1(),
                vmLock.affinity,
            )
        } catch (luaError: LuaError) {
            throw luaError.toLuaScriptException()
        }
    }

    override fun runLuaFunction(
        vmLock: LuaVMLock,
        script: String,
        translations: LocalizationsTable?
    ): LuaFunctionMetadata {
        return try {
            val vmLease = vmLock.requireActiveLease()
            val resolvedScript = luaScriptResolver.resolveLuaScript(script, vmLease)
            luaFunctionMetadataAdapter.process(resolvedScript, script, translations)
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
            val vmLease = vmLock.requireActiveLease()
            val resolvedScript = luaScriptResolver.resolveLuaScript(script, vmLease)
            luaFunctionDataSourceAdapter.createDataPointSequence(
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

    companion object {
        var a: Any? = null
    }

    override suspend fun runLuaCatalogue(
        vmLock: LuaVMLock,
        script: String
    ): LuaFunctionCatalogue {
        return try {
            val vmLease = vmLock.requireActiveLease()
            val resolvedScript = luaScriptResolver.resolveLuaScript(script, vmLease)
            luaFunctionCatalogueAdapter.parseCatalogue(vmLease, resolvedScript)
        } catch (luaError: LuaError) {
            throw LuaScriptException(
                message = luaError.message ?: "",
                luaCauseStackTrace = luaError.luaCause?.stackTraceToString()
            )
        }
    }
}

private fun LuaError.toLuaScriptException() = LuaScriptException(
    message = message ?: "",
    luaCauseStackTrace = luaCause?.stackTraceToString(),
)
