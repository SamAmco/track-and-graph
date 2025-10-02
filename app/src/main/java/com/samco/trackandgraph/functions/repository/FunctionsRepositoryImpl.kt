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
package com.samco.trackandgraph.functions.repository

import android.content.Context
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class FunctionsRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val luaEngine: LuaEngine,
) : FunctionsRepository {

    private val assetDir = "lua-community"

    override fun triggerFetchFunctions() {
        // No-op for now, as requested. We'll fetch on demand in fetchFunctions().
    }

    override suspend fun fetchFunctions(): List<LuaFunctionMetadata> = withContext(Dispatchers.IO) {
        val assetManager = appContext.assets
        val scripts = assetManager.list(assetDir)?.toList().orEmpty()

        val results = mutableListOf<LuaFunctionMetadata>()
        for (filename in scripts) {
            val path = "$assetDir/$filename"
            val script = try {
                assetManager.open(path).bufferedReader().use { it.readText() }
            } catch (t: Throwable) {
                Timber.e(t, "Failed to read script $path; ignoring")
                continue
            }

            try {
                val vmLock = luaEngine.acquireVM()
                val metadata = luaEngine.runLuaFunction(vmLock, script)
                luaEngine.releaseVM(vmLock)
                results.add(metadata)
            } catch (t: Throwable) {
                Timber.e(t, "Failed to parse/execute script for metadata; ignoring")
                // Script failed to parse/execute for metadata; ignore and continue
            }
        }
        results
    }
}
