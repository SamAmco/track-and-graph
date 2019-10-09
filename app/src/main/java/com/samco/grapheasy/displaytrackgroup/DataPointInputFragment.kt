package com.samco.grapheasy.displaytrackgroup

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.samco.grapheasy.R
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.FeatureType
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

class DataPointInputFragment : Fragment(), TextWatcher {
    private lateinit var listener: InputDataPointFragmentListener
    private lateinit var viewModel: InputDataPointViewModel

    private lateinit var feature: Feature
    private lateinit var titleText: TextView
    private lateinit var numberInput: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var buttonsScroll: HorizontalScrollView
    private lateinit var buttonsLayout: LinearLayout

    private val dateDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.systemDefault())

    private val timeDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    private var selectedDateTime: OffsetDateTime = OffsetDateTime.now()
        set(value) {
            viewModel.selectedDateTime = value
            field = value
        }

    interface InputDataPointFragmentListener {
        fun getFeature(): Feature
        fun getViewModel(): InputDataPointViewModel
        fun getDisplayDateTimeForInputDataPoint(): OffsetDateTime?
        fun getValueForInputDataPoint(): String?
        fun onValueSubmitted(value: String, timestamp: OffsetDateTime)
    }

    interface InputDataPointViewModel {
        var selectedDateTime: OffsetDateTime?
        var currentValue: String?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        listener = parentFragment as InputDataPointFragmentListener
        feature = listener.getFeature()
        viewModel = listener.getViewModel()
        val view = inflater.inflate(R.layout.data_point_input_fragment, container, false)

        numberInput = view.findViewById(R.id.numberInput)
        titleText = view.findViewById(R.id.titleText)
        dateButton = view.findViewById(R.id.dateButton)
        timeButton = view.findViewById(R.id.timeButton)
        buttonsScroll = view.findViewById(R.id.buttonsScrollView)
        buttonsLayout = view.findViewById(R.id.buttonsLayout)

        titleText.text = feature.name
        selectedDateTime = getInitialSelectedDateTime()
        updateDateTimeButtonText()
        initDateButton()
        initTimeButton()

        if (feature.featureType == FeatureType.CONTINUOUS) {
            buttonsScroll.visibility = View.GONE
            numberInput.visibility = View.VISIBLE
            numberInput.setText(getInitialEditTextValue())
            numberInput.addTextChangedListener(this)
        } else {
            buttonsScroll.visibility = View.VISIBLE
            numberInput.visibility = View.GONE
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        if (feature.featureType == FeatureType.DISCRETE) {
            createButtons()
        }
    }

    override fun afterTextChanged(editText: Editable?) {
        if (editText.toString().isEmpty() || editText.toString().toLongOrNull() == null) {
            viewModel.currentValue = "0"
        } else viewModel.currentValue = editText.toString()
    }
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

    private fun createButtons() {
        val initValue = listener.getValueForInputDataPoint()
        for (discreteValue in feature.discreteValues) {
            val item = layoutInflater.inflate(R.layout.discrete_value_input_button,
                buttonsLayout, false) as CheckBox
            item.text = discreteValue
            if (initValue == discreteValue) item.isChecked = true
            item.setOnClickListener {
                listener.onValueSubmitted(discreteValue, selectedDateTime)
            }
            buttonsLayout.addView(item)
        }
    }

    private fun getInitialEditTextValue(): String {
        if (viewModel.currentValue != null) return viewModel.currentValue!!
        val parentValue = listener.getValueForInputDataPoint()
        if (parentValue != null) return parentValue
        return ""
    }

    private fun getInitialSelectedDateTime(): OffsetDateTime {
        if (viewModel.selectedDateTime != null) return viewModel.selectedDateTime!!
        val parentDateTime = listener.getDisplayDateTimeForInputDataPoint()
        if (parentDateTime != null) return parentDateTime
        return selectedDateTime
    }

    private fun updateDateTimeButtonText() {
        dateButton.text = selectedDateTime.format(dateDisplayFormatter)
        timeButton.text = selectedDateTime.format(timeDisplayFormatter)
    }

    private fun initDateButton() {
        context?.let {
            dateButton.setOnClickListener {
                val picker = DatePickerDialog(context!!,
                    DatePickerDialog.OnDateSetListener { _, year, month, day ->
                        selectedDateTime = selectedDateTime
                            .withYear(year)
                            .withMonth(month+1)
                            .withDayOfMonth(day)
                        updateDateTimeButtonText()
                    },
                    selectedDateTime.year, selectedDateTime.monthValue, selectedDateTime.dayOfMonth
                )
                picker.show()
            }
        }
    }

    private fun initTimeButton() {
        context?.let {
            timeButton.setOnClickListener {
                val picker = TimePickerDialog(context!!,
                    TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                        selectedDateTime = selectedDateTime
                            .withHour(hour)
                            .withMinute(minute)
                        updateDateTimeButtonText()
                    },
                    selectedDateTime.hour, selectedDateTime.minute, true
                )
                picker.show()
            }
        }
    }
}