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

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.lua.dto.LocalizationsTable
import com.samco.trackandgraph.data.lua.dto.LuaEngineDisabledException
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.LuaFunctionCatalogue
import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import com.samco.trackandgraph.data.lua.dto.LuaGraphResult
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaUiRenderResult
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
import com.samco.trackandgraph.data.lua.dto.LuaValueData
import com.samco.trackandgraph.data.sampling.RawDataSample
import javax.inject.Inject

class GuardedLuaEngineWrapper @Inject internal constructor(
    private val luaEngine: LuaEngineImpl,
    private val luaEngineSwitch: LuaEngineSwitch
) : LuaEngine {

    override suspend fun acquireVM(affinity: LuaVMAffinity?): LuaVMLock {
        if (luaEngineSwitch.enabled) return luaEngine.acquireVM(affinity)
        throw LuaEngineDisabledException()
    }

    override fun releaseVM(vmLock: LuaVMLock) {
        luaEngine.releaseVM(vmLock)
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

    override fun runLuaGraph(
        vmLock: LuaVMLock,
        scriptAccessor: LuaGraphScriptAccessor,
        params: LuaGraphEngineParams
    ): LuaGraphResult {
        return if (luaEngineSwitch.enabled) {
            luaEngine.runLuaGraph(vmLock, scriptAccessor, params)
        } else throw LuaEngineDisabledException()
    }

    override fun runLuaScript(
        vmLock: LuaVMLock,
        script: String,
    ): LuaValueAccessor = if (luaEngineSwitch.enabled) {
        luaEngine.runLuaScript(vmLock, script)
    } else throw LuaEngineDisabledException()

    override fun runLuaScript(
        vmLock: LuaVMLock,
        scriptAccessor: LuaGraphScriptAccessor,
    ): LuaValueAccessor = if (luaEngineSwitch.enabled) {
        luaEngine.runLuaScript(vmLock, scriptAccessor)
    } else throw LuaEngineDisabledException()

    override fun renderLuaUi(
        vmLock: LuaVMLock,
        renderFunction: LuaValueReference,
        renderState: LuaValueReference,
        context: LuaUiContext,
        vararg arguments: LuaValueReference,
    ): LuaUiRenderResult = if (luaEngineSwitch.enabled) {
        luaEngine.renderLuaUi(vmLock, renderFunction, renderState, context, *arguments)
    } else throw LuaEngineDisabledException()

    override fun decodeLuaState(vmLock: LuaVMLock, encoded: String?): LuaValueReference =
        if (luaEngineSwitch.enabled) {
            luaEngine.decodeLuaState(vmLock, encoded)
        } else throw LuaEngineDisabledException()

    override fun callLuaFunction(
        vmLock: LuaVMLock,
        function: LuaValueReference,
        vararg arguments: LuaValueData,
    ): LuaValueAccessor = if (luaEngineSwitch.enabled) {
        luaEngine.callLuaFunction(vmLock, function, *arguments)
    } else throw LuaEngineDisabledException()

    override fun runLuaFunction(
        vmLock: LuaVMLock,
        script: String,
        translations: LocalizationsTable?
    ): LuaFunctionMetadata {
        return if (luaEngineSwitch.enabled) luaEngine.runLuaFunction(vmLock, script, translations)
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
    ): LuaFunctionCatalogue {
        return if (luaEngineSwitch.enabled) {
            luaEngine.runLuaCatalogue(vmLock, script)
        } else throw LuaEngineDisabledException()
    }
}
