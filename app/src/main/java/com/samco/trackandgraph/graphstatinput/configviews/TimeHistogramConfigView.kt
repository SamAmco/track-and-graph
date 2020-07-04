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
import android.widget.AdapterView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.entity.FeatureType
import com.samco.trackandgraph.database.entity.TimeHistogram
import com.samco.trackandgraph.database.entity.TimeHistogramWindow
import com.samco.trackandgraph.database.entity.maxGraphPeriodDurations
import com.samco.trackandgraph.databinding.TimeHistogramInputViewBinding
import com.samco.trackandgraph.graphstatinput.ValidationException

class TimeHistogramConfigView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GraphStatConfigView(
    context,
    attrs,
    defStyleAttr
) {
    private val binding: TimeHistogramInputViewBinding = TimeHistogramInputViewBinding
        .inflate(LayoutInflater.from(context), this, true)

    private lateinit var configData: TimeHistogram

    override fun initFromConfigData(configData: Any?) {
        this.configData = configData as TimeHistogram? ?: createEmptyConfig()
        initFromTimeHistogram()
    }

    private fun createEmptyConfig() = TimeHistogram(
        0L,
        0L,
        allFeatures.getOrElse(0) { null }?.id ?: -1,
        null,
        TimeHistogramWindow.DAY,
        true,
        null
    )

    private fun initFromTimeHistogram() {
        binding.sampleDurationSpinner.setSelection(maxGraphPeriodDurations.indexOf(configData.duration))
        listenToTimeDuration(this, binding.sampleDurationSpinner) {
            configData = configData.copy(duration = it)
        }
        binding.endDateSpinner.setSelection(if (configData.endDate == null) 0 else 1)
        listenToEndDate(this, binding.endDateSpinner, { configData.endDate }) {
            configData = configData.copy(endDate = it)
            updateEndDateText(this, binding.customEndDateText, it)
        }
        listenToFeatureSpinner(this, binding.featureSpinner, configData.featureId) {
            configData = configData.copy(featureId = it.id)
            setSumByDiscreteValueCheckboxVisibility()
        }
        setSumByDiscreteValueCheckboxVisibility()
        listenToTimeWindowSize()
        listenToSumByDiscreteValueCheckbox()
    }

    private fun setSumByDiscreteValueCheckboxVisibility() {
        val currFeature = allFeatures.firstOrNull { it.id == configData.featureId }
        binding.sumDiscreteByValueCheckBox.visibility =
            if (currFeature?.featureType == FeatureType.DISCRETE)
                View.VISIBLE
            else View.GONE
    }

    private fun listenToTimeWindowSize() {
        val selection = TimeHistogramWindow.values().indexOfFirst { it == configData.window }
        if (selection >= 0) binding.selectWindowSpinner.setSelection(selection)
        binding.selectWindowSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {}
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                    configData = configData.copy(window = TimeHistogramWindow.values()[index])
                    emitConfigChange()
                }
            }
    }

    private fun listenToSumByDiscreteValueCheckbox() {
        binding.sumDiscreteByValueCheckBox.isChecked = !configData.sumDiscreteByIndex
        binding.sumDiscreteByValueCheckBox.setOnCheckedChangeListener { _, checked ->
            configData = configData.copy(sumDiscreteByIndex = !checked)
            emitConfigChange()
        }
    }

    override fun validateConfig(): ValidationException? {
        return if (allFeatures.isNullOrEmpty()
            || !allFeatures.map { it.id }.contains(configData.featureId)
        ) {
            ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        } else null
    }

    override fun getConfigData(): Any = configData
}