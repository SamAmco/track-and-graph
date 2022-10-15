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
package com.samco.trackandgraph.adddatapoint

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.database.odtFromString
import com.samco.trackandgraph.databinding.DataPointInputDialogBinding
import com.samco.trackandgraph.util.bindingForViewLifecycle
import com.samco.trackandgraph.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint

const val TRACKER_LIST_KEY = "TRACKER_LIST_KEY"
const val DATA_POINT_TIMESTAMP_KEY = "DATA_POINT_ID"
const val DURATION_SECONDS_KEY = "DURATION_SECONDS_KEY"

@AndroidEntryPoint
open class DataPointInputDialog : DialogFragment(), ViewPager.OnPageChangeListener {
    private val viewModel by viewModels<InputDataPointDialogViewModel>()
    private val inputViews = mutableMapOf<Int, DataPointInputView>()
    private var binding: DataPointInputDialogBinding by bindingForViewLifecycle()

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
        viewModel.state.observe(viewLifecycleOwner) { state ->
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
                    if (viewModel.currentTrackerIndex.value!! < viewModel.trackers.value!!.size - 1) {
                        skip()
                        viewModel.onFinishedTransition()
                    } else dismiss()
                }
                else -> {}
            }
        }
    }

    private fun listenToIndex() {
        viewModel.currentTrackerIndex.observe(viewLifecycleOwner) { index ->
            index?.run { setupViewFeature(viewModel.trackers.value!![index], index) }
        }
    }

    private fun listenToFeatures() {
        viewModel.trackers.observe(viewLifecycleOwner) { features ->
            if (features.isEmpty()) return@observe
            binding.viewPager.adapter = ViewPagerAdapter(
                requireContext(),
                features,
                DataPointInputView.DataPointInputClickListener(this::onAddClicked),
                viewModel.uiStates,
                inputViews
            )
            if (features.size == 1) {
                // only show skip button if there is the pager contains multiple inputs
                // if there is only a single input, there is nothing to "skip"
                //      and the button should therefore not be shown
                binding.indexText.visibility = View.GONE
                binding.skipButton.visibility = View.GONE
            }
        }
    }

    private fun initViewModel() {
        val timestampStr = requireArguments().getString(DATA_POINT_TIMESTAMP_KEY)
        val timestamp = if (timestampStr != null) odtFromString(timestampStr) else null
        val duration = requireArguments().getLong(DURATION_SECONDS_KEY).let {
            if (it > 0) it.toDouble() else null
        }
        viewModel.init(
            requireArguments().getLongArray(TRACKER_LIST_KEY)!!.toList(),
            timestamp,
            duration
        )
    }

    private class ViewPagerAdapter(
        val context: Context,
        val trackers: List<Tracker>,
        val clickListener: DataPointInputView.DataPointInputClickListener,
        val uiStates: Map<Tracker, DataPointInputView.DataPointInputData>,
        val inputViews: MutableMap<Int, DataPointInputView>
    ) : PagerAdapter() {
        private val existingViews = mutableListOf<DataPointInputView>()
        private var currentPosition = -1

        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view == obj
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val view = DataPointInputView(context)
            view.initialize(uiStates[trackers[position]]!!)
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

        override fun setPrimaryItem(container: ViewGroup, position: Int, obj: Any) {
            super.setPrimaryItem(container, position, obj)
            if (currentPosition != position && obj is DataPointInputView) {
                currentPosition = position
                obj.requestFocus()
            }
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            existingViews.remove(`object`) // does nothing if missing
            container.removeView(`object` as View)
        }

        fun updateAllDateTimes() = existingViews.forEach { dpiv -> dpiv.updateDateTimes() }

        override fun getCount() = trackers.size
    }

    override fun onPageScrollStateChanged(state: Int) {}
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) {
        (binding.viewPager.adapter as ViewPagerAdapter).updateAllDateTimes()
        viewModel.currentTrackerIndex.value = position
    }

    @SuppressLint("SetTextI18n")
    private fun setupViewFeature(tracker: Tracker, index: Int) {
        binding.indexText.text = "${index + 1} / ${viewModel.trackers.value!!.size}"
        requireActivity().currentFocus?.clearFocus()
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun onAddClicked() {
        val currIndex = viewModel.currentTrackerIndex.value!!
        val currTracker = viewModel.trackers.value!![currIndex]
        viewModel.uiStates[currTracker]?.timeFixed = true
        onAddClicked(currTracker)
    }

    private fun onAddClicked(tracker: Tracker) {
        onSubmitResult(viewModel.uiStates[tracker]!!)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().currentFocus?.clearFocus()
        requireActivity().window?.hideKeyboard()
    }

    private fun onSubmitResult(dataPointInputData: DataPointInputView.DataPointInputData) {
        viewModel.onDataPointInput(
            DataPoint(
                dataPointInputData.dateTime,
                dataPointInputData.tracker.featureId,
                dataPointInputData.value,
                dataPointInputData.label,
                dataPointInputData.note
            ),
            dataPointInputData.oldDataPoint
        )
    }

    private fun skip() {
        if (binding.viewPager.currentItem == viewModel.trackers.value!!.size - 1) dismiss()
        else binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
    }
}