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

import com.samco.trackandgraph.data.sampling.RawDataSample
import com.samco.trackandgraph.data.lua.dto.LuaGraphResult
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

interface LuaEngine {

    /**
     * Acquires short-lived exclusive access to a Lua runtime. With no [affinity], an available
     * runtime is selected from the pool. Supplying a retained affinity waits for that runtime so
     * its Lua values, closures, and local state can be used again.
     */
    suspend fun acquireVM(affinity: LuaVMAffinity? = null): LuaVMLock

    /** Releases execution access without invalidating the lock's stable runtime affinity. */
    fun releaseVM(vmLock: LuaVMLock)

    fun runLuaGraph(
        vmLock: LuaVMLock,
        script: String,
        params: LuaGraphEngineParams,
    ): LuaGraphResult

    fun runLuaGraph(
        vmLock: LuaVMLock,
        scriptAccessor: LuaGraphScriptAccessor,
        params: LuaGraphEngineParams,
    ): LuaGraphResult

    /** Executes a packaged script's entry point and exposes its returned Lua value. */
    fun runLuaScript(
        vmLock: LuaVMLock,
        scriptAccessor: LuaGraphScriptAccessor,
    ): LuaValueAccessor

    fun renderLuaGraph(
        vmLock: LuaVMLock,
        renderFunction: LuaValueReference,
        renderData: LuaValueReference,
        context: LuaUiContext,
    ): LuaValueAccessor

    /** Calls an affinity-bound Lua function with zero or more Kotlin-owned values. */
    fun callLuaFunction(
        vmLock: LuaVMLock,
        function: LuaValueReference,
        vararg arguments: LuaValueData,
    ): LuaValueAccessor

    fun runLuaFunction(
        vmLock: LuaVMLock,
        script: String,
        translations: LocalizationsTable? = null
    ): LuaFunctionMetadata

    fun runLuaFunctionGenerator(
        vmLock: LuaVMLock,
        script: String,
        dataSources: List<RawDataSample>,
        configuration: List<LuaScriptConfigurationValue>,
    ): Sequence<DataPoint>

    suspend fun runLuaCatalogue(
        vmLock: LuaVMLock,
        script: String,
    ): LuaFunctionCatalogue
}
