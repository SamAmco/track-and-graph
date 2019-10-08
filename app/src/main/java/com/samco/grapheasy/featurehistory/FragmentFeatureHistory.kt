package com.samco.grapheasy.featurehistory

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
import com.samco.grapheasy.R
import com.samco.grapheasy.database.DataPoint
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.databinding.FragmentFeatureHistoryBinding
import com.samco.grapheasy.displaytrackgroup.InputDataPointDialog
import com.samco.grapheasy.ui.YesCancelDialogFragment
import kotlinx.coroutines.*
import org.threeten.bp.OffsetDateTime

class FragmentFeatureHistory : Fragment(),
        YesCancelDialogFragment.YesCancelDialogListener,
        InputDataPointDialog.InputDataPointDialogListener {
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
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                viewModel.feature = dao.getFeatureById(args.feature)
            }
        }
    }

    private fun createViewModel(): FeatureHistoryViewModel {
        val application = requireNotNull(this.activity).application
        val dataSource = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
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
            viewModel.currentActionDataPoint = dataPoint
            val dialog = InputDataPointDialog()
            childFragmentManager.let { dialog.show(it, "input_data_point_dialog") }
        }
    }

    override fun onDataPointInput(dataPoint: DataPoint) {
        val application = requireActivity().application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            withContext(Dispatchers.IO) {
                dao.updateDataPoint(dataPoint)
            }
        }
    }

    override fun getFeature() = viewModel.feature!!

    override fun getDisplayDateTimeForInputDataPoint() = viewModel.currentActionDataPoint!!.timestamp

    override fun getIdForInputDataPoint() = viewModel.currentActionDataPoint!!.id

    override fun getValueForInputDataPoint() = viewModel.currentActionDataPoint!!.value

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
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
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
