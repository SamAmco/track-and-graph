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
package com.samco.trackandgraph.graphstatinput.configviews.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.BarChart
import com.samco.trackandgraph.base.database.dto.BarChartBarPeriod
import com.samco.trackandgraph.base.database.dto.YRangeType
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
    private val singleFeatureConfigBehaviour: SingleFeatureConfigBehaviourImpl = SingleFeatureConfigBehaviourImpl(),
    private val yRangeConfigBehaviour: YRangeConfigBehaviourImpl = YRangeConfigBehaviourImpl(),
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.BarChartConfigData>(
    io,
    default,
    ui,
    gsiProvider,
    dataInteractor
), TimeRangeConfigBehaviour by timeRangeConfigBehaviour,
    SingleFeatureConfigBehaviour by singleFeatureConfigBehaviour,
    YRangeConfigBehaviour by yRangeConfigBehaviour {

    init {
        timeRangeConfigBehaviour.initTimeRangeConfigBehaviour { onUpdate() }
        singleFeatureConfigBehaviour.initSingleFeatureConfigBehaviour(onUpdate = { onUpdate() })
        yRangeConfigBehaviour.initYRangeConfigBehaviour(onUpdate = { onUpdate() })
    }

    var selectedBarPeriod: BarChartBarPeriod by mutableStateOf(BarChartBarPeriod.WEEK)
        private set

    var sumByCount: Boolean by mutableStateOf(false)
        private set

    fun updateSumByCount(value: Boolean) {
        sumByCount = value
        onUpdate()
    }

    fun updateBarPeriod(barChartBarPeriod: BarChartBarPeriod) {
        selectedBarPeriod = barChartBarPeriod
        onUpdate()
    }

    private var barChart = BarChart(
        id = 0,
        graphStatId = 0,
        featureId = 0,
        endDate = null,
        duration = null,
        yRangeType = YRangeType.DYNAMIC,
        yFrom = 0.0,
        yTo = 1.0,
        barPeriod = BarChartBarPeriod.WEEK,
        sumByCount = false
    )

    override fun updateConfig() {
        barChart = barChart.copy(
            featureId = this.featureId ?: -1,
            endDate = sampleEndingAt.asDateTime(),
            duration = selectedDuration.duration,
            yRangeType = yRangeType,
            yFrom = yRangeFrom.text.toDoubleOrNull() ?: 0.0,
            yTo = yRangeTo.text.toDoubleOrNull() ?: 1.0,
            barPeriod = selectedBarPeriod,
            sumByCount = sumByCount
        )
    }

    override fun getConfig(): GraphStatConfigEvent.ConfigData.BarChartConfigData {
        return GraphStatConfigEvent.ConfigData.BarChartConfigData(barChart)
    }

    override suspend fun validate(): GraphStatConfigEvent.ValidationException? {
        return if (singleFeatureConfigBehaviour.featureId == -1L) {
            GraphStatConfigEvent.ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        } else null
    }

    override fun onDataLoaded(config: Any?) {
        val bcConfig = config as? BarChart

        val featureMap = featurePathProvider.sortedFeatureMap()

        timeRangeConfigBehaviour.onConfigLoaded(
            duration = bcConfig?.duration,
            endingAt = bcConfig?.endDate
        )

        singleFeatureConfigBehaviour.onConfigLoaded(
            map = featureMap,
            featureId = bcConfig?.featureId ?: featureMap.keys.first()
        )

        yRangeConfigBehaviour.onConfigLoaded(
            yRangeType = bcConfig?.yRangeType,
            yFrom = bcConfig?.yFrom,
            yTo = bcConfig?.yTo
        )

        bcConfig?.let {
            barChart = it
            selectedBarPeriod = it.barPeriod
            sumByCount = it.sumByCount
        }
    }
}