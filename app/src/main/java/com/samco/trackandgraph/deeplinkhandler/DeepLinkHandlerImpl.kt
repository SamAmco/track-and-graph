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

    override val onLuaDeepLink: SharedFlow<URI> = onLuaDeepLinkChannel
        .receiveAsFlow()
        .shareIn(this, SharingStarted.WhileSubscribed())

    override fun handleUri(uri: String) {
        val uriObj = URI.create(uri)
        if (uriObj.scheme != "trackandgraph") return
        if (uriObj.host == "lua_inject") handleLuaInject(uriObj)
    }

    private fun handleLuaInject(uri: URI) {
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
}