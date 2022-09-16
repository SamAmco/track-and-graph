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
package com.samco.trackandgraph.featurehistory

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.NavButtonStyle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.addtracker.DATA_POINT_TIMESTAMP_KEY
import com.samco.trackandgraph.addtracker.DataPointInputDialog
import com.samco.trackandgraph.addtracker.TRACKER_LIST_KEY
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.stringFromOdt
import com.samco.trackandgraph.base.helpers.getWeekDayNames
import com.samco.trackandgraph.databinding.FragmentFeatureHistoryBinding
import com.samco.trackandgraph.ui.YesCancelDialogFragment
import com.samco.trackandgraph.ui.showDataPointDescriptionDialog
import com.samco.trackandgraph.ui.showFeatureDescriptionDialog
import com.samco.trackandgraph.util.bindingForViewLifecycle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragmentFeatureHistory : Fragment(), YesCancelDialogFragment.YesCancelDialogListener {

    private var binding: FragmentFeatureHistoryBinding by bindingForViewLifecycle()
    private val viewModel by viewModels<FeatureHistoryViewModel>()
    private lateinit var adapter: DataPointAdapter
    private val args: FragmentFeatureHistoryArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeatureHistoryBinding.inflate(inflater, container, false)
        initPreLoadViewState()
        initAdapter()

        viewModel.initViewModel(args.featureId)

        observeIsTracker()
        observeIsDuration()
        observeDataPoints()

        initMenuProvider()
        return binding.root
    }

    private fun initMenuProvider() {
        requireActivity().addMenuProvider(
            FeatureHistoryMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private inner class FeatureHistoryMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.feature_history_menu, menu)
        }

        override fun onMenuItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.infoButton -> showInfo()
                else -> return false
            }
            return true
        }
    }

    private fun observeIsTracker() {
        viewModel.tracker.observe(viewLifecycleOwner) {
            adapter.submitIsTracker(it != null)
        }
    }

    private fun observeIsDuration() {
        viewModel.isDuration.observe(viewLifecycleOwner) {
            adapter.submitIsDuration(it)
        }
    }

    private fun initAdapter() {
        adapter = DataPointAdapter(
            DataPointClickListener(
                this::onEditDataPointClicked,
                this::onDeleteDataPointClicked,
                this::onViewDataPointClicked
            ),
            getWeekDayNames(requireContext())
        )
        binding.dataPointList.adapter = adapter
        binding.dataPointList.layoutManager = LinearLayoutManager(
            context, RecyclerView.VERTICAL, false
        )
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setActionBarConfig(NavButtonStyle.UP, args.featureName)
    }

    private fun showInfo() {
        viewModel.feature.value?.let {
            showFeatureDescriptionDialog(requireContext(), it.name, it.description)
        }
    }

    private fun initPreLoadViewState() {
        binding.noDataPointsHintText.visibility = View.GONE
    }

    private fun onViewDataPointClicked(dataPoint: DataPoint) {
        showDataPointDescriptionDialog(
            requireContext(),
            layoutInflater,
            dataPoint,
            viewModel.isDuration.value ?: false
        )
    }

    private fun observeDataPoints() {
        viewModel.dataPoints.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            adapter.submitDataPoints(it)
            binding.noDataPointsHintText.visibility =
                if (it.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun onEditDataPointClicked(dataPoint: DataPoint) {
        viewModel.tracker.value?.let { tracker ->
            val dialog = DataPointInputDialog()
            val argBundle = Bundle()
            argBundle.putLongArray(TRACKER_LIST_KEY, longArrayOf(tracker.id))
            argBundle.putString(DATA_POINT_TIMESTAMP_KEY, stringFromOdt(dataPoint.timestamp))
            dialog.arguments = argBundle
            dialog.show(childFragmentManager, "input_data_point_dialog")
        }
    }

    private fun onDeleteDataPointClicked(dataPoint: DataPoint) {
        viewModel.currentActionDataPoint = dataPoint
        val dialog = YesCancelDialogFragment
            .create("no id", getString(R.string.ru_sure_del_data_point))
        childFragmentManager.let { dialog.show(it, "ru_sure_del_data_point_fragment") }
    }

    override fun onDialogYes(dialog: YesCancelDialogFragment, id: String?) {
        when (dialog.title) {
            getString(R.string.ru_sure_del_data_point) -> viewModel.deleteDataPoint()
        }
    }
}

