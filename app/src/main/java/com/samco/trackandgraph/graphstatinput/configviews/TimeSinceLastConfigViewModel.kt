/*
* This file is part of Track & Graph
*
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.graphstatinput.configviews

import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.TimeSinceLastStat
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.ui.viewmodels.asTextFieldValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@HiltViewModel
class TimeSinceLastConfigViewModel @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor,
    private val filterableFeatureConfigBehaviour: FilterableFeatureConfigBehaviourImpl = FilterableFeatureConfigBehaviourImpl(),
    private val singleFeatureConfigBehaviour: SingleFeatureConfigBehaviourImpl = SingleFeatureConfigBehaviourImpl(),
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.TimeSinceLastConfigData>(
    io,
    default,
    ui,
    gsiProvider,
    dataInteractor
), FilterableFeatureConfigBehaviour by filterableFeatureConfigBehaviour,
    SingleFeatureConfigBehaviour by singleFeatureConfigBehaviour {

    init {
        filterableFeatureConfigBehaviour.initFilterableFeatureConfigBehaviour(
            onUpdate = { onUpdate() },
            io = io,
            ui = ui,
            coroutineScope = viewModelScope,
            dataInteractor = dataInteractor
        )
        singleFeatureConfigBehaviour.initSingleFeatureConfigBehaviour(
            onUpdate = { onUpdate() },
            featureChangeCallback = { filterableFeatureConfigBehaviour.onFeatureIdUpdated(it) },
        )
    }

    private var timeSinceLastStat: TimeSinceLastStat = TimeSinceLastStat(
        id = 0,
        graphStatId = 0,
        featureId = -1L,
        fromValue = 0.0,
        toValue = 1.0,
        labels = listOf(),
        filterByRange = false,
        filterByLabels = false
    )

    override fun updateConfig() {
        timeSinceLastStat = timeSinceLastStat.copy(
            featureId = singleFeatureConfigBehaviour.featureId ?: -1L,
            fromValue = filterableFeatureConfigBehaviour.fromValue.text.toDoubleOrNull() ?: 0.0,
            toValue = filterableFeatureConfigBehaviour.toValue.text.toDoubleOrNull() ?: 1.0,
            labels = filterableFeatureConfigBehaviour.selectedLabels,
            filterByRange = filterableFeatureConfigBehaviour.filterByRange,
            filterByLabels = filterableFeatureConfigBehaviour.filterByLabel
        )
    }

    override fun getConfig(): GraphStatConfigEvent.ConfigData.TimeSinceLastConfigData {
        return GraphStatConfigEvent.ConfigData.TimeSinceLastConfigData(timeSinceLastStat)
    }

    override suspend fun validate(): GraphStatConfigEvent.ValidationException? {
        if (timeSinceLastStat.featureId == -1L)
            return GraphStatConfigEvent.ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        if (timeSinceLastStat.fromValue > timeSinceLastStat.toValue)
            return GraphStatConfigEvent.ValidationException(R.string.graph_stat_validation_invalid_value_stat_from_to)
        return null
    }

    override fun onDataLoaded(config: Any?) {
        singleFeatureConfigBehaviour.iniFeatureMap(featurePathProvider.sortedFeatureMap())

        if (config !is TimeSinceLastStat) return
        this.timeSinceLastStat = config
        filterableFeatureConfigBehaviour.filterByLabel = config.filterByLabels
        filterableFeatureConfigBehaviour.filterByRange = config.filterByRange
        filterableFeatureConfigBehaviour.fromValue = config.fromValue.asTextFieldValue()
        filterableFeatureConfigBehaviour.toValue = config.toValue.asTextFieldValue()
        filterableFeatureConfigBehaviour.selectedLabels = config.labels
        singleFeatureConfigBehaviour.featureId = config.featureId
        filterableFeatureConfigBehaviour.getAvailableLabels()
    }
}