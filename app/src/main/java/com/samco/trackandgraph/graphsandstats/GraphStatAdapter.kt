package com.samco.trackandgraph.graphsandstats

import android.app.Application
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.GraphOrStat
import com.samco.trackandgraph.database.GraphStatType
import com.samco.trackandgraph.ui.GraphStatCardView
import com.samco.trackandgraph.ui.OrderedListAdapter
import kotlinx.coroutines.*

class GraphStatAdapter(private val clickListener: GraphStatClickListener, application: Application)
    : OrderedListAdapter<GraphOrStat, GraphStatViewHolder>(GraphStatDiffCallback()) {
    private val dataSource = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao

    override fun onBindViewHolder(holder: GraphStatViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener, dataSource)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GraphStatViewHolder {
        return GraphStatViewHolder.from(parent)
    }
}

class GraphStatViewHolder(private val graphStatView: GraphStatCardView)
    : RecyclerView.ViewHolder(graphStatView), PopupMenu.OnMenuItemClickListener {
    private var currJob: Job? = null
    private var clickListener: GraphStatClickListener? = null
    private var graphStat: GraphOrStat? = null
    private var dropElevation = 0f

    fun bind(graphStat: GraphOrStat, clickListener: GraphStatClickListener, dataSource: TrackAndGraphDatabaseDao) {
        this.dropElevation = graphStatView.cardView.cardElevation
        this.graphStat = graphStat
        this.clickListener = clickListener
        currJob?.cancel()
        currJob = Job()
        graphStatView.menuButtonClickListener = { v -> createContextMenu(v) }
        graphStatView.setOnClickListener { clickListener.onClick(graphStat) }
        CoroutineScope(Dispatchers.Main + currJob!!).launch {
            if (!when (graphStat.type) {
                    GraphStatType.LINE_GRAPH -> tryInitLineGraph(dataSource, graphStat)
                    GraphStatType.PIE_CHART -> tryInitPieChart(dataSource, graphStat)
                    GraphStatType.TIME_SINCE -> tryInitTimeSinceStat(dataSource, graphStat)
                    GraphStatType.AVERAGE_TIME_BETWEEN -> tryInitAverageTimeBetween(dataSource, graphStat)
                }) graphStatView.initError(graphStat, R.string.graph_stat_view_not_found)
        }
    }

    fun elevateCard() {
        graphStatView.cardView.postDelayed({
            graphStatView.cardView.cardElevation = graphStatView.cardView.cardElevation * 3f
        }, 10)
    }

    fun dropCard() {
        graphStatView.cardView.cardElevation = dropElevation
    }

    private fun createContextMenu(view: View) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.edit_graph_stat_context_menu, popup.menu)
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        graphStat?.let {
            when (item?.itemId) {
                R.id.edit -> clickListener?.onEdit(it)
                R.id.delete -> clickListener?.onDelete(it)
                else -> {}
            }
        }
        return false
    }

    private suspend fun tryInitPieChart(dataSource: TrackAndGraphDatabaseDao, graphStat: GraphOrStat): Boolean {
        val pieChart = withContext(Dispatchers.IO) {
            dataSource.getPieChartByGraphStatId(graphStat.id)
        } ?: return false
        graphStatView.initFromPieChart(graphStat, pieChart)
        return true
    }

    private suspend fun tryInitAverageTimeBetween(dataSource: TrackAndGraphDatabaseDao, graphStat: GraphOrStat): Boolean {
        val avTimeStat = withContext(Dispatchers.IO) {
            dataSource.getAverageTimeBetweenStatByGraphStatId(graphStat.id)
        } ?: return false
        graphStatView.initAverageTimeBetweenStat(graphStat, avTimeStat)
        return true
    }

    private suspend fun tryInitTimeSinceStat(dataSource: TrackAndGraphDatabaseDao, graphStat: GraphOrStat): Boolean {
        val timeSinceStat = withContext(Dispatchers.IO) {
            dataSource.getTimeSinceLastStatByGraphStatId(graphStat.id)
        } ?: return false
        graphStatView.initTimeSinceStat(graphStat, timeSinceStat)
        return true
    }

    private suspend fun tryInitLineGraph(dataSource: TrackAndGraphDatabaseDao, graphStat: GraphOrStat): Boolean {
        val lineGraph = withContext(Dispatchers.IO) {
            dataSource.getLineGraphByGraphStatId(graphStat.id)
        } ?: return false
        graphStatView.initFromLineGraph(graphStat, lineGraph, true)
        return true
    }

    companion object {
        fun from(parent: ViewGroup): GraphStatViewHolder {
            val graphStat = GraphStatCardView(parent.context)
            graphStat.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            return GraphStatViewHolder(graphStat)
        }
    }
}

class GraphStatDiffCallback : DiffUtil.ItemCallback<GraphOrStat>() {
    override fun areItemsTheSame(oldItem: GraphOrStat, newItem: GraphOrStat) = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: GraphOrStat, newItem: GraphOrStat) = oldItem == newItem
}

class GraphStatClickListener(private val onDelete: (graphStat: GraphOrStat) -> Unit,
                             private val onEdit: (graphStat: GraphOrStat) -> Unit,
                             private val onClick: (graphStat: GraphOrStat) -> Unit
                             ) {
    fun onDelete(graphStat: GraphOrStat) = onDelete.invoke(graphStat)
    fun onEdit(graphStat: GraphOrStat) = onEdit.invoke(graphStat)
    fun onClick(graphStat: GraphOrStat) = onClick.invoke(graphStat)
}