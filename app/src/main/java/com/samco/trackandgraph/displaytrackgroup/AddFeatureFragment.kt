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
package com.samco.trackandgraph.displaytrackgroup

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.room.withTransaction
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.AddFeatureFragmentBinding
import com.samco.trackandgraph.databinding.FeatureDiscreteValueListItemBinding
import kotlinx.coroutines.*
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.forEachIndexed
import androidx.core.view.size
import androidx.core.widget.addTextChangedListener
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.YesCancelDialogFragment
import java.lang.Exception


class AddFeatureFragment : Fragment(),
    AdapterView.OnItemSelectedListener,
    YesCancelDialogFragment.YesCancelDialogListener {

    private val args: AddFeatureFragmentArgs by navArgs()
    private lateinit var binding: AddFeatureFragmentBinding
    private lateinit var viewModel: AddFeatureViewModel
    private var navController: NavController? = null
    private val featureTypeList = listOf(FeatureType.TIMESTAMP, FeatureType.CONTINUOUS, FeatureType.DISCRETE)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.add_feature_fragment, container, false)
        navController = container?.findNavController()

        viewModel = ViewModelProviders.of(this).get(AddFeatureViewModel::class.java)
        viewModel.init(activity!!, args.trackGroupId, args.existingFeatureNames.toList(), args.editFeatureId)

        binding.discreteValuesTextView.visibility = View.INVISIBLE
        binding.discreteValues.visibility = View.INVISIBLE
        binding.addDiscreteValueButton.visibility = View.INVISIBLE
        binding.featureNameText.filters = arrayOf(InputFilter.LengthFilter(MAX_FEATURE_NAME_LENGTH))
        listenToViewModelState()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val imm = activity!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.featureNameText, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun listenToViewModelState() {
        viewModel.state.observe(this, Observer { state ->
            when (state) {
                AddFeatureState.WAITING -> {
                    initViews()
                    binding.progressBar.visibility = View.GONE
                }
                AddFeatureState.ADDING -> {
                    binding.addBar.addButton.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                AddFeatureState.DONE -> navController?.popBackStack()
                AddFeatureState.ERROR -> {
                    val errorMsg = getString(R.string.feature_add_or_update_error_occurred)
                    Toast.makeText(activity!!, errorMsg, Toast.LENGTH_LONG).show()
                    navController?.popBackStack()
                }
            }
        })
    }

    private fun initViews() {
        binding.featureNameText.setText(viewModel.featureName.value)
        viewModel.discreteValues.forEach { v -> inflateDiscreteValue(v) }

        if (viewModel.discreteValues.size == MAX_DISCRETE_VALUES_PER_FEATURE)
            binding.addDiscreteValueButton.isEnabled = false

        if (viewModel.updateMode) initSpinnerInUpdateMode()

        binding.featureTypeSpinner.setSelection(featureTypeList.indexOf(viewModel.featureType.value!!))
        binding.featureTypeSpinner.onItemSelectedListener = this
        binding.featureNameText.setSelection(binding.featureNameText.text.length)
        binding.featureNameText.requestFocus()
        binding.featureNameText.addTextChangedListener {
            viewModel.featureName.value = binding.featureNameText.text.toString()
            validateForm()
        }
        binding.addDiscreteValueButton.setOnClickListener { onAddDiscreteValue() }
        binding.addBar.addButton.setOnClickListener { onAddOrUpdateClicked() }
    }

    private fun initSpinnerInUpdateMode() {
        val itemList = resources.getStringArray(R.array.feature_types)
            .map { s -> s as CharSequence }.toTypedArray()
        binding.featureTypeSpinner.adapter = object: ArrayAdapter<CharSequence>(context!!,
            android.R.layout.simple_spinner_dropdown_item, itemList) {
            override fun isEnabled(position: Int): Boolean {
                return viewModel.isFeatureTypeEnabled(featureTypeList[position])
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                val enabled = viewModel.isFeatureTypeEnabled(featureTypeList[position])
                val color =
                    if (enabled) ContextCompat.getColor(context, R.color.toolBarTextColor)
                    else ContextCompat.getColor(context, R.color.disabledTextColor)
                textView.setTextColor(color)
                return view
            }
        }
    }

    private fun onAddOrUpdateClicked() {
        if (!viewModel.updateMode) viewModel.onAddOrUpdate()
        else {
            val dialog = YesCancelDialogFragment()
            val args = Bundle()
            args.putString("title", getString(R.string.ru_sure_update_feature))
            dialog.arguments = args
            childFragmentManager.let { dialog.show(it, "ru_sure_update_feature_fragment") }
        }
    }

    private fun inflateDiscreteValue(label: AddFeatureViewModel.MutableLabel) {
        val item = FeatureDiscreteValueListItemBinding.inflate(layoutInflater, binding.discreteValues, false)
        val inputText = item.discreteValueNameText
        inputText.filters = arrayOf(InputFilter.LengthFilter(MAX_LABEL_LENGTH))
        inputText.addTextChangedListener {
            label.value = it.toString().trim()
            validateForm()
        }
        inputText.setText(label.value)
        item.deleteButton.setOnClickListener {
            onDeleteDiscreteValue(item)
            validateForm()
        }
        item.upButton.setOnClickListener {
            onUpClickedDiscreteValue(item)
            validateForm()
        }
        item.downButton.setOnClickListener {
            onDownClickedDiscreteValue(item)
            validateForm()
        }
        binding.discreteValues.addView(item.root)
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
            inputText.requestFocus()
        }
        reIndexDiscreteValueViews()
        validateForm()
    }

    private fun onAddDiscreteValue() {
        val label = AddFeatureViewModel.MutableLabel()
        viewModel.discreteValues.add(label)
        inflateDiscreteValue(label)
        if (viewModel.discreteValues.size == MAX_DISCRETE_VALUES_PER_FEATURE)
            binding.addDiscreteValueButton.isEnabled = false
    }

    private fun validateForm() {
        var errorSet = false
        val discreteValueStrings = viewModel.discreteValues
        if (viewModel.featureType.value!! == FeatureType.DISCRETE) {
            if (discreteValueStrings.isNullOrEmpty() || discreteValueStrings.size < 2) {
                setErrorText(getString(R.string.discrete_feature_needs_at_least_two_values))
                errorSet = true
            }
            for (s in discreteValueStrings) {
                if (s.value.isEmpty()) {
                    setErrorText(getString(R.string.discrete_value_must_have_name))
                    errorSet = true
                }
            }
        }
        binding.featureNameText.text.let {
            if (it.isEmpty()) {
                setErrorText(getString(R.string.feature_name_cannot_be_null))
                errorSet = true
            }
            else if (viewModel.existingFeatureNames.contains(it.toString())) {
                setErrorText(getString(R.string.feature_with_that_name_exists))
                errorSet = true
            }
        }
        if (errorSet) {
            binding.addBar.addButton.isEnabled = false
        } else {
            binding.addBar.addButton.isEnabled = true
            setErrorText("")
        }
    }

    private fun setErrorText(text: String) {
        binding.addBar.errorText.text = text
    }

    private fun onDownClickedDiscreteValue(item: FeatureDiscreteValueListItemBinding) {
        val currIndex = binding.discreteValues.indexOfChild(item.root)
        if (currIndex == binding.discreteValues.childCount-1) return
        viewModel.discreteValues.add(currIndex+1, viewModel.discreteValues.removeAt(currIndex))
        binding.discreteValues.removeView(item.root)
        binding.discreteValues.addView(item.root, currIndex+1)
        reIndexDiscreteValueViews()
    }

    private fun onUpClickedDiscreteValue(item: FeatureDiscreteValueListItemBinding) {
        val currIndex = binding.discreteValues.indexOfChild(item.root)
        if (currIndex == 0) return
        viewModel.discreteValues.add(currIndex-1, viewModel.discreteValues.removeAt(currIndex))
        binding.discreteValues.removeView(item.root)
        binding.discreteValues.addView(item.root, currIndex-1)
        reIndexDiscreteValueViews()
    }

    private fun onDeleteDiscreteValue(item: FeatureDiscreteValueListItemBinding) {
        if (viewModel.updateMode && !viewModel.haveWarnedAboutDeletingDiscreteValues) {
            AlertDialog.Builder(context)
                .setTitle(R.string.warning)
                .setMessage(R.string.on_feature_delete_discrete_value_warning)
                .setPositiveButton(R.string.ok, null)
                .show()
            viewModel.haveWarnedAboutDeletingDiscreteValues = true
        }
        val currIndex = binding.discreteValues.indexOfChild(item.root)
        viewModel.discreteValues.removeAt(currIndex)
        binding.discreteValues.removeView(item.root)
        binding.addDiscreteValueButton.isEnabled = true
        reIndexDiscreteValueViews()
    }

    private fun reIndexDiscreteValueViews() {
        binding.discreteValues.forEachIndexed{ i, v ->
            v.findViewById<TextView>(R.id.indexText).text = "$i : "
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
        onFeatureTypeSelected(featureTypeList[position])
    }
    override fun onNothingSelected(p0: AdapterView<*>?) { }

    private fun onFeatureTypeSelected(newType: FeatureType) {
        if (viewModel.updateMode && viewModel.existingFeature.featureType != newType) {
            val message = when (newType) {
                FeatureType.DISCRETE -> null
                FeatureType.CONTINUOUS -> getString(R.string.on_feature_type_change_numerical_warning)
                FeatureType.TIMESTAMP -> getString(R.string.on_feature_type_change_timestamp_warning)
            }

            message?.let {
                AlertDialog.Builder(context)
                    .setTitle(R.string.warning)
                    .setMessage(it)
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
        }
        viewModel.featureType.value = newType
        val vis = if (newType == FeatureType.DISCRETE) View.VISIBLE else View.GONE
        binding.discreteValuesTextView.visibility = vis
        binding.discreteValues.visibility = vis
        binding.addDiscreteValueButton.visibility = vis
        validateForm()
    }

    override fun onDialogYes(dialog: YesCancelDialogFragment) {
        when (dialog.title) {
            getString(R.string.ru_sure_update_feature) -> viewModel.onAddOrUpdate()
        }
    }
}

enum class AddFeatureState { INITIALIZING, WAITING, ADDING, DONE, ERROR }
class AddFeatureViewModel : ViewModel() {
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)
    private lateinit var database: TrackAndGraphDatabase
    private lateinit var dao: TrackAndGraphDatabaseDao

    class MutableLabel(var value: String = "", val updateIndex: Int = -1)

    var featureName = MutableLiveData<String>("")
    var featureType = MutableLiveData<FeatureType>(FeatureType.TIMESTAMP)
    var discreteValues = mutableListOf<MutableLabel>()
    lateinit var existingFeatureNames: List<String?>
        private set

    val state: LiveData<AddFeatureState> get() { return _state }
    private val _state = MutableLiveData<AddFeatureState>(AddFeatureState.INITIALIZING)

    var updateMode: Boolean = false
        private set
    var haveWarnedAboutDeletingDiscreteValues = false
    private var trackGroupId: Long = -1
    lateinit var existingFeature: Feature
        private set

    fun init(activity: Activity, trackGroupId: Long, existingFeatureNames: List<String>, existingFeatureId: Long) {
        if (_state.value != AddFeatureState.INITIALIZING) return

        val application = activity.application
        database = TrackAndGraphDatabase.getInstance(application)
        dao = database.trackAndGraphDatabaseDao
        this.trackGroupId = trackGroupId
        this.existingFeatureNames = existingFeatureNames
        ioScope.launch {
            if (existingFeatureId > -1) {
                updateMode = true
                existingFeature = dao.getFeatureById(existingFeatureId)
                val existingDiscreteValues = existingFeature.discreteValues
                    .sortedBy { f -> f.index }
                    .map { f -> MutableLabel(f.label, f.index) }
                withContext(Dispatchers.Main) {
                    featureName.value = existingFeature.name
                    featureType.value = existingFeature.featureType
                    this@AddFeatureViewModel.existingFeatureNames = existingFeatureNames.minus(featureName.value)
                    discreteValues.addAll(existingDiscreteValues)
                }
            }
            withContext(Dispatchers.Main) { _state.value = AddFeatureState.WAITING }
        }
    }

    fun isFeatureTypeEnabled(type: FeatureType): Boolean {
        if (!updateMode) return true
        // disc -> cont Y
        // cont -> time Y
        // disc -> time Y
        // time -> disc N
        // time -> cont N
        // cont -> disc N
        return when (type) {
            FeatureType.CONTINUOUS -> existingFeature.featureType == FeatureType.CONTINUOUS
                        || existingFeature.featureType == FeatureType.DISCRETE
            FeatureType.DISCRETE -> existingFeature.featureType == FeatureType.DISCRETE
            FeatureType.TIMESTAMP -> true
        }
    }

    fun onAddOrUpdate() {
        _state.value = AddFeatureState.ADDING
        ioScope.launch {
            try {
                database.withTransaction {
                    if (updateMode) updateFeature()
                    else addFeature()
                }
                withContext(Dispatchers.Main) { _state.value = AddFeatureState.DONE }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _state.value = AddFeatureState.ERROR }
            }
        }
    }

    private suspend fun updateFeature() {
        val valOfDiscVal = { v: MutableLabel ->
            if (discreteValues.size == 1) 1 else discreteValues.indexOf(v)
        }

        when (existingFeature.featureType) {
            FeatureType.DISCRETE -> when (featureType.value) {
                FeatureType.CONTINUOUS -> stripDataPointsToValue()
                FeatureType.TIMESTAMP -> stripDataPointsToTimestamp()
                FeatureType.DISCRETE -> updateDiscreteValueDataPoints(valOfDiscVal)
            }
            FeatureType.CONTINUOUS -> when (featureType.value) {
                FeatureType.TIMESTAMP -> stripDataPointsToTimestamp()
            }
        }

        val discVals = discreteValues
            .map { s -> DiscreteValue(valOfDiscVal(s), s.value) }
        val feature = Feature.create(existingFeature.id,
            featureName.value!!, trackGroupId,
            featureType.value!!, discVals,
            existingFeature.displayIndex)
        dao.updateFeature(feature)
    }

    private suspend fun stripDataPointsToTimestamp() {
        val oldDataPoints = dao.getDataPointsForFeatureSync(existingFeature.id)
        val newDataPoints = oldDataPoints.map {
            DataPoint(it.timestamp, it.featureId, 1.0, "")
        }
        dao.updateDataPoints(newDataPoints)
    }

    private suspend fun updateDiscreteValueDataPoints(valOfDiscVal: (MutableLabel) -> Int) {
        existingFeature.discreteValues.map { dv -> dv.index }
            .filter { i -> !discreteValues.map { dv -> dv.updateIndex }.contains(i) }
            .forEach { i -> removeExistingDataPointsForDiscreteValue(i) }

        if (discreteValues.any { dv -> dv.updateIndex > -1 }) {
            val valMap = discreteValues
                .filter { dv -> dv.updateIndex > -1 }
                .map { dv -> dv.updateIndex to Pair(valOfDiscVal(dv), dv.value) }
                .toMap()
            updateExistingDataPointsForDiscreteValue(valMap)
        }
    }

    private suspend fun stripDataPointsToValue() {
        val oldDataPoints = dao.getDataPointsForFeatureSync(existingFeature.id)
        val newDataPoints = oldDataPoints.map {
            DataPoint(it.timestamp, it.featureId, it.value, "")
        }
        dao.updateDataPoints(newDataPoints)
    }

    private suspend fun removeExistingDataPointsForDiscreteValue(index: Int) {
        dao.deleteAllDataPointsForDiscreteValue(existingFeature.id, index.toDouble())
    }

    private suspend fun updateExistingDataPointsForDiscreteValue(valMap: Map<Int, Pair<Int, String>>) {
        val oldValues = dao.getDataPointsForFeatureSync(existingFeature.id)
        val newValues = oldValues.map { v ->
            DataPoint(v.timestamp, v.featureId,
                valMap[v.value.toInt()]!!.first.toDouble(),
                valMap[v.value.toInt()]!!.second
            )
        }
        dao.updateDataPoints(newValues)
    }

    private suspend fun addFeature() {
        val discVals = discreteValues.mapIndexed { i, s ->
            val index = if (discreteValues.size == 1) 1 else i
            DiscreteValue(index, s.value)
        }
        val feature = Feature.create(0,
            featureName.value!!, trackGroupId,
            featureType.value!!, discVals, 0)
        dao.insertFeature(feature)
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }
}
