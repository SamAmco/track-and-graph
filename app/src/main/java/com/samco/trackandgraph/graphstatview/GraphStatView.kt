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
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import com.androidplot.Plot
import com.androidplot.ui.*
import com.androidplot.xy.*
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.graphstatview.decorators.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import com.samco.trackandgraph.util.getColorFromAttr
import org.threeten.bp.OffsetDateTime
import java.text.DecimalFormat

class GraphStatView : FrameLayout, IDecoratableGraphStatView {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrSet: AttributeSet) : super(context, attrSet)

    private var binding = GraphStatViewBinding.inflate(LayoutInflater.from(context), this, true)
    override fun getBinding() = binding

    private var currentDecorator: IGraphStatViewDecorator? = null

    init {
        xyPlotSetup()
    }

    fun setGraphHeight(height: Int) {
        binding.xyPlot.layoutParams.height = height
        binding.pieChart.layoutParams.height = height
        requestLayout()
        invalidate()
    }

    fun getGraphHeight(): Int = binding.xyPlot.layoutParams.height

    private fun xyPlotSetup() {
        binding.xyPlot.layoutManager.remove(binding.xyPlot.legend)
        binding.xyPlot.layoutManager.remove(binding.xyPlot.rangeTitle)
        binding.xyPlot.layoutManager.remove(binding.xyPlot.title)
        binding.xyPlot.domainTitle.position(
            0f,
            HorizontalPositioning.ABSOLUTE_FROM_CENTER,
            0f,
            VerticalPositioning.ABSOLUTE_FROM_BOTTOM,
            Anchor.BOTTOM_MIDDLE
        )
        binding.xyPlot.setBorderStyle(Plot.BorderStyle.NONE, null, null)
        binding.xyPlot.graph.position(
            0f,
            HorizontalPositioning.ABSOLUTE_FROM_LEFT,
            0f,
            VerticalPositioning.ABSOLUTE_FROM_TOP
        )
        binding.xyPlot.setPlotMargins(0f, 0f, 0f, 0f)
        binding.xyPlot.setPlotPadding(0f, 0f, 0f, 0f)
        binding.xyPlot.graph.setPadding(0f, 0f, 0f, 10f)
        binding.xyPlot.graph.setMargins(0f, 20f, 0f, 0f)

        val colorOnSurface = context.getColorFromAttr(R.attr.colorOnSurface)

        binding.xyPlot.domainTitle.labelPaint.color = colorOnSurface
        binding.xyPlot.graph.domainGridLinePaint.color = colorOnSurface
        binding.xyPlot.graph.rangeGridLinePaint.color = colorOnSurface
        binding.xyPlot.graph.domainSubGridLinePaint.color = colorOnSurface
        binding.xyPlot.graph.rangeSubGridLinePaint.color = colorOnSurface
        binding.xyPlot.graph.domainOriginLinePaint.color = colorOnSurface
        binding.xyPlot.graph.domainOriginLinePaint.strokeWidth = 1f
        binding.xyPlot.graph.rangeOriginLinePaint.color = colorOnSurface
        binding.xyPlot.graph.rangeOriginLinePaint.strokeWidth = 1f

        //Setting the layer type enables transparent backgrounds, I don't know why
        // I discovered it here: https://groups.google.com/g/androidplot/c/5QnJXD0uIIU
        binding.xyPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        binding.xyPlot.graph.gridBackgroundPaint.color = Color.TRANSPARENT
        binding.xyPlot.backgroundPaint.color = Color.TRANSPARENT
        binding.xyPlot.graph.backgroundPaint.color = Color.TRANSPARENT
        binding.pieChart.backgroundPaint.color = Color.TRANSPARENT
    }

    fun addLineGraphPanAndZoom() {
        PanZoom.attach(binding.xyPlot, PanZoom.Pan.HORIZONTAL, PanZoom.Zoom.STRETCH_HORIZONTAL)
    }

    private fun cleanAllViews() {
        binding.legendFlexboxLayout.removeAllViews()
        binding.xyPlot.clear()
        binding.xyPlot.removeMarkers()
        binding.xyPlot.graph.paddingLeft = 0f
        binding.xyPlot.graph.paddingBottom = 0f
        binding.xyPlot.setRangeBoundaries(0, 1, BoundaryMode.AUTO)
        binding.xyPlot.setDomainBoundaries(0, 1, BoundaryMode.GROW)
        binding.xyPlot.graph.refreshLayout()
        binding.xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).format =
            DecimalFormat("0.0")
        binding.pieChart.clear()
        blankViews()
        binding.errorMessage.text = ""
        binding.headerText.text = ""
        binding.statMessage.text = ""
    }

    fun initLoading() {
        cleanAllViews()
        binding.progressBar.visibility = View.VISIBLE
    }

    fun placeMarker(time: OffsetDateTime) {
        currentDecorator?.setTimeMarker(time)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : IGraphStatViewData> trySetDecorator(
        decorator: GraphStatViewDecorator<T>,
        data: Any
    ) {
        val graphOrStat = (data as T).graphOrStat
        //We fix the view height while we prepare the view to avoid the layout jumping around in the
        //scroll view
        fixViewHeight()
        cleanAllViews()
        try {
            decorate(data.graphOrStat, decorator, data)
            setDynamicViewHeight()
        } catch (throwable: Throwable) {
            onDecorateThrew(graphOrStat, throwable)
        }
    }

    fun update() = currentDecorator?.update()

    private fun onDecorateThrew(graphOrStat: GraphOrStat?, throwable: Throwable) {
        cleanAllViews()
        currentDecorator = null
        val headerText = graphOrStat?.name ?: ""
        binding.headerText.text = headerText
        binding.errorMessage.visibility = View.VISIBLE
        val t = ErrorMessageResolver(context).getErrorMessage(throwable)
        binding.errorMessage.text = t
        setDynamicViewHeight()
    }

    private fun fixViewHeight() {
        layoutParams = layoutParams.apply { height = getHeight() }
    }

    private fun setDynamicViewHeight() {
        layoutParams = layoutParams.apply { height = WRAP_CONTENT }
    }

    private fun <T : IGraphStatViewData> decorate(
        graphOrStat: GraphOrStat,
        decorator: GraphStatViewDecorator<T>,
        data: T
    ) {
        currentDecorator = decorator
        val headerText = graphOrStat.name
        binding.headerText.text = headerText
        when (data.state) { //Shouldn't really be loading state here but just for completeness
            IGraphStatViewData.State.LOADING -> binding.progressBar.visibility = View.VISIBLE
            IGraphStatViewData.State.READY -> decorator.decorate(this@GraphStatView, data)
            IGraphStatViewData.State.ERROR -> throw data.error!!
        }
    }

    private fun blankViews() {
        binding.legendFlexboxLayout.visibility = View.GONE
        binding.xyPlot.visibility = View.GONE
        binding.pieChart.visibility = View.GONE
        binding.errorMessage.visibility = View.GONE
        binding.statMessage.visibility = View.GONE
        binding.composeView.visibility = View.GONE
    }

    fun <T : IGraphStatViewData> initFromGraphStat(
        data: IGraphStatViewData,
        decorator: GraphStatViewDecorator<T>
    ) {
        if (data.state == IGraphStatViewData.State.LOADING) {
            currentDecorator = null
            blankViews()
            val headerText = data.graphOrStat.name
            binding.headerText.text = headerText
            binding.progressBar.visibility = View.VISIBLE
        } else {
            trySetDecorator(decorator, data)
        }
    }

}
