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
import com.samco.trackandgraph.base.database.dto.TimeSinceLastStat
import com.samco.trackandgraph.databinding.TimeSinceInputLayoutBinding
import com.samco.trackandgraph.ui.DurationInputView

internal class TimeSinceConfigView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ValueStatConfigView(
    context,
    attrs,
    defStyleAttr
) {
    private val binding: TimeSinceInputLayoutBinding = TimeSinceInputLayoutBinding
        .inflate(LayoutInflater.from(context), this, true)

    private lateinit var configData: TimeSinceLastStat

    override fun initFromConfigData(configData: Any?) {
        this.configData = configData as TimeSinceLastStat? ?: createEmptyConfig()
        initValueStatConfigView()
    }

    private fun createEmptyConfig() = TimeSinceLastStat(
        id = 0,
        graphStatId = 0,
        featureId = allFeatureData.getOrElse(0) { null }?.descriptor?.id ?: 0,
        fromValue = 0.0,
        toValue = 1.0,
        labels = emptyList(),
        filterByRange = false,
        filterByLabels = false
    )

    override fun getConfigData(): Any = configData
    override fun getCurrentFeatureId(): Long = configData.featureId
    override fun getCurrentFromValue(): Double = configData.fromValue
    override fun getCurrentToValue(): Double = configData.toValue
    override fun getLabels(): Set<String> = configData.labels.toSet()
    override fun getFilterByLabel(): Boolean = configData.filterByLabels
    override fun getFilterByRange(): Boolean = configData.filterByRange

    override fun getFeatureSpinner(): AppCompatSpinner = binding.valueStatFeatureSpinner
    override fun getLabelButtonsLayout(): LinearLayout = binding.incLabelCard.valueStatLabelsInputLayout
    override fun getLabelCardContentLayout(): View = binding.incLabelCard.labelButtonScrollView

    override fun getLabelCardLayout(): View = binding.incLabelCard.cardLabelInput
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
