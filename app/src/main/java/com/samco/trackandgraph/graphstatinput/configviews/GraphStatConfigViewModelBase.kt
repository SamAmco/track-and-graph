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
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.graphstatinput.ConfigData
import com.samco.trackandgraph.graphstatinput.FeatureDataProvider
import com.samco.trackandgraph.graphstatinput.ValidationException
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

abstract class GraphStatConfigViewModelBase<T : ConfigData<*>>(
    @IODispatcher private val io: CoroutineDispatcher,
    private val gsiProvider: GraphStatInteractorProvider,
    private val dataInteractor: DataInteractor
) : ViewModel() {

    private val configFlow = MutableSharedFlow<T?>()
    private val validationFlow = MutableStateFlow<ValidationException?>(null)

    private var graphStatId: Long? = null
    protected var featureDataProvider: FeatureDataProvider? = null

    protected val allFeatureIds by lazy {
        featureDataProvider?.features?.map { it.featureId }?.toSet() ?: emptySet()
    }

    fun initFromGraphStatId(graphStatId: Long) {
        if (this.graphStatId == graphStatId) return
        this.graphStatId = graphStatId

        viewModelScope.launch(io) {
            loadFeatureData()
            loadGraphStat(graphStatId)
        }
    }

    private suspend fun loadGraphStat(graphStatId: Long) {
        val graphStat = dataInteractor.tryGetGraphStatById(graphStatId) ?: return
        gsiProvider
            .getDataSourceAdapter(graphStat.type)
            .getConfigData(graphStatId)
            .let { pair -> pair?.second?.let { onDataLoaded(it) } }
    }

    private suspend fun loadFeatureData() {
        val allFeatures = dataInteractor.getAllFeaturesSync()
        val allGroups = dataInteractor.getAllGroupsSync()
        val dataSourceData = allFeatures.map { feature ->
            FeatureDataProvider.DataSourceData(
                feature,
                //TODO this isn't going to fly when we implement functions. Iterating all labels
                // of every feature could be too expensive. Need to grab these in a lazy way as needed.
                dataInteractor.getLabelsForFeatureId(feature.featureId).toSet(),
                dataInteractor.getDataSamplePropertiesForFeatureId(feature.featureId)
                    ?: return@map null
            )
        }.filterNotNull()
        featureDataProvider = FeatureDataProvider(dataSourceData, allGroups)
    }

    protected fun updateConfig(config: T) {
        viewModelScope.launch { configFlow.emit(config) }
    }

    protected fun updateValidationException(exception: ValidationException?) {
        viewModelScope.launch { validationFlow.emit(exception) }
    }

    fun getConfigFlow() = configFlow.filterNotNull()
    fun getValidationFlow() = validationFlow

    abstract fun onDataLoaded(config: Any)
}