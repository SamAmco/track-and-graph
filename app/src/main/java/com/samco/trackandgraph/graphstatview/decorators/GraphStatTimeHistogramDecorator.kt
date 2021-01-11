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
import com.samco.trackandgraph.database.entity.TimeHistogramWindow
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeHistogramViewData
import com.samco.trackandgraph.util.getColorFromAttr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.DayOfWeek
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.WeekFields
import timber.log.Timber
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

class GraphStatTimeHistogramDecorator(listMode: Boolean) :
    GraphStatViewDecorator<ITimeHistogramViewData>(listMode) {
    private var binding: GraphStatViewBinding? = null
    private var context: Context? = null
    private var data: ITimeHistogramViewData? = null

    private lateinit var legendKeys: List<Int>
    private lateinit var nameMap: Map<Int, String>

    private fun getLabelInterval(window: TimeHistogramWindow) = when (window) {
        TimeHistogramWindow.HOUR -> 5
        TimeHistogramWindow.DAY -> 2
        TimeHistogramWindow.MONTH -> 5
        TimeHistogramWindow.SIX_MONTHS -> 2
        else -> 1
    }

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
            setUpXAxisTitle()
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

    private fun setUpXAxisTitle() {
        var title = context!!.getString(data!!.window!!.subTitleId)
        if (data!!.window!! == TimeHistogramWindow.WEEK) {
            val weekDayNameIds = mapOf(
                DayOfWeek.MONDAY to R.string.mon,
                DayOfWeek.TUESDAY to R.string.tue,
                DayOfWeek.WEDNESDAY to R.string.wed,
                DayOfWeek.THURSDAY to R.string.thu,
                DayOfWeek.FRIDAY to R.string.fri,
                DayOfWeek.SATURDAY to R.string.sat,
                DayOfWeek.SUNDAY to R.string.sun
            )
            val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
            val firstDayName = context!!.getString(weekDayNameIds[firstDay] ?: error(""))
            var lastDayIndex = DayOfWeek.values().indexOf(firstDay) - 1
            if (lastDayIndex < 0) lastDayIndex = DayOfWeek.values().size - 1
            val lastDay = DayOfWeek.values()[lastDayIndex]
            val lastDayName = context!!.getString(weekDayNameIds[lastDay] ?: error(""))
            title += " ($firstDayName-$lastDayName)"
        }
        binding!!.xyPlot.domainTitle.text = title
    }

    private fun setUpYAxis() {
        val divisions = ((data!!.maxDisplayHeight ?: 0.0) * 10) + 1
        binding!!.xyPlot.setRangeStep(StepMode.SUBDIVIDE, max(2.0, divisions))
    }

    private fun setUpXAxis() {
        binding!!.xyPlot.setDomainStep(StepMode.SUBDIVIDE, data!!.window!!.numBins + 2.0)
        binding!!.xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format =
            object : Format() {
                override fun format(
                    obj: Any,
                    toAppendTo: StringBuffer,
                    pos: FieldPosition
                ): StringBuffer {
                    val zeroIndexOffset =
                        // The bins we get start at index 0. The bin index can represent minutes, hours,
                        // day of the week or day of the month, etc.
                        // Since there is a hour 0 and a minute 0, but not a day or week 0 we have
                        // to add an offset of 1 to the labels if talking about days or weeks.
                        if (data!!.window!! == TimeHistogramWindow.DAY
                            || data!!.window!! == TimeHistogramWindow.HOUR)
                            0  // there is a minute 0 and a hour 0: index 0 -> label 0
                        else 1 // but there is no day 0 or week 0:  index 0 -> label 1

                    val index = (obj as Double).roundToInt() + zeroIndexOffset
                    val str = if (index >= zeroIndexOffset
                                    && index <= data!!.window!!.numBins) {
                        val labelInterval = getLabelInterval(data!!.window!!)
                        if (index == zeroIndexOffset
                            || index == data!!.window!!.numBins
                            || index % labelInterval == 0
                        ) index.toString()
                        else ""
                    } else ""
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

