package com.samco.trackandgraph.graphsandstats

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.GraphOrStat
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.GraphStatType
import com.samco.trackandgraph.databinding.GraphsAndStatsFragmentBinding
import com.samco.trackandgraph.ui.GroupViewHolder
import kotlinx.coroutines.*
import timber.log.Timber

class GraphsAndStatsFragment : Fragment() {
    private var navController: NavController? = null
    private val args: GraphsAndStatsFragmentArgs by navArgs()
    private lateinit var viewModel: GraphsAndStatsViewModel
    private lateinit var binding: GraphsAndStatsFragmentBinding
    private lateinit var adapter: GraphStatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.navController = container?.findNavController()
        viewModel = ViewModelProviders.of(this).get(GraphsAndStatsViewModel::class.java)
        binding = DataBindingUtil.inflate(inflater, R.layout.graphs_and_stats_fragment, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProviders.of(this).get(GraphsAndStatsViewModel::class.java)
        viewModel.initViewModel(requireActivity(), args.graphStatGroupId)

        adapter = GraphStatAdapter(
            GraphStatClickListener(
                viewModel::deleteGraphStat,
                this::onEditGraphStat,
                this::onGraphStatClicked
            ),
            activity!!.application
        )
        binding.graphStatList.adapter = adapter
        ItemTouchHelper(getDragTouchHelper()).attachToRecyclerView(binding.graphStatList)
        binding.graphStatList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        listenToViewModelState()

        setHasOptionsMenu(true)
        return binding.root
    }

    private fun getDragTouchHelper() = object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG, ItemTouchHelper.UP or ItemTouchHelper.DOWN)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (viewHolder != null && viewHolder is GraphStatViewHolder && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder.elevateCard()
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            (viewHolder as GraphStatViewHolder).dropCard()
            viewModel.adjustDisplayIndexes(adapter.getItems())
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }
    }

    private fun onGraphStatClicked(graphOrStat: GraphOrStat) {
        navController?.navigate(GraphsAndStatsFragmentDirections.actionViewGraphStat(graphOrStat.id))
    }

    private fun listenToViewModelState() {
        viewModel.state.observe(this, Observer {
            when (it) {
                GraphsAndStatsViewState.INITIALIZING -> {
                    binding.graphStatsProgressBar.visibility = View.VISIBLE
                }
                GraphsAndStatsViewState.WAITING -> {
                    binding.graphStatsProgressBar.visibility = View.GONE
                    observeGraphStatsAndUpdate(adapter)
                }
            }
        })
    }

    private fun onEditGraphStat(graphOrStat: GraphOrStat) {
        navController?.navigate(
            GraphsAndStatsFragmentDirections.actionGraphStatInput(
                graphStatGroupId = args.graphStatGroupId,
                graphStatId = graphOrStat.id
            )
        )
    }

    private fun observeGraphStatsAndUpdate(adapter: GraphStatAdapter) {
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.graphStatList.smoothScrollToPosition(0)
            }
        })
        viewModel.graphStats.observe(viewLifecycleOwner, Observer {
            it?.let { adapter.submitList(it.toMutableList()) }
        })
    }

    private fun onAddClicked() {
        navController?.navigate(
            GraphsAndStatsFragmentDirections.actionGraphStatInput(graphStatGroupId = args.graphStatGroupId)
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

enum class GraphsAndStatsViewState { INITIALIZING, WAITING }
class GraphsAndStatsViewModel : ViewModel() {
    private var dataSource: TrackAndGraphDatabaseDao? = null
    lateinit var graphStats: LiveData<List<GraphOrStat>>

    private val job = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + job)

    val state: LiveData<GraphsAndStatsViewState> get() { return _state }
    private val _state = MutableLiveData<GraphsAndStatsViewState>(GraphsAndStatsViewState.INITIALIZING)


    fun initViewModel(activity: Activity, graphStatGroupId: Long) {
        if (dataSource != null) return
        _state.value = GraphsAndStatsViewState.INITIALIZING
        dataSource = TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        graphStats = dataSource!!.getGraphsAndStatsByGroupId(graphStatGroupId)
        preenGraphStats()
    }

    private fun preenGraphStats() {
        ioScope.launch {
            val graphStats = dataSource!!.getAllGraphStatsSync()
            graphStats.forEach {
                when (it.type) {
                    GraphStatType.LINE_GRAPH -> if (preenLineGraph(it)) dataSource!!.deleteGraphOrStat(it)
                    GraphStatType.PIE_CHART -> if (preenPieChart(it)) dataSource!!.deleteGraphOrStat(it)
                    GraphStatType.AVERAGE_TIME_BETWEEN -> if (preenAverageTimeBetween(it)) dataSource!!.deleteGraphOrStat(it)
                    GraphStatType.TIME_SINCE -> if (preenTimeSince(it)) dataSource!!.deleteGraphOrStat(it)
                }
            }
            withContext(Dispatchers.Main) { _state.value = GraphsAndStatsViewState.WAITING }
        }
    }

    private fun preenLineGraph(graphOrStat: GraphOrStat): Boolean {
        val lineGraph = dataSource!!.getLineGraphByGraphStatId(graphOrStat.id) ?: return true
        return lineGraph.features.any { dataSource!!.tryGetFeatureById(it.featureId) == null }
    }

    private fun preenPieChart(graphOrStat: GraphOrStat): Boolean {
        return dataSource!!.getPieChartByGraphStatId(graphOrStat.id) == null
    }

    private fun preenAverageTimeBetween(graphOrStat: GraphOrStat): Boolean {
        return dataSource!!.getAverageTimeBetweenStatByGraphStatId(graphOrStat.id) == null
    }

    private fun preenTimeSince(graphOrStat: GraphOrStat): Boolean {
        return dataSource!!.getTimeSinceLastStatByGraphStatId(graphOrStat.id) == null
    }

    fun deleteGraphStat(graphOrStat: GraphOrStat) {
        ioScope.launch { dataSource?.deleteGraphOrStat(graphOrStat) }
    }

    fun adjustDisplayIndexes(graphStats: List<GraphOrStat>) = ioScope.launch {
        val newGraphStats = graphStats.mapIndexed { i, gs -> gs.copy(displayIndex = i) }
        dataSource?.updateGraphStats(newGraphStats)
    }
}