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

import android.content.Context
import android.graphics.Paint
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.androidplot.ui.VerticalPosition
import com.androidplot.ui.VerticalPositioning
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.FastLineAndPointRenderer
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.PanZoom
import com.androidplot.xy.PointLabelFormatter
import com.androidplot.xy.RectRegion
import com.androidplot.xy.StepMode
import com.androidplot.xy.XValueMarker
import com.androidplot.xy.XYGraphWidget
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.databinding.GraphXyPlotBinding
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.Line
import com.samco.trackandgraph.helpers.formatDayMonth
import com.samco.trackandgraph.helpers.formatMonthYear
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.Temporal
import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

@Composable
fun LineGraphView(
    modifier: Modifier = Modifier,
    viewData: ILineGraphViewData,
    graphViewMode: GraphViewMode,
    timeMarker: OffsetDateTime? = null,
) {
    if (!viewData.hasPlottableData) {
        GraphErrorView(
            modifier = modifier,
            error = R.string.graph_stat_view_not_enough_data_graph
        )
    } else {
        LineGraphBodyView(
            modifier = modifier,
            viewData = viewData,
            timeMarker = timeMarker,
            graphViewMode = graphViewMode
        )
    }
}

@Composable
fun LineGraphBodyView(
    modifier: Modifier,
    viewData: ILineGraphViewData,
    timeMarker: OffsetDateTime? = null,
    graphViewMode: GraphViewMode,
) = Column(modifier = modifier) {

    val context = LocalContext.current
    val listMode = graphViewMode is GraphViewMode.ListMode
    val errorColor = MaterialTheme.colorScheme.error.toArgb()
    val textColorPrimary = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidViewBinding(factory = { inflater, parent, attachToParent ->
        val binding = GraphXyPlotBinding.inflate(inflater, parent, attachToParent)

        if (!listMode) {
            PanZoom.attach(binding.xyPlot, PanZoom.Pan.HORIZONTAL, PanZoom.Zoom.STRETCH_HORIZONTAL)
        }

        return@AndroidViewBinding binding
    }, update = {
        xyPlotSetup(
            xyPlot = xyPlot,
            onSurfaceColor = onSurfaceColor,
        )
        xyPlot.clear()

        drawLineGraphFeatures(
            context = context,
            binding = this,
            plottableData = viewData.lines,
            listMode = listMode,
            textColorPrimary = textColorPrimary
        )
        setUpLineGraphXAxis(
            context = context,
            binding = this,
            endTime = viewData.endTime,
        )
        setUpXYPlotYAxis(
            binding = this,
            yAxisSubdivides = viewData.yAxisSubdivides,
            durationBasedRange = viewData.durationBasedRange,
        )
        setLineGraphBounds(
            context = context,
            binding = this,
            bounds = viewData.bounds,
            yRangeType = viewData.yRangeType,
            endTime = viewData.endTime,
            listMode = listMode,
        )
        setTimeMarker(
            binding = this,
            endTime = viewData.endTime,
            timeMarker = timeMarker,
            errorColor = errorColor
        )

        if (graphViewMode is GraphViewMode.FullScreenMode) {
            setGraphHeight(
                graphView = xyPlot,
                graphViewMode = graphViewMode,
                hasLegend = true,
            )
        }
        xyPlot.requestLayout()
    })

    GraphLegend(
        items = viewData.lines.map {
            GraphLegendItem(
                color = Color(getColorInt(it.color)),
                label = it.name
            )
        }
    )
}

private val lineGraphHourMinuteSecondFormat: DateTimeFormatter = DateTimeFormatter
    .ofPattern("HH:mm:ss")
private val lineGraphHoursDateFormat: DateTimeFormatter = DateTimeFormatter
    .ofPattern("HH:mm")

private fun setLineGraphBounds(
    context: Context,
    binding: GraphXyPlotBinding,
    bounds: RectRegion,
    yRangeType: YRangeType,
    endTime: OffsetDateTime,
    listMode: Boolean
) {
    // since we now calculate the bounds to fit the number of intervals we almost always want
    // to set the rangeBoundaries to the bounds.
    // The only exception is when the graph is viewed fullscreen-mode (listMode == False) while dynamic
    if (yRangeType == YRangeType.FIXED || listMode) {
        binding.xyPlot.setRangeBoundaries(bounds.minY, bounds.maxY, BoundaryMode.FIXED)
    }
    binding.xyPlot.bounds.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
    binding.xyPlot.outerLimits.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
    setLineGraphPaddingFromBounds(
        context = context,
        binding = binding,
        bounds = bounds,
        endTime = endTime,
    )
}

private fun setLineGraphPaddingFromBounds(
    context: Context,
    binding: GraphXyPlotBinding,
    bounds: RectRegion,
    endTime: OffsetDateTime
) {
    //Set up Y padding
    val minY = bounds.minY.toDouble()
    val maxY = bounds.maxY.toDouble()
    val maxBound = max(abs(minY), abs(maxY))
    val numDigits = log10(maxBound).toFloat() + 3
    binding.xyPlot.graph.paddingLeft = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        (numDigits - 1) * 3.5f,
        context.resources.displayMetrics
    )

    //Set up X padding
    val formattedTimestamp = getDateTimeFormattedForDuration(
        context = context,
        binding = binding,
        endTime = endTime,
    )
    binding.xyPlot.graph.paddingBottom = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        formattedTimestamp.length * 1f,
        context.resources.displayMetrics
    )
}

private fun setUpLineGraphXAxis(
    context: Context,
    binding: GraphXyPlotBinding,
    endTime: OffsetDateTime
) {
    binding.xyPlot.domainTitle.text = ""
    binding.xyPlot.setDomainStep(StepMode.SUBDIVIDE, 11.0)
    binding.xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format =
        object : Format() {
            override fun format(
                obj: Any,
                toAppendTo: StringBuffer,
                pos: FieldPosition
            ): StringBuffer {
                val millis = (obj as Number).toLong()
                val duration = Duration.ofMillis(millis)

                val formattedTimestamp = getDateTimeFormattedForDuration(
                    context,
                    binding,
                    duration,
                    endTime
                )
                return toAppendTo.append(formattedTimestamp)
            }

            override fun parseObject(source: String, pos: ParsePosition) = null
        }
}

private fun getDateTimeFormattedForDuration(
    context: Context,
    binding: GraphXyPlotBinding,
    duration: Duration = Duration.ZERO,
    endTime: OffsetDateTime
): String {
    val timestamp = endTime
        .atZoneSameInstant(ZoneId.systemDefault())
        .plus(duration)
    val minX = binding.xyPlot.bounds.minX
    val maxX = binding.xyPlot.bounds.maxX
    if (minX == null || maxX == null) return formatDayMonth(context, timestamp)
    val durationRange = Duration.ofMillis(abs(maxX.toLong() - minX.toLong()))
    return when {
        durationRange.toMinutes() < 5L -> lineGraphHourMinuteSecondFormat.format(timestamp)
        durationRange.toDays() >= 304 -> formatMonthYear(context, timestamp)
        durationRange.toDays() >= 1 -> formatDayMonth(context, timestamp)
        else -> lineGraphHoursDateFormat.format(timestamp)
    }
}

private fun drawLineGraphFeatures(
    context: Context,
    binding: GraphXyPlotBinding,
    plottableData: List<Line>,
    listMode: Boolean,
    @ColorInt textColorPrimary: Int
) {
    for (line in plottableData) {
        if (line.line == null) continue
        addSeries(
            context = context,
            binding = binding,
            line = line,
            listMode = listMode,
            textColorPrimary = textColorPrimary
        )
    }
}

private fun addSeries(
    context: Context,
    binding: GraphXyPlotBinding,
    line: Line,
    listMode: Boolean,
    @ColorInt textColorPrimary: Int
) {
    val seriesFormat =
        if (listMode && line.pointStyle != LineGraphPointStyle.CIRCLES_AND_NUMBERS)
            getFastLineAndPointFormatter(context, line, textColorPrimary)
        else getLineAndPointFormatter(context, line, textColorPrimary)
    binding.xyPlot.addSeries(line.line, seriesFormat)
}

private fun getLineAndPointFormatter(
    context: Context,
    line: Line,
    @ColorInt textColorPrimary: Int
): LineAndPointFormatter {
    val formatter = LineAndPointFormatter()

    if (line.pointStyle == LineGraphPointStyle.CIRCLES_ONLY) {
        formatter.linePaint = null
    } else {
        formatter.linePaint.apply {
            color = getColorInt(line.color)
            strokeWidth = getLinePaintWidth(context)
        }
    }

    getVertexPaintColor(line)?.let {
        formatter.vertexPaint.color = it
        formatter.vertexPaint.strokeWidth = getVertexPaintWidth(context)
    } ?: run {
        formatter.vertexPaint = null
    }

    getPointLabelFormatter(context, line, textColorPrimary)?.let {
        formatter.pointLabelFormatter = it
        formatter.setPointLabeler { series, index ->
            DecimalFormat("#.#").format(series.getY(index))
        }
    } ?: run {
        formatter.pointLabelFormatter = null
    }
    formatter.fillPaint = null
    return formatter
}

private fun getFastLineAndPointFormatter(
    context: Context,
    line: Line,
    @ColorInt textColorPrimary: Int
): LineAndPointFormatter {

    // Setting lineColor to null sets the linePaint to null also
    val lineColor =
        if (line.pointStyle == LineGraphPointStyle.CIRCLES_ONLY) null
        else getColorInt(line.color)

    val vertexColor = getVertexPaintColor(line)

    val formatter = FastLineAndPointRenderer.Formatter(
        lineColor,
        vertexColor,
        getPointLabelFormatter(context, line, textColorPrimary)
    )

    formatter.linePaint?.apply {
        isAntiAlias = false
        strokeWidth = getLinePaintWidth(context)
    }

    formatter.vertexPaint?.apply { strokeWidth = getVertexPaintWidth(context) }
    return formatter
}

private fun getLinePaintWidth(
    context: Context
) = context.resources.getDimension(R.dimen.line_graph_line_thickness)

private fun getVertexPaintWidth(
    context: Context
) = context.resources.getDimension(R.dimen.line_graph_vertex_thickness)

private fun getVertexPaintColor(line: Line): Int? {
    return when (line.pointStyle) {
        LineGraphPointStyle.NONE -> null
        else -> getColorInt(line.color)
    }
}

private fun getPointLabelFormatter(
    context: Context,
    line: Line,
    @ColorInt textColorPrimary: Int
): PointLabelFormatter? {
    if (line.pointStyle != LineGraphPointStyle.CIRCLES_AND_NUMBERS) return null
    val pointLabelFormatter = PointLabelFormatter(
        textColorPrimary,
        context.resources.getDimension(R.dimen.line_graph_point_label_h_offset),
        context.resources.getDimension(R.dimen.line_graph_point_label_v_offset)
    )
    pointLabelFormatter.textPaint.textAlign = Paint.Align.RIGHT
    return pointLabelFormatter
}

private fun getMarkerPaint(
    @ColorInt errorColor: Int
): Paint {
    val paint = Paint()
    paint.color = errorColor
    paint.strokeWidth = 3f
    return paint
}

private fun setTimeMarker(
    binding: GraphXyPlotBinding,
    endTime: Temporal,
    timeMarker: OffsetDateTime?,
    @ColorInt errorColor: Int
) {
    binding.xyPlot.removeMarkers()
    if (timeMarker == null) return

    val markerPaint = getMarkerPaint(errorColor)
    val millis = Duration.between(endTime, timeMarker).toMillis()
    binding.xyPlot.addMarker(
        XValueMarker(
            millis,
            null,
            VerticalPosition(0f, VerticalPositioning.ABSOLUTE_FROM_TOP),
            markerPaint,
            null
        )
    )
    binding.xyPlot.redraw()
}