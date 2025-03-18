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

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.downloader.FileDownloader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.net.URI
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class UrlNavigatorImpl @Inject constructor(
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val fileDownloader: FileDownloader,
) : UrlNavigator, CoroutineScope {

    override val coroutineContext: CoroutineContext = Job() + ioDispatcher

    private var _config: Map<String, String>? = null

    override suspend fun navigateTo(context: Context, location: UrlNavigator.Location): Boolean {
        try {
            getOrLoadConfig()[location.urlId]?.let { url ->
                navigateToUrl(context, url)
                return true
            } ?: error("URL not found for location: ${location.urlId}")
        } catch (t: Throwable) {
            Timber.e(t, "Failed to navigate to URL for location: ${location.urlId} falling back to default")
            navigateToUrl(context, "https://github.com/SamAmco/track-and-graph/")
            return false
        }
    }

    override fun triggerNavigation(context: Context, location: UrlNavigator.Location) {
        launch { navigateTo(context, location) }
    }

    private fun navigateToUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private suspend fun getOrLoadConfig(): Map<String, String> {
        if (_config == null) {
            _config = loadConfig()
        }
        return _config!!
    }

    private suspend fun loadConfig(): Map<String, String> {
        val configContent = fileDownloader.downloadFileToString(
            URI("https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/configuration/remote-configuration.json")
        ) ?: error("Failed to load remote configuration")

        val endpoints = JSONObject(configContent).getJSONObject("endpoints")

        return endpoints.keys()
            .asSequence()
            .associateWith { endpoints.getString(it) }
            .toMap()
    }
}