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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatSpinner
import com.samco.trackandgraph.base.database.dto.AverageTimeBetweenStat
import com.samco.trackandgraph.base.database.entity.maxGraphPeriodDurations
import com.samco.trackandgraph.databinding.AverageTimeBetweenInputLayoutBinding
import com.samco.trackandgraph.ui.DurationInputView

internal class AverageTimeBetweenConfigView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ValueStatConfigView(
    context,
    attrs,
    defStyleAttr
) {
    private val binding: AverageTimeBetweenInputLayoutBinding = AverageTimeBetweenInputLayoutBinding
        .inflate(LayoutInflater.from(context), this, true)

    private lateinit var configData: AverageTimeBetweenStat

    override fun initFromConfigData(configData: Any?) {
        this.configData = configData as AverageTimeBetweenStat? ?: createEmptyConfig()
        initValueStatConfigView()
        initFromAverageTimeBetweenStat()
    }

    private fun initFromAverageTimeBetweenStat() {
        binding.sampleDurationSpinner.setSelection(maxGraphPeriodDurations.indexOf(configData.duration))
        listenToTimeDuration(this, binding.sampleDurationSpinner) {
            configData = configData.copy(duration = it)
        }
        binding.endDateSpinner.setSelection(if (configData.endDate == null) 0 else 1)
        listenToEndDate(this, binding.endDateSpinner, { configData.endDate }) {
            configData = configData.copy(endDate = it)
            updateEndDateText(this, binding.customEndDateText, it)
        }
    }

    override fun getConfigData(): Any = configData

    private fun createEmptyConfig() = AverageTimeBetweenStat(
        id = 0,
        graphStatId = 0,
        //Shouldn't actually be possible to get here without at least one feature
        featureId = allFeatureData.getOrElse(0) { null }?.feature?.id ?: 0,
        fromValue = 0.0,
        toValue = 1.0,
        duration = null,
        labels = emptyList(),
        endDate = null,
        filterByRange = false,
        filterByLabels = false
    )

    override fun getCurrentFeatureId(): Long = configData.featureId
    override fun getCurrentFromValue(): Double = configData.fromValue
    override fun getCurrentToValue(): Double = configData.toValue
    override fun getLabels(): Set<String> = configData.labels.toSet()
    override fun getFilterByLabel(): Boolean = configData.filterByLabels
    override fun getFilterByRange(): Boolean = configData.filterByRange

    override fun getFeatureSpinner(): AppCompatSpinner = binding.valueStatFeatureSpinner
    override fun getLabelButtonsLayout(): LinearLayout = binding.incLabelCard.valueStatLabelsInputLayout

    override fun getLabelCardLayout(): View = binding.incLabelCard.cardLabelInput
    override fun getLabelCardContentLayout(): View = binding.incLabelCard.labelButtonScrollView

    override fun getDurationRangeInput(): View = binding.incRangeCard.valueStatDurationRangeInput
    override fun getContinuousValueInputLayout(): View = binding.incRangeCard.valueStatContinuousValueInputLayout
    override fun getToInput(): EditText = binding.incRangeCard.valueStatToInput
    override fun getFromInput(): EditText = binding.incRangeCard.valueStatFromInput
    override fun getFromDurationInput(): DurationInputView = binding.incRangeCard.valueStatDurationFromInput
    override fun getToDurationInput(): DurationInputView = binding.incRangeCard.valueStatDurationToInput
    override fun getFilterByLabelCheckbox(): CheckBox = binding.incLabelCard.checkFilterByLabel
    override fun getFilterByValueCheckbox(): CheckBox = binding.incRangeCard.checkFilterByValue

    override fun onNewFeatureId(featureId: Long) {
        configData = configData.copy(featureId = featureId)
    }

    override fun onNewLabels(labels: Set<String>) {
        configData = configData.copy(labels = labels.toList())
    }

    override fun onNewToValue(value: Double) {
        configData = configData.copy(toValue = value)
    }

    override fun onNewFromValue(value: Double) {
        configData = configData.copy(fromValue = value)
    }

    override fun onFilterByLabelChanged(value: Boolean) {
        configData = configData.copy(filterByLabels = value)
    }

    override fun onFilterByValueChanged(value: Boolean) {
        configData = configData.copy(filterByRange = value)
    }
}
