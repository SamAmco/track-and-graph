package com.samco.trackandgraph.graphstatinput.configviews

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.AverageTimeBetweenStat
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AverageTimeBetweenConfigViewModel @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor,
    private val timeRangeConfigBehaviour: TimeRangeConfigBehaviourImpl = TimeRangeConfigBehaviourImpl()
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.AverageTimeBetweenConfigData>(
    io,
    default,
    ui,
    gsiProvider,
    dataInteractor
), TimeRangeConfigBehaviour by timeRangeConfigBehaviour {

    init {
        timeRangeConfigBehaviour.initTimeRangeConfigBehaviour { onUpdate() }
    }

    private var labelUpdateJob: Job? = null

    var featureMap: Map<Long, String>? by mutableStateOf(null)
        private set

    var featureId: Long? by mutableStateOf(null)
        private set

    var availableLabels: List<String> by mutableStateOf(listOf())
        private set

    var selectedLabels: List<String> by mutableStateOf(listOf())
        private set

    var fromValue: Double by mutableStateOf(0.0)
        private set

    var toValue: Double by mutableStateOf(1.0)
        private set

    var filterByLabel: Boolean by mutableStateOf(false)
        private set

    var filterByRange: Boolean by mutableStateOf(false)
        private set

    var loadingLabels: Boolean by mutableStateOf(false)
        private set

    private var averageTimeBetweenStat: AverageTimeBetweenStat = AverageTimeBetweenStat(
        id = 0,
        graphStatId = 0,
        featureId = -1L,
        fromValue = 0.0,
        toValue = 1.0,
        duration = null,
        labels = listOf(),
        endDate = null,
        filterByRange = false,
        filterByLabels = false
    )

    override fun onUpdate() {
        averageTimeBetweenStat = averageTimeBetweenStat.copy(
            featureId = featureId ?: -1L,
            fromValue = fromValue,
            toValue = toValue,
            duration = selectedDuration.duration,
            labels = selectedLabels,
            endDate = sampleEndingAt.asDateTime(),
            filterByRange = filterByRange,
            filterByLabels = filterByLabel
        )
        super.onUpdate()
    }

    override fun getConfig(): GraphStatConfigEvent.ConfigData.AverageTimeBetweenConfigData {
        return GraphStatConfigEvent.ConfigData.AverageTimeBetweenConfigData(averageTimeBetweenStat)
    }

    override suspend fun validate(): GraphStatConfigEvent.ValidationException? {
        if (averageTimeBetweenStat.featureId == -1L)
            return GraphStatConfigEvent.ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        if (averageTimeBetweenStat.fromValue > averageTimeBetweenStat.toValue)
            return GraphStatConfigEvent.ValidationException(R.string.graph_stat_validation_invalid_value_stat_from_to)
        return null
    }

    override fun onDataLoaded(config: Any?) {
        featureMap = featurePathProvider.sortedFeatureMap()
        featureId = featureMap?.keys?.firstOrNull()

        if (config !is AverageTimeBetweenStat) return
        this.averageTimeBetweenStat = config
        this.filterByLabel = config.filterByLabels
        this.filterByRange = config.filterByRange
        this.fromValue = config.fromValue
        this.toValue = config.toValue
        this.selectedLabels = config.labels
        this.featureId = config.featureId
        getAvailableLabels()
    }

    private fun getAvailableLabels() {
        featureId?.let { fId ->
            labelUpdateJob?.cancel()
            labelUpdateJob = viewModelScope.launch(ui) {
                loadingLabels = true
                val labels = withContext(io) { dataInteractor.getLabelsForFeatureId(fId) }
                availableLabels = labels
                loadingLabels = false
            }
        }
    }

    fun updateFeatureId(id: Long) {
        if (id == featureId) return
        featureId = id
        getAvailableLabels()
        selectedLabels = emptyList()
        onUpdate()
    }

    fun updateFromValue(value: Double) {
        fromValue = value
        onUpdate()
    }

    fun updateToValue(value: Double) {
        toValue = value
        onUpdate()
    }

    fun updateSelectedLabels(labels: List<String>) {
        selectedLabels = labels
        onUpdate()
    }

    fun updateFilterByLabel(filter: Boolean) {
        filterByLabel = filter
        onUpdate()
    }

    fun updateFilterByRange(filter: Boolean) {
        filterByRange = filter
        onUpdate()
    }

}