package com.samco.grapheasy.featurehistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.grapheasy.R
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.databinding.FragmentFeatureHistoryBinding

class FragmentFeatureHistory : Fragment() {
    private lateinit var binding: FragmentFeatureHistoryBinding
    private lateinit var viewModel: FeatureHistoryViewModel
    private val args: FragmentFeatureHistoryArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_feature_history, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel = createViewModel()
        val adapter = DataPointAdapter()
        observeFeatureDataAndUpdate(viewModel, adapter)
        binding.dataPointList.adapter = adapter
        binding.dataPointList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        return binding.root
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
}