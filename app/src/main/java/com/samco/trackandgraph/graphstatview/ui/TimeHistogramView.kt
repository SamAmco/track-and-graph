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

package com.samco.trackandgraph.graphstatview.ui

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.content.ContextCompat.getColor
import com.androidplot.util.PixelUtils
import com.androidplot.xy.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.TimeHistogramWindowData
import com.samco.trackandgraph.base.database.dto.TimeHistogramWindow
import com.samco.trackandgraph.databinding.GraphXyPlotBinding
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeHistogramViewData
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall
import com.samco.trackandgraph.ui.dataVisColorGenerator
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.util.getColorFromAttr
import org.threeten.bp.DayOfWeek
import org.threeten.bp.temporal.WeekFields
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TimeHistogramView(
    modifier: Modifier = Modifier,
    viewData: ITimeHistogramViewData,
    graphHeight: Int? = null
) {
    if (viewData.barValues.isNullOrEmpty()) {
        GraphErrorView(
            modifier = modifier,
            error = R.string.graph_stat_view_not_enough_data_graph
        )
    } else {
        TimeHistogramBodyView(
            modifier = modifier,
            window = viewData.window,
            barValues = viewData.barValues!!,
            maxDisplayHeight = viewData.maxDisplayHeight,
            graphHeight = graphHeight
        )
    }
}

@Composable
private fun TimeHistogramBodyView(
    modifier: Modifier,
    window: TimeHistogramWindowData,
    barValues: List<ITimeHistogramViewData.BarValue>,
    maxDisplayHeight: Double,
    graphHeight: Int?
) = Column(modifier = modifier) {

    val context = LocalContext.current

    AndroidViewBinding(
        factory = { inflater, parent, attachToParent ->
            val binding = GraphXyPlotBinding.inflate(inflater, parent, attachToParent)

            xyPlotSetup(
                context = context,
                xyPlot = binding.xyPlot
            )
            binding.xyPlot.clear()

            setUpXAxis(
                binding = binding,
                window = window
            )
            setUpXAxisTitle(
                context = context,
                binding = binding,
                window = window
            )
            setUpYAxis(
                context = context,
                binding = binding,
                maxDisplayHeight = maxDisplayHeight
            )
            setUpBounds(
                binding = binding,
                window = window,
                maxDisplayHeight = maxDisplayHeight
            )
            drawBars(
                context = context,
                binding = binding,
                barValues = barValues
            )

            return@AndroidViewBinding binding
        },
        update = {
            if (graphHeight != null) xyPlot.layoutParams.height = graphHeight
            xyPlot.requestLayout()
        })

    SpacingSmall()

    if (barValues.size > 1) {
        GraphLegend(
            items = barValues.mapIndexed { i, bar ->
                val colorIndex = (i * dataVisColorGenerator) % dataVisColorList.size
                val label = bar.label
                    .ifEmpty { context.getString(R.string.no_label) }
                GraphLegendItem(
                    color = dataVisColorList[colorIndex],
                    label = label
                )
            }
        )
    }
}

private fun setUpXAxisTitle(
    context: Context,
    binding: GraphXyPlotBinding,
    window: TimeHistogramWindowData
) {
    var title = getNameForWindow(context, window)
    if (window.window == TimeHistogramWindow.WEEK) {
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
        val firstDayName = context.getString(weekDayNameIds[firstDay] ?: error(""))
        var lastDayIndex = DayOfWeek.values().indexOf(firstDay) - 1
        if (lastDayIndex < 0) lastDayIndex = DayOfWeek.values().size - 1
        val lastDay = DayOfWeek.values()[lastDayIndex]
        val lastDayName = context.getString(weekDayNameIds[lastDay] ?: error(""))
        title += " ($firstDayName-$lastDayName)"
    }
    binding.xyPlot.domainTitle.text = title
}

private fun setUpYAxis(
    context: Context,
    binding: GraphXyPlotBinding,
    maxDisplayHeight: Double
) {
    val divisions = (maxDisplayHeight / 10) + 1
    binding.xyPlot.setRangeStep(StepMode.SUBDIVIDE, max(2.0, divisions))
    binding.xyPlot.graph.paddingLeft = context.resources.displayMetrics.scaledDensity * 8f
    binding.xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).format =
        object : Format() {
            override fun format(
                obj: Any,
                toAppendTo: StringBuffer,
                pos: FieldPosition
            ): StringBuffer {
                val percent = (obj as Number).toDouble()
                return toAppendTo.append(atMost1dp(percent) + "%")
            }

            override fun parseObject(source: String, pos: ParsePosition) = null
        }
}

private fun atMost1dp(value: Double): String {
    return if (value % 1 == 0.0) String.format("%.0f", value)
    else String.format("%.1f", value)
}

private fun setUpXAxis(
    binding: GraphXyPlotBinding,
    window: TimeHistogramWindowData
) {
    binding.xyPlot.setDomainStep(StepMode.SUBDIVIDE, window.numBins + 2.0)
    binding.xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format =
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
                    if (window.window == TimeHistogramWindow.DAY
                        || window.window == TimeHistogramWindow.HOUR
                    ) 0  // there is a minute 0 and a hour 0: index 0 -> label 0
                    else 1 // but there is no day 0 or week 0:  index 0 -> label 1

                val index = (obj as Double).roundToInt() + zeroIndexOffset
                val str = if (index >= zeroIndexOffset && index <= window.numBins) {
                    val labelInterval = getLabelInterval(window.window)
                    if (index == zeroIndexOffset
                        || index == window.numBins
                        || index % labelInterval == 0
                    ) index.toString()
                    else ""
                } else ""
                return toAppendTo.append(str)
            }

            override fun parseObject(source: String, pos: ParsePosition) = null
        }
}

private fun setUpBounds(
    binding: GraphXyPlotBinding,
    window: TimeHistogramWindowData,
    maxDisplayHeight: Double
) {
    binding.xyPlot.setRangeBoundaries(
        0,
        maxDisplayHeight,
        BoundaryMode.FIXED
    )
    binding.xyPlot.bounds.set(
        -1.0,
        window.numBins,
        0.0,
        maxDisplayHeight
    )
    binding.xyPlot.outerLimits.set(
        -1.0,
        window.numBins,
        0.0,
        maxDisplayHeight
    )
}

private fun drawBars(
    context: Context,
    binding: GraphXyPlotBinding,
    barValues: List<ITimeHistogramViewData.BarValue>
) {
    val outlineColor = context.getColorFromAttr(R.attr.colorOnSurface)

    barValues.forEachIndexed { i, bv ->
        val series = (barValues[i]).values
        val xySeries = SimpleXYSeries(series, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, bv.label)
        val colorIndex = (i * dataVisColorGenerator) % dataVisColorList.size
        val color = getColor(context, dataVisColorList[colorIndex])
        val seriesFormatter = BarFormatter(color, outlineColor)
        seriesFormatter.borderPaint.strokeWidth = PixelUtils.dpToPix(1f)
        binding.xyPlot.addSeries(xySeries, seriesFormatter)
    }

    val renderer = binding.xyPlot.getRenderer(BarRenderer::class.java)
    renderer.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_GAP, PixelUtils.dpToPix(0f))
    renderer.barOrientation = BarRenderer.BarOrientation.STACKED
}

private fun getNameForWindow(
    context: Context,
    window: TimeHistogramWindowData
): String {
    return when (window.window) {
        TimeHistogramWindow.HOUR -> context.getString(R.string.minutes)
        TimeHistogramWindow.DAY -> context.getString(R.string.hours)
        TimeHistogramWindow.WEEK -> context.getString(R.string.days)
        TimeHistogramWindow.MONTH -> context.getString(R.string.days)
        TimeHistogramWindow.THREE_MONTHS -> context.getString(R.string.weeks)
        TimeHistogramWindow.SIX_MONTHS -> context.getString(R.string.weeks)
        TimeHistogramWindow.YEAR -> context.getString(R.string.months)
    }
}

private fun getLabelInterval(window: TimeHistogramWindow) = when (window) {
    TimeHistogramWindow.HOUR -> 5
    TimeHistogramWindow.DAY -> 2
    TimeHistogramWindow.MONTH -> 5
    TimeHistogramWindow.SIX_MONTHS -> 2
    else -> 1
}