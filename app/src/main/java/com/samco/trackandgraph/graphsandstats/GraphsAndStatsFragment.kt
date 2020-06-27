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
package com.samco.trackandgraph.graphsandstats

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.withTransaction

import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.entity.GraphOrStat
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.entity.GraphStatType
import com.samco.trackandgraph.databinding.GraphsAndStatsFragmentBinding
import com.samco.trackandgraph.graphstatview.GraphStatCardView
import com.samco.trackandgraph.graphstatview.factories.AverageTimeBetweenDataFactory
import com.samco.trackandgraph.graphstatview.factories.LineGraphDataFactory
import com.samco.trackandgraph.graphstatview.factories.PieChartDataFactory
import com.samco.trackandgraph.graphstatview.factories.TimeSinceViewDataFactory
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import com.samco.trackandgraph.ui.*
import kotlinx.coroutines.*
import org.threeten.bp.Instant

class GraphsAndStatsFragment : Fragment() {
    private var navController: NavController? = null
    private val args: GraphsAndStatsFragmentArgs by navArgs()
    private val viewModel by viewModels<GraphsAndStatsViewModel>()
    private lateinit var binding: GraphsAndStatsFragmentBinding
    private lateinit var adapter: GraphStatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.navController = container?.findNavController()
        binding =
            DataBindingUtil.inflate(inflater, R.layout.graphs_and_stats_fragment, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val database = TrackAndGraphDatabase
            .getInstance(requireActivity().applicationContext)
        viewModel.initViewModel(database, args.graphStatGroupId)

        adapter = GraphStatAdapter(
            GraphStatClickListener(
                viewModel::deleteGraphStat,
                this::onEditGraphStat,
                this::onGraphStatClicked,
                this::onMoveGraphStatClicked,
                viewModel::duplicateGraphOrStat
            )
        )
        binding.graphStatList.adapter = adapter
        ItemTouchHelper(getDragTouchHelper()).attachToRecyclerView(binding.graphStatList)
        binding.graphStatList.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        listenToViewModelState()

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = args.graphStatGroupName
        return binding.root
    }

    private fun getDragTouchHelper() = object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeFlag(
                ItemTouchHelper.ACTION_STATE_DRAG,
                ItemTouchHelper.UP or ItemTouchHelper.DOWN
            )
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
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
            viewModel.adjustDisplayIndexes(adapter.getItems().map { it.second })
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    }

    private fun onMoveGraphStatClicked(graphOrStat: IGraphStatViewData) {
        val dialog = MoveToDialogFragment()
        val args = Bundle()
        args.putString(MOVE_DIALOG_TYPE_KEY, MOVE_DIALOG_TYPE_GRAPH)
        args.putLong(MOVE_DIALOG_GROUP_KEY, graphOrStat.graphOrStat.id)
        dialog.arguments = args
        childFragmentManager.let { dialog.show(it, "move_graph_group_dialog") }
    }

    private fun onGraphStatClicked(graphOrStat: IGraphStatViewData) {
        navController?.navigate(GraphsAndStatsFragmentDirections.actionViewGraphStat(graphOrStat.graphOrStat.id))
    }

    private fun listenToViewModelState() {
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (it) {
                GraphsAndStatsViewState.INITIALIZING -> {
                    binding.graphStatsProgressBar.visibility = View.VISIBLE
                }
                GraphsAndStatsViewState.NO_FEATURES -> {
                    setHasOptionsMenu(false)
                    binding.graphStatsProgressBar.visibility = View.GONE
                    binding.noGraphsHintText.text = getString(R.string.no_features_graph_stats_hint)
                }
                GraphsAndStatsViewState.WAITING -> {
                    binding.graphStatsProgressBar.visibility = View.GONE
                    observeGraphStatsAndUpdate(adapter)
                }
                else -> {
                }
            }
        })
    }

    private fun onEditGraphStat(graphOrStat: IGraphStatViewData) {
        navController?.navigate(
            GraphsAndStatsFragmentDirections.actionGraphStatInput(
                graphStatGroupId = args.graphStatGroupId,
                graphStatId = graphOrStat.graphOrStat.id
            )
        )
    }

    private fun observeGraphStatsAndUpdate(adapter: GraphStatAdapter) {
        viewModel.graphStats.observe(viewLifecycleOwner, Observer {
            it?.let { adapter.submitList(it.toMutableList()) }
            if (it.isNullOrEmpty()) {
                binding.noGraphsHintText.text = getString(R.string.no_graph_stats_hint)
                binding.noGraphsHintText.visibility = View.VISIBLE
            } else {
                binding.noGraphsHintText.visibility = View.INVISIBLE
            }
        })
        viewModel.scrollToTop.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.graphStatList.postDelayed({
                    binding.graphStatList.smoothScrollToPosition(0)
                }, 100)
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        for (i in 0..(binding.graphStatList.layoutManager?.childCount ?: 0)) {
            val child = binding.graphStatList.getChildAt(i)
            if (child is GraphStatCardView?) child?.dispose()
        }
    }
}

enum class GraphsAndStatsViewState { INITIALIZING, NO_FEATURES, WAITING }
class GraphsAndStatsViewModel : ViewModel() {
    private var database: TrackAndGraphDatabase? = null
    private var dataSource: TrackAndGraphDatabaseDao? = null
    val graphStats: LiveData<List<Pair<Instant, IGraphStatViewData>>>
        get() = _graphStats
    private val _graphStats = MutableLiveData<List<Pair<Instant, IGraphStatViewData>>>()

    val scrollToTop: LiveData<Boolean>
        get() = _scrollToTop
    private val _scrollToTop = MutableLiveData<Boolean>()

    private val job = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + job)
    private val workScope = CoroutineScope(Dispatchers.Default + job)

    val state: LiveData<GraphsAndStatsViewState>
        get() = _state
    private val _state = MutableLiveData(GraphsAndStatsViewState.INITIALIZING)

    private var graphsAndStatsObserver: Observer<List<GraphOrStat>>? = null

    fun initViewModel(database: TrackAndGraphDatabase, graphStatGroupId: Long) {
        if (this.database != null) return
        _state.value = GraphsAndStatsViewState.INITIALIZING
        this.database = database
        dataSource = database.trackAndGraphDatabaseDao
        ioScope.launch {
            preenGraphStats()
            withContext(Dispatchers.Main) { observeGraphsAndStats(graphStatGroupId) }
        }
    }

    private suspend fun preenGraphStats() {
        val graphStats = dataSource!!.getAllGraphStatsSync()
        graphStats.forEach {
            when (it.type) {
                GraphStatType.LINE_GRAPH ->
                    if (preenLineGraph(it)) dataSource!!.deleteGraphOrStat(it)
                GraphStatType.PIE_CHART ->
                    if (preenPieChart(it)) dataSource!!.deleteGraphOrStat(it)
                GraphStatType.AVERAGE_TIME_BETWEEN ->
                    if (preenAverageTimeBetween(it)) dataSource!!.deleteGraphOrStat(it)
                GraphStatType.TIME_SINCE ->
                    if (preenTimeSince(it)) dataSource!!.deleteGraphOrStat(it)
            }
        }
        val numFeatures = dataSource!!.getNumFeatures()
        withContext(Dispatchers.Main) {
            if (numFeatures <= 0) _state.value = GraphsAndStatsViewState.NO_FEATURES
            else _state.value = GraphsAndStatsViewState.WAITING
        }
    }

    private fun observeGraphsAndStats(graphStatGroupId: Long) {
        graphsAndStatsObserver = Observer { list ->
            val newFirst = list.firstOrNull()
            val oldFirst = _graphStats.value?.firstOrNull()?.second?.graphOrStat
            val moreItems = list.size > _graphStats.value?.size ?: 0
            if (_graphStats.value == null || (moreItems && newFirst != oldFirst)) {
                _scrollToTop.value = true
                _scrollToTop.value = false
            }
            workScope.launch { onNewGraphsAndStats(list) }
        }
        dataSource!!.getGraphsAndStatsByGroupId(graphStatGroupId)
            .observeForever(graphsAndStatsObserver!!)
    }

    private suspend fun onNewGraphsAndStats(graphsAndStats: List<GraphOrStat>) {
        if (!_graphStats.value.isNullOrEmpty() && graphsAndStats.size <= _graphStats.value!!.size) {
            iterateGraphStatDataFactories(graphsAndStats, true)
        } else {
            val loadingStates = graphsAndStats.map {
                when (it.type) {
                    GraphStatType.LINE_GRAPH -> Pair(Instant.now(), ILineGraphViewData.loading(it))
                    GraphStatType.PIE_CHART -> Pair(Instant.now(), IPieChartViewData.loading(it))
                    GraphStatType.TIME_SINCE -> Pair(Instant.now(), ITimeSinceViewData.loading(it))
                    GraphStatType.AVERAGE_TIME_BETWEEN ->
                        Pair(Instant.now(), IAverageTimeBetweenViewData.loading(it))
                }
            }
            withContext(Dispatchers.Main) { _graphStats.value = loadingStates }
            iterateGraphStatDataFactories(graphsAndStats, false)
        }
    }

    private suspend fun iterateGraphStatDataFactories(
        graphsAndStats: List<GraphOrStat>,
        batchUpdate: Boolean
    ) {
        val batch = mutableListOf<IGraphStatViewData>()
        for (index in graphsAndStats.indices) {
            val graphOrStat = graphsAndStats[index]
            val factory = when (graphOrStat.type) {
                GraphStatType.LINE_GRAPH -> LineGraphDataFactory(dataSource!!, graphOrStat)
                GraphStatType.PIE_CHART -> PieChartDataFactory(dataSource!!, graphOrStat)
                GraphStatType.TIME_SINCE -> TimeSinceViewDataFactory(dataSource!!, graphOrStat)
                GraphStatType.AVERAGE_TIME_BETWEEN ->
                    AverageTimeBetweenDataFactory(dataSource!!, graphOrStat)
            }
            val viewData = factory.getViewData()
            if (batchUpdate) batch.add(index, viewData)
            else withContext(Dispatchers.Main) { updateGraphStatData(index, viewData) }
        }
        if (batchUpdate) {
            val newItems = batch.map { Pair(Instant.now(), it) }
            withContext(Dispatchers.Main) { _graphStats.value = newItems }
        }
    }

    @Synchronized
    private fun updateGraphStatData(index: Int, data: IGraphStatViewData) {
        val currList = _graphStats.value ?: emptyList()
        if (index >= 0 && index < currList.size) {
            val newList = currList.toMutableList()
            newList.removeAt(index)
            newList.add(index, Pair(Instant.now(), data))
            _graphStats.value = newList
        }
    }

    private fun preenLineGraph(graphOrStat: GraphOrStat): Boolean {
        val lineGraph = dataSource!!.getLineGraphByGraphStatId(graphOrStat.id) ?: return true
        return lineGraph.features.any { dataSource!!.tryGetFeatureByIdSync(it.featureId) == null }
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

    fun deleteGraphStat(graphOrStat: IGraphStatViewData) {
        ioScope.launch { dataSource?.deleteGraphOrStat(graphOrStat.graphOrStat) }
    }

    fun duplicateGraphOrStat(graphOrStatViewData: IGraphStatViewData) {
        ioScope.launch {
            database?.withTransaction {
                duplicateGraphOrStat(graphOrStatViewData.graphOrStat)
            }
        }
    }

    private fun duplicateGraphOrStat(graphOrStat: GraphOrStat) {
        val originalId = graphOrStat.id
        val newId = dataSource!!.insertGraphOrStat(graphOrStat.copy(id = 0))
        when (graphOrStat.type) {
            GraphStatType.LINE_GRAPH -> {
                val lineGraph = dataSource!!.getLineGraphByGraphStatId(originalId)
                lineGraph?.let {
                    val copy = it.toLineGraph().copy(id = 0, graphStatId = newId)
                    val newLineGraphId = dataSource!!.insertLineGraph(copy)
                    val newFeatures = lineGraph.features.map { f ->
                        f.copy(id = 0, lineGraphId = newLineGraphId)
                    }
                    dataSource!!.insertLineGraphFeatures(newFeatures)
                }
            }
            GraphStatType.PIE_CHART -> {
                val pieChart = dataSource!!.getPieChartByGraphStatId(originalId)
                val copy = pieChart?.copy(id = 0, graphStatId = newId)
                copy?.let { dataSource!!.insertPieChart(it) }
            }
            GraphStatType.AVERAGE_TIME_BETWEEN -> {
                val avTimeStat =
                    dataSource!!.getAverageTimeBetweenStatByGraphStatId(originalId)
                val copy = avTimeStat?.copy(id = 0, graphStatId = newId)
                copy?.let { dataSource!!.insertAverageTimeBetweenStat(it) }
            }
            GraphStatType.TIME_SINCE -> {
                val timeSinceStat =
                    dataSource!!.getTimeSinceLastStatByGraphStatId(originalId)
                val copy = timeSinceStat?.copy(id = 0, graphStatId = newId)
                copy?.let { dataSource!!.insertTimeSinceLastStat(it) }
            }
        }
    }

    fun adjustDisplayIndexes(graphStats: List<IGraphStatViewData>) = ioScope.launch {
        val newGraphStats = graphStats.mapIndexed { i, gs -> gs.graphOrStat.copy(displayIndex = i) }
        dataSource?.updateGraphStats(newGraphStats)
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }
}
