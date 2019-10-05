package com.samco.grapheasy.displaytrackgroup

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.samco.grapheasy.R
import com.samco.grapheasy.database.DataPoint
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.FeatureType
import kotlinx.android.synthetic.main.feature_input_dialog.*
import timber.log.Timber

class AddDataPointDialog : DialogFragment() {
    private lateinit var listener: AddDataPointDialogListener
    private lateinit var alertDialog: AlertDialog
    private lateinit var feature: Feature
    private lateinit var titleText: TextView
    private lateinit var numberInput: EditText
    private lateinit var buttonsScroll: ScrollView
    private lateinit var buttonsLayout: LinearLayout

    interface AddDataPointDialogListener {
        fun onAddDataPoint(dataPoint: DataPoint)
        fun getFeature(): Feature
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as AddDataPointDialogListener
            feature = listener.getFeature()
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() +
                    " must implement YesCancelDialogListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        Timber.d("onCreateDialog")
        return activity?.let {
            val view = it.layoutInflater.inflate(R.layout.data_point_input_dialog, null)
            numberInput = view.findViewById(R.id.numberInput)
            titleText = view.findViewById(R.id.titleText)
            buttonsScroll = view.findViewById(R.id.buttonsScrollView)
            buttonsLayout = view.findViewById(R.id.buttonsLayout)

            titleText.text = feature.name

            var builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setNegativeButton(R.string.cancel) { _, _ -> {} }

            if (feature.featureType == FeatureType.CONTINUOUS) {
                buttonsScroll.visibility = View.GONE
                numberInput.visibility = View.VISIBLE
                builder.setPositiveButton(R.string.add) { _, _ -> onPositiveClicked() }
            } else {
                buttonsScroll.visibility = View.VISIBLE
                numberInput.visibility = View.GONE
            }

            alertDialog = builder.create()
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onStart() {
        super.onStart()
        if (feature.featureType == FeatureType.DISCRETE) createButtons()
    }

    private fun createButtons() {
        Timber.d("Discrete values: ${feature.discreteValues.joinToString()}")
        for (discreteValue in feature.discreteValues) {
            val item = layoutInflater.inflate(R.layout.discrete_value_input_button, buttonsLayout, false) as Button
            item.text = discreteValue
            buttonsLayout.addView(item)
        }
    }

    //TODO implement onPositiveClicked
    private fun onPositiveClicked() {

    }
}