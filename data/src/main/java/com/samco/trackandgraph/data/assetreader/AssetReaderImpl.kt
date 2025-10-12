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
package com.samco.trackandgraph.data.assetreader

import android.content.Context
import com.samco.trackandgraph.data.di.IODispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class AssetReaderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : AssetReader {
    override fun readAssetToString(assetPath: String): String =
        context.assets.open(assetPath).bufferedReader().use { it.readText() }

    override suspend fun findFilesWithSuffix(
        assetDirectoryPath: String,
        suffix: String
    ): List<String> = withContext(ioDispatcher) {
        try {
            val files = context.assets.list(assetDirectoryPath) ?: emptyArray()
            files.filter { it.endsWith(suffix) }
                .map { "$assetDirectoryPath/$it" }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to find files with suffix $suffix in asset directory $assetDirectoryPath")
            emptyList()
        }
    }
}
