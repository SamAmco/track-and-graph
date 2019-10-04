package com.samco.grapheasy.displaytrackgroup

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import com.samco.grapheasy.R
import com.samco.grapheasy.database.FeatureType

const val EXISTING_FEATURES_ARG_KEY = "existingFeatures"
const val EXISTING_FEATURES_DELIM = ","

class AddFeatureDialogFragment : DialogFragment(), AdapterView.OnItemSelectedListener {
    private lateinit var scrollView: ScrollView
    private lateinit var baseLinearLayout: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var nameEditText: EditText
    private lateinit var featureTypeSpinner: Spinner
    private lateinit var discreteValuesTextView: TextView
    private lateinit var discreteValuesLinearLayout: LinearLayout
    private lateinit var addDiscreteValueButton: ImageButton
    private lateinit var alertDialog: AlertDialog
    private lateinit var listener: AddFeatureDialogListener
    private lateinit var existingFeatures: List<String>

    private var selectedFeatureType = FeatureType.CONTINUOUS

    interface AddFeatureDialogListener {
        fun onAddFeature(name: String, featureType: FeatureType, discreteValues: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as AddFeatureDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() +
                    " must implement YesCancelDialogListener"))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onStart() {
        super.onStart()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        return activity?.let {
            val view = it.layoutInflater.inflate(R.layout.feature_input_dialog, null)
            scrollView = view.findViewById(R.id.scrollView)
            baseLinearLayout = view.findViewById(R.id.baseLinearLayout)
            errorText = view.findViewById(R.id.errorText)
            nameEditText = view.findViewById(R.id.featureNameText)
            featureTypeSpinner = view.findViewById(R.id.featureTypeSpinner)
            discreteValuesTextView = view.findViewById(R.id.discreteValuesTextView)
            discreteValuesLinearLayout = view.findViewById(R.id.discreteValues)
            addDiscreteValueButton = view.findViewById(R.id.addDiscreteValueButton)
            existingFeatures = arguments
                ?.getString(EXISTING_FEATURES_ARG_KEY)
                ?.split(EXISTING_FEATURES_DELIM)
                ?: listOf()

            nameEditText.setSelection(nameEditText.text.length)
            nameEditText.addTextChangedListener(formValidator())
            nameEditText.requestFocus()
            featureTypeSpinner.onItemSelectedListener = this
            addDiscreteValueButton.setOnClickListener { onAddDiscreteValue() }
            var builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(R.string.add) { _, _ -> onPositiveClicked() }
                .setNegativeButton(R.string.cancel) { _, _ -> {} }
            alertDialog = builder.create()
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun onAddDiscreteValue() {
        val item = layoutInflater.inflate(R.layout.feature_discrete_value_list_item, discreteValuesLinearLayout, false)
        item.findViewById<EditText>(R.id.featureNameText).addTextChangedListener(formValidator())
        item.findViewById<ImageButton>(R.id.deleteButton).setOnClickListener { onDeleteDiscreteValue(item) }
        item.findViewById<ImageButton>(R.id.upButton).setOnClickListener { onUpClickedDiscreteValue(item) }
        item.findViewById<ImageButton>(R.id.downButton).setOnClickListener { onDownClickedDiscreteValue(item) }
        discreteValuesLinearLayout.addView(item)
        reIndexDiscreteValueListItems()
        baseLinearLayout.clearFocus()
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        validateForm()
    }

    private fun formValidator() = object: TextWatcher {
        override fun afterTextChanged(p0: Editable?) { validateForm() }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    }

    private fun validateForm() {
        var errorSet = false
        var discreteValueStrings = getDiscreteValues()
        if (selectedFeatureType == FeatureType.DISCRETE && discreteValueStrings.isNullOrEmpty()) {
            setErrorText(getString(R.string.discrete_feature_needs_at_least_one_value))
            errorSet = true
        }
        for (s in discreteValueStrings) {
            if (s.isEmpty()) {
                setErrorText(getString(R.string.discrete_value_must_have_name))
                errorSet = true
            }
        }
        nameEditText.text.let {
            if (it.isEmpty()) {
                setErrorText(getString(R.string.feature_name_cannot_be_null))
                errorSet = true
            }
            else if (existingFeatures.contains(it.toString())) {
                setErrorText(getString(R.string.feature_with_that_name_exists))
                errorSet = true
            }
        }
        if (errorSet) {
            setPositiveButtonEnabled(false)
        } else {
            setPositiveButtonEnabled(true)
            clearErrorText()
        }
    }

    private fun setErrorText(text: String) {
        errorText.text = text
        errorText.visibility = View.VISIBLE
    }

    private fun clearErrorText() {
        setErrorText("")
        errorText.visibility = View.GONE
    }

    private fun setPositiveButtonEnabled(enabled: Boolean) {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = enabled
    }

    private fun onDownClickedDiscreteValue(item: View) {
        val currIndex = discreteValuesLinearLayout.indexOfChild(item)
        if (currIndex == discreteValuesLinearLayout.childCount-1) return
        discreteValuesLinearLayout.removeView(item)
        discreteValuesLinearLayout.addView(item, currIndex+1)
        reIndexDiscreteValueListItems()
    }

    private fun onUpClickedDiscreteValue(item: View) {
        val currIndex = discreteValuesLinearLayout.indexOfChild(item)
        if (currIndex == 0) return
        discreteValuesLinearLayout.removeView(item)
        discreteValuesLinearLayout.addView(item, currIndex-1)
        reIndexDiscreteValueListItems()
    }

    private fun onDeleteDiscreteValue(item: View) {
        discreteValuesLinearLayout.removeView(item)
        reIndexDiscreteValueListItems()
    }

    private fun reIndexDiscreteValueListItems() {
        discreteValuesLinearLayout.children.forEachIndexed { index, view ->
            view.findViewById<TextView>(R.id.indexText).text = "$index :"
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) = when(position) {
        0 -> onFeatureTypeSelected(false)
        1 -> onFeatureTypeSelected(true)
        else -> {}
    }
    override fun onNothingSelected(p0: AdapterView<*>?) { }

    private fun onFeatureTypeSelected(discrete: Boolean) {
        selectedFeatureType = if (discrete) FeatureType.DISCRETE else FeatureType.CONTINUOUS
        val vis = if (discrete) View.VISIBLE else View.GONE
        discreteValuesTextView.visibility = vis
        discreteValuesLinearLayout.visibility = vis
        addDiscreteValueButton.visibility = vis
    }

    private fun getDiscreteValues() = discreteValuesLinearLayout.children
        .map { v -> v.findViewById<EditText>(R.id.featureNameText).text.toString() }
        .toList()

    private fun onPositiveClicked() {
        listener.onAddFeature(nameEditText.text.toString(), selectedFeatureType, getDiscreteValues().joinToString())
    }

}