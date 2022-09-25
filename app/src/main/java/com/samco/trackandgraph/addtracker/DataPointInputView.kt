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
package com.samco.trackandgraph.addtracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.widget.addTextChangedListener
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.helpers.doubleFormatter
import com.samco.trackandgraph.base.helpers.formatDayMonthYear
import com.samco.trackandgraph.databinding.DataPointInputViewBinding
import com.samco.trackandgraph.util.focusAndShowKeyboard
import com.samco.trackandgraph.util.getDoubleFromText
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class DataPointInputView : FrameLayout {

    private val binding: DataPointInputViewBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        binding = DataPointInputViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private lateinit var state: DataPointInputData

    private var clickListener: DataPointInputClickListener? = null

    private val timeDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    fun initialize(state: DataPointInputData) {
        this.state = state
        binding.titleText.text = state.tracker.name

        initDateButton()
        initTimeButton()
        initNoteTextInput()
        setSelectedDateTime(state.dateTime)

        when (state.tracker.dataType) {
            DataType.CONTINUOUS -> initContinuous()
            DataType.DURATION -> initDuration()
        }
    }

    private fun initDuration() {
        binding.buttonsScrollView.visibility = View.GONE
        binding.numberInput.visibility = View.GONE
        binding.durationInput.visibility = View.VISIBLE
        binding.durationInput.setTimeInSeconds(state.value.toLong())
        binding.durationInput.setDurationChangedListener { state.value = it.toDouble() }
        binding.durationInput.setDoneListener {
            state.timeFixed = true
            clickListener?.onClick?.invoke(state.tracker)
        }
    }

    private fun initNoteTextInput() {
        if (state.note.isNotEmpty()) {
            binding.noteInputText.setText(state.note)
            binding.addNoteButton.visibility = View.GONE
            binding.noteInputText.visibility = View.VISIBLE
        }
        binding.addNoteButton.setOnClickListener {
            binding.addNoteButton.visibility = View.GONE
            binding.noteInputText.visibility = View.VISIBLE
            binding.noteInputText.focusAndShowKeyboard()
        }
        binding.noteInputText.addTextChangedListener { state.note = it.toString() }
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        return when (state.tracker.dataType) {
            DataType.CONTINUOUS -> {
                binding.numberInput.focusAndShowKeyboard()
                binding.numberInput.requestFocus()
            }
            DataType.DURATION -> {
                binding.durationInput.requestFocus()
            }
            else -> super.requestFocus(direction, previouslyFocusedRect)
        }
    }

    private fun initContinuous() {
        binding.buttonsScrollView.visibility = View.GONE
        binding.numberInput.visibility = View.VISIBLE
        binding.durationInput.visibility = View.GONE
        binding.numberInput.addTextChangedListener {
            if (it.toString().isNotBlank()) {
                state.value = getDoubleFromText(it.toString())
            } else state.value = 1.0
        }
        binding.numberInput.setOnEditorActionListener { _, i, _ ->
            return@setOnEditorActionListener if ((i and EditorInfo.IME_MASK_ACTION) != 0) {
                state.timeFixed = true
                clickListener?.onClick?.invoke(state.tracker)
                true
            } else false
        }
        val text = if (state.value == 1.0) "" else doubleFormatter.format(state.value)
        binding.numberInput.setText(text)
        binding.numberInput.setSelection(binding.numberInput.text.length)
    }

    class DataPointInputData(
        var tracker: Tracker,
        var dateTime: OffsetDateTime,
        var value: Double,
        var label: String,
        var note: String,
        var timeFixed: Boolean,
        val onDateTimeChanged: (OffsetDateTime) -> Unit,
        val oldDataPoint: DataPoint?
    )

    class DataPointInputClickListener(val onClick: (Tracker) -> Unit)

    fun setOnClickListener(clickListener: DataPointInputClickListener) {
        this.clickListener = clickListener
    }

    fun updateDateTimes() = setSelectedDateTime(state.dateTime)

    private fun setSelectedDateTime(dateTime: OffsetDateTime) {
        state.dateTime = dateTime
        binding.dateButton.text = formatDayMonthYear(context, dateTime)
        binding.timeButton.text = dateTime.format(timeDisplayFormatter)
    }

    private fun initDateButton() {
        binding.dateButton.setOnClickListener {
            val picker = DatePickerDialog(
                context!!,
                { _, year, month, day ->
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
        binding.timeButton.setOnClickListener {
            val picker = TimePickerDialog(
                context!!,
                { _, hour, minute ->
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
