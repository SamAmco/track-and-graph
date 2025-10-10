/*
* This file is part of Track & Graph
*
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.remoteconfig

import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.downloader.FileDownloader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.URI
import javax.inject.Inject

class RemoteConfigProviderImpl @Inject constructor(
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val fileDownloader: FileDownloader,
    private val json: Json,
) : RemoteConfigProvider {

    private val fetchMutex = Mutex()
    private var cachedConfiguration: RemoteConfiguration? = null

    override suspend fun getRemoteConfiguration(): RemoteConfiguration? {
        return fetchMutex.withLock {
            // Double-check: if we already have a successful configuration, return it
            cachedConfiguration?.let { return@withLock it }

            // Try to fetch new configuration
            tryFetchConfiguration()?.also { newConfig ->
                // Only cache on success
                cachedConfiguration = newConfig
                Timber.d("Successfully fetched and cached remote configuration")
            }
        }
    }

    private suspend fun tryFetchConfiguration(): RemoteConfiguration? = withContext(ioDispatcher) {
        try {
            val configContent = fileDownloader.downloadFileToString(
                URI("https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/configuration/remote-configuration.json")
            ) ?: return@withContext null

            json.decodeFromString<RemoteConfiguration>(configContent)
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch remote configuration")
            null
        }
    }

}