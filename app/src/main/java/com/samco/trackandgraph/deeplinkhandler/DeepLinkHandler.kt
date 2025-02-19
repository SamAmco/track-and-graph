package com.samco.trackandgraph.deeplinkhandler

import kotlinx.coroutines.flow.SharedFlow
import java.net.URI

interface DeepLinkHandler {
    val onLuaDeepLink: SharedFlow<URI>

    fun handleUri(uri: String)
}