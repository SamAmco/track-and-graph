package com.samco.grapheasy.graphsandstats

import android.app.Application
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.grapheasy.R
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.database.GraphEasyDatabaseDao
import com.samco.grapheasy.database.GraphOrStat
import com.samco.grapheasy.database.GraphStatType
import com.samco.grapheasy.ui.GraphStatView
import kotlinx.coroutines.*

class GraphStatAdapter(private val clickListener: GraphStatClickListener, application: Application)
    : ListAdapter<GraphOrStat, GraphStatAdapter.ViewHolder>(GraphStatDiffCallback()) {

    private val dataSource = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener, dataSource)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder(private val graphStatView: GraphStatView)
        : RecyclerView.ViewHolder(graphStatView), PopupMenu.OnMenuItemClickListener {
        private var currJob: Job? = null
        private var clickListener: GraphStatClickListener? = null
        private var graphStat: GraphOrStat? = null

        fun bind(graphStat: GraphOrStat, clickListener: GraphStatClickListener, dataSource: GraphEasyDatabaseDao) {
            this.graphStat = graphStat
            this.clickListener = clickListener
            currJob?.cancel()
            currJob = Job()
            graphStatView.clickListener = { v -> createContextMenu(v) }
            CoroutineScope(Dispatchers.Main + currJob!!).launch {
                if (!when (graphStat.type) {
                    GraphStatType.LINE_GRAPH -> tryInitLineGraph(dataSource, graphStat)
                    GraphStatType.PIE_CHART -> tryInitPieChart(dataSource, graphStat)
                    GraphStatType.TIME_SINCE -> tryInitTimeSinceStat(dataSource, graphStat)
                    GraphStatType.AVERAGE_TIME_BETWEEN -> tryInitAverageTimeBetween(dataSource, graphStat)
                }) graphStatView.initError(graphStat, R.string.graph_stat_view_not_found)
            }
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

        private suspend fun tryInitPieChart(dataSource: GraphEasyDatabaseDao, graphStat: GraphOrStat): Boolean {
            val pieChart = withContext(Dispatchers.IO) {
                dataSource.getPieChartByGraphStatId(graphStat.id)
            } ?: return false
            graphStatView.initFromPieChart(graphStat, pieChart)
            return true
        }

        private suspend fun tryInitAverageTimeBetween(dataSource: GraphEasyDatabaseDao, graphStat: GraphOrStat): Boolean {
            val avTimeStat = withContext(Dispatchers.IO) {
                dataSource.getAverageTimeBetweenStatByGraphStatId(graphStat.id)
            } ?: return false
            graphStatView.initAverageTimeBetweenStat(graphStat, avTimeStat)
            return true
        }

        private suspend fun tryInitTimeSinceStat(dataSource: GraphEasyDatabaseDao, graphStat: GraphOrStat): Boolean {
            val timeSinceStat = withContext(Dispatchers.IO) {
                dataSource.getTimeSinceLastStatByGraphStatId(graphStat.id)
            } ?: return false
            graphStatView.initTimeSinceStat(graphStat, timeSinceStat)
            return true
        }

        private suspend fun tryInitLineGraph(dataSource: GraphEasyDatabaseDao, graphStat: GraphOrStat): Boolean {
            val lineGraph = withContext(Dispatchers.IO) {
                dataSource.getLineGraphByGraphStatId(graphStat.id)
            } ?: return false
            graphStatView.initFromLineGraph(graphStat, lineGraph, true)
            return true
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val graphStat = GraphStatView(parent.context)
                graphStat.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                return ViewHolder(graphStat)
            }
        }
    }
}

class GraphStatDiffCallback() : DiffUtil.ItemCallback<GraphOrStat>() {
    //TODO GraphStatDiffCallback.areItemsTheSame
    override fun areItemsTheSame(oldItem: GraphOrStat, newItem: GraphOrStat) = false

    //TODO GraphStatDiffCallback.areContentsTheSame
    override fun areContentsTheSame(oldItem: GraphOrStat, newItem: GraphOrStat) = false
}

class GraphStatClickListener(private val onDelete: (graphStat: GraphOrStat) -> Unit,
                             private val onEdit: (graphStat: GraphOrStat) -> Unit) {
    fun onDelete(graphStat: GraphOrStat) = onDelete.invoke(graphStat)
    fun onEdit(graphStat: GraphOrStat) = onEdit.invoke(graphStat)
}