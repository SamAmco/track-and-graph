package com.samco.grapheasy.graphsandstats

import android.app.Application
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.grapheasy.R
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.database.GraphEasyDatabaseDao
import com.samco.grapheasy.database.GraphOrStat
import com.samco.grapheasy.databinding.GraphStatViewBinding
import com.samco.grapheasy.ui.GraphStatView
import kotlinx.coroutines.*
import timber.log.Timber

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
        : RecyclerView.ViewHolder(graphStatView) {
        private var currJob: Job? = null

        fun bind(graphStat: GraphOrStat, clickListener: GraphStatClickListener, dataSouce: GraphEasyDatabaseDao) {
            currJob?.cancel()
            currJob = Job()
            graphStatView.clickListener = clickListener
            CoroutineScope(Dispatchers.Main + currJob!!).launch {
                if (tryInitLineGraph(dataSouce, graphStat)) return@launch
                if (tryInitPieChart(dataSouce, graphStat)) return@launch
                if (tryInitAverageTimeBetween(dataSouce, graphStat)) return@launch
                if (tryInitTimeSinceStat(dataSouce, graphStat)) return@launch
                else graphStatView.initError(graphStat, R.string.graph_stat_view_not_found)
            }
        }

        private suspend fun tryInitPieChart(dataSouce: GraphEasyDatabaseDao, graphStat: GraphOrStat): Boolean {
            val pieChart = withContext(Dispatchers.IO) { dataSouce.getPieChartByGraphStatId(graphStat.id) }
            if (pieChart != null) {
                Timber.d("Initing pie chart")
                graphStatView.initFromPieChart(graphStat, pieChart)
                return true
            }
            return false
        }

        private suspend fun tryInitAverageTimeBetween(dataSouce: GraphEasyDatabaseDao, graphStat: GraphOrStat): Boolean {
            val avTimeStat = withContext(Dispatchers.IO) { dataSouce.getAverageTimeBetweenStatByGraphStatId(graphStat.id) }
            if (avTimeStat != null) {
                Timber.d("Initing average time since stat")
                graphStatView.initAverageTimeBetweenStat(graphStat, avTimeStat)
                return true
            }
            return false
        }

        private suspend fun tryInitTimeSinceStat(dataSouce: GraphEasyDatabaseDao, graphStat: GraphOrStat): Boolean {
            val timeSinceStat = withContext(Dispatchers.IO) { dataSouce.getTimeSinceLastStatByGraphStatId(graphStat.id) }
            if (timeSinceStat != null) {
                Timber.d("Initing time since stat")
                graphStatView.initTimeSinceStat(graphStat, timeSinceStat)
                return true
            }
            return false
        }

        private suspend fun tryInitLineGraph(dataSouce: GraphEasyDatabaseDao, graphStat: GraphOrStat): Boolean {
            val lineGraph = withContext(Dispatchers.IO) { dataSouce.getLineGraphByGraphStatId(graphStat.id) }
            if (lineGraph != null) {
                Timber.d("Initing line graph")
                graphStatView.initFromLineGraph(graphStat, lineGraph)
                return true
            }
            return false
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                Timber.d("Creating new view")
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

class GraphStatClickListener