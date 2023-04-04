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
package com.samco.trackandgraph.graphstatinput.configviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.ui.FeaturePathProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

abstract class GraphStatConfigViewModelBase<T : GraphStatConfigEvent.ConfigData<*>>(
    private val io: CoroutineDispatcher,
    private val default: CoroutineDispatcher,
    private val ui: CoroutineDispatcher,
    private val gsiProvider: GraphStatInteractorProvider,
    protected val dataInteractor: DataInteractor
) : ViewModel() {

    private var configFlow = MutableStateFlow<GraphStatConfigEvent>(GraphStatConfigEvent.Loading)

    private var updateJob: Job? = null

    private var graphStatId: Long? = null

    protected lateinit var featurePathProvider: FeaturePathProvider
        private set

    fun initFromGraphStatId(graphStatId: Long) {
        if (this.graphStatId == graphStatId) return
        this.graphStatId = graphStatId

        viewModelScope.launch(io) {
            configFlow.emit(GraphStatConfigEvent.Loading)
            loadFeaturePathProvider()
            loadGraphStat(graphStatId)
            withContext(ui) { onUpdate() }
        }
    }

    private suspend fun loadFeaturePathProvider() {
        val allFeatures = dataInteractor.getAllFeaturesSync()
        val allGroups = dataInteractor.getAllGroupsSync()
        featurePathProvider = FeaturePathProvider(allFeatures, allGroups)
    }

    private suspend fun loadGraphStat(graphStatId: Long) {
        val graphStat = dataInteractor.tryGetGraphStatById(graphStatId) ?: return
        val configData = gsiProvider
            .getDataSourceAdapter(graphStat.type)
            .getConfigData(graphStatId)
            ?.second
        withContext(ui) { onDataLoaded(configData) }
    }

    protected open fun onUpdate() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch(default) {
            configFlow.emit(GraphStatConfigEvent.Loading)
            configFlow.emit(validate() ?: getConfig())
        }
    }

    abstract fun getConfig(): T

    abstract suspend fun validate(): GraphStatConfigEvent.ValidationException?

    fun getConfigFlow(): Flow<GraphStatConfigEvent?> = configFlow

    abstract fun onDataLoaded(config: Any?)
}