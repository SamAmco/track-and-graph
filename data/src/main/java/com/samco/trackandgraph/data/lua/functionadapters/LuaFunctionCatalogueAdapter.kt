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
package com.samco.trackandgraph.data.lua.functionadapters

import com.samco.trackandgraph.data.lua.ApiLevelCalculator
import com.samco.trackandgraph.data.lua.LuaScriptResolver
import com.samco.trackandgraph.data.lua.VMLease
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import org.luaj.vm2.LuaValue
import timber.log.Timber
import javax.inject.Inject

internal class LuaFunctionCatalogueAdapter @Inject constructor(
    private val luaScriptResolver: LuaScriptResolver,
    private val apiLevelCalculator: ApiLevelCalculator,
    private val luaFunctionMetadataAdapter: LuaFunctionMetadataAdapter,
) {
    private data class CatalogueFunction(
        val version: Version,
        val script: String,
    )

    suspend fun parseCatalogue(vmLease: VMLease, catalogue: LuaValue): List<LuaFunctionMetadata> {
        val maxApiLevel = apiLevelCalculator.getMaxApiLevel(vmLease)
        return getCatalogFunctions(catalogue)
            .filter { it.version.major <= maxApiLevel }
            .map {
                val resolvedScript = luaScriptResolver.resolveLuaScript(it.script, vmLease)
                luaFunctionMetadataAdapter.process(resolvedScript, it.script)
            }
    }

    private fun getCatalogFunctions(catalogue: LuaValue): List<CatalogueFunction> {
        val catalogueFunctions = catalogue["functions"]
        if (!catalogueFunctions.istable()) {
            throw IllegalArgumentException("Catalogue functions must be a table")
        }
        val functions = mutableListOf<CatalogueFunction>()

        val catalogueTable = catalogueFunctions.checktable()!!
        
        // Use pairs() to iterate over the table properly
        val keys = catalogueTable.keys()
        for (key in keys) {
            // Skip non-numeric keys for array-like iteration
            if (!key.isnumber()) continue
            
            val catalogueFunction = catalogueTable[key]

            if (catalogueFunction.isnil()) {
                Timber.w("Catalogue at ${key.toint()} was nil")
                continue
            }

            val catalogueVersion = catalogueFunction["version"]
            val catalogueScript = catalogueFunction["script"]

            if (!catalogueVersion.isstring()) {
                Timber.w("Catalogue at ${key.toint()} contained a missing or invalid version")
                continue
            }

            if (!catalogueScript.isstring()) {
                Timber.w("Catalogue at ${key.toint()} contained a missing or invalid script")
                continue
            }

            functions.add(
                CatalogueFunction(
                    version = catalogueVersion.checkjstring()!!.toVersion(),
                    script = catalogueScript.checkjstring()!!
                )
            )
        }

        return functions
    }
}