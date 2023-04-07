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

    //This will be available after onDataLoaded is called
    protected lateinit var featurePathProvider: FeaturePathProvider
        private set

    /**
     * You must call this before using the view model even if the intention is to create a new graph
     * or stat. This will load the config data from the database and set the [featurePathProvider].
     * If creating a new graph or stat then you can pass in -1L as the graphStatId.
     */
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
        val configData = dataInteractor.tryGetGraphStatById(graphStatId)?.let {
            gsiProvider
                .getDataSourceAdapter(it.type)
                .getConfigData(graphStatId)
                ?.second
        }
        withContext(ui) { onDataLoaded(configData) }
    }

    /**
     * Call this to let [GraphStatInputViewModel] know when the demo data or validation exception
     * may have changed and need updating
     */
    protected fun onUpdate() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch(default) {
            configFlow.emit(GraphStatConfigEvent.Loading)
            updateConfig()
            configFlow.emit(validate() ?: getConfig())
        }
    }

    /**
     * Should update the config data for the graph or stat. This will be validated when [validate]
     * is called and returned when [getConfig] is called.
     */
    abstract fun updateConfig()

    /**
     * Should return the current config data for the graph or stat
     */
    abstract fun getConfig(): T

    /**
     * Should return a validation exception if the config is invalid or null if it is valid
     */
    abstract suspend fun validate(): GraphStatConfigEvent.ValidationException?

    /**
     * A flow of events to be relayed to the [GraphStatInputViewModel]
     */
    fun getConfigFlow(): Flow<GraphStatConfigEvent?> = configFlow

    /**
     * Called when the config data has been loaded from the database and [featurePathProvider] is
     * available.
     */
    abstract fun onDataLoaded(config: Any?)
}