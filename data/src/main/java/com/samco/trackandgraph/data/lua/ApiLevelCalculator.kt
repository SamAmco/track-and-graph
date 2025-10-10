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

import com.samco.trackandgraph.data.assetreader.AssetReader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.luaj.vm2.LuaValue
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculates the maximum API level supported by the app by parsing all .apispec.lua files
 */
@Singleton
internal class ApiLevelCalculator @Inject constructor(
    private val assetReader: AssetReader
) {

    private val mutex = Mutex()
    private var cachedApiLevel: Int? = null

    /**
     * Gets the maximum API level supported by the app.
     * This is calculated lazily and cached for subsequent calls.
     * @param vmLease the VM lock to use for parsing spec files
     */
    suspend fun getMaxApiLevel(vmLease: VMLease): Int {
        return cachedApiLevel ?: mutex.withLock {
            cachedApiLevel ?: run {
                val level = calculateMaxApiLevel(vmLease)
                cachedApiLevel = level
                level
            }
        }
    }

    private suspend fun calculateMaxApiLevel(vmLease: VMLease): Int {
        try {
            // Find all .apispec.lua files in the assets
            val apiSpecFiles = assetReader.findFilesWithSuffix(
                "generated/lua-api",
                ".apispec.lua"
            )

            if (apiSpecFiles.isEmpty()) {
                Timber.w("No .apispec.lua files found in assets")
                return DEFAULT_API_LEVEL
            }

            var maxApiLevel = 0

            // Parse each spec file to extract API level
            for (filePath in apiSpecFiles) {
                try {
                    val content = assetReader.readAssetToString(filePath)
                    val apiLevel = parseApiLevelFromSpec(content, vmLease)
                    if (apiLevel > maxApiLevel) {
                        maxApiLevel = apiLevel
                    }
                    val filename = filePath.substringAfterLast('/')
                    Timber.d("Found API level $apiLevel in $filename")
                } catch (e: Exception) {
                    val filename = filePath.substringAfterLast('/')
                    Timber.e(e, "Failed to parse API level from $filename")
                }
            }

            if (maxApiLevel == 0) {
                Timber.e("No valid API levels found in spec files, using default 0")
                return DEFAULT_API_LEVEL
            }

            Timber.i("Calculated max API level: $maxApiLevel")
            return maxApiLevel

        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate API level, using default")
            return DEFAULT_API_LEVEL
        }
    }

    private fun parseApiLevelFromSpec(specContent: String, vmLease: VMLease): Int {
        // Use the provided VM lock to get globals
        val globals = vmLease.globals

        // Execute the spec file to get the table
        val chunk = globals.load(specContent)
        val result = chunk.call()

        // Find the highest integer value across all keys in the table
        if (result.istable()) {
            var maxLevel = 0

            // Iterate through all key-value pairs in the table
            var key = result.next(LuaValue.NIL).arg1()
            while (!key.isnil()) {
                val value = result[key]
                if (value.isint()) {
                    val level = value.toint()
                    if (level > maxLevel) {
                        maxLevel = level
                    }
                }
                key = result.next(key).arg1()
            }

            if (maxLevel > 0) {
                return maxLevel
            }
        }

        throw IllegalArgumentException("No valid integer values found in spec table")
    }

    companion object {
        private const val DEFAULT_API_LEVEL = 0
    }
}
