package com.samco.trackandgraph.graphstatinput.configviews

import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@HiltViewModel
class TimeHistogramConfigViewModel @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.TimeHistogramConfigData>(
    io,
    default,
    ui,
    gsiProvider,
    dataInteractor
) {
    override fun getConfig(): GraphStatConfigEvent.ConfigData.TimeHistogramConfigData {
        TODO("Not yet implemented")
    }

    override suspend fun validate(): GraphStatConfigEvent.ValidationException? {
        TODO("Not yet implemented")
    }

    override fun onDataLoaded(config: Any?) {
        TODO("Not yet implemented")
    }
}
