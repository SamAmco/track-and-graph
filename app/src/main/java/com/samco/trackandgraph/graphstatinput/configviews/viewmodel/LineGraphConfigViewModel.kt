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
package com.samco.trackandgraph.graphstatinput.configviews.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.DurationPlottingMode
import com.samco.trackandgraph.data.database.dto.GraphEndDate
import com.samco.trackandgraph.data.database.dto.LineGraphAveraginModes
import com.samco.trackandgraph.data.database.dto.LineGraphFeature
import com.samco.trackandgraph.data.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.di.DefaultDispatcher
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent
import com.samco.trackandgraph.graphstatinput.GraphStatConfigEvent.ValidationException
import com.samco.trackandgraph.graphstatinput.configviews.behaviour.TimeRangeConfigBehaviour
import com.samco.trackandgraph.graphstatinput.configviews.behaviour.TimeRangeConfigBehaviourImpl
import com.samco.trackandgraph.graphstatinput.configviews.behaviour.YRangeConfigBehaviour
import com.samco.trackandgraph.graphstatinput.configviews.behaviour.YRangeConfigBehaviourImpl
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.ui.dataVisColorGenerator
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.ui.viewmodels.asValidatedDouble
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LineGraphConfigViewModel @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor,
    dataSampler: DataSampler,
    private val timeRangeConfigBehaviour: TimeRangeConfigBehaviourImpl = TimeRangeConfigBehaviourImpl(),
    private val yRangeConfigBehaviour: YRangeConfigBehaviourImpl = YRangeConfigBehaviourImpl()
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.LineGraphConfigData>(
    io = io,
    default = default,
    ui = ui,
    gsiProvider = gsiProvider,
    dataInteractor = dataInteractor,
    dataSampler = dataSampler
), TimeRangeConfigBehaviour by timeRangeConfigBehaviour,
    YRangeConfigBehaviour by yRangeConfigBehaviour {

    init {
        timeRangeConfigBehaviour.initTimeRangeConfigBehaviour { onUpdate() }
        yRangeConfigBehaviour.initYRangeConfigBehaviour { onUpdate() }
    }

    inner class FeatureTextFields(
        name: String,
        offset: String = "0",
        scale: String = "1"
    ) {
        var name by mutableStateOf(TextFieldValue(name, TextRange(name.length)))
            private set
        var offset by mutableStateOf(TextFieldValue(offset, TextRange(offset.length)))
            private set
        var scale by mutableStateOf(TextFieldValue(scale, TextRange(scale.length)))
            private set

        fun updateName(name: TextFieldValue, triggerUpdate: Boolean = true) {
            this.name = name
            if (triggerUpdate) onUpdate()
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

    data class LineGraphFeatureUiData(
        val nameTextField: TextFieldValue,
        val offsetTextField: TextFieldValue,
        val scaleTextField: TextFieldValue,
        val selectedFeatureText: String,
        val colorIndex: Int,
        val averagingMode: LineGraphAveraginModes,
        val plottingMode: LineGraphPlottingModes,
        val pointStyle: LineGraphPointStyle,
        val isDurationFeature: Boolean,
        val durationPlottingMode: DurationPlottingMode,
        val featureId: Long
    )

    private var lineGraphFeatures by mutableStateOf(emptyList<LineGraphFeature>())
    private val featureTextFields = mutableListOf<FeatureTextFields>()

    val lineGraphFeatureUiDataList by derivedStateOf {
        lineGraphFeatures.mapIndexed { index, feature ->
            val isDuration = featurePathProvider.getDataSampleProperties(feature.featureId)?.isDuration == true
            val textFields = featureTextFields.getOrNull(index)
            LineGraphFeatureUiData(
                nameTextField = textFields?.name ?: TextFieldValue(),
                offsetTextField = textFields?.offset ?: TextFieldValue(),
                scaleTextField = textFields?.scale ?: TextFieldValue(),
                selectedFeatureText = featurePathProvider.getPathForFeature(feature.featureId),
                colorIndex = feature.colorIndex,
                averagingMode = feature.averagingMode,
                plottingMode = feature.plottingMode,
                pointStyle = feature.pointStyle,
                isDurationFeature = isDuration,
                durationPlottingMode = feature.durationPlottingMode,
                featureId = feature.featureId
            )
        }
    }

    private val isTimeBasedRange = snapshotFlow { lineGraphFeatures }
        .map { lgfs -> lgfs.any { it.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE } }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private var lineGraph = LineGraphWithFeatures(
        id = 0L,
        graphStatId = 0L,
        features = listOf(),
        sampleSize = null,
        yRangeType = YRangeType.DYNAMIC,
        yFrom = 0.0,
        yTo = 1.0,
        endDate = GraphEndDate.Latest
    )

    override fun updateConfig() {
        lineGraph = lineGraph.copy(
            sampleSize = selectedDuration.temporalAmount,
            endDate = sampleEndingAt.asGraphEndDate(),
            yRangeType = yRangeType,
            features = lineGraphFeatures.mapIndexed { index, f ->
                f.copy(
                    name = featureTextFields[index].name.text,
                    offset = featureTextFields[index].offset.text.toDoubleOrNull() ?: 0.0,
                    scale = featureTextFields[index].scale.text.toDoubleOrNull() ?: 1.0
                )
            },
            yFrom = getYFrom(),
            yTo = getYTo()
        )
    }

    private fun getYFrom(): Double =
        if (isTimeBasedRange.value) yRangeFromDurationViewModel.getDurationAsDouble()
        else yRangeFrom.text.toDoubleOrNull() ?: 0.0

    private fun getYTo(): Double =
        if (isTimeBasedRange.value) yRangeToDurationViewModel.getDurationAsDouble()
        else yRangeTo.text.toDoubleOrNull() ?: 1.0

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

    fun onAddLineGraphFeatureClicked() = viewModelScope.launch {
        val firstFeature = featurePathProvider.features.firstOrNull()
        val featureName = firstFeature?.name ?: ""
        val isDuration = firstFeature?.featureId?.let {
            featurePathProvider.getDataSampleProperties(it)?.isDuration
        } == true
        val durationPlottingMode =
            if (isDuration) DurationPlottingMode.DURATION_IF_POSSIBLE
            else DurationPlottingMode.NONE
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
                    durationPlottingMode = durationPlottingMode
                )
            )
        }
        onUpdate()
    }

    override fun onDataLoaded(config: Any?) {

        val lgConfig = config as? LineGraphWithFeatures

        timeRangeConfigBehaviour.onConfigLoaded(
            sampleSize = lgConfig?.sampleSize,
            endingAt = lgConfig?.endDate
        )

        yRangeConfigBehaviour.onConfigLoaded(
            yRangeType = lgConfig?.yRangeType,
            yFrom = lgConfig?.yFrom,
            yTo = lgConfig?.yTo,
            timeBasedRange = isTimeBasedRange
        )

        lgConfig?.let {
            lineGraph = it
            lineGraphFeatures = it.features
            it.features.forEach { lgf ->
                featureTextFields.add(
                    FeatureTextFields(
                        lgf.name,
                        lgf.offset.toString(),
                        lgf.scale.toString()
                    )
                )
            }
        }

        viewModelScope.launch {
            isTimeBasedRange.drop(1).collect {
                onUpdate()
            }
        }
    }

    fun removeLineGraphFeature(index: Int) {
        lineGraphFeatures = lineGraphFeatures.toMutableList().apply { removeAt(index) }
        featureTextFields.removeAt(index)
        onUpdate()
    }

    fun onUpdateFeatureName(index: Int, text: TextFieldValue) {
        featureTextFields.getOrNull(index)?.updateName(text)
    }

    fun onUpdateFeatureOffset(index: Int, text: TextFieldValue) {
        featureTextFields.getOrNull(index)?.updateOffset(text)
    }

    fun onUpdateFeatureScale(index: Int, text: TextFieldValue) {
        featureTextFields.getOrNull(index)?.updateScale(text)
    }

    fun onUpdateFeatureColor(index: Int, colorIndex: Int) {
        lineGraphFeatures = lineGraphFeatures.toMutableList().apply {
            set(index, get(index).copy(colorIndex = colorIndex))
        }
        onUpdate()
    }

    fun onUpdateFeaturePointStyle(index: Int, pointStyle: LineGraphPointStyle) {
        lineGraphFeatures = lineGraphFeatures.toMutableList().apply {
            set(index, get(index).copy(pointStyle = pointStyle))
        }
        onUpdate()
    }

    fun onUpdateFeatureAveragingMode(index: Int, averagingMode: LineGraphAveraginModes) {
        lineGraphFeatures = lineGraphFeatures.toMutableList().apply {
            set(index, get(index).copy(averagingMode = averagingMode))
        }
        onUpdate()
    }

    fun onUpdateFeaturePlottingMode(index: Int, plottingMode: LineGraphPlottingModes) {
        lineGraphFeatures = lineGraphFeatures.toMutableList().apply {
            set(index, get(index).copy(plottingMode = plottingMode))
        }
        onUpdate()
    }

    fun onUpdateFeatureDurationPlottingMode(index: Int, durationPlottingMode: DurationPlottingMode) {
        lineGraphFeatures = lineGraphFeatures.toMutableList().apply {
            set(index, get(index).copy(durationPlottingMode = durationPlottingMode))
        }
        onUpdate()
    }

    fun onSelectFeature(index: Int, featureId: Long) {
        val isDuration = featurePathProvider.getDataSampleProperties(featureId)?.isDuration == true
        val defaultDurationMode = if (isDuration) DurationPlottingMode.DURATION_IF_POSSIBLE else DurationPlottingMode.NONE
        val newFeatureName = featurePathProvider.featureName(featureId) ?: ""
        val oldFeatureName = featurePathProvider.featureName(lineGraphFeatures[index].featureId) ?: ""

        // Update the feature
        lineGraphFeatures = lineGraphFeatures.toMutableList().apply {
            set(
                index, get(index).copy(
                    featureId = featureId,
                    durationPlottingMode = defaultDurationMode
                )
            )
        }

        // Update the name field if it matches the old feature name
        val textFields = featureTextFields[index]
        if (textFields.name.text == oldFeatureName) {
            textFields.updateName(
                TextFieldValue(newFeatureName, TextRange(newFeatureName.length)),
                triggerUpdate = false
            )
        }

        onUpdate()
    }
}