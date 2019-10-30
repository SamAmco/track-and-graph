package com.samco.grapheasy.graphsandstats

import android.app.Activity
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.samco.grapheasy.R
import com.samco.grapheasy.database.GraphOrStat
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.database.GraphEasyDatabaseDao
import com.samco.grapheasy.databinding.GraphsAndStatsFragmentBinding
import kotlinx.coroutines.*

class GraphsAndStatsFragment : Fragment() {
    private var navController: NavController? = null
    private lateinit var viewModel: GraphsAndStatsViewModel
    private lateinit var binding: GraphsAndStatsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()
        viewModel = ViewModelProviders.of(this).get(GraphsAndStatsViewModel::class.java)
        binding = DataBindingUtil.inflate(inflater, R.layout.graphs_and_stats_fragment, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProviders.of(this).get(GraphsAndStatsViewModel::class.java)
        viewModel.initViewModel(requireActivity())
        val adapter = GraphStatAdapter(
            GraphStatClickListener(
                this::onDeleteGraphStat,
                this::onEditGraphStat
            ),
            activity!!.application
        )
        binding.graphStatList.adapter = adapter
        binding.graphStatList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        observeGraphStatsAndUpdate(adapter)

        setHasOptionsMenu(true)
        return binding.root
    }

    private fun onDeleteGraphStat(graphOrStat: GraphOrStat) = viewModel.deleteGraphStat(graphOrStat)

    //TODO onEditGraphStat
    private fun onEditGraphStat(graphOrStat: GraphOrStat) { }

    private fun observeGraphStatsAndUpdate(adapter: GraphStatAdapter) {
        viewModel.graphStatDataSamplers.observe(viewLifecycleOwner, Observer {
            it?.let { adapter.submitList(it) }
        })
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.graphStatList.smoothScrollToPosition(0)
            }
        })
    }

    private fun onAddClicked() {
        navController?.navigate(
            GraphsAndStatsFragmentDirections.actionGraphStatInput()
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.graphs_and_stats_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> onAddClicked()
        }
        return super.onOptionsItemSelected(item)
    }

}

class GraphsAndStatsViewModel : ViewModel() {
    private var dataSource: GraphEasyDatabaseDao? = null
    lateinit var graphStatDataSamplers: LiveData<List<GraphOrStat>>
    private val job = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + job)

    fun initViewModel(activity: Activity) {
        if (dataSource != null) return
        dataSource = GraphEasyDatabase.getInstance(activity.application).graphEasyDatabaseDao
        graphStatDataSamplers = dataSource!!.getDataSamplerSpecs()
    }

    fun deleteGraphStat(graphOrStat: GraphOrStat) {
        ioScope.launch { dataSource?.deleteGraphOrStat(graphOrStat) }
    }
}
