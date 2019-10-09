package com.samco.grapheasy.displaytrackgroup

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.samco.grapheasy.database.DataPoint
import org.threeten.bp.OffsetDateTime
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import com.samco.grapheasy.R
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.FeatureType

class InputDataPointDialog : DialogFragment(), DataPointInputFragment.InputDataPointFragmentListener {
    private lateinit var listener: InputDataPointDialogListener
    private lateinit var viewModel: DataPointInputFragment.InputDataPointViewModel

    private lateinit var cancelButton: Button
    private lateinit var skipButton: Button
    private lateinit var addButton: Button

    interface InputDataPointDialogListener {
        fun getDisplayDateTimeForInputDataPoint(): OffsetDateTime?
        fun getIdForInputDataPoint(): Long?
        fun getValueForInputDataPoint(): String?
        fun onDataPointInput(dataPoint: DataPoint)
        //TODO this should get a list of features and where there is more than one we need to use a view pager to input values for all of them
        fun getFeature(): Feature
        fun getViewModel(): DataPointInputFragment.InputDataPointViewModel
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return activity?.let {
            listener = parentFragment as InputDataPointDialogListener
            viewModel = listener.getViewModel()
            val view = it.layoutInflater.inflate(R.layout.data_point_input_dialog, null)
            childFragmentManager.beginTransaction().add(R.id.dataPointInputFragment, DataPointInputFragment()).commit()

            cancelButton = view.findViewById(R.id.cancelButton)
            skipButton = view.findViewById(R.id.skipButton)
            addButton = view.findViewById(R.id.addButton)

            cancelButton.setOnClickListener { onCancelClicked() }
            skipButton.setOnClickListener { onSkipClicked() }
            addButton.setOnClickListener { onAddClicked() }

            if (listener.getFeature().featureType == FeatureType.DISCRETE) {
                addButton.visibility = View.GONE
            }
            //TODO when to show skip
            if (true) {
                skipButton.visibility = View.GONE
            }

            view
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun getFeature(): Feature = listener.getFeature()
    override fun getViewModel(): DataPointInputFragment.InputDataPointViewModel = listener.getViewModel()
    override fun getDisplayDateTimeForInputDataPoint(): OffsetDateTime? = listener.getDisplayDateTimeForInputDataPoint()
    override fun getValueForInputDataPoint(): String? = listener.getValueForInputDataPoint()

    override fun onValueSubmitted(value: String, timestamp: OffsetDateTime) {
        onSubmitResult(value, timestamp)
        dismiss()
    }

    private fun onCancelClicked() {
        clearViewModel()
        dismiss()
    }

    private fun onSkipClicked() {
        clearViewModel()
        dismiss()
    }

    private fun onAddClicked() {
        val value = if (viewModel.currentValue.isNullOrEmpty()) "0" else viewModel.currentValue!!
        onSubmitResult(value, viewModel.selectedDateTime!!)
        clearViewModel()
        dismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        clearViewModel()
    }

    private fun clearViewModel() {
        viewModel.selectedDateTime = null
        viewModel.currentValue = null
    }

    private fun onSubmitResult(value: String, timestamp: OffsetDateTime) {
        var id = listener.getIdForInputDataPoint()
        if (id == null) id = 0
        val dataPoint = DataPoint(id, listener.getFeature().id, value, timestamp)
        listener.onDataPointInput(dataPoint)
        clearViewModel()
    }
}
