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
package com.samco.trackandgraph.deeplinkhandler

import com.samco.trackandgraph.base.model.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DeepLinkHandlerImpl @Inject constructor(
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : CoroutineScope, DeepLinkHandler {

    override val coroutineContext: CoroutineContext = Job() + ioDispatcher

    private val onLuaDeepLinkChannel = Channel<URI>()
    private val onLuaScriptChannel = Channel<String>()

    override val onLuaDeepLink: SharedFlow<URI> = onLuaDeepLinkChannel
        .receiveAsFlow()
        .shareIn(this, SharingStarted.WhileSubscribed())

    override val onLuaScript: SharedFlow<String> = onLuaScriptChannel
        .receiveAsFlow()
        .shareIn(this, SharingStarted.WhileSubscribed())

    override fun handleUri(uri: String) {
        val uriObj = URI.create(uri)
        if (uriObj.scheme != "trackandgraph") return
        if (uriObj.host == "lua_inject_url") handleLuaUrl(uriObj)
        if (uriObj.host == "lua_inject_file") handleLuaFile(uriObj)
    }

    private fun handleLuaUrl(uri: URI) {
        val queryParams = uri.query?.split("&")?.associate {
            val (key, value) = it.split("=", limit = 2)
            key to URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        } ?: emptyMap()

        val scriptUrl = try {
            URL(queryParams["url"]) // validates URL format
        } catch (e: MalformedURLException) {
            return // invalid URL format
        }

        launch { onLuaDeepLinkChannel.send(scriptUrl.toURI()) }
    }

    private fun handleLuaFile(uri: URI) {
        val queryParams = uri.query?.split("&")?.associate {
            val (key, value) = it.split("=", limit = 2)
            key to URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        } ?: emptyMap()

        val scriptPath = queryParams["path"] ?: return

        val file = File(scriptPath)
        if (!file.exists()) Timber.e("File not found: $scriptPath, uri: $uri")

        val scriptText = file.readText()

        launch { onLuaScriptChannel.send(scriptText) }
    }
}