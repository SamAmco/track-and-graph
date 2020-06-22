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
import android.widget.LinearLayout
import com.androidplot.Plot
import com.androidplot.ui.HorizontalPositioning
import com.androidplot.ui.Size
import com.androidplot.ui.VerticalPositioning
import com.androidplot.xy.*
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import kotlinx.coroutines.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.database.entity.*
import com.samco.trackandgraph.util.getColorFromAttr
import org.threeten.bp.OffsetDateTime

class GraphStatView : LinearLayout, IDecoratableGraphStatView {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrSet: AttributeSet) : super(context, attrSet)

    private var binding = GraphStatViewBinding.inflate(LayoutInflater.from(context), this, true)
    override fun getBinding() = binding

    //TODO we shouldn't have a job inside a view. When we change configuration we don't want to have
    // to set up the view all over again. We need a view model somewhere for this view
    private var viewJob: Job? = null
    private var viewScope: CoroutineScope? = null
    private var decorJob: Job? = null

    //TODO should this data source come from somewhere else? Seems a bit weird to get a database ref
    // in a view
    private val dataSource =
        TrackAndGraphDatabase.getInstance(context.applicationContext).trackAndGraphDatabaseDao

    private var currentDecorator: IGraphStatViewDecorator? = null

    override fun getDataSource() = dataSource

    init {
        basicLineGraphSetup()
        initError(null, R.string.graph_stat_view_invalid_setup)
    }

    private fun resetJob() {
        decorJob?.cancel()
        decorJob = null
        viewJob?.cancel()
        viewJob = Job()
        viewScope = CoroutineScope(Dispatchers.Main + viewJob!!)
    }

    private fun basicLineGraphSetup() {
        binding.lineGraph.layoutManager.remove(binding.lineGraph.domainTitle)
        binding.lineGraph.layoutManager.remove(binding.lineGraph.rangeTitle)
        binding.lineGraph.layoutManager.remove(binding.lineGraph.title)
        binding.lineGraph.setBorderStyle(Plot.BorderStyle.NONE, null, null)
        binding.lineGraph.graph.size = Size.FILL
        binding.lineGraph.graph.position(
            0f,
            HorizontalPositioning.ABSOLUTE_FROM_LEFT,
            0f,
            VerticalPositioning.ABSOLUTE_FROM_TOP
        )
        binding.lineGraph.setPlotMargins(0f, 0f, 0f, 0f)
        binding.lineGraph.setPlotPadding(0f, 0f, 0f, 0f)
        binding.lineGraph.graph.setPadding(0f, 0f, 0f, 0f)
        binding.lineGraph.graph.setMargins(0f, 20f, 0f, 50f)

        val colorOnSurface = context.getColorFromAttr(R.attr.colorOnSurface)
        val colorSurface = context.getColorFromAttr(R.attr.colorSurface)
        binding.lineGraph.graph.domainGridLinePaint.color = colorOnSurface
        binding.lineGraph.graph.rangeGridLinePaint.color = colorOnSurface
        binding.lineGraph.graph.domainSubGridLinePaint.color = colorOnSurface
        binding.lineGraph.graph.rangeSubGridLinePaint.color = colorOnSurface
        binding.lineGraph.graph.domainOriginLinePaint.color = colorOnSurface
        binding.lineGraph.graph.domainOriginLinePaint.strokeWidth = 1f
        binding.lineGraph.graph.rangeOriginLinePaint.color = colorOnSurface
        binding.lineGraph.graph.rangeOriginLinePaint.strokeWidth = 1f

        binding.lineGraph.graph.gridBackgroundPaint.color = colorSurface
    }

    fun addLineGraphPanAndZoom() {
        PanZoom.attach(binding.lineGraph, PanZoom.Pan.HORIZONTAL, PanZoom.Zoom.STRETCH_HORIZONTAL)
    }

    private fun cleanAllViews() {
        binding.legendFlexboxLayout.removeAllViews()
        binding.lineGraph.clear()
        binding.lineGraph.removeMarkers()
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

    fun placeMarker(time: OffsetDateTime) {
        viewScope?.launch {
            decorJob?.join()
            currentDecorator?.setTimeMarker(time)
        }
    }

    private fun trySetDecorator(
        graphOrStat: GraphOrStat,
        decorator: IGraphStatViewDecorator
    ) {
        resetJob()
        decorJob = viewScope?.launch {
            try {
                cleanAllViews()
                currentDecorator = decorator
                decorator.decorate(this@GraphStatView)
            } catch (exception: Exception) {
                val initException =
                    (if (exception is GraphStatInitException) exception else null) ?: return@launch
                val errorTextId = initException.errorTextId
                cleanAllViews()
                val errorDecorator = GraphStatErrorDecorator(graphOrStat, errorTextId)
                currentDecorator = errorDecorator
                errorDecorator.decorate(this@GraphStatView)
            }
        }
    }

    fun initError(graphOrStat: GraphOrStat?, errorTextId: Int) {
        resetJob()
        decorJob = viewScope?.launch {
            cleanAllViews()
            val errorDecorator = GraphStatErrorDecorator(graphOrStat, errorTextId)
            currentDecorator = errorDecorator
            errorDecorator.decorate(this@GraphStatView)
        }
    }

    fun initTimeSinceStat(
        graphOrStat: GraphOrStat,
        timeSinceLastStat: TimeSinceLastStat,
        onSampledDataCallback: SampleDataCallback? = null
    ) {
        trySetDecorator(
            graphOrStat,
            GraphStatTimeSinceDecorator(graphOrStat, timeSinceLastStat, onSampledDataCallback)
        )
    }

    fun initAverageTimeBetweenStat(
        graphOrStat: GraphOrStat,
        timeBetweenStat: AverageTimeBetweenStat,
        onSampledDataCallback: SampleDataCallback? = null
    ) {
        trySetDecorator(
            graphOrStat,
            GraphStatAverageTimeBetweenDecorator(
                graphOrStat,
                timeBetweenStat,
                onSampledDataCallback
            )
        )
    }

    fun initFromLineGraph(
        graphOrStat: GraphOrStat,
        lineGraph: LineGraphWithFeatures,
        listViewMode: Boolean = false,
        onSampledDataCallback: SampleDataCallback? = null
    ) {
        trySetDecorator(
            graphOrStat,
            GraphStatLineGraphDecorator(graphOrStat, lineGraph, listViewMode, onSampledDataCallback)
        )
    }

    fun initFromPieChart(
        graphOrStat: GraphOrStat,
        pieChart: PieChart,
        onSampledDataCallback: SampleDataCallback? = null
    ) {
        trySetDecorator(
            graphOrStat,
            GraphStatPieChartDecorator(graphOrStat, pieChart, onSampledDataCallback)
        )
    }

    fun dispose() = viewJob?.cancel()
}
