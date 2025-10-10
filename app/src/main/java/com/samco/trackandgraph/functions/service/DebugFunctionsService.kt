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
package com.samco.trackandgraph.functions.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * Debug implementation of FunctionsService that reads files from the assets directory
 */
class DebugFunctionsService @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val json: Json
) : FunctionsService {

    private val catalogDir = "functions-catalog"
    private val luaFileName = "community-functions.lua"
    private val signatureFileName = "community-functions.sig.json"

    override suspend fun fetchFunctionsCatalog(): FunctionsCatalogData =
        withContext(Dispatchers.IO) {
            val assetManager = appContext.assets

            try {
                val luaScriptBytes = assetManager.open("$catalogDir/$luaFileName")
                    .use { it.readBytes() }

                val signatureJson = assetManager.open("$catalogDir/$signatureFileName")
                    .bufferedReader().use { it.readText() }

                val signatureData = json.decodeFromString<SignatureData>(signatureJson)

                Timber.d("Successfully loaded functions catalog from assets")
                FunctionsCatalogData(luaScriptBytes, signatureData)
            } catch (e: Exception) {
                Timber.e(e, "Failed to read functions catalog from assets")
                throw e
            }
        }
}
