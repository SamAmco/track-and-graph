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
import com.samco.trackandgraph.data.sampling.RawDataSample
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.lua.dto.LocalizationsTable
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.LuaFunctionCatalogue
import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
import com.samco.trackandgraph.data.lua.dto.LuaValueData
import com.samco.trackandgraph.data.lua.apiimpl.TranslatedStringParser
import com.samco.trackandgraph.data.lua.functionadapters.LuaFunctionCatalogueAdapter
import org.luaj.vm2.LuaError
import io.github.z4kn4fein.semver.toVersion
import javax.inject.Inject

internal class LuaEngineImpl @Inject constructor(
    private val luaGraphScriptAdapter: LuaGraphScriptAdapter,
    private val luaScriptResolver: LuaScriptResolver,
    private val luaFunctionDataSourceAdapter: LuaFunctionDataSourceAdapter,
    private val luaFunctionMetadataAdapter: LuaFunctionMetadataAdapter,
    private val luaVMProvider: LuaVMProvider,
    private val luaFunctionCatalogueAdapter: LuaFunctionCatalogueAdapter,
    private val translatedStringParser: TranslatedStringParser,
) : LuaEngine {

    override suspend fun acquireVM(affinity: LuaVMAffinity?): LuaVMLock =
        luaVMProvider.acquire(affinity)

    override fun releaseVM(vmLock: LuaVMLock) = luaVMProvider.release(vmLock.asLease())

    override fun runLuaGraph(
        vmLock: LuaVMLock,
        script: String,
        params: LuaGraphEngineParams,
    ): LuaGraphResult = runLuaGraphFromAccessor(
        vmLock = vmLock,
        scriptAccessor = InlineLuaGraphScriptAccessor(script),
        params = params,
    )

    override fun runLuaGraphFromAccessor(
        vmLock: LuaVMLock,
        scriptAccessor: LuaGraphScriptAccessor,
        params: LuaGraphEngineParams
    ): LuaGraphResult {
        val vmLease = vmLock.requireActiveLease()
        return try {
            val resolvedScript = luaScriptResolver.resolveLuaGraphScript(scriptAccessor, vmLease)
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

    override fun readLuaGraphPackageMetadata(
        vmLock: LuaVMLock,
        scriptAccessor: LuaGraphScriptAccessor,
    ): LuaGraphPackageMetadata {
        val vmLease = vmLock.requireActiveLease()
        try {
            val descriptor = luaScriptResolver.resolveLuaGraphScript(scriptAccessor, vmLease)
            require(descriptor.istable()) { "A packaged Lua graph must return a table" }
            require(descriptor["purpose"].checkjstring() == "GRAPH") {
                "A packaged Lua graph must declare purpose = 'GRAPH'"
            }
            return LuaGraphPackageMetadata(
                packageId = descriptor["id"].checkjstring()!!,
                version = descriptor["version"].checkjstring()!!,
                title = requireNotNull(translatedStringParser.parse(descriptor["title"])) {
                    "A packaged Lua graph must declare a string or translated table title"
                },
            ).also {
                require(it.packageId.isNotBlank()) { "Lua graph package id must not be blank" }
                require(it.version.isNotBlank()) { "Lua graph package version must not be blank" }
                require(it.packageId.length <= 200) { "Lua graph package id is too long" }
                it.version.toVersion()
            }
        } catch (luaError: LuaError) {
            throw luaError.toLuaScriptException()
        }
    }

    override fun renderLuaGraph(
        vmLock: LuaVMLock,
        renderFunction: LuaValueReference,
        renderData: LuaValueReference,
        context: LuaUiContext,
    ): LuaValueAccessor {
        vmLock.requireActiveLease()
        return try {
            luaGraphScriptAdapter.render(vmLock, renderFunction, renderData, context)
        } catch (luaError: LuaError) {
            throw luaError.toLuaScriptException()
        }
    }

    override fun callLuaUiFunction(
        vmLock: LuaVMLock,
        function: LuaValueReference,
        argument: LuaValueData,
    ): LuaValueAccessor {
        vmLock.requireActiveLease()
        return try {
            luaGraphScriptAdapter.call(vmLock, function, argument)
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
