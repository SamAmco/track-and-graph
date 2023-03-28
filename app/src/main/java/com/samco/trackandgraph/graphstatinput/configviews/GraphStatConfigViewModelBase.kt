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
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.ui.FeaturePathProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class GraphStatConfigViewModelBase<T : GraphStatConfigEvent.ConfigData<*>>(
    private val io: CoroutineDispatcher,
    private val default: CoroutineDispatcher,
    private val ui: CoroutineDispatcher,
    private val gsiProvider: GraphStatInteractorProvider,
    protected val dataInteractor: DataInteractor
) : ViewModel() {

    private var configFlow = MutableSharedFlow<GraphStatConfigEvent>()

    private var updateJob: Job? = null

    private var graphStatId: Long? = null

    protected val featurePathProvider: StateFlow<FeaturePathProvider> = flow {
        val allFeatures = dataInteractor.getAllFeaturesSync()
        val allGroups = dataInteractor.getAllGroupsSync()
        emit(FeaturePathProvider(allFeatures, allGroups))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FeaturePathProvider(emptyList(), emptyList()))

    fun initFromGraphStatId(graphStatId: Long) {
        if (this.graphStatId == graphStatId) return
        this.graphStatId = graphStatId

        viewModelScope.launch(io) {
            configFlow.emit(GraphStatConfigEvent.Loading)
            loadGraphStat(graphStatId)
            featurePathProvider.first()
            withContext(ui) { onUpdate() }
        }
    }

    private suspend fun loadGraphStat(graphStatId: Long) {
        val graphStat = dataInteractor.tryGetGraphStatById(graphStatId) ?: return
        gsiProvider
            .getDataSourceAdapter(graphStat.type)
            .getConfigData(graphStatId)
            .let { pair -> pair?.second?.let { onDataLoaded(it) } }
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

    abstract fun onDataLoaded(config: Any)
}