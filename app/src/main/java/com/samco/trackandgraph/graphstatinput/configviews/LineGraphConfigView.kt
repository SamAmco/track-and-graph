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

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import android.widget.AdapterView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DurationPlottingMode
import com.samco.trackandgraph.base.database.dto.LineGraphAveraginModes
import com.samco.trackandgraph.base.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.base.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.base.database.doubleFormatter
import com.samco.trackandgraph.base.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.base.database.dto.YRangeType
import com.samco.trackandgraph.base.database.entity.LineGraphFeature
import com.samco.trackandgraph.base.database.entity.maxGraphPeriodDurations
import com.samco.trackandgraph.databinding.LineGraphInputViewBinding
import com.samco.trackandgraph.graphstatinput.ValidationException
import com.samco.trackandgraph.graphstatinput.customviews.LineGraphFeatureConfig
import com.samco.trackandgraph.graphstatinput.customviews.LineGraphFeatureConfigListItemView
import com.samco.trackandgraph.ui.dataVisColorGenerator
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.util.getDoubleFromText

internal class LineGraphConfigView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GraphStatConfigView(
    context,
    attrs,
    defStyleAttr
) {
    private val binding: LineGraphInputViewBinding = LineGraphInputViewBinding
        .inflate(LayoutInflater.from(context), this, true)

    private lateinit var configData: LineGraphWithFeatures

    private val lgfConfigIndices = mutableListOf<LineGraphFeatureConfigListItemView>()

    override fun initFromConfigData(configData: Any?) {
        this.configData = configData as LineGraphWithFeatures? ?: createEmptyConfig()
        initFromLineGraph()
    }

    private fun createEmptyConfig() = LineGraphWithFeatures(
        0,
        0,
        emptyList(),
        null,
        YRangeType.DYNAMIC,
        0.0,
        1.0,
        null
    )

    private fun initFromLineGraph() {
        binding.sampleDurationSpinner.setSelection(maxGraphPeriodDurations.indexOf(configData.duration))
        listenToTimeDuration(this, binding.sampleDurationSpinner) {
            configData = configData.copy(duration = it)
        }
        binding.endDateSpinner.setSelection(if (configData.endDate == null) 0 else 1)
        listenToEndDate(this, binding.endDateSpinner, { configData.endDate }) {
            configData = configData.copy(endDate = it)
            updateEndDateText(this, binding.customEndDateText, it)
        }

        createLineGraphFeatureViews()
        listenToAddLineGraphFeatureButton()
        initYRangeFromTo()
        listenToYRangeFixedFromTo()
        initYRangeSpinner()
        listenToYRangeTypeSpinner()
    }

    private fun initYRangeSpinner() {
        binding.yRangeStyleSpinner.setSelection(configData.yRangeType.ordinal)
    }

    private fun listenToYRangeTypeSpinner() {
        binding.yRangeStyleSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {}
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                    configData = configData.copy(yRangeType = YRangeType.values()[index])
                    emitConfigChange()
                    updateYRangeInputType()
                }
            }
    }

    private fun initYRangeFromTo() {
        binding.yRangeFrom.setText(doubleFormatter.format(configData.yFrom))
        binding.yRangeFromDuration.setTimeInSeconds(configData.yFrom.toLong())
        binding.yRangeTo.setText(doubleFormatter.format(configData.yTo))
        binding.yRangeToDuration.setTimeInSeconds(configData.yTo.toLong())
    }

    private fun listenToYRangeFixedFromTo() {
        binding.yRangeFrom.addTextChangedListener { editText ->
            configData = configData.copy(yFrom = getDoubleFromText(editText.toString()))
            emitConfigChange()
        }
        binding.yRangeFromDuration.setDurationChangedListener {
            configData = configData.copy(yFrom = it.toDouble())
            emitConfigChange()
        }

        binding.yRangeTo.addTextChangedListener { editText ->
            configData = configData.copy(yTo = getDoubleFromText(editText.toString()))
            emitConfigChange()
        }
        binding.yRangeToDuration.setDurationChangedListener {
            configData = configData.copy(yTo = it.toDouble())
            emitConfigChange()
        }

        val doneListener: () -> Unit = { onHideKeyboardListener?.invoke() }
        binding.yRangeToDuration.setDoneListener(doneListener)
        binding.yRangeFromDuration.setDoneListener(doneListener)
    }

    private fun listenToAddLineGraphFeatureButton() {
        binding.addFeatureButton.isClickable = true
        binding.addFeatureButton.setOnClickListener {
            val nextIndex = binding.lineGraphFeaturesLayout.childCount
            val nextColorIndex = (nextIndex * dataVisColorGenerator) % dataVisColorList.size
            val newLgf = LineGraphFeature(
                0, -1, -1, "", nextColorIndex,
                LineGraphAveraginModes.NO_AVERAGING, LineGraphPlottingModes.WHEN_TRACKED,
                LineGraphPointStyle.NONE, 0.toDouble(), 1.toDouble(), DurationPlottingMode.NONE
            )
            val newFeatures = configData.features.toMutableList()
            newFeatures.add(newLgf)
            configData = configData.copy(features = newFeatures)
            inflateLineGraphFeatureView(nextIndex, newLgf)
            emitConfigChange()
        }
    }

    private fun createLineGraphFeatureViews() {
        configData.features.forEachIndexed { i, lgf -> inflateLineGraphFeatureView(i, lgf) }
    }

    private fun inflateLineGraphFeatureView(index: Int, lineGraphFeature: LineGraphFeature) {
        val featureConfig = LineGraphFeatureConfig.fromLineGraphFeature(lineGraphFeature)
        val view = LineGraphFeatureConfigListItemView(context, featureDataProvider, featureConfig)
        lgfConfigIndices.add(index, view)
        view.setOnRemoveListener {
            binding.lineGraphFeaturesLayout.removeView(view)
            val viewIndex = lgfConfigIndices.indexOf(view)
            val features = configData.features.toMutableList()
            features.removeAt(viewIndex)
            configData = configData.copy(features = features)
            lgfConfigIndices.remove(view)
            emitConfigChange()
        }
        view.setOnUpdateListener {
            val newFeatures = configData.features.toMutableList()
            val viewIndex = lgfConfigIndices.indexOf(view)
            newFeatures.removeAt(viewIndex)
            newFeatures.add(viewIndex, it)
            configData = configData.copy(features = newFeatures)
            updateYRangeInputType()
            emitConfigChange()
        }
        val params = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        view.layoutParams = params
        binding.lineGraphFeaturesLayout.addView(view, index)
        //Using post delayed is a bit of a hack and doesn't guarantee it will work but it seems
        // to work most of the time and I'm not sure what the better solution is right now.
        binding.lineGraphFeaturesLayout.postDelayed({
            onScrollListener?.invoke(View.FOCUS_DOWN)
            view.requestFocus()
        }, 100)
    }

    private fun updateYRangeInputType() {
        when (configData.yRangeType) {
            YRangeType.DYNAMIC -> {
                binding.yRangeFromToLayout.visibility = View.GONE
                binding.yRangeFromToDurationLayout.visibility = View.GONE
            }
            YRangeType.FIXED -> {
                if (configData.features.any { it.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE }) {
                    binding.yRangeFromToDurationLayout.visibility = View.VISIBLE
                    binding.yRangeFromToLayout.visibility = View.GONE
                    binding.yRangeFromDuration.setTimeInSeconds(configData.yFrom.toLong())
                    binding.yRangeToDuration.setTimeInSeconds(configData.yTo.toLong())
                } else {
                    binding.yRangeFromToDurationLayout.visibility = View.GONE
                    binding.yRangeFromToLayout.visibility = View.VISIBLE
                    binding.yRangeFrom.setText(configData.yFrom.toString())
                    binding.yRangeTo.setText(configData.yTo.toString())
                }
            }
        }
    }

    override fun getConfigData(): Any = configData

    override fun validateConfig(): ValidationException? {
        if (configData.features.isEmpty())
            return ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        val featureIds = allFeatureData.map { data -> data.feature.id }.toSet()
        configData.features.forEach { f ->
            if (f.colorIndex !in dataVisColorList.indices)
                return ValidationException(R.string.graph_stat_validation_unrecognised_color)
            if (!featureIds.contains(f.featureId))
                return ValidationException(R.string.graph_stat_validation_invalid_line_graph_feature)
        }
        if (configData.features.any { it.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE }
            && !configData.features.all { it.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE }) {
            return ValidationException(R.string.graph_stat_validation_mixed_time_value_line_graph_features)
        }
        if (configData.yRangeType == YRangeType.FIXED && configData.yFrom >= configData.yTo) {
            return ValidationException(R.string.graph_stat_validation_bad_fixed_range)
        }
        return null
    }
}
