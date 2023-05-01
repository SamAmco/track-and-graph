package com.samco.trackandgraph.graphstatinput.configviews

import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.LastValueStat
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.customviews.SampleEndingAt
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.ui.viewmodels.asTextFieldValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@HiltViewModel
class LastValueConfigViewModel @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor,
    private val endingAtConfigBehaviour: TimeRangeConfigBehaviourImpl = TimeRangeConfigBehaviourImpl(),
    private val singleFeatureConfigBehaviour: SingleFeatureConfigBehaviourImpl = SingleFeatureConfigBehaviourImpl(),
    private val filterableFeatureConfigBehaviour: FilterableFeatureConfigBehaviourImpl = FilterableFeatureConfigBehaviourImpl()
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.LastValueConfigData>(
    io,
    default,
    ui,
    gsiProvider,
    dataInteractor
),
    EndingAtConfigBehaviour by endingAtConfigBehaviour,
    FilterableFeatureConfigBehaviour by filterableFeatureConfigBehaviour,
    SingleFeatureConfigBehaviour by singleFeatureConfigBehaviour {

    init {
        endingAtConfigBehaviour.initTimeRangeConfigBehaviour { onUpdate() }
        filterableFeatureConfigBehaviour.initFilterableFeatureConfigBehaviour(
            onUpdate = { onUpdate() },
            io = io,
            ui = ui,
            coroutineScope = viewModelScope,
            dataInteractor = dataInteractor
        )
        singleFeatureConfigBehaviour.initSingleFeatureConfigBehaviour(
            onUpdate = { onUpdate() },
            featureChangeCallback = { filterableFeatureConfigBehaviour.onFeatureIdUpdated(it) }
        )
    }

    private var lastValueStat = LastValueStat(
        id = 0,
        graphStatId = 0,
        featureId = -1L,
        endDate = null,
        fromValue = 0.0,
        toValue = 0.0,
        labels = emptyList(),
        filterByRange = false,
        filterByLabels = false
    )

    override fun updateConfig() {
        lastValueStat = lastValueStat.copy(
            featureId = singleFeatureConfigBehaviour.featureId ?: -1L,
            endDate = endingAtConfigBehaviour.sampleEndingAt.asDateTime(),
            fromValue = filterableFeatureConfigBehaviour.fromValue.text.toDoubleOrNull() ?: 0.0,
            toValue = filterableFeatureConfigBehaviour.toValue.text.toDoubleOrNull() ?: 1.0,
            labels = filterableFeatureConfigBehaviour.selectedLabels,
            filterByRange = filterableFeatureConfigBehaviour.filterByRange,
            filterByLabels = filterableFeatureConfigBehaviour.filterByLabel
        )
    }

    override fun getConfig(): GraphStatConfigEvent.ConfigData.LastValueConfigData {
        return GraphStatConfigEvent.ConfigData.LastValueConfigData(lastValueStat)
    }

    override suspend fun validate(): GraphStatConfigEvent.ValidationException? {
        return if (singleFeatureConfigBehaviour.featureId == -1L) {
            GraphStatConfigEvent.ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        } else null
    }

    override fun onDataLoaded(config: Any?) {
        singleFeatureConfigBehaviour.iniFeatureMap(featurePathProvider.sortedFeatureMap())

        if (config !is LastValueStat) return
        this.lastValueStat = config

        //When you set this feature ID it will trigger a callback that updates the filterable
        // feature config behaviour
        singleFeatureConfigBehaviour.featureId = config.featureId
        filterableFeatureConfigBehaviour.getAvailableLabels()

        endingAtConfigBehaviour.sampleEndingAt = SampleEndingAt.fromDateTime(config.endDate)
        filterableFeatureConfigBehaviour.filterByLabel = config.filterByLabels
        filterableFeatureConfigBehaviour.filterByRange = config.filterByRange
        filterableFeatureConfigBehaviour.fromValue = config.fromValue.asTextFieldValue()
        filterableFeatureConfigBehaviour.toValue = config.toValue.asTextFieldValue()
        filterableFeatureConfigBehaviour.selectedLabels = config.labels
    }
}