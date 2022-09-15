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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.NavButtonStyle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.DataSourceDescriptor
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.base.database.stringFromOdt
import com.samco.trackandgraph.base.helpers.getWeekDayNames
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.databinding.FragmentFeatureHistoryBinding
import com.samco.trackandgraph.addtracker.DATA_POINT_TIMESTAMP_KEY
import com.samco.trackandgraph.addtracker.FEATURE_LIST_KEY
import com.samco.trackandgraph.addtracker.DataPointInputDialog
import com.samco.trackandgraph.ui.YesCancelDialogFragment
import com.samco.trackandgraph.ui.showDataPointDescriptionDialog
import com.samco.trackandgraph.ui.showFeatureDescriptionDialog
import com.samco.trackandgraph.util.bindingForViewLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

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

        viewModel.initViewModel(args.dataSource)

        observeFeature()

        setHasOptionsMenu(true)
        return binding.root
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.infoButton -> showInfo()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.feature_history_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun initPreLoadViewState() {
        binding.noDataPointsHintText.visibility = View.GONE
    }

    private fun observeFeature() {
        viewModel.feature.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer
            adapter = DataPointAdapter(
                DataPointClickListener(
                    this::onEditDataPointClicked,
                    this::onDeleteDataPointClicked,
                    this::onViewDataPointClicked
                ),
                getWeekDayNames(requireContext()),
                it.featureType
            )
            binding.dataPointList.adapter = adapter
            binding.dataPointList.layoutManager = LinearLayoutManager(
                context, RecyclerView.VERTICAL, false
            )
            observeDataPoints()
        })
    }

    private fun onViewDataPointClicked(dataPoint: DataPoint) {
        showDataPointDescriptionDialog(
            requireContext(),
            layoutInflater,
            dataPoint,
            viewModel.feature.value!!.featureType
        )
    }

    private fun observeDataPoints() {
        viewModel.dataPoints.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
                binding.noDataPointsHintText.visibility =
                    if (it.isEmpty()) View.VISIBLE else View.GONE
            }
        })
    }

    private fun onEditDataPointClicked(dataPoint: DataPoint) {
        viewModel.feature.value?.let {
            val dialog = DataPointInputDialog()
            val argBundle = Bundle()
            argBundle.putLongArray(FEATURE_LIST_KEY, longArrayOf(args.feature))
            argBundle.putString(DATA_POINT_TIMESTAMP_KEY, stringFromOdt(dataPoint.timestamp))
            dialog.arguments = argBundle
            childFragmentManager.let { dialog.show(it, "input_data_point_dialog") }
        }
    }

    private fun onDeleteDataPointClicked(dataPoint: DataPoint) {
        viewModel.currentActionDataPoint = dataPoint
        val dialog = YesCancelDialogFragment()
        val args = Bundle()
        args.putString("title", getString(R.string.ru_sure_del_data_point))
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "ru_sure_del_data_point_fragment") }
    }

    override fun onDialogYes(dialog: YesCancelDialogFragment, id: String?) {
        when (dialog.title) {
            getString(R.string.ru_sure_del_data_point) -> viewModel.deleteDataPoint()
        }
    }
}

@HiltViewModel
class FeatureHistoryViewModel @Inject constructor(
    private val dataInteractor: DataInteractor
): ViewModel() {
    private val _dataSample = MutableLiveData<DataSample?>(null)
    val dataSample: LiveData<DataSample?> = _dataSample

    lateinit var dataPoints: LiveData<List<DataPoint>>
        private set
    var currentActionDataPoint: DataPoint? = null

    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    private var dataSource: DataSourceDescriptor? = null

    fun initViewModel(dataSourceDescriptor: DataSourceDescriptor) {
        if (this.dataSource != null) return
        this.dataSource = dataSourceDescriptor
        this.dataPoints = dataInteractor.getDataSampleForFeatureId()
        ioScope.launch {
            val feature = dataInteractor.getFeatureById(featureId)

            withContext(Dispatchers.Main) { _feature.value = feature }
        }
    }

    fun deleteDataPoint() = currentActionDataPoint?.let {
        ioScope.launch { dataInteractor.deleteDataPoint(it) }
    }

    override fun onCleared() {
        super.onCleared()
        updateJob.cancel()
    }
}
