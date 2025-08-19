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

package com.samco.trackandgraph.graphstatinput.configviews.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.LuaGraphFeature
import com.samco.trackandgraph.data.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.di.DefaultDispatcher
import com.samco.trackandgraph.data.model.di.IODispatcher
import com.samco.trackandgraph.data.model.di.MainDispatcher
import com.samco.trackandgraph.deeplinkhandler.DeepLinkHandler
import com.samco.trackandgraph.downloader.FileDownloader
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.remoteconfig.RemoteConfigProvider
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import javax.inject.Inject

@HiltViewModel
class LuaGraphConfigViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    private val deepLinkHandler: DeepLinkHandler,
    private val fileDownloader: FileDownloader,
    private val urlNavigator: UrlNavigator,
    private val remoteConfigProvider: RemoteConfigProvider,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor,
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.LuaConfigData>(
    io = io,
    default = default,
    ui = ui,
    gsiProvider = gsiProvider,
    dataInteractor = dataInteractor
) {

    var featureMap: Map<Long, String>? by mutableStateOf(null)
        private set

    var script: TextFieldValue by mutableStateOf(TextFieldValue(""))
        private set

    private data class ScriptPreviewData(
        val text: String,
        val startPosition: Int?
    )

    private val scriptPreviewData: StateFlow<ScriptPreviewData> = snapshotFlow {
        val prefix = "--- PREVIEW_START\n"
        val suffix = "--- PREVIEW_END"

        val previewStart = script.text.indexOf(prefix)
        val previewEnd = script.text.indexOf(suffix)
        if (previewStart == -1 || previewEnd == -1) {
            return@snapshotFlow ScriptPreviewData(script.text, null)
        }

        val previewText = script.text.substring(previewStart + prefix.length, previewEnd)

        val startPosition = previewStart + prefix.length
        ScriptPreviewData(previewText, startPosition)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ScriptPreviewData("", null))

    val scriptPreview: StateFlow<TextFieldValue> = scriptPreviewData
        .map { TextFieldValue(it.text) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TextFieldValue(""))

    var selectedFeatures: List<LuaGraphFeature> by mutableStateOf(emptyList())
        private set

    private val featureTextFields = mutableListOf<MutableState<TextFieldValue>>()

    private var luaGraph = LuaGraphWithFeatures(
        id = 0,
        graphStatId = 0,
        script = "",
        features = emptyList()
    )

    private val pendingScriptUriDownload = MutableStateFlow<URI?>(null)
    private val pendingScriptInstall = MutableStateFlow<String?>(null)

    val showUserConfirmDeepLink = combine(
        pendingScriptUriDownload,
        pendingScriptInstall,
    ) { uri, script -> uri != null || script != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)


    sealed interface NetworkError {
        data class Uri(val uri: String) : NetworkError
        data object Generic : NetworkError
    }

    private val networkErrorEvents = Channel<NetworkError>()
    val networkErrorToastEvents: ReceiveChannel<NetworkError> = networkErrorEvents

    init {
        viewModelScope.launch { observeDeepLinksUrls() }
        viewModelScope.launch { observeDeepLinkScripts() }
    }

    private suspend fun observeDeepLinksUrls() {
        deepLinkHandler.onLuaDeepLink.collect {
            onReceivedDeepLink(it)
        }
    }

    private suspend fun observeDeepLinkScripts() {
        deepLinkHandler.onLuaScript.collect {
            pendingScriptInstall.emit(it)
        }
    }

    private suspend fun onReceivedDeepLink(uri: URI) {
        val trustedSources = mutableListOf<String>()

        try {
            val trustedSourcesObj = remoteConfigProvider.getRemoteConfigArray(
                RemoteConfigProvider.RemoteConfig.TRUSTED_LUA_GRAPH_SOURCES
            )
            // Convert JSONObject to JSONArray if needed
            val jsonArray = trustedSourcesObj

            if (jsonArray != null) {
                for (i in 0 until jsonArray.length()) {
                    trustedSources.add(jsonArray.getString(i))
                }
            }
        } catch (e: Exception) {
            // Handle parsing errors gracefully
            Timber.e(e, "Failed to parse trusted sources")
        }

        if (trustedSources.isEmpty()) {
            networkErrorEvents.send(NetworkError.Generic)
            return
        }

        if (trustedSources.none { uri.toString().startsWith(it) }) {
            pendingScriptUriDownload.emit(uri)
            return
        }
        downloadAndInstallScriptFromUri(uri)
    }

    private fun downloadAndInstallScriptFromUri(uri: URI) = withUpdate {
        val scriptText = fileDownloader.downloadFileToString(uri)
        if (scriptText == null) {
            networkErrorEvents.send(NetworkError.Uri(uri.toString()))
        } else {
            script = TextFieldValue(scriptText)
            onUpdate()
        }
    }

    override fun updateConfig() {
        luaGraph = luaGraph.copy(
            script = script.text,
            features = selectedFeatures.mapIndexed { idx, ft ->
                ft.copy(name = featureTextFields[idx].value.text)
            }
        )
    }

    override fun getConfig(): GraphStatConfigEvent.ConfigData.LuaConfigData {
        return GraphStatConfigEvent.ConfigData.LuaConfigData(
            config = luaGraph
        )
    }

    override suspend fun validate(): GraphStatConfigEvent.ValidationException? {
        if (script.text.isBlank()) {
            return GraphStatConfigEvent.ValidationException(R.string.lua_script_empty)
        }

        val inputNames = featureTextFields.map { it.value.text }
        if (inputNames.size != inputNames.distinct().size) {
            return GraphStatConfigEvent.ValidationException(R.string.lua_feature_names_not_unique)
        }
        return null
    }

    fun readFile(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch(io) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val scriptText = BufferedReader(InputStreamReader(inputStream)).readText()
                script = TextFieldValue(scriptText)
                onUpdate()
            }
        }
    }

    fun setScriptText(text: TextFieldValue) {
        script = text
        onUpdate()
    }

    fun onAddFeatureClicked() {
        val featureId = featurePathProvider.features.firstOrNull()?.featureId ?: return
        val name = featurePathProvider.featureName(featureId) ?: return
        selectedFeatures = selectedFeatures.toMutableList().apply {
            add(
                LuaGraphFeature(
                    id = this.size.toLong(),
                    luaGraphId = luaGraph.id,
                    featureId = featureId,
                    name = name
                )
            )
            featureTextFields.add(mutableStateOf(TextFieldValue(name, TextRange(name.length))))
        }
        onUpdate()
    }

    fun onRemoveFeatureClicked(index: Int) {
        selectedFeatures = selectedFeatures.toMutableList().apply {
            removeAt(index)
            featureTextFields.removeAt(index)
        }
        onUpdate()
    }

    fun onSelectFeatureClicked(index: Int, featureId: Long) {
        val oldFeatureName = featurePathProvider.featureName(selectedFeatures[index].featureId) ?: ""
        val newFeatureName = featurePathProvider.featureName(featureId) ?: return
        val name: String

        if (oldFeatureName == featureTextFields[index].value.text) {
            featureTextFields[index].value = TextFieldValue(newFeatureName, TextRange(newFeatureName.length))
            name = newFeatureName
        } else {
            name = selectedFeatures[index].name
        }

        selectedFeatures = selectedFeatures.toMutableList().apply {
            set(
                index,
                LuaGraphFeature(
                    id = index.toLong(),
                    luaGraphId = luaGraph.id,
                    featureId = featureId,
                    name = name
                )
            )
        }
        onUpdate()
    }

    fun onClickInPreview(textFieldValue: TextFieldValue) {
        val previewStartIndex = scriptPreviewData.value.startPosition ?: return
        val selection = TextRange(textFieldValue.selection.start + previewStartIndex)
        script = script.copy(
            text = script.text,
            selection = selection
        )
    }

    fun getTextFieldFor(index: Int): TextFieldValue {
        return featureTextFields[index].value
    }

    fun onUpdateFeatureName(index: Int, text: TextFieldValue) {
        featureTextFields[index].value = text
        onUpdate()
    }

    fun updateScriptFromClipboard(text: String) {
        script = TextFieldValue(text)
        onUpdate()
    }

    fun onUserConfirmDeepLink() {
        viewModelScope.launch {
            pendingScriptUriDownload.value?.let {
                pendingScriptUriDownload.emit(null)
                downloadAndInstallScriptFromUri(it)
                return@launch
            }

            pendingScriptInstall.value?.let {
                pendingScriptInstall.emit(null)
                script = TextFieldValue(it)
                onUpdate()
                return@launch
            }
        }
    }

    fun onUserCancelDeepLink() {
        viewModelScope.launch {
            pendingScriptUriDownload.emit(null)
        }
    }

    override fun onDataLoaded(config: Any?) {
        val lgConfig = config as? LuaGraphWithFeatures
        featureMap = featurePathProvider.sortedFeatureMap()
        lgConfig?.let { luaGraph = it }
        lgConfig?.script?.let { script = TextFieldValue(it) }
        lgConfig?.features?.let { features ->
            selectedFeatures = features
            featureTextFields.clear()
            featureTextFields.addAll(features.map {
                mutableStateOf(
                    TextFieldValue(it.name, TextRange(it.name.length))
                )
            })
        }
    }

    fun openCommunityScripts() {
        viewModelScope.launch { urlNavigator.navigateTo(context, UrlNavigator.Location.LUA_COMMUNITY_SCRIPTS_ROOT) }
    }
}
