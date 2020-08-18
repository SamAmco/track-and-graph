/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.ui

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.database.entity.*
import com.samco.trackandgraph.databinding.CustomiseCardDialogBinding
import kotlinx.coroutines.*

const val CUSTOMISE_CARD_DIALOG_FEATURE_KEY = "customise_card_group"

class CustomiseCardDialogFragment : DialogFragment() {
    private val viewModel by viewModels<CustomiseCardDialogViewModel>()
    private lateinit var binding: CustomiseCardDialogBinding
    private val featurePeriodList = listOf(
        FeatureShowCountPeriod.ALL,
        FeatureShowCountPeriod.YEARLY,
        FeatureShowCountPeriod.MONTHLY,
        FeatureShowCountPeriod.WEEKLY,
        FeatureShowCountPeriod.DAILY
    )
    private val featureMethodList = listOf(
        FeatureShowCountMethod.COUNT_ENTRIES,
        FeatureShowCountMethod.SUM_VALUES
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = CustomiseCardDialogBinding.inflate(inflater, container, false)
        return activity?.let {
            val id = requireArguments().getLong(CUSTOMISE_CARD_DIALOG_FEATURE_KEY)
            viewModel.init(requireActivity(), id)
            dialog?.setCanceledOnTouchOutside(true)

            listenToViewModelState()
            binding.root
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun getOnPeriodSelectedListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                viewModel.setPeriodTo(featurePeriodList[position])
            }
        }
    }
    private fun getOnMethodSelectedListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                viewModel.setMethodTo(featureMethodList[position])
            }
        }
    }
    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun listenToViewModelState() {
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                CustomiseCardDialogState.INITIALIZING -> { setInitialViewState() }
                CustomiseCardDialogState.WAITING -> { onViewModelReady() }
                else -> {}
            }
        })
    }

    private fun setInitialViewState() {}
    private fun onViewModelReady() {
        binding.featureShowCountPeriodSpinner.setSelection(featurePeriodList.indexOf(viewModel.featurePeriod.value!!))
        binding.featureShowCountMethodSpinner.setSelection(featureMethodList.indexOf(viewModel.featureMethod.value!!))
        binding.featureShowCountPeriodSpinner.onItemSelectedListener = getOnPeriodSelectedListener()
        binding.featureShowCountMethodSpinner.onItemSelectedListener = getOnMethodSelectedListener()
        binding.closeButton.setOnClickListener { dismiss() }
    }

    // this seems useless after all
    /*private fun initSpinners() {
        val itemPeriodList = resources.getStringArray(R.array.feature_show_count_periods)
            .map { s -> s as CharSequence }.toTypedArray()
        val itemMethodList = resources.getStringArray(R.array.feature_show_count_methods)
            .map { s -> s as CharSequence }.toTypedArray()

        binding.featureShowCountPeriodSpinner.adapter = object : ArrayAdapter<CharSequence>(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item, itemPeriodList
        ) {
            override fun isEnabled(position: Int): Boolean {
                return featurePeriodList[position] == viewModel.featurePeriod
            }
            override fun getDropDownView( position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                val color =
                    if (isEnabled(position)) view.context.getColorFromAttr(android.R.attr.textColorPrimary)
                    else view.context.getColorFromAttr(android.R.attr.textColorHint)
                textView.setTextColor(color)
                return view
            }
        }
        binding.featureShowCountMethodSpinner.adapter = object : ArrayAdapter<CharSequence>(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item, itemMethodList
        ) {
            override fun isEnabled(position: Int): Boolean {
                return featureMethodList[position] == viewModel.featureMethod
            }
            override fun getDropDownView( position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                val color =
                    if (isEnabled(position)) view.context.getColorFromAttr(android.R.attr.textColorPrimary)
                    else view.context.getColorFromAttr(android.R.attr.textColorHint)
                textView.setTextColor(color)
                return view
            }
        }
    }
    */
}

enum class CustomiseCardDialogState { INITIALIZING, WAITING, UPDATING }  // UPDATING then WAITING again

class CustomiseCardDialogViewModel : ViewModel() {
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)
    private var database: TrackAndGraphDatabase? = null
    private var dao: TrackAndGraphDatabaseDao? = null

    val state: LiveData<CustomiseCardDialogState> get() { return _state }
    private val _state = MutableLiveData<CustomiseCardDialogState>(CustomiseCardDialogState.INITIALIZING)

    val featurePeriod = MutableLiveData(FeatureShowCountPeriod.ALL)
    val featureMethod = MutableLiveData(FeatureShowCountMethod.COUNT_ENTRIES)

    private lateinit var feature: Feature

    fun init(activity: Activity, id: Long) {
        if (database != null) return
        database = TrackAndGraphDatabase.getInstance(activity.application)
        dao = database!!.trackAndGraphDatabaseDao
        if (_state.value != CustomiseCardDialogState.INITIALIZING) return

        ioScope.launch {
            feature = dao!!.getFeatureById(id)
            withContext(Dispatchers.Main) {
                featurePeriod.value = feature.showCountPeriod
                featureMethod.value = feature.showCountMethod
                _state.value = CustomiseCardDialogState.WAITING
            }
        }
    }

    fun setPeriodTo(period:FeatureShowCountPeriod) = ioScope.launch {
        featurePeriod.postValue(period)
        if (_state.value != CustomiseCardDialogState.WAITING) return@launch
        withContext(Dispatchers.Main) { _state.value = CustomiseCardDialogState.UPDATING }
        val newFeature = feature.copy(showCountPeriod = period)
        dao!!.updateFeature(newFeature)
        withContext(Dispatchers.Main) { _state.value = CustomiseCardDialogState.WAITING }
    }

    fun setMethodTo(meth:FeatureShowCountMethod) = ioScope.launch {
        featureMethod.postValue(meth)
        if (_state.value != CustomiseCardDialogState.WAITING) return@launch
        withContext(Dispatchers.Main) { _state.value = CustomiseCardDialogState.UPDATING }
        val newFeature = feature.copy(showCountMethod = meth)
        dao!!.updateFeature(newFeature)
        withContext(Dispatchers.Main) { _state.value = CustomiseCardDialogState.WAITING }
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }

}