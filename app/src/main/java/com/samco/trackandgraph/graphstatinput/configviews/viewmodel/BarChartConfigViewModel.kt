package com.samco.trackandgraph.graphstatinput.configviews.viewmodel

import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.configviews.behaviour.*
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@HiltViewModel
class BarChartConfigViewModel @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor,
    private val timeRangeConfigBehaviour: TimeRangeConfigBehaviourImpl = TimeRangeConfigBehaviourImpl(),
    private val filterableFeatureConfigBehaviour: FilterableFeatureConfigBehaviourImpl = FilterableFeatureConfigBehaviourImpl(),
    private val singleFeatureConfigBehaviour: SingleFeatureConfigBehaviourImpl = SingleFeatureConfigBehaviourImpl(),
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.BarChartConfigData>(
    io,
    default,
    ui,
    gsiProvider,
    dataInteractor
), TimeRangeConfigBehaviour by timeRangeConfigBehaviour,
    FilterableFeatureConfigBehaviour by filterableFeatureConfigBehaviour,
    SingleFeatureConfigBehaviour by singleFeatureConfigBehaviour {

    override fun updateConfig() {
        TODO("Not yet implemented")
    }

    override fun getConfig(): GraphStatConfigEvent.ConfigData.BarChartConfigData {
        TODO("Not yet implemented")
    }

    override suspend fun validate(): GraphStatConfigEvent.ValidationException? {
        TODO("Not yet implemented")
    }

    override fun onDataLoaded(config: Any?) {
        TODO("Not yet implemented")
    }
}