package com.samco.grapheasy.graphsandstats

import android.app.Activity
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.samco.grapheasy.R
import com.samco.grapheasy.database.DataSamplerSpec
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.database.GraphEasyDatabaseDao
import com.samco.grapheasy.databinding.GraphsAndStatsFragmentBinding

class GraphsAndStatsFragment : Fragment() {
    private lateinit var viewModel: GraphsAndStatsViewModel
    private lateinit var binding: GraphsAndStatsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProviders.of(this).get(GraphsAndStatsViewModel::class.java)
        binding = DataBindingUtil.inflate(inflater, R.layout.graphs_and_stats_fragment, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProviders.of(this).get(GraphsAndStatsViewModel::class.java)
        viewModel.initViewModel(requireActivity())
        val adapter = GraphStatAdapter(GraphStatClickListener())
        binding.graphStatList.adapter = adapter
        binding.graphStatList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        observeGraphStatsAndUpdate(adapter)
        return binding.root
    }

    private fun observeGraphStatsAndUpdate(adapter: GraphStatAdapter) {
        viewModel.graphStatDataSamplers.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it.map { s -> GraphStat.fromDataSamplerSpec(s) })
            }
        })
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.graphStatList.smoothScrollToPosition(0)
            }
        })
    }
}

class GraphsAndStatsViewModel : ViewModel() {
    private var dataSource: GraphEasyDatabaseDao? = null
    lateinit var graphStatDataSamplers: LiveData<List<DataSamplerSpec>>

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource = GraphEasyDatabase.getInstance(activity.application).graphEasyDatabaseDao
        graphStatDataSamplers = dataSource!!.getDataSamplerSpecs()
    }
}
