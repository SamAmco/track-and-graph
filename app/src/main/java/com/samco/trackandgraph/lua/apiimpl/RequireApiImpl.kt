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
package com.samco.trackandgraph.lua.apiimpl

import com.samco.trackandgraph.assetreader.AssetReader
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import javax.inject.Inject

class RequireApiImpl @Inject constructor(
    private val assetReader: AssetReader,
    private val coreApiImpl: CoreApiImpl,
) {
    companion object {
        const val REQUIRE = "require"
    }

    fun installIn(globals: Globals) {
        globals[REQUIRE] = getRequireLuaFunction(
            mapOf(
                "tng.core" to lazyCore(globals),
                "tng.graph" to lazyGraph(globals),
                "test.core" to lazyTest(globals),
            )
        )
    }

    private fun lazyCore(globals: Globals) = lazy {
        val fileContents = assetReader.readAssetToString("generated/lua-api/core.lua")
        val core = globals.load(fileContents).call().checktable()!!
        coreApiImpl.installIn(core)
        return@lazy core
    }

    private fun lazyGraph(globals: Globals) = lazy {
        val fileContents = assetReader.readAssetToString("generated/lua-api/graph.lua")
        return@lazy globals.load(fileContents).call().checktable()!!
    }

    private fun lazyTest(globals: Globals) = lazy {
        val fileContents = assetReader.readAssetToString("generated/lua-test/core.lua")
        return@lazy globals.load(fileContents).call().checktable()!!
    }

    private fun getRequireLuaFunction(installedApis: Map<String, Lazy<LuaValue>>): LuaValue = oneArgFunction { arg: LuaValue ->
        val moduleName = arg.tojstring()
        if (moduleName in installedApis) {
            return@oneArgFunction installedApis[moduleName]?.value ?: LuaValue.NIL
        } else {
            throw LuaError("module '$moduleName' not found")
        }
    }
}
