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
    protected abstract fun getLabels(): Set<String>
    protected abstract fun getFilterByLabel(): Boolean
    protected abstract fun getFilterByRange(): Boolean

    protected abstract fun getFeatureSpinner(): AppCompatSpinner
    protected abstract fun getLabelButtonsLayout(): LinearLayout
    protected abstract fun getLabelCardLayout(): View
    protected abstract fun getLabelCardContentLayout(): View
    protected abstract fun getDurationRangeInput(): View
    protected abstract fun getContinuousValueInputLayout(): View
    protected abstract fun getToInput(): EditText
    protected abstract fun getFromInput(): EditText
    protected abstract fun getFromDurationInput(): DurationInputView
    protected abstract fun getToDurationInput(): DurationInputView
    protected abstract fun getFilterByLabelCheckbox(): CheckBox
    protected abstract fun getFilterByValueCheckbox(): CheckBox

    protected abstract fun onNewFeatureId(featureId: Long)
    protected abstract fun onNewLabels(labels: Set<String>)
    protected abstract fun onNewToValue(value: Double)
    protected abstract fun onNewFromValue(value: Double)
    protected abstract fun onFilterByLabelChanged(value: Boolean)
    protected abstract fun onFilterByValueChanged(value: Boolean)

    private fun getCurrentFeatureData(): FeatureDataProvider.FeatureData? {
        val featId = getCurrentFeatureId()
        return allFeatureData.firstOrNull { it.feature.id == featId }
    }

    override fun validateConfig(): ValidationException? {
        if (getCurrentFeatureData() == null)
            return ValidationException(R.string.graph_stat_validation_no_line_graph_features) //Shouldn't happen
        if (getCurrentFromValue() > getCurrentToValue())
            return ValidationException(R.string.graph_stat_validation_invalid_value_stat_from_to)
        return null
    }

    protected fun initValueStatConfigView() {
        listenToValueStat()
        listenToContinuousRangeInputView()
        listenToDurationRangeInputView()
        listenToFilterCheckboxes()
        updateInputView()
    }

    private fun listenToFilterCheckboxes() {
        getFilterByLabelCheckbox().setOnCheckedChangeListener { _, isChecked ->
            onFilterByLabelChanged(isChecked)
            setupLabelCardLayout(isChecked)
            emitConfigChange()
        }
        getFilterByValueCheckbox().setOnCheckedChangeListener { _, isChecked ->
            onFilterByValueChanged(isChecked)
            setupRangeCardLayout(isChecked)
            emitConfigChange()
        }
    }

    private fun setupLabelCardLayout(isEnabled: Boolean) {
        getLabelCardContentLayout().visibility = if (isEnabled) View.VISIBLE else View.GONE
    }

    private fun setupRangeCardLayout(isEnabled: Boolean) {
        val featureData = getCurrentFeatureData() ?: return
        val duration = featureData.dataProperties.isDuration
        if (duration) {
            getDurationRangeInput().visibility = if (isEnabled) View.VISIBLE else View.GONE
            getContinuousValueInputLayout().visibility = View.GONE
        } else {
            getDurationRangeInput().visibility = View.GONE
            getContinuousValueInputLayout().visibility = if (isEnabled) View.VISIBLE else View.GONE
        }
    }

    private fun listenToValueStat() {
        listenToFeatureSpinner(this, getFeatureSpinner(), getCurrentFeatureId()) {
            if (getCurrentFeatureId() != it.id) {
                onFilterByValueChanged(false)
                onFilterByLabelChanged(false)
                onNewFeatureId(it.id)
                updateInputView()
            }
        }
    }

    private fun updateInputView() {
        val featureData = getCurrentFeatureData() ?: return
        val labels = featureData.labels
        getLabelCardLayout().visibility = if (labels.isNotEmpty()) View.VISIBLE else View.GONE
        if (labels.isNotEmpty()) setUpLabelButtonsView(labels)

        getFilterByLabelCheckbox().isChecked = getFilterByLabel()
        getFilterByValueCheckbox().isChecked = getFilterByRange()

        val duration = featureData.dataProperties.isDuration
        getDurationRangeInput().visibility = if (duration) View.VISIBLE else View.GONE
        setUpDurationRangeInputView()
        setUpContinuousRangeInputView()
        setupLabelCardLayout(getFilterByLabel())
        setupRangeCardLayout(getFilterByRange())
        emitConfigChange()
    }

    private fun setUpLabelButtonsView(labels: Set<String>) {
        val buttonsLayout = getLabelButtonsLayout()
        buttonsLayout.removeAllViews()
        val inflater = LayoutInflater.from(context)
        for (label in labels) {
            val item = inflater.inflate(
                R.layout.discrete_value_input_button,
                buttonsLayout, false
            ) as CheckBox
            item.text = label
            item.isChecked = getLabels().contains(label)
            item.setOnCheckedChangeListener { _, checked ->
                val newLabels = getLabels().toMutableSet()
                if (checked) newLabels.add(label)
                else newLabels.remove(label)
                onNewLabels(newLabels)
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
