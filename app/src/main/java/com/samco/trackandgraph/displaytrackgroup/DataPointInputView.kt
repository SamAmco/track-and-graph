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
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DiscreteValue
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.ui.DurationInputView
import com.samco.trackandgraph.ui.doubleFormatter
import com.samco.trackandgraph.ui.formatDayMonthYear
import com.samco.trackandgraph.util.getDoubleFromText
import com.samco.trackandgraph.util.showKeyboard
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class DataPointInputView : FrameLayout {
    private val titleText: TextView
    private val noteInput: EditText
    private val addNoteButton: View
    private val numberInput: EditText
    private val dateButton: Button
    private val timeButton: Button
    private val buttonsScroll: HorizontalScrollView
    private val buttonsLayout: LinearLayout
    private val durationInput: DurationInputView

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val view = inflate(context, R.layout.data_point_input_view, this)
        noteInput = view.findViewById(R.id.noteInputText)
        addNoteButton = view.findViewById(R.id.addNoteButton)
        numberInput = view.findViewById(R.id.numberInput)
        titleText = view.findViewById(R.id.titleText)
        dateButton = view.findViewById(R.id.dateButton)
        timeButton = view.findViewById(R.id.timeButton)
        buttonsScroll = view.findViewById(R.id.buttonsScrollView)
        buttonsLayout = view.findViewById(R.id.buttonsLayout)
        durationInput = view.findViewById(R.id.durationInput)
    }

    private lateinit var state: DataPointInputData
    private lateinit var discreteValueCheckBoxes: MutableMap<DiscreteValue, CheckBox>

    private var clickListener: DataPointInputClickListener? = null

    private val timeDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    fun initialize(state: DataPointInputData) {
        this.state = state
        titleText.text = state.feature.name

        initDateButton()
        initTimeButton()
        initNoteTextInput()
        setSelectedDateTime(state.dateTime)

        when (state.feature.featureType) {
            DataType.CONTINUOUS -> initContinuous()
            DataType.DISCRETE -> initDiscrete()
            DataType.DURATION -> initDuration()
        }
    }

    private fun initDuration() {
        buttonsScroll.visibility = View.GONE
        numberInput.visibility = View.GONE
        durationInput.visibility = View.VISIBLE
        durationInput.setTimeInSeconds(state.value.toLong())
        durationInput.setDurationChangedListener { state.value = it.toDouble() }
        durationInput.setDoneListener {
            state.timeFixed = true
            clickListener?.onClick?.invoke(state.feature)
        }
    }

    private fun initNoteTextInput() {
        if (state.note.isNotEmpty()) {
            noteInput.setText(state.note)
            addNoteButton.visibility = View.GONE
            noteInput.visibility = View.VISIBLE
        }
        addNoteButton.setOnClickListener {
            addNoteButton.visibility = View.GONE
            noteInput.visibility = View.VISIBLE
            context.showKeyboard()
            noteInput.post { noteInput.requestFocus() }
        }
        noteInput.addTextChangedListener { state.note = it.toString() }
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        return if (state.feature.featureType == DataType.CONTINUOUS) numberInput.requestFocus()
        else super.requestFocus(direction, previouslyFocusedRect)
    }

    private fun initDiscrete() {
        buttonsScroll.visibility = View.VISIBLE
        numberInput.visibility = View.GONE
        durationInput.visibility = View.GONE
        createButtons()
        if (state.label.isNotEmpty()) {
            buttonsLayout.children
                .map { v -> v.findViewById<CheckBox>(R.id.checkbox) }
                .first { cb -> cb.text == state.label }
                .isChecked = true
        }
    }

    private fun initContinuous() {
        buttonsScroll.visibility = View.GONE
        numberInput.visibility = View.VISIBLE
        durationInput.visibility = View.GONE
        numberInput.addTextChangedListener {
            if (it.toString().isNotBlank()) {
                state.value = getDoubleFromText(it.toString())
            } else state.value = 1.0
        }
        numberInput.setOnEditorActionListener { _, i, _ ->
            return@setOnEditorActionListener if ((i and EditorInfo.IME_MASK_ACTION) != 0) {
                state.timeFixed = true
                clickListener?.onClick?.invoke(state.feature)
                true
            } else false
        }
        val text = if (state.value == 1.0) "" else doubleFormatter.format(state.value)
        numberInput.setText(text)
        numberInput.setSelection(numberInput.text.length)
    }

    class DataPointInputData(
        var feature: Feature,
        var dateTime: OffsetDateTime,
        var value: Double,
        var label: String,
        var note: String,
        var timeFixed: Boolean,
        val onDateTimeChanged: (OffsetDateTime) -> Unit,
        val oldDataPoint: DataPoint?
    )

    class DataPointInputClickListener(val onClick: (Feature) -> Unit)

    fun setOnClickListener(clickListener: DataPointInputClickListener) {
        this.clickListener = clickListener
    }

    private fun createButtons() {
        discreteValueCheckBoxes = mutableMapOf()
        val inflater = LayoutInflater.from(context)
        for (discreteValue in state.feature.discreteValues.sortedBy { f -> f.index }) {
            val item = inflater.inflate(
                R.layout.discrete_value_input_button,
                buttonsLayout,
                false
            ) as CheckBox
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
            discreteValueCheckBoxes.filter { kvp -> kvp.key != discreteValue }
                .forEach { kvp -> kvp.value.isChecked = false }
            clickListener!!.onClick(state.feature)
        }
    }

    fun updateDateTimes() = setSelectedDateTime(state.dateTime)

    private fun setSelectedDateTime(dateTime: OffsetDateTime) {
        state.dateTime = dateTime
        dateButton.text = formatDayMonthYear(context, dateTime)
        timeButton.text = dateTime.format(timeDisplayFormatter)
    }

    private fun initDateButton() {
        dateButton.setOnClickListener {
            val picker = DatePickerDialog(
                context!!,
                DatePickerDialog.OnDateSetListener { _, year, month, day ->
                    setSelectedDateTime(
                        ZonedDateTime.of(state.dateTime.toLocalDateTime(), ZoneId.systemDefault())
                            .withYear(year)
                            .withMonth(month + 1)
                            .withDayOfMonth(day)
                            .toOffsetDateTime()
                    )
                    state.timeFixed = true
                    state.onDateTimeChanged(state.dateTime)
                }, state.dateTime.year, state.dateTime.monthValue - 1, state.dateTime.dayOfMonth
            )
            picker.show()
        }
    }

    private fun initTimeButton() {
        timeButton.setOnClickListener {
            val picker = TimePickerDialog(
                context!!,
                TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                    setSelectedDateTime(
                        ZonedDateTime.of(state.dateTime.toLocalDateTime(), ZoneId.systemDefault())
                            .withHour(hour)
                            .withMinute(minute)
                            .toOffsetDateTime()
                    )
                    state.timeFixed = true
                    state.onDateTimeChanged(state.dateTime)
                }, state.dateTime.hour, state.dateTime.minute, true
            )
            picker.show()
        }
    }
}
