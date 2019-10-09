package com.samco.grapheasy.displaytrackgroup

import android.app.*
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.samco.grapheasy.database.DataPoint
import com.samco.grapheasy.database.FeatureType
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import com.samco.grapheasy.R
import com.samco.grapheasy.database.Feature

class InputDataPointDialog : DialogFragment(), TextWatcher {
    private lateinit var listener: InputDataPointDialogListener
    private lateinit var viewModel:InputDataPointViewModel

    private lateinit var alertDialog: AlertDialog
    private lateinit var feature: Feature
    private lateinit var titleText: TextView
    private lateinit var numberInput: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var buttonsScroll: HorizontalScrollView
    private lateinit var buttonsLayout: LinearLayout

    private var selectedDateTime: OffsetDateTime = OffsetDateTime.now()
        set(value) {
            viewModel.selectedDateTime = value
            field = value
       }

    private val dateDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.systemDefault())

    private val timeDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    interface InputDataPointViewModel {
        var selectedDateTime: OffsetDateTime?
        var currentValue: String?
    }

    interface InputDataPointDialogListener {
        fun getDisplayDateTimeForInputDataPoint(): OffsetDateTime?
        fun getIdForInputDataPoint(): Long?
        fun getValueForInputDataPoint(): String?
        fun onDataPointInput(dataPoint: DataPoint)
        fun getFeature(): Feature
        fun getViewModel(): InputDataPointViewModel
    }

    private fun createView(activity: Activity): View {
        feature = listener.getFeature()
        viewModel = listener.getViewModel()

        val view = activity.layoutInflater.inflate(R.layout.data_point_input_dialog, null)
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

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        return activity?.let {
            listener = parentFragment as InputDataPointDialogListener
            val view = createView(it)
            val builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setNegativeButton(R.string.cancel) { _, _ -> clearViewModel() }
            if (feature.featureType == FeatureType.CONTINUOUS) {
                builder.setPositiveButton(R.string.add) { _, _ -> onPositiveClicked() }
            }
            alertDialog = builder.create()
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        clearViewModel()
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

    override fun afterTextChanged(editText: Editable?) {
        viewModel.currentValue = editText.toString()
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }


    private fun updateDateTimeButtonText() {
        dateButton.text = selectedDateTime.format(dateDisplayFormatter)
        timeButton.text = selectedDateTime.format(timeDisplayFormatter)
    }

    override fun onStart() {
        super.onStart()
        if (feature.featureType == FeatureType.DISCRETE) {
            createButtons()
        }
    }

    private fun createButtons() {
        val initValue = listener.getValueForInputDataPoint()
        for (discreteValue in feature.discreteValues) {
            val item = layoutInflater.inflate(R.layout.discrete_value_input_button,
                buttonsLayout, false) as CheckBox
            item.text = discreteValue
            if (initValue == discreteValue) item.isChecked = true
            item.setOnClickListener {
                onSubmitResult(discreteValue)
                val positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                positiveButton.performClick()
            }
            buttonsLayout.addView(item)
        }
    }

    private fun onPositiveClicked() {
        onSubmitResult(numberInput.text.toString())
    }

    private fun clearViewModel() {
        viewModel.selectedDateTime = null
        viewModel.currentValue = null
    }

    private fun onSubmitResult(value: String) {
        var id = listener.getIdForInputDataPoint()
        if (id == null) id = 0
        val dataPoint = DataPoint(id, feature.id, value, selectedDateTime)
        listener.onDataPointInput(dataPoint)
        clearViewModel()
    }
}
