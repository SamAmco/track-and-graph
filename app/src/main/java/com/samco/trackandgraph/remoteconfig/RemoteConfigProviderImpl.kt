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

import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.downloader.FileDownloader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.URI
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class RemoteConfigProviderImpl @Inject constructor(
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val fileDownloader: FileDownloader,
) : RemoteConfigProvider, CoroutineScope {

    override val coroutineContext: CoroutineContext = Job() + ioDispatcher

    private sealed interface RemoteConfigResult {
        data class Success(val jsonObject: JSONObject) : RemoteConfigResult
        data class Failure(val error: Throwable) : RemoteConfigResult
    }

    private val remoteConfig = flow {
        try {
            val configContent = fileDownloader.downloadFileToString(
                URI("https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/configuration/remote-configuration.json")
            ) ?: error("Failed to load remote configuration")

            emit(RemoteConfigResult.Success(JSONObject(configContent)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to load remote configuration")
            emit(RemoteConfigResult.Failure(e))
        }
    }.shareIn(this, SharingStarted.Companion.Lazily, replay = 1)

    override suspend fun getRemoteConfigObject(subConfig: RemoteConfigProvider.RemoteConfig): JSONObject? {
        return remoteConfig.map {
            when (it) {
                is RemoteConfigResult.Success -> it.jsonObject.getJSONObject(subConfig.urlId)
                is RemoteConfigResult.Failure -> null
            }
        }.first()
    }

    override suspend fun getRemoteConfigArray(subConfig: RemoteConfigProvider.RemoteConfig): JSONArray? {
        return remoteConfig.map {
            when (it) {
                is RemoteConfigResult.Success -> it.jsonObject.getJSONArray(subConfig.urlId)
                is RemoteConfigResult.Failure -> null
            }
        }.first()
    }
}