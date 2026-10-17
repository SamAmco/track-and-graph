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
package com.samco.trackandgraph.data.lua.apiimpl

import com.samco.trackandgraph.data.assetreader.AssetReader
import com.samco.trackandgraph.data.lua.LuaGraphScriptAccessor
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

internal class RequireApiImpl @Inject constructor(
    private val assetReader: AssetReader,
    private val moduleLoadInterceptor: ModuleLoadInterceptor,
    private val coreApiImpl: CoreApiImpl,
) {
    companion object {
        const val REQUIRE = "require"
    }

    fun installIn(globals: Globals) {
        val installedApis = installedApis(globals)
        val loadedModules = mutableMapOf<String, LuaValue>()
        globals[REQUIRE] = oneArgFunction { arg ->
            val moduleName = arg.checkjstring()!!
            loadedModules[moduleName] ?: installedApis[moduleName]?.value
                ?.let { moduleLoadInterceptor.onModuleLoad(globals, moduleName, it) }
                ?.also { loadedModules[moduleName] = it }
                ?: throw LuaError("module '$moduleName' not found")
        }
    }

    fun createRequireFunction(
        globals: Globals,
        environment: LuaTable,
        scriptAccessor: LuaGraphScriptAccessor,
    ): LuaValue {
        val loadedModules = mutableMapOf<String, LuaValue>()
        val loadingModules = mutableSetOf<String>()
        val installedRequire = globals[REQUIRE]
        return oneArgFunction { arg: LuaValue ->
            val moduleName = arg.checkjstring()!!
            loadedModules[moduleName]?.let { return@oneArgFunction it }
            if (moduleName.startsWith("tng.") || moduleName.startsWith("test.")) {
                return@oneArgFunction installedRequire.call(arg)
            }

            if (!loadingModules.add(moduleName)) {
                throw LuaError("circular require for module '$moduleName'")
            }
            try {
                val moduleScript = scriptAccessor.moduleScript(moduleName)
                    ?: throw LuaError("module '$moduleName' not found")
                val result = globals.load(moduleScript, "@$moduleName.lua", environment).call()
                    .let { if (it.isnil()) LuaValue.valueOf(true) else it }
                loadedModules[moduleName] = result
                result
            } finally {
                loadingModules.remove(moduleName)
            }
        }
    }

    private fun installedApis(globals: Globals): Map<String, Lazy<LuaTable>> = mapOf(
        "tng.core" to lazyCore(globals),
        "tng.graph" to lazyPureLuaModule(globals, "graph"),
        "tng.graphext" to lazyPureLuaModule(globals, "graphext"),
        "tng.config" to lazyPureLuaModule(globals, "config"),
        "tng.aggregation" to lazyPureLuaModule(globals, "aggregation"),
        "tng.canvas" to lazyPureLuaModule(globals, "canvas"),
        "tng.theme" to lazyPureLuaModule(globals, "theme"),
        "tng.ui" to lazyPureLuaModule(globals, "ui"),
        "tng.random" to lazyRandom(globals),
        "test.core" to lazyTest(globals),
    )

    private fun lazyRandom(globals: Globals) = lazy {
        try {
            val fileContents = assetReader.readAssetToString("generated/lua-api/random.lua")
            val random = globals.load(fileContents).call().checktable()!!
            RandomApiImpl().installIn(random)
            return@lazy random
        } catch (t: Throwable) {
            Timber.e(t, "Failed to load random module")
            return@lazy LuaTable()
        }
    }

    private fun lazyCore(globals: Globals) = lazy {
        try {
            val fileContents = assetReader.readAssetToString("generated/lua-api/core.lua")
            val core = globals.load(fileContents).call().checktable()!!
            coreApiImpl.installIn(core)
            return@lazy core
        } catch (t: Throwable) {
            Timber.e(t, "Failed to load core module")
            return@lazy LuaTable()
        }
    }

    private fun lazyPureLuaModule(globals: Globals, name: String) = lazy {
        try {
            val fileContents = assetReader.readAssetToString("generated/lua-api/$name.lua")
            return@lazy globals.load(fileContents).call().checktable()!!
        } catch (t: Throwable) {
            Timber.e(t, "Failed to load $name API")
            return@lazy LuaTable()
        }
    }

    private fun lazyTest(globals: Globals) = lazy {
        try {
            val fileContents = assetReader.readAssetToString("generated/lua-test/core.lua")
            return@lazy globals.load(fileContents).call().checktable()!!
        } catch (t: Throwable) {
            Timber.e(t, "Failed to load lua-test/core API")
            return@lazy LuaTable()
        }
    }

}
