package com.samco.trackandgraph.graphstatinput.configviews

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.PieChart
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
class PieChartConfigViewModel @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.PieChartConfigData>(
    io,
    default,
    ui,
    gsiProvider,
    dataInteractor
) {
    var selectedDuration by mutableStateOf(GraphStatDurations.ALL_DATA)
        private set

    var sampleEndingAt by mutableStateOf<SampleEndingAt>(SampleEndingAt.Latest)
        private set

    var featureId: Long? by mutableStateOf(null)
        private set

    var featureMap: Map<Long, String>? by mutableStateOf(null)
        private set

    private var pieChart = PieChart(
        id = 0L,
        graphStatId = 0L,
        featureId = -1L,
        duration = null,
        endDate = null
    )

    override fun onUpdate() {
        pieChart = pieChart.copy(
            featureId = this.featureId ?: -1L,
            duration = selectedDuration.duration,
            endDate = sampleEndingAt.asDateTime()
        )
        super.onUpdate()
    }

    override fun getConfig(): GraphStatConfigEvent.ConfigData.PieChartConfigData {
        return GraphStatConfigEvent.ConfigData.PieChartConfigData(pieChart)
    }

    private suspend fun hasLabels(featureId: Long): Boolean {
        return dataInteractor.getLabelsForFeatureId(featureId).isNotEmpty()
    }

    override suspend fun validate(): GraphStatConfigEvent.ValidationException? {
        val id = pieChart.featureId
        if (id == -1L || !hasLabels(id)) {
            return GraphStatConfigEvent.ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        }
        return null
    }

    override fun onDataLoaded(config: Any?) {
        featureMap = featurePathProvider.sortedFeatureMap()
        featureId = featureMap?.keys?.firstOrNull()

        if (config !is PieChart) return
        pieChart = config
        selectedDuration = GraphStatDurations.fromDuration(config.duration)
        sampleEndingAt = SampleEndingAt.fromDateTime(config.endDate)
        featureId = config.featureId
    }

    fun onFeatureSelected(featureId: Long) {
        this.featureId = featureId
        this.onUpdate()
    }

    fun updateDuration(duration: GraphStatDurations) {
        this.selectedDuration = duration
        this.onUpdate()
    }

    fun updateSampleEndingAt(endingAt: SampleEndingAt) {
        this.sampleEndingAt = endingAt
        this.onUpdate()
    }
}