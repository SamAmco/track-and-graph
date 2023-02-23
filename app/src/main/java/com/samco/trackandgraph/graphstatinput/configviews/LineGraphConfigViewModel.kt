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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DurationPlottingMode
import com.samco.trackandgraph.base.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.base.database.dto.YRangeType
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent.ValidationException
import com.samco.trackandgraph.graphstatinput.customviews.SampleEndingAt
import com.samco.trackandgraph.graphstatinput.dtos.GraphStatDurations
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.ui.dataVisColorList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LineGraphConfigViewModel @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.LineGraphConfigData>(
    io,
    default,
    gsiProvider,
    dataInteractor
) {

    var selectedDuration by mutableStateOf(GraphStatDurations.ALL_DATA)
        private set
    var sampleEndingAt by mutableStateOf<SampleEndingAt>(SampleEndingAt.Latest)
        private set
    var yRangeType by mutableStateOf(YRangeType.DYNAMIC)
        private set

    private var lineGraph = LineGraphWithFeatures(
        id = 0L,
        graphStatId = 0L,
        features = listOf(),
        duration = null,
        yRangeType = YRangeType.DYNAMIC,
        yFrom = 0.0,
        yTo = 1.0,
        endDate = null
    )

    private val allFeatureIds: StateFlow<Set<Long>> by lazy {
        flow {
            withContext(io) {
                emit(dataInteractor.getAllFeaturesSync().map { it.featureId }.toSet())
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    }

    //TODO remove this code when you can but you might need something like this to get the feature paths
    // at some point
/*
    protected val allFeatureIds by lazy {
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
        val featureDataProvider = FeatureDataProvider(dataSourceData, allGroups)
        featureDataProvider.features?.map { it.featureId }?.toSet() ?: emptySet()
    }
*/

    override fun onUpdate() {
        lineGraph = lineGraph.copy(
            duration = selectedDuration.duration
        )
        super.onUpdate()
    }

    override fun getConfig() = GraphStatConfigEvent.ConfigData.LineGraphConfigData(lineGraph)

    override suspend fun validate(): ValidationException? {
        if (lineGraph.features.isEmpty()) {
            return ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        }
        lineGraph.features.forEach { f ->
            if (f.colorIndex !in dataVisColorList.indices) {
                return ValidationException(R.string.graph_stat_validation_unrecognised_color)
            }
            if (!allFeatureIds.value.contains(f.featureId)) {
                return ValidationException(R.string.graph_stat_validation_invalid_line_graph_feature)
            }
        }
        if (lineGraph.features.any { it.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE }
            && !lineGraph.features.all { it.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE }) {
            return ValidationException(R.string.graph_stat_validation_mixed_time_value_line_graph_features)
        }
        if (lineGraph.yRangeType == YRangeType.FIXED && lineGraph.yFrom >= lineGraph.yTo) {
            return ValidationException(R.string.graph_stat_validation_bad_fixed_range)
        }
        return null
    }

    fun updateDuration(duration: GraphStatDurations) {
        selectedDuration = duration
        onUpdate()
    }

    fun updateSampleEndingAt(endingAt: SampleEndingAt) {
        sampleEndingAt = endingAt
        onUpdate()
    }

    fun updateYRangeType(type: YRangeType) {
        yRangeType = type
        onUpdate()
    }

    override fun onDataLoaded(config: Any) {
        if (config !is LineGraphWithFeatures) return
        lineGraph = config
    }
}