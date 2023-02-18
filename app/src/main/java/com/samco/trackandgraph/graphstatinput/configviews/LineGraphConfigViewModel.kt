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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DurationPlottingMode
import com.samco.trackandgraph.base.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.base.database.dto.YRangeType
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.graphstatinput.ConfigData
import com.samco.trackandgraph.graphstatinput.ValidationException
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.ui.dataVisColorList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.threeten.bp.Duration
import javax.inject.Inject

@HiltViewModel
class LineGraphConfigViewModel @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor
) : GraphStatConfigViewModelBase<ConfigData.LineGraphConfigData>(io, gsiProvider, dataInteractor) {

    var selectedDuration by mutableStateOf(GraphStatDurations.ALL_DATA)
        private set

    var lineGraph = LineGraphWithFeatures(
        id = 0L,
        graphStatId = 0L,
        features = listOf(),
        duration = null,
        yRangeType = YRangeType.DYNAMIC,
        yFrom = 0.0,
        yTo = 1.0,
        endDate = null
    )
        private set

    private fun onUpdate() {
        lineGraph = lineGraph.copy(
            duration = selectedDuration.duration
        )
        val config = ConfigData.LineGraphConfigData(lineGraph)
        updateConfig(config)
        validateConfig()
    }

    private fun validateConfig() {
        if (lineGraph.features.isEmpty()) {
            updateValidationException(ValidationException(R.string.graph_stat_validation_no_line_graph_features))
            return
        }
        lineGraph.features.forEach { f ->
            if (f.colorIndex !in dataVisColorList.indices) {
                updateValidationException(ValidationException(R.string.graph_stat_validation_unrecognised_color))
                return
            }
            if (!allFeatureIds.contains(f.featureId)) {
                updateValidationException(ValidationException(R.string.graph_stat_validation_invalid_line_graph_feature))
                return
            }
        }
        if (lineGraph.features.any { it.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE }
            && !lineGraph.features.all { it.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE }) {
            updateValidationException(ValidationException(R.string.graph_stat_validation_mixed_time_value_line_graph_features))
            return
        }
        if (lineGraph.yRangeType == YRangeType.FIXED && lineGraph.yFrom >= lineGraph.yTo) {
            updateValidationException(ValidationException(R.string.graph_stat_validation_bad_fixed_range))
            return
        }
        updateValidationException(null)
    }

    fun setDuration(duration: GraphStatDurations) {
        selectedDuration = duration
        onUpdate()
    }

    override fun onDataLoaded(config: Any) {
        if (config !is LineGraphWithFeatures) return
    }
}