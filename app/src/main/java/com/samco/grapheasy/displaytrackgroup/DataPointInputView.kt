package com.samco.grapheasy.displaytrackgroup

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.samco.grapheasy.R
import com.samco.grapheasy.database.DiscreteValue
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.FeatureType
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber

class DataPointInputView(context: Context, val state: DataPointInputData)
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
        val view = inflate(context, R.layout.data_point_input_fragment, this)

        numberInput = view.findViewById(R.id.numberInput)
        titleText = view.findViewById(R.id.titleText)
        dateButton = view.findViewById(R.id.dateButton)
        timeButton = view.findViewById(R.id.timeButton)
        buttonsScroll = view.findViewById(R.id.buttonsScrollView)
        buttonsLayout = view.findViewById(R.id.buttonsLayout)

        titleText.text = state.feature.name

        if (state.dateModifiable) {
            initDateButton()
            initTimeButton()
            setSelectedDateTime(state.dateTime)
        } else {
            dateButton.visibility = View.INVISIBLE
            timeButton.visibility = View.INVISIBLE
        }

        if (state.feature.featureType == FeatureType.CONTINUOUS) {
            buttonsScroll.visibility = View.GONE
            numberInput.visibility = View.VISIBLE
            numberInput.requestFocus()
            numberInput.addTextChangedListener(this)
        } else {
            buttonsScroll.visibility = View.VISIBLE
            numberInput.visibility = View.GONE
        }
        if (state.feature.featureType == FeatureType.DISCRETE) {
            createButtons()
        }

        if (state.feature.featureType == FeatureType.CONTINUOUS) {
            numberInput.setText(state.value)
        } else if (state.label.isNotEmpty()) {
            buttonsLayout.children
                .map{ v -> v.findViewById<CheckBox>(R.id.checkbox) }
                .first { cb -> cb.text == state.label }
                .isChecked = true
        }
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    override fun afterTextChanged(p0: Editable?) {
        val double = numberInput.text.toString().toDoubleOrNull() ?: 0
        state.value = double.toString()
    }

    class DataPointInputData(
        var feature: Feature,
        var dateTime: OffsetDateTime,
        var value: String,
        var label: String,
        var dateModifiable: Boolean
    )
    class DataPointInputClickListener(val onClick: (DataPointInputData) -> Unit)
    fun setOnClickListener(clickListener: DataPointInputClickListener) { this.clickListener = clickListener }

    private fun createButtons() {
        discreteValueCheckBoxes = mutableMapOf()
        val inflater = LayoutInflater.from(context)
        for (discreteValue in state.feature.discreteValues.sortedBy { f -> f.index }) {
            val item = inflater.inflate(R.layout.discrete_value_input_button, buttonsLayout, false) as CheckBox
            Timber.d(discreteValue.label)
            item.text = discreteValue.label
            item.setOnClickListener { onDiscreteValueClicked(discreteValue) }
            discreteValueCheckBoxes[discreteValue] = item
            buttonsLayout.addView(item)
        }
    }

    private fun onDiscreteValueClicked(discreteValue: DiscreteValue) {
        if (clickListener != null) {
            state.value = discreteValue.index.toString()
            state.label = discreteValue.label
            discreteValueCheckBoxes.filter { kvp -> kvp.key != discreteValue }.forEach { kvp -> kvp.value.isChecked = false }
            clickListener!!.onClick(DataPointInputData(state.feature, state.dateTime,
                discreteValue.index.toString(), discreteValue.label, state.dateModifiable))
        }
    }

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
                }, state.dateTime.year, state.dateTime.monthValue, state.dateTime.dayOfMonth
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
                }, state.dateTime.hour, state.dateTime.minute, true
            )
            picker.show()
        }
    }
}
