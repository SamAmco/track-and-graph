/* 
* This file is part of Track & Graph
* 
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.displaytrackgroup

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.util.getDoubleFromText
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

//TODO if a feature has a default value we should pre-populate with that value
class DataPointInputView(context: Context, private val state: DataPointInputData)
    : ConstraintLayout(context), TextWatcher {

    private val titleText: TextView
    private val numberInput: EditText
    private val dateButton: Button
    private val timeButton: Button
    private val buttonsScroll: HorizontalScrollView
    private val buttonsLayout: LinearLayout
    private lateinit var discreteValueCheckBoxes: MutableMap<DiscreteValue, CheckBox>

    private var clickListener: DataPointInputClickListener? = null

    private val dateDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.systemDefault())

    private val timeDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    init {
        val view = inflate(context, R.layout.data_point_input_view, this)

        numberInput = view.findViewById(R.id.numberInput)
        titleText = view.findViewById(R.id.titleText)
        dateButton = view.findViewById(R.id.dateButton)
        timeButton = view.findViewById(R.id.timeButton)
        buttonsScroll = view.findViewById(R.id.buttonsScrollView)
        buttonsLayout = view.findViewById(R.id.buttonsLayout)

        titleText.text = state.feature.name

        initDateButton()
        initTimeButton()
        setSelectedDateTime(state.dateTime)

        when (state.feature.featureType) {
            FeatureType.CONTINUOUS -> initContinuous()
            FeatureType.DISCRETE -> initDiscrete()
        }
    }

    private fun initDiscrete() {
        buttonsScroll.visibility = View.VISIBLE
        numberInput.visibility = View.GONE
        createButtons()
        if (state.label.isNotEmpty()) {
            buttonsLayout.children
                .map{ v -> v.findViewById<CheckBox>(R.id.checkbox) }
                .first { cb -> cb.text == state.label }
                .isChecked = true
        }
    }

    private fun initContinuous() {
        buttonsScroll.visibility = View.GONE
        numberInput.visibility = View.VISIBLE
        numberInput.requestFocus()
        numberInput.addTextChangedListener(this)
        numberInput.setOnEditorActionListener { _, i, _ ->
            return@setOnEditorActionListener if ((i and EditorInfo.IME_MASK_ACTION) != 0) {
                state.timeFixed = true
                clickListener?.onClick?.invoke(state.feature)
                true
            } else false
        }
        val text = if (state.value == 0.toDouble()) "" else doubleFormatter.format(state.value)
        numberInput.setText(text)
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    override fun afterTextChanged(p0: Editable?) {
        state.value = getDoubleFromText(numberInput.text.toString())
    }

    class DataPointInputData(
        var feature: Feature,
        var dateTime: OffsetDateTime,
        var value: Double,
        var label: String,
        var timeFixed: Boolean,
        val onDateTimeChanged: (OffsetDateTime) -> Unit,
        val oldDataPoint: DataPoint?
    )
    class DataPointInputClickListener(val onClick: (Feature) -> Unit)
    fun setOnClickListener(clickListener: DataPointInputClickListener) { this.clickListener = clickListener }

    private fun createButtons() {
        discreteValueCheckBoxes = mutableMapOf()
        val inflater = LayoutInflater.from(context)
        for (discreteValue in state.feature.discreteValues.sortedBy { f -> f.index }) {
            val item = inflater.inflate(R.layout.discrete_value_input_button, buttonsLayout, false) as CheckBox
            item.text = discreteValue.label
            item.setOnClickListener { onDiscreteValueClicked(discreteValue) }
            discreteValueCheckBoxes[discreteValue] = item
            buttonsLayout.addView(item)
        }
    }

    private fun onDiscreteValueClicked(discreteValue: DiscreteValue) {
        if (clickListener != null) {
            state.value = discreteValue.index.toDouble()
            state.label = discreteValue.label
            state.timeFixed = true
            discreteValueCheckBoxes.filter { kvp -> kvp.key != discreteValue }.forEach { kvp -> kvp.value.isChecked = false }
            clickListener!!.onClick(state.feature)
        }
    }

    fun updateDateTimes() = setSelectedDateTime(state.dateTime)

    private fun setSelectedDateTime(dateTime: OffsetDateTime) {
        state.dateTime = dateTime
        dateButton.text = dateTime.format(dateDisplayFormatter)
        timeButton.text = dateTime.format(timeDisplayFormatter)
    }

    private fun initDateButton() {
        dateButton.setOnClickListener {
            val picker = DatePickerDialog(context!!,
                DatePickerDialog.OnDateSetListener { _, year, month, day ->
                    setSelectedDateTime(state.dateTime
                        .withYear(year)
                        .withMonth(month+1)
                        .withDayOfMonth(day))
                    state.timeFixed = true
                    state.onDateTimeChanged(state.dateTime)
                }, state.dateTime.year, state.dateTime.monthValue-1, state.dateTime.dayOfMonth
            )
            picker.show()
        }
    }

    private fun initTimeButton() {
        timeButton.setOnClickListener {
            val picker = TimePickerDialog(context!!,
                TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                    setSelectedDateTime(state.dateTime
                        .withHour(hour)
                        .withMinute(minute))
                    state.timeFixed = true
                    state.onDateTimeChanged(state.dateTime)
                }, state.dateTime.hour, state.dateTime.minute, true
            )
            picker.show()
        }
    }
}
