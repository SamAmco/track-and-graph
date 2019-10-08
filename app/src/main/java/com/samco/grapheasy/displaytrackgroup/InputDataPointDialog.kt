package com.samco.grapheasy.displaytrackgroup

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
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
import com.samco.grapheasy.R
import com.samco.grapheasy.database.Feature

class InputDataPointDialog : DialogFragment() {
    private lateinit var listener: InputDataPointDialogListener
    private lateinit var alertDialog: AlertDialog
    private lateinit var feature: Feature
    private lateinit var titleText: TextView
    private lateinit var numberInput: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var buttonsScroll: HorizontalScrollView
    private lateinit var buttonsLayout: LinearLayout

    private var selectedDateTime: OffsetDateTime = OffsetDateTime.now()

    private val dateDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.systemDefault())

    private val timeDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    interface InputDataPointDialogListener {
        fun getDisplayDateTimeForInputDataPoint(): OffsetDateTime?
        fun getIdForInputDataPoint(): Long?
        fun getValueForInputDataPoint(): String?
        fun onDataPointInput(dataPoint: DataPoint)
        fun getFeature(): Feature
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        return activity?.let {
            listener = parentFragment as InputDataPointDialogListener
            feature = listener.getFeature()

            val view = it.layoutInflater.inflate(R.layout.data_point_input_dialog, null)
            numberInput = view.findViewById(R.id.numberInput)
            titleText = view.findViewById(R.id.titleText)
            dateButton = view.findViewById(R.id.dateButton)
            timeButton = view.findViewById(R.id.timeButton)
            buttonsScroll = view.findViewById(R.id.buttonsScrollView)
            buttonsLayout = view.findViewById(R.id.buttonsLayout)

            titleText.text = feature.name
            val parentDateTime = listener.getDisplayDateTimeForInputDataPoint()
            if (parentDateTime != null) selectedDateTime = parentDateTime
            updateDateTimeButtonText()
            initDateButton()
            initTimeButton()

            val builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setNegativeButton(R.string.cancel) { _, _ -> {} }

            if (feature.featureType == FeatureType.CONTINUOUS) {
                buttonsScroll.visibility = View.GONE
                numberInput.visibility = View.VISIBLE
                val initValue = listener.getValueForInputDataPoint()
                if (initValue != null) numberInput.setText(initValue)
                builder.setPositiveButton(R.string.add) { _, _ -> onPositiveClicked() }
            } else {
                buttonsScroll.visibility = View.VISIBLE
                numberInput.visibility = View.GONE
            }

            alertDialog = builder.create()
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
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

    private fun onSubmitResult(value: String) {
        var id = listener.getIdForInputDataPoint()
        if (id == null) id = 0
        val dataPoint = DataPoint(id, feature.id, value, selectedDateTime)
        listener.onDataPointInput(dataPoint)
    }
}
