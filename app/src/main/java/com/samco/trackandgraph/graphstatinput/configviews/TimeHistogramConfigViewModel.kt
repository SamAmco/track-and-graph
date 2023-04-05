package com.samco.trackandgraph.graphstatinput.configviews

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.TimeHistogram
import com.samco.trackandgraph.base.database.dto.TimeHistogramWindow
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.customviews.SampleEndingAt
import com.samco.trackandgraph.graphstatinput.dtos.GraphStatDurations
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
    dataInteractor: DataInteractor,
    private val timeRangeConfigBehaviour: TimeRangeConfigBehaviourImpl = TimeRangeConfigBehaviourImpl(),
    private val singleFeatureConfigBehaviour: SingleFeatureConfigBehaviourImpl = SingleFeatureConfigBehaviourImpl(),
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.TimeHistogramConfigData>(
    io,
    default,
    ui,
    gsiProvider,
    dataInteractor
), TimeRangeConfigBehaviour by timeRangeConfigBehaviour,
    SingleFeatureConfigBehaviour by singleFeatureConfigBehaviour {

    init {
        timeRangeConfigBehaviour.initTimeRangeConfigBehaviour { onUpdate() }
        singleFeatureConfigBehaviour.initSingleFeatureConfigBehaviour(onUpdate = { onUpdate() })
    }

    var selectedWindow: TimeHistogramWindow by mutableStateOf(TimeHistogramWindow.DAY)
        private set

    var sumByCount: Boolean by mutableStateOf(false)
        private set

    private var timeHistogram = TimeHistogram(
        id = 0L,
        graphStatId = 0L,
        featureId = -1L,
        duration = null,
        window = TimeHistogramWindow.DAY,
        sumByCount = false,
        endDate = null
    )

    override fun updateConfig() {
        timeHistogram = timeHistogram.copy(
            featureId = singleFeatureConfigBehaviour.featureId ?: -1L,
            duration = timeRangeConfigBehaviour.selectedDuration.duration,
            window = selectedWindow,
            sumByCount = sumByCount,
            endDate = timeRangeConfigBehaviour.sampleEndingAt.asDateTime()
        )
    }

    override fun getConfig(): GraphStatConfigEvent.ConfigData.TimeHistogramConfigData {
        return GraphStatConfigEvent.ConfigData.TimeHistogramConfigData(timeHistogram)
    }

    override suspend fun validate(): GraphStatConfigEvent.ValidationException? {
        return if (singleFeatureConfigBehaviour.featureId == -1L) {
            GraphStatConfigEvent.ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        } else null
    }

    override fun onDataLoaded(config: Any?) {
        singleFeatureConfigBehaviour.iniFeatureMap(featurePathProvider.sortedFeatureMap())

        if (config !is TimeHistogram) return
        this.timeHistogram = config
        timeRangeConfigBehaviour.selectedDuration = GraphStatDurations.fromDuration(config.duration)
        timeRangeConfigBehaviour.sampleEndingAt = SampleEndingAt.fromDateTime(config.endDate)
        singleFeatureConfigBehaviour.featureId = config.featureId
        selectedWindow = config.window
        sumByCount = config.sumByCount
    }

    fun updateWindow(window: TimeHistogramWindow) {
        this.selectedWindow = window
        onUpdate()
    }

    fun updateSumByCount(sumByCount: Boolean) {
        this.sumByCount = sumByCount
        onUpdate()
    }
}
