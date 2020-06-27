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
import com.samco.trackandgraph.graphstatview.decorators.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import com.samco.trackandgraph.util.getColorFromAttr
import org.threeten.bp.OffsetDateTime
import java.text.DecimalFormat

class GraphStatView : LinearLayout, IDecoratableGraphStatView {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrSet: AttributeSet) : super(context, attrSet)

    private var binding = GraphStatViewBinding.inflate(LayoutInflater.from(context), this, true)
    override fun getBinding() = binding

    private var viewJob: Job? = null
    private var viewScope: CoroutineScope? = null
    private var decorJob: Job? = null

    private var currentDecorator: IGraphStatViewDecoratorBase? = null

    init {
        basicLineGraphSetup()
        initError(R.string.graph_stat_view_invalid_setup)
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
        binding.lineGraph.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).format =
            DecimalFormat("0.0")
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

    private fun <T : IGraphStatViewData> trySetDecorator(
        decorator: IGraphStatViewDecorator<T>,
        data: T
    ) {
        resetJob()
        decorJob = viewScope?.launch {
            cleanAllViews()
            try {
                decorate(data.graphOrStat, decorator, data)
            } catch (exception: Exception) {
                if (exception !is GraphStatInitException) return@launch
                cleanAllViews()
                currentDecorator = null
                val headerText = data.graphOrStat.name
                binding.headerText.text = headerText
                binding.errorMessage.visibility = View.VISIBLE
                binding.errorMessage.text = context.getString(exception.errorTextId)
            }
        }
    }

    private suspend fun <T : IGraphStatViewData> decorate(
        graphOrStat: GraphOrStat,
        decorator: IGraphStatViewDecorator<T>,
        data: T
    ) {
        currentDecorator = decorator
        val headerText = graphOrStat.name
        binding.headerText.text = headerText
        when (data.state) {
            IGraphStatViewData.State.LOADING -> binding.progressBar.visibility = View.VISIBLE
            IGraphStatViewData.State.READY -> decorator.decorate(this@GraphStatView, data)
            IGraphStatViewData.State.ERROR -> throw data.error!!
        }
    }

    fun initError(errorTextId: Int) {
        resetJob()
        decorJob = viewScope?.launch {
            cleanAllViews()
            binding.errorMessage.visibility = View.VISIBLE
            binding.errorMessage.text = context.getString(errorTextId)
        }
    }

    fun initFromGraphStat(data: IGraphStatViewData, listMode: Boolean = false) {
        //TODO what if it's just a loading state, the cast might cause a crash
        when (data.graphOrStat.type) {
            GraphStatType.LINE_GRAPH -> {
                trySetDecorator(
                    GraphStatLineGraphDecorator(listMode),
                    data as ILineGraphViewData
                )
            }
            GraphStatType.PIE_CHART -> {
                trySetDecorator(
                    GraphStatPieChartDecorator(),
                    data as IPieChartViewData
                )
            }
            GraphStatType.AVERAGE_TIME_BETWEEN -> {
                trySetDecorator(
                    GraphStatAverageTimeBetweenDecorator(),
                    data as IAverageTimeBetweenViewData
                )
            }
            GraphStatType.TIME_SINCE -> {
                trySetDecorator(
                    GraphStatTimeSinceDecorator(),
                    data as ITimeSinceViewData
                )
            }
        }
    }

    fun dispose() = viewJob?.cancel()
}
