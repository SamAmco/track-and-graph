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
import com.samco.trackandgraph.data.lua.apiimpl.TranslatedStringParser
import com.samco.trackandgraph.data.lua.dto.LocalizationsTable
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.LuaFunctionCatalogue
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import org.luaj.vm2.LuaValue
import timber.log.Timber
import javax.inject.Inject

internal class LuaFunctionCatalogueAdapter @Inject constructor(
    private val luaScriptResolver: LuaScriptResolver,
    private val apiLevelCalculator: ApiLevelCalculator,
    private val luaFunctionMetadataAdapter: LuaFunctionMetadataAdapter,
    private val translatedStringParser: TranslatedStringParser,
) {

    companion object {
        private const val FUNCTIONS = "functions"
        private const val TRANSLATIONS = "translations"
        private const val VERSION = "version"
        private const val SCRIPT = "script"
        private const val DEPRECATED = "deprecated"
    }

    private data class CatalogueFunction(
        val version: Version,
        val script: String,
        val deprecated: Int?,
    )

    suspend fun parseCatalogue(vmLease: VMLease, catalogue: LuaValue): LuaFunctionCatalogue {
        val maxApiLevel = apiLevelCalculator.getMaxApiLevel(vmLease)
        val translations = getCatalogTranslations(catalogue)

        val functions = getCatalogFunctions(catalogue)
            .filter {
                // Include if version is compatible AND not deprecated at this API level
                it.version.major <= maxApiLevel &&
                (it.deprecated == null || it.deprecated > maxApiLevel)
            }
            .map {
                val resolvedScript = luaScriptResolver.resolveLuaScript(it.script, vmLease)
                luaFunctionMetadataAdapter.process(resolvedScript, it.script, translations)
            }

        return LuaFunctionCatalogue(
            functions = functions
        )
    }

    private fun getCatalogTranslations(catalogue: LuaValue): LocalizationsTable {
        val catalogueTranslations = catalogue[TRANSLATIONS]
        if (catalogueTranslations.isnil() || !catalogueTranslations.istable()) {
            return emptyMap()
        }

        val translations = mutableMapOf<String, TranslatedString>()
        val translationsTable = catalogueTranslations.checktable()!!

        val keys = translationsTable.keys()
        for (key in keys) {
            if (!key.isstring()) continue

            val translationKey = key.checkjstring()!!
            val translatedString = translatedStringParser.parse(translationsTable[key])

            if (translatedString != null) {
                translations[translationKey] = translatedString
            }
        }

        return translations
    }

    private fun getCatalogFunctions(catalogue: LuaValue): List<CatalogueFunction> {
        val catalogueFunctions = catalogue[FUNCTIONS]
        if (!catalogueFunctions.istable()) {
            throw IllegalArgumentException("Catalogue functions must be a table")
        }
        val functions = mutableListOf<CatalogueFunction>()

        val catalogueTable = catalogueFunctions.checktable()!!

        // Iterate over string keys (map format: functionId -> function data)
        val keys = catalogueTable.keys()
        for (key in keys) {
            // Skip non-string keys for map-like iteration
            if (!key.isstring()) continue

            val functionId = key.checkjstring()!!
            val catalogueFunction = catalogueTable[key]

            if (catalogueFunction.isnil()) {
                Timber.w("Catalogue function '$functionId' was nil")
                continue
            }

            val catalogueVersion = catalogueFunction[VERSION]
            val catalogueScript = catalogueFunction[SCRIPT]
            val catalogueDeprecated = catalogueFunction[DEPRECATED]

            if (!catalogueVersion.isstring()) {
                Timber.w("Catalogue function '$functionId' contained a missing or invalid version")
                continue
            }

            if (!catalogueScript.isstring()) {
                Timber.w("Catalogue function '$functionId' contained a missing or invalid script")
                continue
            }

            // Parse deprecated (optional)
            val deprecated = if (catalogueDeprecated.isnil()) {
                null
            } else if (catalogueDeprecated.isnumber()) {
                catalogueDeprecated.toint()
            } else {
                Timber.w("Catalogue function '$functionId' contained invalid deprecated field (not a number)")
                null
            }

            functions.add(
                CatalogueFunction(
                    version = catalogueVersion.checkjstring()!!.toVersion(),
                    script = catalogueScript.checkjstring()!!,
                    deprecated = deprecated
                )
            )
        }

        return functions
    }

}