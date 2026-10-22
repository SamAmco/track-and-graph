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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.LuaGraphFeature
import com.samco.trackandgraph.data.database.dto.LuaGraphPackage
import com.samco.trackandgraph.data.database.dto.LuaGraphSource
import com.samco.trackandgraph.data.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.di.DefaultDispatcher
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.di.MainDispatcher
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.ZipLuaGraphScriptAccessor
import com.samco.trackandgraph.data.lua.dto.LuaUiContent
import com.samco.trackandgraph.data.lua.dto.LuaValueData
import com.samco.trackandgraph.data.lua.isZipArchive
import com.samco.trackandgraph.deeplinkhandler.DeepLinkHandler
import com.samco.trackandgraph.downloader.FileDownloader
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.lua.ui.LuaGraphConfigurationProvider
import com.samco.trackandgraph.remoteconfig.RemoteConfigProvider
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.z4kn4fein.semver.toVersion
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
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.inject.Inject

data class LuaGraphFeatureUiData(
    val nameTextField: MutableState<TextFieldValue>,
    val selectedFeatureText: String
)

data class LuaGraphPackageUiData(
    val packageId: String,
    val version: String,
)

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
    dataSampler: DataSampler,
    private val luaEngine: LuaEngine,
    private val luaGraphConfigurationProvider: LuaGraphConfigurationProvider,
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.LuaConfigData>(
    io = io,
    default = default,
    ui = ui,
    gsiProvider = gsiProvider,
    dataInteractor = dataInteractor,
    dataSampler = dataSampler
) {


    var script: TextFieldValue by mutableStateOf(TextFieldValue(""))
        private set

    var configurationContent: LuaUiContent? by mutableStateOf(null)
        private set

    var showingConfiguration by mutableStateOf(false)
        private set

    val packageUiData: LuaGraphPackageUiData?
        get() = (luaGraph.source as? LuaGraphSource.Package)?.packageData?.let {
            LuaGraphPackageUiData(
                packageId = it.packageId,
                version = it.version,
            )
        }

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

    private var selectedFeatures: List<LuaGraphFeature> by mutableStateOf(emptyList())
    private val featureTextFields = mutableListOf<MutableState<TextFieldValue>>()

    val featureUiDataList by derivedStateOf {
        selectedFeatures.mapIndexed { index, feature ->
            LuaGraphFeatureUiData(
                nameTextField = featureTextFields[index],
                selectedFeatureText = getSelectedFeatureText(feature.featureId)
            )
        }
    }

    private var luaGraph = LuaGraphWithFeatures(
        id = 0,
        graphStatId = 0,
        source = LuaGraphSource.Inline(""),
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
        data object InvalidPackage : NetworkError
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
        val trustedSources = try {
            remoteConfigProvider.getRemoteConfiguration()?.trustedLuaScriptSources ?: emptyList()
        } catch (e: Exception) {
            // Handle parsing errors gracefully
            Timber.e(e, "Failed to parse trusted sources")
            emptyList()
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
        val bytes = fileDownloader.downloadFileToBytes(uri)
        if (bytes == null) {
            networkErrorEvents.send(NetworkError.Uri(uri.toString()))
            return@withUpdate
        }

        try {
            installImportedFile(bytes)
            updateConfig()
        } catch (t: Throwable) {
            Timber.e(t, "Failed to import downloaded Lua graph file")
            networkErrorEvents.send(NetworkError.InvalidPackage)
        }
    }

    override fun updateConfig() {
        luaGraph = luaGraph.copy(
            source = when (val source = luaGraph.source) {
                is LuaGraphSource.Inline -> source.copy(script = script.text)
                is LuaGraphSource.Package -> source
            },
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

        val inputNames = featureUiDataList.map { it.nameTextField.value.text }
        if (inputNames.size != inputNames.distinct().size) {
            return GraphStatConfigEvent.ValidationException(R.string.lua_feature_names_not_unique)
        }
        return null
    }

    fun readFile(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch(io) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= ZipLuaGraphScriptAccessor.MAX_ARCHIVE_BYTES) {
                            "Imported Lua graph file is too large"
                        }
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                } ?: return@launch

                installImportedFile(bytes)
                onUpdate()
            } catch (t: Throwable) {
                Timber.e(t, "Failed to import Lua graph file")
                networkErrorEvents.send(NetworkError.InvalidPackage)
            }
        }
    }

    private suspend fun installImportedFile(bytes: ByteArray) {
        require(bytes.size <= ZipLuaGraphScriptAccessor.MAX_ARCHIVE_BYTES) {
            "Imported Lua graph file is too large"
        }
        if (bytes.isZipArchive()) {
            installPackage(bytes)
        } else {
            setInlineScript(TextFieldValue(bytes.toString(Charsets.UTF_8)))
        }
        refreshConfiguration(autoShow = true)
    }

    private suspend fun installPackage(archive: ByteArray) {
        val accessor = ZipLuaGraphScriptAccessor(archive)
        val (packageId, version) = readPackageIdentity(accessor)
        luaGraph = luaGraph.copy(
            source = LuaGraphSource.Package(
                LuaGraphPackage(
                    packageId = packageId,
                    version = version,
                    archive = archive,
                )
            ),
            configuration = null,
        )
        script = TextFieldValue(accessor.mainScript())
    }

    private suspend fun readPackageIdentity(
        accessor: ZipLuaGraphScriptAccessor,
    ): Pair<String, String> {
        val vmLock = luaEngine.acquireVM()
        return try {
            val descriptor = luaEngine.runLuaScript(vmLock, accessor)
            require(descriptor["purpose"]?.asString() == "GRAPH") {
                "A packaged Lua graph must declare purpose = 'GRAPH'"
            }
            val packageId = requireNotNull(descriptor["id"]?.asString()) {
                "A packaged Lua graph must declare an id"
            }
            val version = requireNotNull(descriptor["version"]?.asString()) {
                "A packaged Lua graph must declare a version"
            }
            require(packageId.isNotBlank()) { "Lua graph package id must not be blank" }
            require(packageId.length <= 200) { "Lua graph package id is too long" }
            require(version.isNotBlank()) { "Lua graph package version must not be blank" }
            version.toVersion()
            packageId to version
        } finally {
            luaEngine.releaseVM(vmLock)
        }
    }

    fun setScriptText(text: TextFieldValue) {
        setInlineScript(text)
        onUpdate()
    }

    fun onScriptEditFinished() {
        viewModelScope.launch { refreshConfiguration(autoShow = true) }
    }

    private fun setInlineScript(text: TextFieldValue) {
        luaGraph = luaGraph.copy(
            source = LuaGraphSource.Inline(text.text),
            configuration = null,
        )
        configurationContent = null
        showingConfiguration = false
        script = text
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
        }
        featureTextFields.removeAt(index)
        onUpdate()
    }

    fun onUpdateFeatureName(index: Int, text: TextFieldValue) {
        featureTextFields[index].value = text
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


    fun updateScriptFromClipboard(text: String) {
        setInlineScript(TextFieldValue(text))
        onUpdate()
        viewModelScope.launch { refreshConfiguration(autoShow = true) }
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
                setInlineScript(TextFieldValue(it))
                onUpdate()
                refreshConfiguration(autoShow = true)
                return@launch
            }
        }
    }

    fun onUserCancelDeepLink() {
        viewModelScope.launch {
            pendingScriptUriDownload.emit(null)
        }
    }

    private fun getSelectedFeatureText(featureId: Long): String {
        return featurePathProvider.getPathForFeature(featureId)
    }

    override fun onDataLoaded(config: Any?) {
        val lgConfig = config as? LuaGraphWithFeatures
        lgConfig?.let { luaGraph = it }
        lgConfig?.source?.let { source ->
            when (source) {
                is LuaGraphSource.Inline -> {
                    script = TextFieldValue(source.script)
                }
                is LuaGraphSource.Package -> {
                    val accessor = ZipLuaGraphScriptAccessor(source.packageData.archive)
                    script = TextFieldValue(accessor.mainScript())
                }
            }
        }
        lgConfig?.features?.let { features ->
            selectedFeatures = features
            featureTextFields.clear()
            featureTextFields.addAll(features.map {
                mutableStateOf(
                    TextFieldValue(it.name, TextRange(it.name.length))
                )
            })
        }
        viewModelScope.launch { refreshConfiguration(autoShow = true) }
    }

    fun showSourceControls() {
        showingConfiguration = false
    }

    fun onConfigurationChanged(configuration: LuaValueData?) {
        luaGraph = luaGraph.copy(configuration = configuration?.let(::lazyOf))
        onUpdate()
    }

    private suspend fun refreshConfiguration(autoShow: Boolean) {
        val source = luaGraph.source
        val configuration = luaGraph.configuration
        val content = try {
            withContext(default) {
                luaGraphConfigurationProvider(source, configuration)
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to resolve Lua graph configuration UI")
            null
        }
        withContext(ui) {
            if (luaGraph.source != source) return@withContext
            configurationContent = content
            if (autoShow) showingConfiguration = content != null
        }
    }

    fun openCommunityScripts() {
        viewModelScope.launch { urlNavigator.navigateTo(context, UrlNavigator.Location.LUA_COMMUNITY_SCRIPTS_ROOT) }
    }
}
