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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.R
import com.samco.trackandgraph.base.database.dto.LuaGraphFeature
import com.samco.trackandgraph.base.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class LuaGraphConfigViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
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

    var selectedFeatures: List<LuaGraphFeature> by mutableStateOf(emptyList())
        private set

    private val featureTextFields = mutableListOf<TextFieldValue>()

    private var luaGraph = LuaGraphWithFeatures(
        id = 0,
        graphStatId = 0,
        script = "",
        features = emptyList()
    )

    override fun updateConfig() {
        luaGraph = luaGraph.copy(
            script = script.text,
            features = selectedFeatures.mapIndexed { idx, ft ->
                ft.copy(name = featureTextFields[idx].text)
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
        return null
    }

    fun readFile(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch(io) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val scriptText = BufferedReader(InputStreamReader(inputStream)).readText()
                script = TextFieldValue(scriptText, TextRange(scriptText.length))
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
            featureTextFields.add(TextFieldValue(name, TextRange(name.length)))
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

        if (oldFeatureName == selectedFeatures[index].name) {
            featureTextFields[index] = TextFieldValue(newFeatureName, TextRange(newFeatureName.length))
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

    fun getTextFieldFor(index: Int): TextFieldValue {
        return featureTextFields[index]
    }

    fun onUpdateFeatureName(index: Int, text: TextFieldValue) {
        featureTextFields[index] = text
        onUpdate()
    }

    override fun onDataLoaded(config: Any?) {
        val lgConfig = config as? LuaGraphWithFeatures
        featureMap = featurePathProvider.sortedFeatureMap()
        lgConfig?.let { luaGraph = it }
        lgConfig?.script?.let { script = TextFieldValue(it, TextRange(it.length)) }
        lgConfig?.features?.let { feature ->
            selectedFeatures = feature
            featureTextFields.clear()
            featureTextFields.addAll(feature.map { TextFieldValue(it.name, TextRange(it.name.length)) })
        }
    }
}