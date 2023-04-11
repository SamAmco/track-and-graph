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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent.ValidationException
import com.samco.trackandgraph.graphstatinput.customviews.SampleEndingAt
import com.samco.trackandgraph.graphstatinput.dtos.GraphStatDurations
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.ui.dataVisColorGenerator
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.ui.viewmodels.asValidatedDouble
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LineGraphConfigViewModel @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor,
    private val timeRangeConfigBehaviour: TimeRangeConfigBehaviourImpl = TimeRangeConfigBehaviourImpl()
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.LineGraphConfigData>(
    io,
    default,
    ui,
    gsiProvider,
    dataInteractor
), TimeRangeConfigBehaviour by timeRangeConfigBehaviour {

    init {
        timeRangeConfigBehaviour.initTimeRangeConfigBehaviour { onUpdate() }
    }

    inner class FeatureTextFields(
        name: String,
        offset: String = "0",
        scale: String = "1"
    ) {
        var customName = false
            private set

        var name by mutableStateOf(TextFieldValue(""))
            private set
        var offset by mutableStateOf(TextFieldValue(""))
            private set
        var scale by mutableStateOf(TextFieldValue(""))
            private set

        init {
            this.name = TextFieldValue(name, TextRange(name.length))
            this.offset = TextFieldValue(offset, TextRange(offset.length))
            this.scale = TextFieldValue(scale, TextRange(scale.length))
        }

        fun updateName(name: TextFieldValue, ignoreCustomFlag: Boolean = false) {
            val triggerUpdate = !ignoreCustomFlag && name.text != this.name.text
            this.name = name
            if (triggerUpdate) {
                customName = true
                onUpdate()
            }
        }

        fun updateOffset(offset: TextFieldValue) {
            this.offset = offset.asValidatedDouble()
            onUpdate()
        }

        fun updateScale(scale: TextFieldValue) {
            this.scale = scale.asValidatedDouble()
            onUpdate()
        }
    }

    var featureNameMap: Map<Long, String> by mutableStateOf(emptyMap())

    var yRangeType by mutableStateOf(YRangeType.DYNAMIC)
        private set
    var yRangeFrom by mutableStateOf(TextFieldValue("0.0"))
        private set
    var yRangeTo by mutableStateOf(TextFieldValue("1.0"))
        private set
    var lineGraphFeatures by mutableStateOf(emptyList<LineGraphFeature>())
        private set


    private val featureTextFields = mutableListOf<FeatureTextFields>()

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

    override fun updateConfig() {
        lineGraph = lineGraph.copy(
            duration = selectedDuration.duration,
            endDate = sampleEndingAt.asDateTime(),
            yRangeType = yRangeType,
            features = lineGraphFeatures.mapIndexed { index, f ->
                f.copy(
                    name = featureTextFields[index].name.text,
                    offset = featureTextFields[index].offset.text.toDoubleOrNull() ?: 0.0,
                    scale = featureTextFields[index].scale.text.toDoubleOrNull() ?: 1.0
                )
            },
            yFrom = yRangeFrom.text.toDoubleOrNull() ?: 0.0,
            yTo = yRangeTo.text.toDoubleOrNull() ?: 1.0
        )
    }

    override fun getConfig(): GraphStatConfigEvent.ConfigData.LineGraphConfigData {
        return GraphStatConfigEvent.ConfigData.LineGraphConfigData(lineGraph)
    }

    override suspend fun validate(): ValidationException? {
        if (lineGraph.features.isEmpty()) {
            return ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        }
        lineGraph.features.forEach { f ->
            if (f.colorIndex !in dataVisColorList.indices) {
                return ValidationException(R.string.graph_stat_validation_unrecognised_color)
            }
            if (!featureNameMap.keys.contains(f.featureId)) {
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

    fun updateYRangeType(type: YRangeType) {
        yRangeType = type
        onUpdate()
    }

    fun updateYRangeFrom(from: TextFieldValue) {
        yRangeFrom = from.asValidatedDouble()
        onUpdate()
    }

    fun updateYRangeTo(to: TextFieldValue) {
        yRangeTo = to.asValidatedDouble()
        onUpdate()
    }

    fun onAddLineGraphFeatureClicked() = viewModelScope.launch {
        val firstFeature = featurePathProvider.features.firstOrNull()
        val featureName = firstFeature?.name ?: ""
        featureTextFields.add(FeatureTextFields(featureName))
        lineGraphFeatures = lineGraphFeatures.toMutableList().apply {
            add(
                LineGraphFeature(
                    id = 0L,
                    lineGraphId = -1L,
                    featureId = firstFeature?.featureId ?: -1L,
                    name = featureName,
                    colorIndex = (size * dataVisColorGenerator) % dataVisColorList.size,
                    averagingMode = LineGraphAveraginModes.NO_AVERAGING,
                    plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                    pointStyle = LineGraphPointStyle.NONE,
                    offset = 0.toDouble(),
                    scale = 1.toDouble(),
                    durationPlottingMode = DurationPlottingMode.NONE,
                )
            )
        }
        onUpdate()
    }

    override fun onDataLoaded(config: Any?) {
        featureNameMap = featurePathProvider.sortedFeatureMap()

        if (config !is LineGraphWithFeatures) return
        lineGraph = config
        timeRangeConfigBehaviour.selectedDuration = GraphStatDurations.fromDuration(config.duration)
        timeRangeConfigBehaviour.sampleEndingAt = SampleEndingAt.fromDateTime(config.endDate)
        yRangeType = config.yRangeType
        yRangeFrom = TextFieldValue(config.yFrom.toString())
        yRangeTo = TextFieldValue(config.yTo.toString())
        lineGraphFeatures = config.features
        config.features.forEach {
            featureTextFields.add(
                FeatureTextFields(
                    it.name,
                    it.offset.toString(),
                    it.scale.toString()
                )
            )
        }
    }

    fun removeLineGraphFeature(index: Int) {
        lineGraphFeatures = lineGraphFeatures.toMutableList().apply { removeAt(index) }
        featureTextFields.removeAt(index)
        onUpdate()
    }

    fun updateLineGraphFeature(index: Int, newLgf: LineGraphFeature) = viewModelScope.launch {
        val newFeatureName = featurePathProvider.features
            .firstOrNull { it.featureId == newLgf.featureId }?.name ?: ""

        lineGraphFeatures = lineGraphFeatures.toMutableList().apply {
            val textFields = featureTextFields[index]
            if (!textFields.customName) textFields.updateName(
                TextFieldValue(
                    newFeatureName,
                    TextRange(newFeatureName.length)
                ),
                ignoreCustomFlag = true
            )
            set(
                index, newLgf.copy(
                    name = textFields.name.text,
                    offset = textFields.offset.text.toDoubleOrNull() ?: 0.toDouble(),
                    scale = textFields.scale.text.toDoubleOrNull() ?: 1.toDouble()
                )
            )
        }
        onUpdate()
    }

    fun getTextFieldsFor(index: Int): FeatureTextFields {
        return featureTextFields[index]
    }
}