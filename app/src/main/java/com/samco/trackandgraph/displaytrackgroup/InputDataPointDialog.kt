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
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import org.threeten.bp.OffsetDateTime
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.Feature
import com.samco.trackandgraph.database.entity.FeatureType
import com.samco.trackandgraph.databinding.DataPointInputDialogBinding
import kotlinx.android.synthetic.main.data_point_input_dialog.*
import kotlinx.coroutines.*

const val FEATURE_LIST_KEY = "FEATURE_LIST_KEY"
const val DATA_POINT_TIMESTAMP_KEY = "DATA_POINT_ID"

open class InputDataPointDialog : DialogFragment(), ViewPager.OnPageChangeListener {
    private val viewModel by viewModels<InputDataPointDialogViewModel>()
    private val inputViews = mutableMapOf<Int, DataPointInputView>()
    private lateinit var binding: DataPointInputDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return activity?.let {
            initViewModel()
            binding = DataPointInputDialogBinding.inflate(inflater, container, false)

            binding.cancelButton.setOnClickListener { dismiss() }
            binding.skipButton.setOnClickListener { skip() }
            binding.addButton.setOnClickListener { onAddClicked() }

            listenToFeatures()
            listenToIndex()
            listenToState()

            binding.viewPager.addOnPageChangeListener(this)
            dialog?.setCanceledOnTouchOutside(true)

            binding.root
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun listenToState() {
        viewModel.state.observe(this, Observer { state ->
            when (state) {
                InputDataPointDialogState.LOADING -> {
                    binding.addButton.isEnabled = false
                }
                InputDataPointDialogState.WAITING -> {
                    binding.addButton.isEnabled = true
                }
                InputDataPointDialogState.ADDING -> {
                    binding.addButton.isEnabled = false
                }
                InputDataPointDialogState.ADDED -> {
                    binding.addButton.isEnabled = true
                    if (viewModel.currentFeatureIndex.value!! < viewModel.features.value!!.size - 1) {
                        skip()
                        viewModel.onFinishedTransition()
                    } else dismiss()
                }
                else -> {
                }
            }
        })
    }

    private fun listenToIndex() {
        viewModel.currentFeatureIndex.observe(this, Observer { index ->
            if (index != null) setupViewFeature(viewModel.features.value!![index], index)
        })
    }

    private fun listenToFeatures() {
        viewModel.features.observe(this, Observer { features ->
            if (features.isEmpty()) return@Observer
            binding.viewPager.adapter = ViewPagerAdapter(
                requireContext(),
                features,
                DataPointInputView.DataPointInputClickListener(this::onAddClicked),
                viewModel.uiStates,
                inputViews
            )
            if (features.size == 1) indexText.visibility = View.INVISIBLE
        })
    }

    private fun initViewModel() {
        val timestampStr = requireArguments().getString(DATA_POINT_TIMESTAMP_KEY)
        val timestamp = if (timestampStr != null) odtFromString(timestampStr) else null
        viewModel.init(
            requireActivity(),
            requireArguments().getLongArray(FEATURE_LIST_KEY)!!.toList(),
            timestamp
        )
    }

    private class ViewPagerAdapter(
        val context: Context,
        val features: List<Feature>,
        val clickListener: DataPointInputView.DataPointInputClickListener,
        val uiStates: Map<Feature, DataPointInputView.DataPointInputData>,
        val inputViews: MutableMap<Int, DataPointInputView>
    ) : PagerAdapter() {
        private val existingViews = mutableListOf<DataPointInputView>()

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object`
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val view = DataPointInputView(context)
            view.initialize(uiStates[features[position]]!!)
            val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            view.layoutParams = params
            view.setOnClickListener(clickListener)
            inputViews[position] = view
            container.addView(view)
            existingViews.add(view)
            return view
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            super.setPrimaryItem(container, position, `object`)
            if (`object` is DataPointInputView) `object`.requestFocus()
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            if (existingViews.contains(`object`)) existingViews.remove(`object`)
            container.removeView(`object` as View)
        }

        fun updateAllDateTimes() {
            existingViews.forEach { dpiv -> dpiv.updateDateTimes() }
        }

        override fun getCount() = features.size
    }

    override fun onPageScrollStateChanged(state: Int) {}
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        (binding.viewPager.adapter as ViewPagerAdapter).updateAllDateTimes()
        viewModel.currentFeatureIndex.value = position
    }

    private fun setupViewFeature(feature: Feature, index: Int) {
        if (feature.featureType != FeatureType.DISCRETE) binding.addButton.visibility = View.VISIBLE
        else binding.addButton.visibility = View.INVISIBLE
        indexText.text = "${index + 1} / ${viewModel.features.value!!.size}"

        //SHOW/HIDE KEYBOARD
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (feature.featureType != FeatureType.DISCRETE) imm.toggleSoftInput(
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
        else imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun onAddClicked() {
        val currIndex = viewModel.currentFeatureIndex.value!!
        val currFeature = viewModel.features.value!![currIndex]
        viewModel.uiStates[currFeature]?.timeFixed = true
        onAddClicked(currFeature)
    }

    private fun onAddClicked(feature: Feature) {
        onSubmitResult(viewModel.uiStates[feature]!!)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().currentFocus?.clearFocus()
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(
            requireActivity().window.decorView.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun onSubmitResult(dataPointInputData: DataPointInputView.DataPointInputData) {
        viewModel.onDataPointInput(
            DataPoint(
                dataPointInputData.dateTime, dataPointInputData.feature.id,
                dataPointInputData.value, dataPointInputData.label, dataPointInputData.note
            ),
            dataPointInputData.oldDataPoint
        )
    }

    private fun skip() {
        if (binding.viewPager.currentItem == viewModel.features.value!!.size - 1) dismiss()
        else binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
    }
}

enum class InputDataPointDialogState { LOADING, WAITING, ADDING, ADDED }
class InputDataPointDialogViewModel : ViewModel() {
    private lateinit var dao: TrackAndGraphDatabaseDao
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    val state: LiveData<InputDataPointDialogState>
        get() {
            return _state
        }
    private val _state by lazy {
        val state = MutableLiveData<InputDataPointDialogState>()
        state.value = InputDataPointDialogState.LOADING
        return@lazy state
    }

    lateinit var uiStates: Map<Feature, DataPointInputView.DataPointInputData>
    val features: LiveData<List<Feature>>
        get() {
            return _features
        }
    private val _features by lazy {
        val feats = MutableLiveData<List<Feature>>()
        feats.value = listOf()
        return@lazy feats
    }

    val currentFeatureIndex: MutableLiveData<Int?>
        get() {
            return _currentFeatureIndex
        }
    private val _currentFeatureIndex by lazy {
        val index = MutableLiveData<Int?>()
        index.value = null
        return@lazy index
    }

    fun init(activity: Activity, featureIds: List<Long>, dataPointTimestamp: OffsetDateTime?) {
        if (features.value!!.isNotEmpty()) return
        val application = activity.application
        dao = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao
        _state.value = InputDataPointDialogState.LOADING
        ioScope.launch {
            val featureData = dao.getFeaturesByIdsSync(featureIds)
            val dataPointData = dataPointTimestamp?.let {
                dao.getDataPointByTimestampAndFeatureSync(featureData[0].id, it)
            }
            uiStates = getUIStatesForFeatures(featureData, dataPointData)

            withContext(Dispatchers.Main) {
                _features.value = featureData
                _currentFeatureIndex.value = 0
                _state.value = InputDataPointDialogState.WAITING
            }
        }
    }

    private fun getUIStatesForFeatures(
        featureData: List<Feature>,
        dataPointData: DataPoint?
    ): Map<Feature, DataPointInputView.DataPointInputData> {
        val timestamp = dataPointData?.timestamp ?: OffsetDateTime.now()
        val timeFixed = dataPointData != null
        return featureData.map { f ->
            val dataPointValue = when {
                dataPointData?.value != null -> dataPointData.value
                f.hasDefaultValue -> f.defaultValue
                f.featureType == FeatureType.CONTINUOUS -> 1.0
                else -> 0.0
            }
            val dataPointLabel = dataPointData?.label
                ?: if (f.hasDefaultValue) f.getDefaultLabel() else ""
            val dataPointNote = dataPointData?.note ?: ""
            f to DataPointInputView.DataPointInputData(
                f,
                timestamp,
                dataPointValue,
                dataPointLabel,
                dataPointNote,
                timeFixed,
                this@InputDataPointDialogViewModel::onDateTimeSelected,
                dataPointData
            )
        }.toMap()
    }

    private fun onDateTimeSelected(dateTime: OffsetDateTime) {
        uiStates.values.forEach { dp ->
            if (!dp.timeFixed) {
                dp.dateTime = dateTime
            }
        }
    }

    fun onDataPointInput(newDataPoint: DataPoint, oldDataPoint: DataPoint?) {
        if (state.value != InputDataPointDialogState.WAITING) return
        _state.value = InputDataPointDialogState.ADDING
        ioScope.launch {
            if (oldDataPoint != null) dao.deleteDataPoint(oldDataPoint)
            dao.insertDataPoint(newDataPoint)
            withContext(Dispatchers.Main) { _state.value = InputDataPointDialogState.ADDED }
        }
    }

    fun onFinishedTransition() {
        _state.value = InputDataPointDialogState.WAITING
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }
}
