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
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatSpinner
import com.samco.trackandgraph.database.entity.TimeSinceLastStat
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
        0,
        0,
        allFeatures.getOrElse(0) { null }?.id ?: 0,
        0.0,
        1.0,
        emptyList()
    )

    override fun getConfigData(): Any = configData
    override fun getCurrentFeatureId(): Long = configData.featureId
    override fun getCurrentFromValue(): Double = configData.fromValue
    override fun getCurrentToValue(): Double = configData.toValue
    override fun getDiscreteValues(): List<Int> = emptyList() //TODO figure out ui stuff configData.discreteValues

    override fun getFeatureSpinner(): AppCompatSpinner = binding.valueStatFeatureSpinner
    override fun getDiscreteValueButtonsLayout(): LinearLayout =
        binding.valueStatDiscreteValueButtonsLayout

    override fun getDiscreteValueInputLayout(): View = binding.valueStatDiscreteValueInputLayout
    override fun getDurationRangeInput(): View = binding.valueStatDurationRangeInput
    override fun getContinuousValueInputLayout(): View = binding.valueStatContinuousValueInputLayout
    override fun getToInput(): EditText = binding.valueStatToInput
    override fun getFromInput(): EditText = binding.valueStatFromInput
    override fun getFromDurationInput(): DurationInputView = binding.valueStatDurationFromInput
    override fun getToDurationInput(): DurationInputView = binding.valueStatDurationToInput

    override fun onNewFeatureId(featureId: Long) {
        configData = configData.copy(featureId = featureId)
    }

    override fun onNewDiscreteValues(discreteValues: List<Int>) {
        //TODO figure this out
        //configData = configData.copy(discreteValues = discreteValues)
    }

    override fun onNewToValue(value: Double) {
        configData = configData.copy(toValue = value)
    }

    override fun onNewFromValue(value: Double) {
        configData = configData.copy(fromValue = value)
    }
}
