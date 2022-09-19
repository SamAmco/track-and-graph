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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.core.view.forEachIndexed
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
import com.samco.trackandgraph.databinding.FeatureDiscreteValueListItemBinding
import com.samco.trackandgraph.ui.YesCancelDialogFragment
import com.samco.trackandgraph.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.absoluteValue


//TODO this whole fragment/view model is a mess. There is way too much logic in the fragment
// this is not a good example of anything
@AndroidEntryPoint
class AddTrackerFragment : Fragment(), YesCancelDialogFragment.YesCancelDialogListener {
    private val args: AddTrackerFragmentArgs by navArgs()
    private var binding: AddTrackerFragmentBinding by bindingForViewLifecycle()
    private val viewModel by viewModels<AddTrackerViewModel>()
    private var navController: NavController? = null

    private val trackerTypeList = listOf(
        DataType.DISCRETE,
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
        binding.discreteValuesTextView.visibility = View.INVISIBLE
        binding.discreteValues.visibility = View.INVISIBLE
        binding.addDiscreteValueButton.visibility = View.INVISIBLE
        binding.defaultNumericalInput.visibility = View.INVISIBLE
        binding.defaultDurationInput.visibility = View.INVISIBLE
        binding.defaultDiscreteScrollView.visibility = View.INVISIBLE
        binding.durationNumericConversionModeSpinner.visibility = View.INVISIBLE
        binding.durationToNumericModeHeader.visibility = View.INVISIBLE
        binding.numericToDurationModeHeader.visibility = View.INVISIBLE
    }

    private fun onViewModelReady() {
        initViewFromViewModel()
        observeHasDefaultValue()
        observeFeatureType()
    }

    private fun getOnFeatureSelectedListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                viewModel.dataType.value = trackerTypeList[position]
            }
        }
    }

    private fun initViewFromViewModel() {
        viewModel.discreteValues.forEach { v -> inflateDiscreteValue(v) }
        if (viewModel.updateMode) initSpinnerInUpdateMode()

        binding.trackerDescription.setText(viewModel.trackerDescription)
        binding.trackerTypeSpinner.setSelection(trackerTypeList.indexOf(viewModel.dataType.value!!))
        binding.trackerTypeSpinner.onItemSelectedListener = getOnFeatureSelectedListener()

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
        binding.addDiscreteValueButton.setOnClickListener { onAddDiscreteValue() }
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
            onTrackerDataTypeChanged(it)
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
                DataType.DISCRETE -> {
                    if (currentDefaultValueIsInvalidDiscreteValue()) {
                        viewModel.trackerDefaultValue.value = 0.0
                    }
                    updateCurrentlySelectedDefaultDiscreteValue()
                    binding.defaultDiscreteScrollView.visibility = View.VISIBLE
                    binding.defaultNumericalInput.visibility = View.GONE
                    binding.defaultDurationInput.visibility = View.GONE
                }
                DataType.CONTINUOUS -> {
                    binding.defaultNumericalInput.setText(viewModel.trackerDefaultValue.value!!.toString())
                    binding.defaultDiscreteScrollView.visibility = View.GONE
                    binding.defaultNumericalInput.visibility = View.VISIBLE
                    binding.defaultDurationInput.visibility = View.GONE
                }
                DataType.DURATION -> {
                    binding.defaultDurationInput.setTimeInSeconds(viewModel.trackerDefaultValue.value!!.toLong())
                    binding.defaultDiscreteScrollView.visibility = View.GONE
                    binding.defaultNumericalInput.visibility = View.GONE
                    binding.defaultDurationInput.visibility = View.VISIBLE
                }
                null -> {}
            }
        } else {
            binding.defaultDiscreteScrollView.visibility = View.GONE
            binding.defaultNumericalInput.visibility = View.GONE
            binding.defaultDurationInput.visibility = View.GONE
        }
    }

    private fun currentDefaultValueIsInvalidDiscreteValue(): Boolean {
        val currentDefaultValue = viewModel.trackerDefaultValue.value!!
        return currentDefaultValue > viewModel.discreteValues.size - 1
                || currentDefaultValue < 0
                || (currentDefaultValue - currentDefaultValue.toInt().toDouble()).absoluteValue > 0
    }

    private fun initSpinnerInUpdateMode() {
        val itemList = resources.getStringArray(R.array.tracker_types)
            .map { s -> s as CharSequence }.toTypedArray()
        binding.trackerTypeSpinner.adapter = object : ArrayAdapter<CharSequence>(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item, itemList
        ) {
            override fun isEnabled(position: Int): Boolean {
                return viewModel.isFeatureTypeEnabled(trackerTypeList[position])
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                val enabled = viewModel.isFeatureTypeEnabled(trackerTypeList[position])
                val color =
                    if (enabled) view.context.getColorFromAttr(android.R.attr.textColorPrimary)
                    else view.context.getColorFromAttr(android.R.attr.textColorHint)
                textView.setTextColor(color)
                return view
            }
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

    private fun inflateDiscreteValue(label: AddTrackerViewModel.MutableLabel) {
        inflateDiscreteValueInputCard(label)
        inflateDiscreteValueDefaultButton(label)
        reIndexDiscreteValueViews()
        validateForm()
    }

    private fun updateCurrentlySelectedDefaultDiscreteValue() {
        val selected = viewModel.trackerDefaultValue.value!!.toInt()
        binding.defaultDiscreteButtonsLayout.children.forEachIndexed { i, view ->
            (view as CheckBox).isChecked = i == selected
        }
    }

    private fun inflateDiscreteValueDefaultButton(label: AddTrackerViewModel.MutableLabel) {
        val layout = binding.defaultDiscreteButtonsLayout
        val item =
            layoutInflater.inflate(R.layout.discrete_value_input_button, layout, false) as CheckBox
        item.text = label.label
        val index = viewModel.discreteValues.indexOf(label)
        item.isChecked =
            viewModel.trackerHasDefaultValue.value!! && index == viewModel.trackerDefaultValue.value?.toInt()
        item.setOnClickListener {
            viewModel.trackerDefaultValue.value = viewModel.discreteValues.indexOf(label).toDouble()
            updateCurrentlySelectedDefaultDiscreteValue()
        }
        layout.addView(item, index)
    }

    private fun updateDefaultValueButtonsText() {
        binding.defaultDiscreteButtonsLayout.children.forEachIndexed { i, view ->
            (view as CheckBox).text = viewModel.discreteValues[i].label
        }
    }

    private fun inflateDiscreteValueInputCard(label: AddTrackerViewModel.MutableLabel) {
        val item = FeatureDiscreteValueListItemBinding.inflate(
            layoutInflater,
            binding.discreteValues,
            false
        )
        val inputText = item.discreteValueNameText
        inputText.addTextChangedListener {
            label.label = it.toString().trim()
            updateDefaultValueButtonsText()
            validateForm()
        }
        inputText.setText(label.label)
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
    }

    private fun onAddDiscreteValue() {
        val label = AddTrackerViewModel.MutableLabel()
        viewModel.discreteValues.add(label)
        inflateDiscreteValue(label)
    }

    private fun validateForm() {
        var errorSet = false
        val discreteValueStrings = viewModel.discreteValues
        if (viewModel.dataType.value!! == DataType.DISCRETE) {
            if (discreteValueStrings.isEmpty() || discreteValueStrings.size < 2) {
                setErrorText(getString(R.string.discrete_tracker_needs_at_least_two_values))
                errorSet = true
            }
            for (s in discreteValueStrings) {
                if (s.label.isEmpty()) {
                    setErrorText(getString(R.string.discrete_value_must_have_name))
                    errorSet = true
                }
            }
        }
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

    private fun onDownClickedDiscreteValue(item: FeatureDiscreteValueListItemBinding) {
        val currIndex = binding.discreteValues.indexOfChild(item.root)
        if (currIndex == binding.discreteValues.childCount - 1) return
        viewModel.discreteValues.add(currIndex + 1, viewModel.discreteValues.removeAt(currIndex))
        binding.discreteValues.removeView(item.root)
        binding.discreteValues.addView(item.root, currIndex + 1)
        val defaultButtonView = binding.defaultDiscreteButtonsLayout.getChildAt(currIndex)
        binding.defaultDiscreteButtonsLayout.removeViewAt(currIndex)
        binding.defaultDiscreteButtonsLayout.addView(defaultButtonView, currIndex + 1)
        if (currIndex == viewModel.trackerDefaultValue.value!!.toInt())
            viewModel.trackerDefaultValue.value = viewModel.trackerDefaultValue.value!! + 1
        reIndexDiscreteValueViews()
    }

    private fun onUpClickedDiscreteValue(item: FeatureDiscreteValueListItemBinding) {
        val currIndex = binding.discreteValues.indexOfChild(item.root)
        if (currIndex == 0) return
        viewModel.discreteValues.add(currIndex - 1, viewModel.discreteValues.removeAt(currIndex))
        binding.discreteValues.removeView(item.root)
        binding.discreteValues.addView(item.root, currIndex - 1)
        val defaultButtonView = binding.defaultDiscreteButtonsLayout.getChildAt(currIndex)
        binding.defaultDiscreteButtonsLayout.removeViewAt(currIndex)
        binding.defaultDiscreteButtonsLayout.addView(defaultButtonView, currIndex - 1)
        if (currIndex == viewModel.trackerDefaultValue.value!!.toInt())
            viewModel.trackerDefaultValue.value = viewModel.trackerDefaultValue.value!! - 1
        reIndexDiscreteValueViews()
    }

    private fun onDeleteDiscreteValue(item: FeatureDiscreteValueListItemBinding) {
        if (viewModel.updateMode && !viewModel.haveWarnedAboutDeletingDiscreteValues) {
            AlertDialog.Builder(context)
                .setTitle(R.string.warning)
                .setMessage(R.string.on_tracker_delete_discrete_value_warning)
                .setPositiveButton(R.string.ok, null)
                .show()
            viewModel.haveWarnedAboutDeletingDiscreteValues = true
        }
        val currIndex = binding.discreteValues.indexOfChild(item.root)
        viewModel.discreteValues.removeAt(currIndex)
        binding.discreteValues.removeView(item.root)
        binding.addDiscreteValueButton.isEnabled = true
        binding.defaultDiscreteButtonsLayout.removeViewAt(currIndex)
        if (currIndex == viewModel.trackerDefaultValue.value!!.toInt())
            viewModel.trackerDefaultValue.value = 0.0
        reIndexDiscreteValueViews()
    }

    @SuppressLint("SetTextI18n")
    private fun reIndexDiscreteValueViews() {
        binding.discreteValues.forEachIndexed { i, v ->
            v.findViewById<TextView>(R.id.indexText).text = "$i : "
        }
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

    private fun onTrackerDataTypeChanged(dataType: DataType) {
        showOnTrackerTypeUpdatedMessage(dataType)
        updateDurationNumericConversionUI()
        updateDefaultValuesViewFromViewModel(viewModel.trackerHasDefaultValue.value!!)
        val vis = if (dataType == DataType.DISCRETE) View.VISIBLE else View.GONE
        binding.discreteValuesTextView.visibility = vis
        binding.discreteValues.visibility = vis
        binding.addDiscreteValueButton.visibility = vis
        validateForm()
    }

    private fun showOnTrackerTypeUpdatedMessage(newType: DataType) {
        val oldType = viewModel.existingTracker?.dataType
        if (viewModel.updateMode && oldType != null && oldType != newType) {
            val message = getOnDataTypeChangedMessage(oldType, newType)

            message?.let {
                AlertDialog.Builder(context)
                    .setTitle(R.string.warning)
                    .setMessage(it)
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
        }
    }

    private fun getOnDataTypeChangedMessage(oldType: DataType, newType: DataType): String? {
        return when (oldType) {
            DataType.DISCRETE -> when (newType) {
                DataType.DISCRETE -> null
                DataType.CONTINUOUS -> getString(R.string.on_tracker_type_change_numerical_warning)
                else -> null
            }
            else -> null
        }
    }

    override fun onDialogYes(dialog: YesCancelDialogFragment, id: String?) {
        when (dialog.title) {
            getString(R.string.ru_sure_update_tracker) -> viewModel.onAddOrUpdate()
        }
    }
}