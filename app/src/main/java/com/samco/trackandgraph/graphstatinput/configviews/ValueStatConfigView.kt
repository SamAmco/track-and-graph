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
import androidx.core.widget.addTextChangedListener
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.doubleFormatter
import com.samco.trackandgraph.database.entity.Feature
import com.samco.trackandgraph.database.entity.FeatureType
import com.samco.trackandgraph.graphstatinput.ValidationException
import com.samco.trackandgraph.ui.DurationInputView
import com.samco.trackandgraph.util.getDoubleFromText

internal abstract class ValueStatConfigView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GraphStatConfigView (
    context,
    attrs,
    defStyleAttr
) {
    protected abstract fun getCurrentFeatureId(): Long
    protected abstract fun getCurrentFromValue(): Double
    protected abstract fun getCurrentToValue(): Double
    protected abstract fun getDiscreteValues(): List<Int>
    protected abstract fun getFeatureSpinner(): AppCompatSpinner
    protected abstract fun getDiscreteValueButtonsLayout(): LinearLayout
    protected abstract fun getDiscreteValueInputLayout(): View
    protected abstract fun getDurationRangeInput(): View
    protected abstract fun getContinuousValueInputLayout(): View
    protected abstract fun getToInput(): EditText
    protected abstract fun getFromInput(): EditText
    protected abstract fun getFromDurationInput(): DurationInputView
    protected abstract fun getToDurationInput(): DurationInputView

    protected abstract fun onNewFeatureId(featureId: Long)
    protected abstract fun onNewDiscreteValues(discreteValues: List<Int>)
    protected abstract fun onNewToValue(value: Double)
    protected abstract fun onNewFromValue(value: Double)

    private fun getCurrentFeature(): Feature? {
        val featId = getCurrentFeatureId()
        return allFeatures.firstOrNull { it.id == featId }
    }

    override fun validateConfig(): ValidationException? {
        val currFeature = getCurrentFeature()
            ?: return ValidationException(R.string.graph_stat_validation_no_line_graph_features)
        if (currFeature.featureType == FeatureType.DISCRETE && getDiscreteValues().isEmpty()) {
            return ValidationException(R.string.graph_stat_validation_invalid_value_stat_discrete_value)
        } else if (getCurrentFromValue() > getCurrentToValue()){
            return ValidationException(R.string.graph_stat_validation_invalid_value_stat_from_to)
        }
        return null
    }

    protected fun initValueStatConfigView() {
        listenToValueStat()
        listenToContinuousRangeInputView()
        listenToDurationRangeInputView()
        updateInputView(getCurrentFeature())
    }

    private fun listenToValueStat() {
        listenToFeatureSpinner(this, getFeatureSpinner(), getCurrentFeatureId()) {
            onNewFeatureId(it.id)
            updateInputView(it)
        }
    }

    private fun updateInputView(feature: Feature?) {
        when (feature?.featureType) {
            FeatureType.DISCRETE -> {
                sanitizeValueStatDiscreteValues()
                setUpDiscreteValueInputView()
                getDiscreteValueInputLayout().visibility = View.VISIBLE
                getDurationRangeInput().visibility = View.GONE
                getContinuousValueInputLayout().visibility = View.GONE
            }
            FeatureType.CONTINUOUS -> {
                setUpContinuousRangeInputView()
                getDiscreteValueInputLayout().visibility = View.GONE
                getDurationRangeInput().visibility = View.GONE
                getContinuousValueInputLayout().visibility = View.VISIBLE
            }
            FeatureType.DURATION -> {
                setUpDurationRangeInputView()
                getDiscreteValueInputLayout().visibility = View.GONE
                getContinuousValueInputLayout().visibility = View.GONE
                getDurationRangeInput().visibility = View.VISIBLE
            }
        }
    }

    private fun sanitizeValueStatDiscreteValues() {
        val allowedDiscreteValues = getCurrentFeature()?.discreteValues?.map { it.index }
        val newValues = if (allowedDiscreteValues != null) {
            getDiscreteValues()
                .filter { allowedDiscreteValues.contains(it) }
                .distinct()
        } else emptyList()
        onNewDiscreteValues(newValues)
        emitConfigChange()
    }

    private fun setUpDiscreteValueInputView() {
        val discreteValues = getCurrentFeature()?.discreteValues ?: emptyList()
        val buttonsLayout = getDiscreteValueButtonsLayout()
        buttonsLayout.removeAllViews()
        val inflater = LayoutInflater.from(context)
        for (discreteValue in discreteValues.sortedBy { it.index }) {
            val item = inflater.inflate(
                R.layout.discrete_value_input_button,
                buttonsLayout, false
            ) as CheckBox
            item.text = discreteValue.label
            item.isChecked = getDiscreteValues().contains(discreteValue.index)
            item.setOnCheckedChangeListener { _, checked ->
                val newDiscreteValues = getDiscreteValues().toMutableList()
                if (checked) newDiscreteValues.add(discreteValue.index)
                else newDiscreteValues.remove(discreteValue.index)
                onNewDiscreteValues(newDiscreteValues)
                emitConfigChange()
            }
            buttonsLayout.addView(item)
        }
    }

    private fun listenToContinuousRangeInputView() {
        getToInput().addTextChangedListener { editText ->
            val newValue = getDoubleFromText(editText.toString())
            onNewToValue(newValue)
            emitConfigChange()
        }
        getFromInput().addTextChangedListener { editText ->
            val newValue = getDoubleFromText(editText.toString())
            onNewFromValue(newValue)
            emitConfigChange()
        }
    }

    private fun setUpContinuousRangeInputView() {
        getToInput().setText(doubleFormatter.format(getCurrentToValue()))
        getFromInput().setText(doubleFormatter.format(getCurrentFromValue()))
    }

    private fun listenToDurationRangeInputView() {
        getToDurationInput().setDurationChangedListener {
            onNewToValue(it.toDouble())
            emitConfigChange()
        }
        getFromDurationInput().setDurationChangedListener {
            onNewFromValue(it.toDouble())
            emitConfigChange()
        }
        val doneListener: () -> Unit = { onHideKeyboardListener?.invoke() }
        getToDurationInput().setDoneListener(doneListener)
        getFromDurationInput().setDoneListener(doneListener)
    }

    private fun setUpDurationRangeInputView() {
        getToDurationInput().setTimeInSeconds(getCurrentToValue().toLong())
        getFromDurationInput().setTimeInSeconds(getCurrentFromValue().toLong())
    }
}
