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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LineGraphConfigViewModel @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    gsiProvider: GraphStatInteractorProvider,
    dataInteractor: DataInteractor
) : GraphStatConfigViewModelBase<GraphStatConfigEvent.ConfigData.LineGraphConfigData>(
    io,
    default,
    ui,
    gsiProvider,
    dataInteractor
) {

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
            if (!ignoreCustomFlag && name.text != this.name.text) customName = true
            this.name = name
            onUpdate()
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

    val featureNameMap: Map<Long, String>
        get() = featurePathProvider.value.features.associate { it.featureId to it.name }

    var selectedDuration by mutableStateOf(GraphStatDurations.ALL_DATA)
        private set
    var sampleEndingAt by mutableStateOf<SampleEndingAt>(SampleEndingAt.Latest)
        private set
    var yRangeType by mutableStateOf(YRangeType.DYNAMIC)
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

    private val allFeatureIds: StateFlow<Set<Long>> by lazy {
        flow {
            val ids = withContext(io) {
                dataInteractor.getAllFeaturesSync().map { it.featureId }.toSet()
            }
            emit(ids)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    }

    override fun onUpdate() {
        //TODO fill out the rest of the properties
        lineGraph = lineGraph.copy(
            duration = selectedDuration.duration,
            endDate = sampleEndingAt.asDateTime(),
            yRangeType = yRangeType,
            features = lineGraphFeatures
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

    fun onAddLineGraphFeatureClicked() = viewModelScope.launch {
        val firstFeature = featurePathProvider.first().features.firstOrNull()
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
    }

    override fun onDataLoaded(config: Any) {
        if (config !is LineGraphWithFeatures) return
        selectedDuration = GraphStatDurations.fromDuration(config.duration)
        sampleEndingAt = SampleEndingAt.fromDateTime(config.endDate)
        yRangeType = config.yRangeType
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
        onUpdate()
    }

    fun removeLineGraphFeature(index: Int) {
        lineGraphFeatures = lineGraphFeatures.toMutableList().apply { removeAt(index) }
        onUpdate()
    }

    fun updateLineGraphFeature(index: Int, newLgf: LineGraphFeature) = viewModelScope.launch {
        val newFeatureName = featureNameMap.getOrElse(newLgf.featureId) { "" }

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