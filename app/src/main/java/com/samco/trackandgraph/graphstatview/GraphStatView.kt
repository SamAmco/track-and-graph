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
package com.samco.trackandgraph.graphstatview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.androidplot.Plot
import com.androidplot.ui.HorizontalPositioning
import com.androidplot.ui.Size
import com.androidplot.ui.VerticalPositioning
import com.androidplot.xy.*
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import kotlinx.coroutines.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class GraphStatView : FrameLayout, IDecoratableGraphStatView {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrSet: AttributeSet) : super(context, attrSet)

    private var binding = GraphStatViewBinding.inflate(LayoutInflater.from(context), this, true)
    override fun getBinding() = binding
    private var currJob: Job? = null
    private var viewScope: CoroutineScope? = null
    private val dataSource = TrackAndGraphDatabase.getInstance(context.applicationContext).trackAndGraphDatabaseDao
    override fun getDataSource() = dataSource
    private var currentDecorator: IGraphStatViewDecorator? = null

    init {
        basicLineGraphSetup()
        initError(null, R.string.graph_stat_view_invalid_setup)
    }

    private fun resetJob() {
        currJob?.cancel()
        currJob = Job()
        viewScope = CoroutineScope(Dispatchers.Main + currJob!!)
    }

    private fun basicLineGraphSetup() {
        binding.lineGraph.layoutManager.remove(binding.lineGraph.domainTitle)
        binding.lineGraph.layoutManager.remove(binding.lineGraph.rangeTitle)
        binding.lineGraph.layoutManager.remove(binding.lineGraph.title)
        binding.lineGraph.setBorderStyle(Plot.BorderStyle.NONE, null, null)
        binding.lineGraph.graph.size = Size.FILL
        binding.lineGraph.graph.position(0f, HorizontalPositioning.ABSOLUTE_FROM_LEFT, 0f, VerticalPositioning.ABSOLUTE_FROM_TOP)
        binding.lineGraph.setPlotMargins(0f, 0f, 0f, 0f)
        binding.lineGraph.setPlotPadding(0f, 0f, 0f, 0f)
        binding.lineGraph.graph.setPadding(0f,0f,0f,0f)
        binding.lineGraph.graph.setMargins(0f, 20f, 0f, 50f)
    }

    fun addLineGraphPanAndZoom() {
        PanZoom.attach(binding.lineGraph, PanZoom.Pan.HORIZONTAL, PanZoom.Zoom.STRETCH_HORIZONTAL)
    }

    private fun cleanAllViews() {
        binding.legendFlexboxLayout.removeAllViews()
        binding.lineGraph.clear()
        binding.lineGraph.graph.paddingLeft = 0f
        binding.lineGraph.setRangeBoundaries(0, 1, BoundaryMode.AUTO)
        binding.lineGraph.setDomainBoundaries(0, 1, BoundaryMode.GROW)
        binding.lineGraph.graph.refreshLayout()
        binding.pieChart.clear()
        binding.progressBar.visibility = View.GONE
        binding.lineGraph.visibility = View.GONE
        binding.pieChart.visibility = View.GONE
        binding.errorMessage.visibility = View.GONE
        binding.statMessage.visibility = View.GONE
        binding.errorMessage.text = ""
        binding.headerText.text = ""
        binding.statMessage.text = ""
    }

    fun initLoading() {
        resetJob()
        cleanAllViews()
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun trySetDecorator(graphOrStat: GraphOrStat, decorator: IGraphStatViewDecorator) {
        resetJob()
        viewScope?.launch {
            try {
                cleanAllViews()
                this@GraphStatView.currentDecorator = decorator
                currentDecorator?.decorate(this@GraphStatView)
            } catch (exception: Exception) {
                cleanAllViews()
                val initException = if (exception is GraphStatInitException) exception else null
                val errorTextId = initException?.errorTextId ?: R.string.graph_stat_view_unkown_error
                this@GraphStatView.currentDecorator = GraphStatErrorDecorator(graphOrStat, errorTextId)
                currentDecorator?.decorate(this@GraphStatView)
            }
        }
    }

    fun initError(graphOrStat: GraphOrStat?, errorTextId: Int) {
        resetJob()
        viewScope?.launch {
            cleanAllViews()
            currentDecorator = GraphStatErrorDecorator(graphOrStat, errorTextId)
            currentDecorator?.decorate(this@GraphStatView)
        }
    }

    fun initTimeSinceStat(graphOrStat: GraphOrStat, timeSinceLastStat: TimeSinceLastStat) {
        try {
            resetJob()
            cleanAllViews()
            binding.statMessage.visibility = View.INVISIBLE
            initHeader(binding, graphOrStat)
            viewScope!!.launch { initTimeSinceStatBody(graphOrStat, timeSinceLastStat) }
        } catch (e: Exception) {
            initError(graphOrStat, R.string.graph_stat_view_unkown_error)
        }
    }

    private suspend fun initTimeSinceStatBody(graphOrStat: GraphOrStat, timeSinceLastStat: TimeSinceLastStat) {
        binding.progressBar.visibility = View.VISIBLE
        val feature = withContext(Dispatchers.IO) { dataSource.getFeatureById(timeSinceLastStat.featureId) }
        var lastDataPointTimeStamp: OffsetDateTime? = null
        val lastDataPoint = withContext(Dispatchers.IO) {
            dataSource.getLastDataPointBetween(feature.id, timeSinceLastStat.fromValue, timeSinceLastStat.toValue)
        }
        if (lastDataPoint != null) lastDataPointTimeStamp = lastDataPoint.timestamp
        binding.statMessage.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        if (lastDataPointTimeStamp == null) { initError(graphOrStat, R.string.graph_stat_view_not_enough_data_stat) }
        else {
            while (true) {
                setTimeSinceStatText(Duration.between(lastDataPointTimeStamp, OffsetDateTime.now()))
                delay(1000)
                yield()
            }
        }
    }

    fun initAverageTimeBetweenStat(graphOrStat: GraphOrStat, timeBetweenStat: AverageTimeBetweenStat) {
        try {
            resetJob()
            cleanAllViews()
            binding.statMessage.visibility = View.INVISIBLE
            initHeader(binding, graphOrStat)
            viewScope!!.launch { initAverageTimeBetweenStatBody(graphOrStat, timeBetweenStat) }
        } catch (e: Exception) { initError(graphOrStat, R.string.graph_stat_view_unkown_error) }
    }

    private suspend fun initAverageTimeBetweenStatBody(graphOrStat: GraphOrStat, timeBetweenStat: AverageTimeBetweenStat) {
        binding.progressBar.visibility = View.VISIBLE
        val feature = withContext(Dispatchers.IO) { dataSource.getFeatureById(timeBetweenStat.featureId) }
        val dataPoints = withContext(Dispatchers.IO) {
            if (timeBetweenStat.duration == null) {
                dataSource.getDataPointsBetween(feature.id, timeBetweenStat.fromValue, timeBetweenStat.toValue)
            } else {
                val now = OffsetDateTime.now()
                val cutOff = now.minus(timeBetweenStat.duration)
                dataSource.getDataPointsBetweenInTimeRange(feature.id, timeBetweenStat.fromValue, timeBetweenStat.toValue, cutOff, now)
            }
        }
        if (dataPoints.size < 2) initError(graphOrStat, R.string.graph_stat_view_not_enough_data_stat)
        else {
            val totalMillis = Duration.between(dataPoints.first().timestamp, dataPoints.last().timestamp).toMillis().toDouble()
            val averageMillis = totalMillis / dataPoints.size.toDouble()
            setAverageTimeBetweenStatText(averageMillis)
        }

        binding.statMessage.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    private fun setAverageTimeBetweenStatText(millis: Double) {
        val days = "%.1f".format((millis / 86400000).toFloat())
        binding.statMessage.text = "$days ${context.getString(R.string.days)}"
    }

    private fun setTimeSinceStatText(duration: Duration) {
        val totalSeconds = duration.toMillis() / 1000.toDouble()
        val daysNum = (totalSeconds / 86400).toInt()
        val days = daysNum.toString()
        val hours = "%02d".format(((totalSeconds % 86400) / 3600).toInt())
        val minutes = "%02d".format(((totalSeconds % 3600) / 60).toInt())
        val seconds = "%02d".format((totalSeconds % 60).toInt())
        val hms = "$hours:$minutes:$seconds"
        binding.statMessage.text =
            when {
                daysNum == 1 -> "$days ${context.getString(R.string.day)}\n$hms"
                daysNum > 0 -> "$days ${context.getString(R.string.days)}\n$hms"
                else -> hms
            }
    }

    fun initFromLineGraph(graphOrStat: GraphOrStat, lineGraph: LineGraph, listViewMode: Boolean = false) {
        trySetDecorator(
            graphOrStat,
            GraphStatLineGraphDecorator(graphOrStat, lineGraph, listViewMode)
        )
    }

    fun initFromPieChart(graphOrStat: GraphOrStat, pieChart: PieChart) {
        trySetDecorator(
            graphOrStat,
            GraphStatPieChartDecorator(graphOrStat, pieChart)
        )
    }

    fun dispose() { currJob?.cancel() }
}


