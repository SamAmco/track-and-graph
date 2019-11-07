package com.samco.trackandgraph.displaytrackgroup

import android.app.Activity
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
import androidx.core.view.forEachIndexed
import androidx.core.widget.addTextChangedListener
import com.samco.trackandgraph.R


class AddFeatureFragment : Fragment(), AdapterView.OnItemSelectedListener {
    private val args: AddFeatureFragmentArgs by navArgs()
    private lateinit var binding: AddFeatureFragmentBinding
    private lateinit var viewModel: AddFeatureViewModel
    private var navController: NavController? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProviders.of(this).get(AddFeatureViewModel::class.java)
        binding = DataBindingUtil.inflate(inflater, R.layout.add_feature_fragment, container, false)
        navController = container?.findNavController()

        binding.discreteValuesTextView.visibility = View.INVISIBLE
        binding.discreteValues.visibility = View.INVISIBLE
        binding.addDiscreteValueButton.visibility = View.INVISIBLE
        binding.discreteValuesTipText.visibility = View.INVISIBLE
        binding.featureNameText.setSelection(binding.featureNameText.text.length)
        binding.featureNameText.filters = arrayOf(InputFilter.LengthFilter(MAX_FEATURE_NAME_LENGTH))
        binding.featureNameText.addTextChangedListener {
            viewModel.featureName.value = binding.featureNameText.text.toString()
            validateForm()
        }
        binding.featureNameText.requestFocus()
        binding.featureTypeSpinner.onItemSelectedListener = this
        binding.addDiscreteValueButton.setOnClickListener { onAddDiscreteValue() }
        binding.addBar.addButton.setOnClickListener { onAddClicked() }
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
        viewModel.addFeatureState.observe(this, Observer { state ->
            when (state) {
                AddFeatureState.WAITING -> binding.progressBar.visibility = View.INVISIBLE
                AddFeatureState.ADDING -> {
                    binding.addBar.addButton.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                AddFeatureState.DONE -> navController?.popBackStack()
            }
        })
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        binding.featureNameText.setText(viewModel.featureName.value)
        binding.featureTypeSpinner.setSelection(spinnerIndexOf(viewModel.featureType.value!!))
        viewModel.discreteValues.forEachIndexed { i, v -> inflateDiscreteValue(i, v) }
        if (viewModel.discreteValues.size == MAX_DISCRETE_VALUES_PER_FEATURE)
            binding.addDiscreteValueButton.isEnabled = false
    }

    private fun spinnerIndexOf(featureType: FeatureType): Int = when(featureType) {
        FeatureType.CONTINUOUS -> 0
        else -> 1
    }

    private fun inflateDiscreteValue(viewModelIndex: Int, initialText: String) {
        val item = FeatureDiscreteValueListItemBinding.inflate(layoutInflater, binding.discreteValues, false)
        val inputText = item.discreteValueNameText
        inputText.filters = arrayOf(InputFilter.LengthFilter(MAX_LABEL_LENGTH))
        inputText.addTextChangedListener {
            viewModel.discreteValues[viewModelIndex] = it.toString().trim()
            validateForm()
        }
        inputText.setText(initialText)
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
        viewModel.discreteValues.add("")
        inflateDiscreteValue(viewModel.discreteValues.size-1, "")
        if (viewModel.discreteValues.size == MAX_DISCRETE_VALUES_PER_FEATURE)
            binding.addDiscreteValueButton.isEnabled = false
    }

    private fun validateForm() {
        var errorSet = false
        var discreteValueStrings = viewModel.discreteValues
        if (viewModel.featureType.value!! == FeatureType.DISCRETE) {
            if (discreteValueStrings.isNullOrEmpty()) {
                setErrorText(getString(R.string.discrete_feature_needs_at_least_one_value))
                errorSet = true
            }
            for (s in discreteValueStrings) {
                if (s.isEmpty()) {
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
            else if (args.existingFeatureNames.contains(it.toString())) {
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
        val currIndex = binding.discreteValues.indexOfChild(item.root)
        viewModel.discreteValues.removeAt(currIndex)
        binding.discreteValues.removeView(item.root)
        binding.addDiscreteValueButton.isEnabled = true
    }

    private fun reIndexDiscreteValueViews() {
        binding.discreteValues.forEachIndexed{ i, v ->
            v.findViewById<TextView>(R.id.indexText).text = "$i : "
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) = when(position) {
        0 -> onFeatureTypeSelected(false)
        1 -> onFeatureTypeSelected(true)
        else -> {}
    }
    override fun onNothingSelected(p0: AdapterView<*>?) { }

    private fun onFeatureTypeSelected(discrete: Boolean) {
        viewModel.featureType.value = if (discrete) FeatureType.DISCRETE else FeatureType.CONTINUOUS
        val vis = if (discrete) View.VISIBLE else View.GONE
        binding.discreteValuesTextView.visibility = vis
        binding.discreteValues.visibility = vis
        binding.addDiscreteValueButton.visibility = vis
        binding.discreteValuesTipText.visibility = vis
        validateForm()
    }

    private fun onAddClicked() {
        viewModel.onAddFeature(activity!!, binding.featureNameText.text.toString(), args.trackGroupId)
    }
}

enum class AddFeatureState { WAITING, ADDING, DONE }
class AddFeatureViewModel : ViewModel() {
    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)

    var featureName = MutableLiveData<String>("")
    var featureType = MutableLiveData<FeatureType>(FeatureType.CONTINUOUS)
    var discreteValues = mutableListOf<String>()
    val addFeatureState: LiveData<AddFeatureState> get() { return _isAdding }
    private val _isAdding by lazy {
        val adding = MutableLiveData<AddFeatureState>()
        adding.value = AddFeatureState.WAITING
        return@lazy adding
    }

    fun onAddFeature(activity: Activity, name: String, trackGroupId: Long) {
        val application = activity.application
        val database = TrackAndGraphDatabase.getInstance(application)
        val dao = database.trackAndGraphDatabaseDao
        uiScope.launch {
            _isAdding.value = AddFeatureState.ADDING
            withContext(Dispatchers.IO) {
                val discVals = discreteValues.mapIndexed { i, s -> DiscreteValue(i, s) }
                val feature = Feature.create(0, name, trackGroupId, featureType.value!!, discVals, 0)
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
