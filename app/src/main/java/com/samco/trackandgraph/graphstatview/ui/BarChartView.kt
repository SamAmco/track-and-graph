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
package com.samco.trackandgraph.graphstatview.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.content.ContextCompat.getColor
import com.androidplot.ui.VerticalPosition
import com.androidplot.ui.VerticalPositioning
import com.androidplot.util.PixelUtils
import com.androidplot.xy.BarFormatter
import com.androidplot.xy.BarRenderer
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.PanZoom
import com.androidplot.xy.RectRegion
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.StepMode
import com.androidplot.xy.XValueMarker
import com.androidplot.xy.XYGraphWidget
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.YRangeType
import com.samco.trackandgraph.base.helpers.formatTimeDuration
import com.samco.trackandgraph.base.helpers.getDayMonthFormatter
import com.samco.trackandgraph.base.helpers.getMonthYearFormatter
import com.samco.trackandgraph.databinding.GraphXyPlotBinding
import com.samco.trackandgraph.graphstatview.factories.viewdto.IBarChartData
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall
import com.samco.trackandgraph.ui.dataVisColorGenerator
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.util.getColorFromAttr
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun BarChartView(
    modifier: Modifier = Modifier,
    viewData: IBarChartData,
    listMode: Boolean,
    timeMarker: OffsetDateTime? = null,
    graphHeight: Int? = null
) {
    if (viewData.xDates.isEmpty() || viewData.bars.isEmpty()) {
        GraphErrorView(
            modifier = modifier,
            error = R.string.graph_stat_view_not_enough_data_graph
        )
    } else {
        BarChartBodyView(
            modifier = modifier,
            xDates = viewData.xDates,
            bars = viewData.bars,
            durationBasedRange = viewData.durationBasedRange,
            endTime = viewData.endTime,
            yRangeType = viewData.yRangeType,
            bounds = viewData.bounds,
            yAxisRangeParameters = viewData.yAxisRangeParameters,
            listMode = listMode,
            timeMarker = timeMarker,
            graphHeight = graphHeight
        )
    }
}

private class BarMarkerStore {
    private var highlightedIndex: Int? = null

    private val lock = Any()

    fun setHighlightedIndex(index: Int) = synchronized(lock) {
        highlightedIndex = index
    }

    fun clearHighlightedIndex() = synchronized(lock) {
        highlightedIndex = null
    }

    fun getHighlightedIndex() = synchronized(lock) {
        highlightedIndex
    }
}

@Composable
private fun BarChartBodyView(
    modifier: Modifier = Modifier,
    xDates: List<ZonedDateTime>,
    bars: List<SimpleXYSeries>,
    durationBasedRange: Boolean,
    endTime: ZonedDateTime,
    yRangeType: YRangeType,
    bounds: RectRegion,
    yAxisRangeParameters: Pair<StepMode, Double>,
    listMode: Boolean,
    timeMarker: OffsetDateTime?,
    graphHeight: Int? = null
) = Column(modifier = modifier) {

    val context = LocalContext.current

    //TODO we want to make the selected index a state and hoist it up to the parent
    // then we can draw an overlay with relevant information on it.
    val barMarkerStore = remember(timeMarker) {
        val barMarkerStore = BarMarkerStore()
        timeMarker?.let { marker ->
            val zonedMarker = marker.atZoneSameInstant(endTime.zone)
            val index = xDates.indexOfLast { zonedMarker.isAfter(it) } + 1
            if (index in xDates.indices) barMarkerStore.setHighlightedIndex(index)
        }
        barMarkerStore
    }

    AndroidViewBinding(
        factory = { inflater, parent, attachToParent ->
            val binding = GraphXyPlotBinding.inflate(inflater, parent, attachToParent)

            xyPlotSetup(
                context = context,
                xyPlot = binding.xyPlot
            )
            binding.xyPlot.clear()

            setBarChartBounds(
                binding = binding,
                bounds = bounds,
            )

            val xAxisFormatter = getXAxisFormatter(
                context = context,
                xDates = xDates,
            )

            setXAxisFormatter(
                binding = binding,
                xDates = xDates,
                xAxisFormatter = xAxisFormatter
            )

            setBarChartPaddingFromBounds(
                context = context,
                binding = binding,
                bounds = bounds,
                endTime = endTime,
                xAxisFormatter = xAxisFormatter
            )

            setUpLineGraphYAxis(
                binding = binding,
                yAxisRangeParameters = yAxisRangeParameters,
                durationBasedRange = durationBasedRange
            )
            drawBars(
                context = context,
                binding = binding,
                bars = bars
            )


            return@AndroidViewBinding binding
        },
        update = {

            setXAxisNumLabels(
                context = context,
                binding = this,
                xDates = xDates,
            )

            if (!listMode) {
                attachPanZoomClickListener(
                    binding = this,
                    barMarkerStore = barMarkerStore,
                )
                redrawMarkers(
                    binding = this,
                    barMarkerStore = barMarkerStore,
                )
            }

            if (graphHeight != null) xyPlot.layoutParams.height = graphHeight
            xyPlot.requestLayout()
            xyPlot.redraw()
        })

    SpacingSmall()

    if (bars.size > 1) {
        GraphLegend(
            items = bars.mapIndexed { i, bar ->
                val colorIndex = (i * dataVisColorGenerator) % dataVisColorList.size
                val label = bar.title
                    .ifEmpty { context.getString(R.string.no_label) }
                GraphLegendItem(
                    color = dataVisColorList[colorIndex],
                    label = label
                )
            }
        )
    }
}

@SuppressLint("ClickableViewAccessibility")
private fun attachPanZoomClickListener(
    binding: GraphXyPlotBinding,
    barMarkerStore: BarMarkerStore
) {
    val renderer = binding.xyPlot.getRenderer(BarRenderer::class.java)

    binding.xyPlot.setOnTouchListener(object : PanZoom(
        binding.xyPlot,
        Pan.HORIZONTAL,
        Zoom.STRETCH_HORIZONTAL
    ) {

        private var down = false
        private var pointerX = 0f
        private var pointerY = 0f

        override fun zoom(motionEvent: MotionEvent?) {
            super.zoom(motionEvent)
            redrawMarkers(binding, barMarkerStore)
        }

        override fun pan(motionEvent: MotionEvent?) {
            super.pan(motionEvent)
            redrawMarkers(binding, barMarkerStore)
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val rounded = renderer.plot.screenToSeriesX(event.x).toDouble().roundToInt()

            //Detect a click from a single pointer, otherwise call super
            if (event.pointerCount == 1) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        down = true
                        pointerX = event.x
                        pointerY = event.y
                        return super.onTouch(view, event)
                    }

                    MotionEvent.ACTION_UP -> {
                        val smallX = abs(event.x - pointerX) < 10
                        val smallY = abs(event.y - pointerY) < 10
                        if (down && smallX && smallY) {
                            toggleBarMarker(
                                binding = binding,
                                barMarkerStore = barMarkerStore,
                                index = rounded
                            )
                            view.performClick()
                        }
                        down = false
                    }

                    else -> {
                        down = false
                        return super.onTouch(view, event)
                    }
                }
            } else {
                down = false
                return super.onTouch(view, event)
            }
            return super.onTouch(view, event)
        }
    })
}

//TODO a lot of the below code is copy/pasted from other view classes, would be good to refactor
// the common code out
private val lineGraphHourMinuteSecondFormat: DateTimeFormatter = DateTimeFormatter
    .ofPattern("HH:mm:ss")
private val lineGraphHoursDateFormat: DateTimeFormatter = DateTimeFormatter
    .ofPattern("HH:mm")

private fun setXAxisFormatter(
    binding: GraphXyPlotBinding,
    xDates: List<ZonedDateTime>,
    xAxisFormatter: DateTimeFormatter
) {
    binding.xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format =
        object : Format() {
            override fun format(
                obj: Any,
                toAppendTo: StringBuffer,
                pos: FieldPosition
            ): StringBuffer {
                //We minus a tiny amount to favour labelling the previous bar if we're right on the
                // border between two
                val number = (obj as Number).toDouble() - 0.0001
                val rounded = number.roundToInt()
                //Shouldn't ever happen that we get a date outside of the range, but just in case
                val date =
                    if (rounded in xDates.indices) xDates[rounded]
                    else if (rounded < 0) xDates.first()
                    else xDates.last()

                return toAppendTo.append(
                    if (number < 0) ""
                    else date.format(xAxisFormatter)
                )
            }

            override fun parseObject(source: String, pos: ParsePosition) = null
        }
}

private fun setXAxisNumLabels(
    context: Context,
    binding: GraphXyPlotBinding,
    xDates: List<ZonedDateTime>,
) {
    val displayMetrics = context.resources.displayMetrics
    val dpWidth = binding.xyPlot.width / displayMetrics.density

    if (dpWidth < 0.1f) return

    val maxLabels = (dpWidth / 30.0).toInt().toDouble()

    var xStep = xDates.size
    for (i in 1 until (xDates.size / 2)) {
        if (xDates.size / i <= maxLabels && xDates.size % i == 0) {
            xStep = xDates.size / i
            break
        }
    }

    binding.xyPlot.setDomainStep(StepMode.SUBDIVIDE, min((xStep + 1).toDouble(), maxLabels))
}

private fun setUpLineGraphYAxis(
    binding: GraphXyPlotBinding,
    yAxisRangeParameters: Pair<StepMode, Double>,
    durationBasedRange: Boolean
) {
    binding.xyPlot.setRangeStep(
        yAxisRangeParameters.first,
        yAxisRangeParameters.second
    )
    if (durationBasedRange) {
        binding.xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).format =
            object : Format() {
                override fun format(
                    obj: Any,
                    toAppendTo: StringBuffer,
                    pos: FieldPosition
                ): StringBuffer {
                    val sec = (obj as Number).toDouble().roundToLong()
                    return toAppendTo.append(formatTimeDuration(sec))
                }

                override fun parseObject(source: String, pos: ParsePosition) = null
            }
    }
}

private fun setBarChartBounds(binding: GraphXyPlotBinding, bounds: RectRegion) {
    binding.xyPlot.setRangeBoundaries(bounds.minY, bounds.maxY, BoundaryMode.FIXED)
    binding.xyPlot.bounds.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
    binding.xyPlot.outerLimits.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
}

private fun setBarChartPaddingFromBounds(
    context: Context,
    binding: GraphXyPlotBinding,
    bounds: RectRegion,
    endTime: ZonedDateTime,
    xAxisFormatter: DateTimeFormatter
) {
    //Set up Y padding
    val minY = bounds.minY.toDouble()
    val maxY = bounds.maxY.toDouble()
    val maxBound = max(abs(minY), abs(maxY))
    val numDigits = log10(maxBound).toFloat() + 3
    binding.xyPlot.graph.paddingLeft =
        (numDigits - 1) * (context.resources.displayMetrics.scaledDensity) * 3.5f

    //Set up X padding
    val formattedTimestamp = xAxisFormatter.format(endTime)
    binding.xyPlot.graph.paddingBottom =
        formattedTimestamp.length * (context.resources.displayMetrics.scaledDensity)
}

private fun getXAxisFormatter(
    context: Context,
    xDates: List<ZonedDateTime>
): DateTimeFormatter {
    val minX = xDates.firstOrNull()
    val maxX = xDates.lastOrNull()
    if (minX == null || maxX == null) return getDayMonthFormatter(context)
    val durationRange = Duration.between(minX, maxX)
    return when {
        durationRange.toMinutes() < 5L -> lineGraphHourMinuteSecondFormat
        durationRange.toDays() >= 304 -> getMonthYearFormatter(context)
        durationRange.toDays() >= 1 -> getDayMonthFormatter(context)
        else -> lineGraphHoursDateFormat
    }
}

private fun drawBars(
    context: Context,
    binding: GraphXyPlotBinding,
    bars: List<SimpleXYSeries>
) {
    val outlineColor = context.getColorFromAttr(R.attr.colorOnSurface)

    // if there are more than 60 bars, we don't want to draw the borders
    // I chose 60 simply because it's the first round number after the number of weeks in a year
    val xfermode =
        if (bars.isNotEmpty() && bars[0].getyVals().size < 60) null
        else PorterDuffXfermode(PorterDuff.Mode.DST)

    bars.forEachIndexed { i, bv ->
        val colorIndex = (i * dataVisColorGenerator) % dataVisColorList.size
        val color = getColor(context, dataVisColorList[colorIndex])
        val seriesFormatter = BarFormatter(color, outlineColor)
        seriesFormatter.borderPaint.xfermode = xfermode
        binding.xyPlot.addSeries(bv, seriesFormatter)
    }

    val renderer = binding.xyPlot.getRenderer(BarRenderer::class.java)
    renderer.setBarGroupWidth(BarRenderer.BarGroupWidthMode.FIXED_GAP, PixelUtils.dpToPix(0f))
    renderer.barOrientation = BarRenderer.BarOrientation.STACKED
}

private fun getMarkerPaint(binding: GraphXyPlotBinding): Paint {
    //Calculate the marker width
    val renderer = binding.xyPlot.getRenderer(BarRenderer::class.java)
    //If the gridRect is null it will throw an exception, this can happen if the graph hasn't been
    // laid out yet
    if (binding.xyPlot.graph.gridRect == null) return Paint()
    val width = abs(renderer.plot.seriesToScreenX(1) - renderer.plot.seriesToScreenX(0))

    val paint = Paint()
    paint.color = Color.BLACK
    paint.alpha = (0.2f * 255f).toInt()
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
    paint.strokeWidth = width
    return paint
}

private fun toggleBarMarker(
    binding: GraphXyPlotBinding,
    barMarkerStore: BarMarkerStore,
    index: Int
) {
    if (barMarkerStore.getHighlightedIndex() == index) barMarkerStore.clearHighlightedIndex()
    else barMarkerStore.setHighlightedIndex(index)
    redrawMarkers(binding, barMarkerStore)
}

private fun redrawMarkers(
    binding: GraphXyPlotBinding,
    barMarkerStore: BarMarkerStore
) {
    binding.xyPlot.removeMarkers()
    val markerPaint = getMarkerPaint(binding)
    barMarkerStore.getHighlightedIndex()?.let {
        binding.xyPlot.addMarker(
            XValueMarker(
                it,
                null,
                VerticalPosition(0f, VerticalPositioning.ABSOLUTE_FROM_TOP),
                markerPaint,
                null
            )
        )
    }
    binding.xyPlot.redraw()
}
