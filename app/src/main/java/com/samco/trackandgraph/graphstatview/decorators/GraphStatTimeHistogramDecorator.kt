/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatview.decorators

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.androidplot.util.PixelUtils
import com.androidplot.xy.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.dataVisColorGenerator
import com.samco.trackandgraph.database.dataVisColorList
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeHistogramViewData
import com.samco.trackandgraph.util.getColorFromAttr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.OffsetDateTime
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import kotlin.math.max
import kotlin.math.roundToInt

class GraphStatTimeHistogramDecorator(listMode: Boolean) :
    GraphStatViewDecorator<ITimeHistogramViewData>(listMode) {
    private var binding: GraphStatViewBinding? = null
    private var context: Context? = null
    private var data: ITimeHistogramViewData? = null

    private lateinit var legendKeys: List<Int>
    private lateinit var nameMap: Map<Int, String>

    override suspend fun decorate(
        view: IDecoratableGraphStatView,
        data: ITimeHistogramViewData
    ) {
        binding = view.getBinding()
        context = view.getContext()
        this.data = data

        binding!!.statMessage.visibility = View.INVISIBLE
        initTimeHistogramBody()
    }

    private suspend fun initTimeHistogramBody() {
        withContext(Dispatchers.Main) {
            binding!!.xyPlot.visibility = View.INVISIBLE
            binding!!.progressBar.visibility = View.VISIBLE
        }
        if (!data!!.barValues.isNullOrEmpty()) {
            drawLegend()
            setUpXAxis()
            setUpYAxis()
            setUpBounds()
            drawBars()
            binding!!.xyPlot.redraw()
            withContext(Dispatchers.Main) {
                binding!!.xyPlot.visibility = View.VISIBLE
                binding!!.progressBar.visibility = View.GONE
            }
        } else {
            throw GraphStatInitException(R.string.graph_stat_view_not_enough_data_graph)
        }
    }

    //TODO we will need to reset the number of divisions when initing a line graph
    private fun setUpYAxis() {
        val divisions = ((data!!.maxDisplayHeight ?: 0.0) * 10) + 1
        binding!!.xyPlot.setRangeStep(StepMode.SUBDIVIDE, max(2.0, divisions))
    }

    //TODO we will need to reset the number of divisions when initing a line graph
    //TODO we need a subtitle explaining what the numbers mean
    //TODO 24 seems to be too many subdivides, at a certain point we need to divide the number by 2
    // or possibly change the angle of the text
    private fun setUpXAxis() {
        binding!!.xyPlot.setDomainStep(StepMode.SUBDIVIDE, data!!.window!!.numBins.toDouble() + 2)
        binding!!.xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format =
            object : Format() {
                override fun format(
                    obj: Any,
                    toAppendTo: StringBuffer,
                    pos: FieldPosition
                ): StringBuffer {
                    val index = (obj as Double).roundToInt() + 1
                    val str =
                        if (index > 0 && index <= data!!.window!!.numBins) index.toString()
                        else ""
                    return toAppendTo.append(str)
                }

                override fun parseObject(source: String, pos: ParsePosition) = null
            }
    }

    private fun setUpBounds() {
        binding!!.xyPlot.setRangeBoundaries(
            0,
            data!!.maxDisplayHeight ?: 1.0,
            BoundaryMode.FIXED
        )
        binding!!.xyPlot.bounds.set(
            -1.0,
            data!!.window!!.numBins,
            0.0,
            data!!.maxDisplayHeight ?: 1.0
        )
        binding!!.xyPlot.outerLimits.set(
            -1.0,
            data!!.window!!.numBins,
            0.0,
            data!!.maxDisplayHeight ?: 1.0
        )
    }

    private fun drawLegend() {
        legendKeys = data!!.barValues!!.keys.toList().sorted()
        nameMap = data!!.discreteValues!!.map { it.index to it.label }.toMap()
        if (legendKeys.size > 1) {
            for (l in legendKeys) {
                val colorIndex = (l * dataVisColorGenerator) % dataVisColorList.size
                nameMap.getOrElse(l) { null }?.let { name ->
                    inflateGraphLegendItem(binding!!, context!!, colorIndex, name)
                }
            }
        } else {
            binding?.legendFlexboxLayout?.removeAllViews()
        }
    }

    private fun drawBars() {
        val outlineColor = context!!.getColorFromAttr(R.attr.colorOnSurface)

        for (key in legendKeys) {
            val series = (data!!.barValues!![key] ?: error("")).toTypedArray()
            val name = nameMap[key] ?: error("")
            val xySeries = SimpleXYSeries(SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, name, *series)
            val colorIndex = (key * dataVisColorGenerator) % dataVisColorList.size
            val color = ContextCompat.getColor(context!!, dataVisColorList[colorIndex])
            val seriesFormatter = BarFormatter(color, outlineColor)
            seriesFormatter.borderPaint.strokeWidth = PixelUtils.dpToPix(1f)
            binding!!.xyPlot.addSeries(xySeries, seriesFormatter)
        }

        val renderer = binding!!.xyPlot.getRenderer(BarRenderer::class.java)
        renderer.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_GAP, PixelUtils.dpToPix(0f))
        renderer.barOrientation = BarRenderer.BarOrientation.STACKED
    }

    override fun setTimeMarker(time: OffsetDateTime) {}
}

