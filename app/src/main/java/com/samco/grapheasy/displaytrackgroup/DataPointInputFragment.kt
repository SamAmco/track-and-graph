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
import com.samco.grapheasy.database.DataPoint
import com.samco.grapheasy.database.DiscreteValue
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.FeatureType
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

const val FEATURE_ID_KEY = "FEATURE_ID"

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

    interface InputDataPointFragmentListener {
        fun getViewModel(): InputDataPointViewModel
        fun onValueSubmitted(dataPoint: DataPoint)
    }

    data class DataPointDisplayData(val featureId: Long) {
        lateinit var feature: Feature
        lateinit var dataPoint: DataPoint
    }

    interface InputDataPointViewModel {
        fun putDataPointDisplayData(displayData: DataPointDisplayData)
        fun getDataPointDisplayData(featureId: Long): DataPointDisplayData
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        listener = parentFragment as InputDataPointFragmentListener
        viewModel = listener.getViewModel()
        val view = inflater.inflate(R.layout.data_point_input_fragment, container, false)

        numberInput = view.findViewById(R.id.numberInput)
        titleText = view.findViewById(R.id.titleText)
        dateButton = view.findViewById(R.id.dateButton)
        timeButton = view.findViewById(R.id.timeButton)
        buttonsScroll = view.findViewById(R.id.buttonsScrollView)
        buttonsLayout = view.findViewById(R.id.buttonsLayout)

        initFeatureData()

        titleText.text = feature.name
        updateDateTimeButtonText()
        initDateButton()
        initTimeButton()

        if (feature.featureType == FeatureType.CONTINUOUS) {
            buttonsScroll.visibility = View.GONE
            numberInput.visibility = View.VISIBLE
            numberInput.requestFocus()
            numberInput.addTextChangedListener(this)
        } else {
            buttonsScroll.visibility = View.VISIBLE
            numberInput.visibility = View.GONE
        }

        return view
    }

    private fun setSelectedDateTime(dateTime: OffsetDateTime) {
        val displayData = DataPointDisplayData(feature.id)
        displayData.feature = feature
        displayData.dataPoint = displayData.dataPoint.copy(timestamp = dateTime)
        viewModel.putDataPointDisplayData(displayData)
    }

    private fun initFeatureData() {
        arguments?.let {
            val initDisplayData = viewModel.getDataPointDisplayData(it.getLong(FEATURE_ID_KEY))
            feature = initDisplayData.feature
            numberInput.setText(initDisplayData.dataPoint.value)
        }
    }

    override fun onStart() {
        super.onStart()
        if (feature.featureType == FeatureType.DISCRETE) {
            createButtons()
        }
    }

    override fun afterTextChanged(editText: Editable?) {
        val displayData = viewModel.getDataPointDisplayData(feature.id)
        displayData.dataPoint = displayData.dataPoint.copy(value = editText.toString())
        viewModel.putDataPointDisplayData(displayData)
    }
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

    private fun createButtons() {
        val initDataPoint = viewModel.getDataPointDisplayData(feature.id).dataPoint
        for (discreteValue in feature.discreteValues.sortedBy { f -> f.index }) {
            val item = layoutInflater.inflate(R.layout.discrete_value_input_button,
                buttonsLayout, false) as CheckBox
            item.text = discreteValue.label
            if (initDataPoint.label == discreteValue.label) item.isChecked = true
            item.setOnClickListener {
                var displayData = viewModel.getDataPointDisplayData(feature.id)
                displayData.dataPoint = displayData.dataPoint.copy(value = discreteValue.index.toString(), label = discreteValue.label)
                viewModel.putDataPointDisplayData(displayData)
                listener.onValueSubmitted(displayData.dataPoint)
            }
            buttonsLayout.addView(item)
        }
    }

    private fun updateDateTimeButtonText() {
        val displayData = viewModel.getDataPointDisplayData(feature.id)
        dateButton.text = displayData.dataPoint.timestamp.format(dateDisplayFormatter)
        timeButton.text = displayData.dataPoint.timestamp.format(timeDisplayFormatter)
    }

    private fun initDateButton() {
        context?.let {
            dateButton.setOnClickListener {
                val displayData = viewModel.getDataPointDisplayData(feature.id)
                val dateTime = displayData.dataPoint.timestamp
                val picker = DatePickerDialog(context!!,
                    DatePickerDialog.OnDateSetListener { _, year, month, day ->
                        setSelectedDateTime(dateTime
                            .withYear(year)
                            .withMonth(month+1)
                            .withDayOfMonth(day))
                        updateDateTimeButtonText()
                    },
                    dateTime.year, dateTime.monthValue, dateTime.dayOfMonth
                )
                picker.show()
            }
        }
    }

    private fun initTimeButton() {
        context?.let {
            timeButton.setOnClickListener {
                val displayData = viewModel.getDataPointDisplayData(feature.id)
                val dateTime = displayData.dataPoint.timestamp
                val picker = TimePickerDialog(context!!,
                    TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                        setSelectedDateTime(dateTime
                            .withHour(hour)
                            .withMinute(minute))
                        updateDateTimeButtonText()
                    },
                    dateTime.hour, dateTime.minute, true
                )
                picker.show()
            }
        }
    }
}