package com.samco.trackandgraph.featurehistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.DataPoint
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.stringFromOdt
import com.samco.trackandgraph.databinding.FragmentFeatureHistoryBinding
import com.samco.trackandgraph.displaytrackgroup.DATA_POINT_TIMESTAMP_KEY
import com.samco.trackandgraph.displaytrackgroup.FEATURE_LIST_KEY
import com.samco.trackandgraph.displaytrackgroup.InputDataPointDialog
import com.samco.trackandgraph.ui.YesCancelDialogFragment
import kotlinx.coroutines.*


class FragmentFeatureHistory : Fragment(), YesCancelDialogFragment.YesCancelDialogListener {

    private lateinit var binding: FragmentFeatureHistoryBinding
    private lateinit var viewModel: FeatureHistoryViewModel
    private val args: FragmentFeatureHistoryArgs by navArgs()

    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_feature_history, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        initFeature()
        viewModel = createViewModel()
        val adapter = DataPointAdapter(DataPointClickListener(
            this::onEditDataPointClicked,
            this::onDeleteDataPointClicked
        ))
        observeFeatureDataAndUpdate(viewModel, adapter)
        binding.dataPointList.adapter = adapter
        binding.dataPointList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        (activity as AppCompatActivity).supportActionBar?.title = args.featureName
        return binding.root
    }

    private fun initFeature() {
        val application = requireActivity().application
        val dao = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                viewModel.feature = dao.getFeatureById(args.feature)
            }
        }
    }

    private fun createViewModel(): FeatureHistoryViewModel {
        val application = requireNotNull(this.activity).application
        val dataSource = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao
        val viewModelFactory = FeatureHistoryViewModelFactory(args.feature, dataSource)
        return ViewModelProviders.of(this, viewModelFactory).get(FeatureHistoryViewModel::class.java)
    }

    private fun observeFeatureDataAndUpdate(featureHistoryViewModel: FeatureHistoryViewModel, adapter: DataPointAdapter) {
        featureHistoryViewModel.dataPoints.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })
    }

    private fun onEditDataPointClicked(dataPoint: DataPoint) {
        viewModel.feature?.let {
            val dialog = InputDataPointDialog()
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
        var args = Bundle()
        args.putString("title", getString(R.string.ru_sure_del_data_point))
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "ru_sure_del_data_point_fragment") }
    }

    override fun onDialogYes(dialog: YesCancelDialogFragment) {
        when (dialog.title) {
            getString(R.string.ru_sure_del_data_point) -> deleteDataPoint(viewModel.currentActionDataPoint!!)
        }
    }

    private fun deleteDataPoint(dataPoint: DataPoint) {
        val application = requireActivity().application
        val dao = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteDataPoint(dataPoint)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob.cancel()
    }
}
