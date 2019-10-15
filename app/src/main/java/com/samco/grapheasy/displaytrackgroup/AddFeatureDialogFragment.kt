package com.samco.grapheasy.displaytrackgroup

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.room.withTransaction
import com.samco.grapheasy.R
import com.samco.grapheasy.database.*
import kotlinx.coroutines.*

const val EXISTING_FEATURES_ARG_KEY = "existingFeatures"
const val EXISTING_FEATURES_DELIM = ","

class AddFeatureDialogFragment : DialogFragment(), AdapterView.OnItemSelectedListener {
    private val trackGroupId: Long by lazy { arguments!!.getLong(TRACK_GROUP_ID_KEY) }
    private lateinit var scrollView: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var baseLinearLayout: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var nameEditText: EditText
    private lateinit var featureTypeSpinner: Spinner
    private lateinit var discreteValuesTextView: TextView
    private lateinit var discreteValuesLinearLayout: LinearLayout
    private lateinit var addDiscreteValueButton: ImageButton
    private lateinit var alertDialog: AlertDialog
    private lateinit var viewModel: AddFeatureDialogViewModel
    private lateinit var existingFeatures: List<String>

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        return activity?.let {
            viewModel = ViewModelProviders.of(this).get(AddFeatureDialogViewModel::class.java)
            val view = it.layoutInflater.inflate(R.layout.feature_input_dialog, null)
            scrollView = view.findViewById(R.id.scrollView)
            progressBar = view.findViewById(R.id.progressBar)
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

            initFromViewModel()

            nameEditText.setSelection(nameEditText.text.length)
            nameEditText.filters = arrayOf(InputFilter.LengthFilter(MAX_FEATURE_NAME_LENGTH))
            nameEditText.addTextChangedListener(nameEditTextFormValidator())
            nameEditText.requestFocus()
            featureTypeSpinner.onItemSelectedListener = this
            addDiscreteValueButton.setOnClickListener { onAddDiscreteValue() }
            var builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(R.string.add) { _, _ -> null }
                .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            alertDialog = builder.create()
            alertDialog.setCanceledOnTouchOutside(true)
            alertDialog.setOnShowListener {onShowListener() }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun onShowListener() {
        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.isEnabled = false
        positiveButton.setOnClickListener { onPositiveClicked() }
        listenToAddState()
        inflateDiscreteValuesFromViewModel()
        alertDialog.setOnCancelListener { null }
    }

    private fun listenToAddState() {
        viewModel.addFeatureState.observe(this, Observer { state ->
            when (state) {
                AddFeatureState.WAITING -> progressBar.visibility = View.INVISIBLE
                AddFeatureState.ADDING -> {
                    setPositiveButtonEnabled(false)
                    progressBar.visibility = View.VISIBLE
                }
                AddFeatureState.DONE -> dismiss()
            }
        })
    }

    private fun initFromViewModel() {
        if (viewModel.featureName != null) nameEditText.setText(viewModel.featureName!!)
        else viewModel.featureName = ""
        if (viewModel.featureType != null) featureTypeSpinner.setSelection(spinnerIndexOf(viewModel.featureType!!))
        else viewModel.featureType = FeatureType.CONTINUOUS
        if (viewModel.discreteValues == null) viewModel.discreteValues = mutableListOf()
    }

    private fun inflateDiscreteValuesFromViewModel() {
        viewModel.discreteValues!!.forEachIndexed { i, v -> inflateDiscreteValue(i, v.label) }
    }

    private fun spinnerIndexOf(featureType: FeatureType): Int = when(featureType) {
        FeatureType.CONTINUOUS -> 0
        else -> 1
    }

    private fun inflateDiscreteValue(viewModelIndex: Int, initialText: String) {
        val item = layoutInflater.inflate(R.layout.feature_discrete_value_list_item, discreteValuesLinearLayout, false)
        val inputText = item.findViewById<EditText>(R.id.discreteValueNameText)
        inputText.setText(initialText)
        inputText.filters = arrayOf(InputFilter.LengthFilter(MAX_LABEL_LENGTH))
        inputText.addTextChangedListener(discreteValueTextFormValidator(viewModelIndex))
        item.findViewById<ImageButton>(R.id.deleteButton).setOnClickListener { onDeleteDiscreteValue(item) }
        item.findViewById<ImageButton>(R.id.upButton).setOnClickListener { onUpClickedDiscreteValue(item) }
        item.findViewById<ImageButton>(R.id.downButton).setOnClickListener { onDownClickedDiscreteValue(item) }
        discreteValuesLinearLayout.addView(item)
        reIndexDiscreteValueListItems()
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
            inputText.requestFocus()
        }
        validateForm()
    }

    private fun onAddDiscreteValue() {
        inflateDiscreteValue(viewModel.discreteValues!!.size, "")
        if (viewModel.discreteValues!!.size == MAX_DISCRETE_VALUES_PER_FEATURE)
            addDiscreteValueButton.isEnabled = false
    }

    private fun discreteValueTextFormValidator(index: Int) = object: TextWatcher {
        override fun afterTextChanged(editText: Editable?) {
            viewModel.discreteValues!![index] = viewModel.discreteValues!![index]
                .copy(label = editText.toString().trim())
            validateForm()
        }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    }

    private fun nameEditTextFormValidator() = object: TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
            viewModel.featureName = nameEditText.text.toString()
            validateForm()
        }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
    }

    private fun validateForm() {
        var errorSet = false
        var discreteValueStrings = viewModel.discreteValues!!
        if (viewModel.featureType!! == FeatureType.DISCRETE && discreteValueStrings.isNullOrEmpty()) {
            setErrorText(getString(R.string.discrete_feature_needs_at_least_one_value))
            errorSet = true
        }
        for (s in discreteValueStrings) {
            if (s.label.isEmpty()) {
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
        addDiscreteValueButton.isEnabled = true
    }

    private fun reIndexDiscreteValueListItems() {
        viewModel.discreteValues = mutableListOf()
        discreteValuesLinearLayout.children.forEachIndexed { index, view ->
            view.findViewById<TextView>(R.id.indexText).text = "$index :"
            val currText = view.findViewById<EditText>(R.id.discreteValueNameText).text.toString()
            viewModel.discreteValues!!.add(DiscreteValue(index, currText.trim()))
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) = when(position) {
        0 -> onFeatureTypeSelected(false)
        1 -> onFeatureTypeSelected(true)
        else -> {}
    }
    override fun onNothingSelected(p0: AdapterView<*>?) { }

    private fun onFeatureTypeSelected(discrete: Boolean) {
        viewModel.featureType = if (discrete) FeatureType.DISCRETE else FeatureType.CONTINUOUS
        val vis = if (discrete) View.VISIBLE else View.GONE
        discreteValuesTextView.visibility = vis
        discreteValuesLinearLayout.visibility = vis
        addDiscreteValueButton.visibility = vis
        validateForm()
    }

    private fun onPositiveClicked() {
        viewModel.onAddFeature(activity!!, nameEditText.text.toString(), trackGroupId)
    }
}

enum class AddFeatureState { WAITING, ADDING, DONE }
class AddFeatureDialogViewModel : ViewModel() {
    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)

    var featureName: String? = null
    var featureType: FeatureType? = null
    var discreteValues: MutableList<DiscreteValue>? = null
    val addFeatureState: LiveData<AddFeatureState> get() { return _isAdding }
    private val _isAdding by lazy {
        val adding = MutableLiveData<AddFeatureState>()
        adding.value = AddFeatureState.WAITING
        return@lazy adding
    }

    fun onAddFeature(activity: Activity, name: String, trackGroupId: Long) {
        val application = activity.application
        val database = GraphEasyDatabase.getInstance(application)
        val dao = database.graphEasyDatabaseDao
        uiScope.launch {
            _isAdding.value = AddFeatureState.ADDING
            withContext(Dispatchers.IO) {
                val feature = Feature(0, name, trackGroupId, featureType!!, discreteValues!!)
                database.withTransaction { dao.insertFeature(feature) }
            }
            _isAdding.value = AddFeatureState.DONE
        }
    }

    override fun onCleared() {
        super.onCleared()
        uiScope.cancel()
    }
}
