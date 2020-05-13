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

class GraphStatView : FrameLayout, IDecoratableGraphStatView {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrSet: AttributeSet) : super(context, attrSet)

    private var binding = GraphStatViewBinding.inflate(LayoutInflater.from(context), this, true)
    override fun getBinding() = binding
    private var currJob: Job? = null
    private var viewScope: CoroutineScope? = null
    //TODO should this data source come from somewhere else? Seems a bit weird to get a database ref
    // in a view
    private val dataSource = TrackAndGraphDatabase.getInstance(context.applicationContext).trackAndGraphDatabaseDao
    override fun getDataSource() = dataSource

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
                decorator.decorate(this@GraphStatView)
            } catch (exception: Exception) {
                cleanAllViews()
                val initException = if (exception is GraphStatInitException) exception else null
                val errorTextId = initException?.errorTextId ?: R.string.graph_stat_view_unkown_error
                GraphStatErrorDecorator(graphOrStat, errorTextId).decorate(this@GraphStatView)
            }
        }
    }

    fun initError(graphOrStat: GraphOrStat?, errorTextId: Int) {
        resetJob()
        viewScope?.launch {
            cleanAllViews()
            GraphStatErrorDecorator(graphOrStat, errorTextId).decorate(this@GraphStatView)
        }
    }

    fun initTimeSinceStat(graphOrStat: GraphOrStat, timeSinceLastStat: TimeSinceLastStat) {
        trySetDecorator(
            graphOrStat,
            GraphStatTimeSinceDecorator(graphOrStat, timeSinceLastStat)
        )
    }

    fun initAverageTimeBetweenStat(graphOrStat: GraphOrStat, timeBetweenStat: AverageTimeBetweenStat) {
        trySetDecorator(
            graphOrStat,
            GraphStatAverageTimeBetweenDecorator(graphOrStat, timeBetweenStat)
        )
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

    fun dispose() = currJob?.cancel()
}
