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
package com.samco.trackandgraph.addtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.NavButtonStyle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.model.TrackerHelper.DurationNumericConversionMode
import com.samco.trackandgraph.databinding.AddTrackerFragmentBinding
import com.samco.trackandgraph.ui.YesCancelDialogFragment
import com.samco.trackandgraph.util.*
import dagger.hilt.android.AndroidEntryPoint


//TODO this whole fragment/view model is a mess. There is way too much logic in the fragment
// this is not a good example of anything
@AndroidEntryPoint
class AddTrackerFragment : Fragment(), YesCancelDialogFragment.YesCancelDialogListener {
    private val args: AddTrackerFragmentArgs by navArgs()
    private var binding: AddTrackerFragmentBinding by bindingForViewLifecycle()
    private val viewModel by viewModels<AddTrackerViewModel>()
    private var navController: NavController? = null

    private val trackerTypeList = listOf(
        DataType.CONTINUOUS,
        DataType.DURATION
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        navController = container?.findNavController()
        binding = AddTrackerFragmentBinding.inflate(inflater, container, false)

        viewModel.init(args.groupId, args.editFeatureId)

        listenToViewModelState()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setActionBarConfig(
            NavButtonStyle.UP,
            getString(R.string.add_tracker)
        )
    }

    override fun onStop() {
        super.onStop()
        requireActivity().window.hideKeyboard(requireActivity().currentFocus?.windowToken)
    }

    private fun listenToViewModelState() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                AddTrackerState.INITIALIZING -> {
                    setInitialViewState()
                }
                AddTrackerState.SET_FOCUS -> {
                    binding.trackerNameText.focusAndShowKeyboard()
                }
                AddTrackerState.WAITING -> {
                    onViewModelReady()
                    binding.progressBar.visibility = View.GONE
                }
                AddTrackerState.ADDING -> {
                    binding.addBar.addButton.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                AddTrackerState.DONE -> {
                    navController?.popBackStack()
                }
                AddTrackerState.ERROR -> {
                    val errorMsg = getString(R.string.feature_add_or_update_error_occurred)
                    Toast.makeText(requireActivity(), errorMsg, Toast.LENGTH_LONG).show()
                    navController?.popBackStack()
                }
                else -> {
                }
            }
        }
    }

    private fun setInitialViewState() {
        binding.defaultNumericalInput.visibility = View.INVISIBLE
        binding.defaultDurationInput.visibility = View.INVISIBLE
        binding.durationNumericConversionModeSpinner.visibility = View.INVISIBLE
        binding.durationToNumericModeHeader.visibility = View.INVISIBLE
        binding.numericToDurationModeHeader.visibility = View.INVISIBLE
    }

    private fun onViewModelReady() {
        initViewFromViewModel()
        observeHasDefaultValue()
        observeFeatureType()
    }

    private fun getOnDataTypeSelectedListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                viewModel.dataType.value = trackerTypeList[position]
            }
        }
    }

    private fun initViewFromViewModel() {
        binding.trackerDescription.setText(viewModel.trackerDescription)
        binding.trackerTypeSpinner.setSelection(trackerTypeList.indexOf(viewModel.dataType.value!!))
        binding.trackerTypeSpinner.onItemSelectedListener = getOnDataTypeSelectedListener()

        binding.trackerNameText.setText(viewModel.trackerName)
        binding.trackerNameText.setSelection(binding.trackerNameText.text.length)
        binding.trackerNameText.requestFocus()
        binding.trackerNameText.addTextChangedListener {
            viewModel.trackerName = binding.trackerNameText.text.toString()
            validateForm()
        }
        binding.trackerDescription.addTextChangedListener {
            viewModel.trackerDescription = binding.trackerDescription.text.toString()
        }
        binding.addBar.addButton.setOnClickListener { onAddOrUpdateClicked() }
        binding.hasDefaultValueCheckbox.setOnCheckedChangeListener { _, checked ->
            viewModel.trackerHasDefaultValue.value = checked
        }
        binding.hasDefaultValueCheckbox.isChecked = viewModel.trackerHasDefaultValue.value!!
        binding.defaultNumericalInput.addTextChangedListener {
            viewModel.trackerDefaultValue.value = getDoubleFromText(it.toString())
        }
        binding.defaultDurationInput.setDurationChangedListener {
            viewModel.trackerDefaultValue.value = it.toDouble()
        }
        binding.defaultDurationInput.setDoneListener {
            requireActivity().window.hideKeyboard()
        }
        binding.durationNumericConversionModeSpinner.onItemSelectedListener =
            getOnDurationNumericConversionModeSelectedListener()
    }

    private fun getOnDurationNumericConversionModeSelectedListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                viewModel.durationNumericConversionMode.value =
                    DurationNumericConversionMode.values()[position]
            }
        }
    }

    private fun observeFeatureType() {
        viewModel.dataType.observe(viewLifecycleOwner) {
            onTrackerDataTypeChanged()
        }
    }

    private fun observeHasDefaultValue() {
        viewModel.trackerHasDefaultValue.observe(viewLifecycleOwner) {
            updateDefaultValuesViewFromViewModel(it)
        }
    }

    private fun updateDefaultValuesViewFromViewModel(checked: Boolean) {
        if (checked) {
            when (viewModel.dataType.value) {
                DataType.CONTINUOUS -> {
                    binding.defaultNumericalInput.setText(viewModel.trackerDefaultValue.value!!.toString())
                    binding.defaultNumericalInput.visibility = View.VISIBLE
                    binding.defaultDurationInput.visibility = View.GONE
                }
                DataType.DURATION -> {
                    binding.defaultDurationInput.setTimeInSeconds(viewModel.trackerDefaultValue.value!!.toLong())
                    binding.defaultNumericalInput.visibility = View.GONE
                    binding.defaultDurationInput.visibility = View.VISIBLE
                }
                null -> {}
            }
        } else {
            binding.defaultNumericalInput.visibility = View.GONE
            binding.defaultDurationInput.visibility = View.GONE
        }
    }

    private fun onAddOrUpdateClicked() {
        if (!viewModel.updateMode) viewModel.onAddOrUpdate()
        else {
            val dialog = YesCancelDialogFragment
                .create("no id", getString(R.string.ru_sure_update_tracker))
            childFragmentManager.let { dialog.show(it, "ru_sure_update_tracker_fragment") }
        }
    }

    //TODO move this to the view model
    private fun validateForm() {
        var errorSet = false
        binding.trackerNameText.text.let {
            if (it.isEmpty()) {
                setErrorText(getString(R.string.tracker_name_cannot_be_null))
                errorSet = true
            } else if (viewModel.disallowedNames.contains(it.toString())) {
                setErrorText(getString(R.string.tracker_with_that_name_exists))
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

    private fun updateDurationNumericConversionUI() {
        val durationToNumeric = viewModel.updateMode
                && viewModel.existingTracker?.dataType == DataType.DURATION
                && viewModel.dataType.value == DataType.CONTINUOUS
        val numericToDuration = viewModel.updateMode
                && viewModel.existingTracker?.dataType == DataType.CONTINUOUS
                && viewModel.dataType.value == DataType.DURATION
        binding.durationToNumericModeHeader.visibility =
            if (durationToNumeric) View.VISIBLE else View.GONE
        binding.numericToDurationModeHeader.visibility =
            if (numericToDuration) View.VISIBLE else View.GONE
        binding.durationNumericConversionModeSpinner.visibility =
            if (durationToNumeric || numericToDuration) View.VISIBLE else View.GONE
    }

    private fun onTrackerDataTypeChanged() {
        updateDurationNumericConversionUI()
        updateDefaultValuesViewFromViewModel(viewModel.trackerHasDefaultValue.value!!)
        validateForm()
    }

    override fun onDialogYes(dialog: YesCancelDialogFragment, id: String?) {
        when (dialog.title) {
            getString(R.string.ru_sure_update_tracker) -> viewModel.onAddOrUpdate()
        }
    }
}