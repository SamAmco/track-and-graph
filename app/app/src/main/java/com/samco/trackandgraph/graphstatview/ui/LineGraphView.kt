/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.graphstatview.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.graphstatview.factories.helpers.DataDisplayIntervalHelper
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.Line
import com.samco.trackandgraph.graphstatview.factories.viewdto.LineGraphPoint
import com.samco.trackandgraph.helpers.formatTimeDuration
import kotlinx.coroutines.yield
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin
import timber.log.Timber

private const val X_LABEL_ANGLE = -28f
private const val REVEAL_DURATION_MILLIS = 450
private const val PERFORMANCE_LOG_TAG = "LineGraphPerf"
private const val APPROXIMATE_Y_TICK_SPACING_DP = 32f
private const val MINIMUM_Y_TICK_COUNT = 3
private const val MAXIMUM_Y_TICK_COUNT = 8
private val lineWidth = 2.dp
private val vertexWidth = 6.dp
private val pointLabelTextSize = 9.sp
private val plotStartPadding = 14.dp
private val plotEndPadding = 8.dp
private val plotTopPadding = 8.dp
private val plotBottomPadding = 10.dp
private val yAxisLabelPadding = 6.dp
private val xAxisTickLength = 3.dp
private val xAxisLabelPadding = 6.dp
private val axisLabelMinimumGap = 4.dp
private val timeMarkerWidth = 3.dp
private val pointLabelPadding = 3.dp
private val yAxisIntervalHelper = DataDisplayIntervalHelper()

internal enum class LineGraphPinchAxis { HORIZONTAL, VERTICAL }

private val lineGraphSecondFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val lineGraphMinuteFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val lineGraphNumberFormatter = DecimalFormat("#,##0.###")

@Composable
fun LineGraphView(
    modifier: Modifier = Modifier,
    viewData: ILineGraphViewData,
    graphViewMode: GraphViewMode,
    graphBackgroundColor: Color,
    timeMarker: OffsetDateTime? = null,
) {
    val performanceLogger = remember(viewData) {
        LineGraphPerformanceLogger(viewData.graphOrStat.id)
    }
    val renderableLines = remember(viewData.lines, performanceLogger) {
        val startedAt = performanceLogger.now()
        val result = viewData.lines.map { line ->
            line.copy(points = line.points.filter { point -> point.value.isFinite() })
        }
        performanceLogger.recordPreparation(
            startedAt = startedAt,
            inputPointCount = viewData.lines.sumOf { it.points.size },
            finitePointCount = result.sumOf { it.points.size },
        )
        result
    }
    if (!viewData.hasPlottableData || renderableLines.none { it.points.size >= 2 }) {
        GraphErrorView(modifier, R.string.graph_stat_view_not_enough_data_graph)
        return
    }

    LineGraphBodyView(
        modifier = modifier,
        viewData = viewData,
        lines = renderableLines,
        timeMarker = timeMarker,
        graphViewMode = graphViewMode,
        graphBackgroundColor = graphBackgroundColor,
        performanceLogger = performanceLogger,
    )
}

@Composable
private fun LineGraphBodyView(
    modifier: Modifier,
    viewData: ILineGraphViewData,
    lines: List<Line>,
    timeMarker: OffsetDateTime?,
    graphViewMode: GraphViewMode,
    graphBackgroundColor: Color,
    performanceLogger: LineGraphPerformanceLogger,
) = Column(modifier = modifier) {
    val allPoints = remember(lines, performanceLogger) {
        val startedAt = performanceLogger.now()
        lines.flatMap { it.points }.sortedBy { it.timestamp }.also { points ->
            performanceLogger.recordPointPreparation(startedAt, points.size)
        }
    }
    val fullMinX = allPoints.first().timestamp.toInstant().toEpochMilli()
    val fullMaxX = allPoints.last().timestamp.toInstant().toEpochMilli()
        .let { if (it == fullMinX) it + 1L else it }
    val isInteractive = graphViewMode is GraphViewMode.FullScreenMode
    var zoom by remember(viewData) { mutableDoubleStateOf(1.0) }
    var centerFraction by remember(viewData) { mutableDoubleStateOf(0.5) }
    val maximumZoom = maximumLineGraphZoom(fullMinX, fullMaxX)
    val viewport = calculateLineGraphViewport(fullMinX, fullMaxX, zoom, centerFraction)
    val visibleMinX = viewport.minX
    val visibleMaxX = viewport.maxX
    val visibleSpan = visibleMaxX - visibleMinX
    val xLabelText = LineGraphXLabelText(
        months = stringArrayResource(R.array.abbreviated_months).toList(),
        weekdays = stringArrayResource(R.array.abbreviated_weekdays).toList(),
        weekdayDayFormat = stringResource(R.string.compact_weekday_day_format),
        dayMonthFormat = stringResource(R.string.compact_day_month_format),
        monthYearFormat = stringResource(R.string.compact_month_year_format),
    )

    val textMeasurer = rememberTextMeasurer()
    val axisTextStyle = graphAxisTextStyle
    val pointTextStyle = axisTextStyle.copy(fontSize = pointLabelTextSize)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = graphGridLineAlpha)
    val markerColor = MaterialTheme.colorScheme.error
    val density = LocalDensity.current
    val graphHeight = graphHeightFor(graphViewMode, hasLegend = true)
    val maximumXLabelWidth = if (isInteractive) {
        remember(visibleXLabelFormat(visibleSpan), xLabelText, textMeasurer, axisTextStyle) {
            maximumLineGraphXLabelWidth(visibleSpan, xLabelText) { label ->
                textMeasurer.measure(label, axisTextStyle).size
            }
        }
    } else null
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var layout by remember(viewData) { mutableStateOf<LineGraphLayout?>(null) }
    var fullYViewport by remember(viewData) { mutableStateOf<LineGraphYViewport?>(null) }
    var requestedYViewport by remember(viewData) { mutableStateOf<LineGraphYViewport?>(null) }
    var displayedYViewport by remember(viewData) { mutableStateOf<LineGraphYViewport?>(null) }
    var verticalZoomCenter by remember(viewData) { mutableDoubleStateOf(0.0) }
    var yLayoutRevision by remember(viewData) { mutableIntStateOf(0) }
    val reveal = remember(viewData) { Animatable(0f) }
    val horizontalViewportTransform = rememberUpdatedState<(Float, Float) -> Unit> { panX, gestureZoom ->
        val plotWidth = layout?.plotRect?.width ?: return@rememberUpdatedState
        zoom = (zoom * gestureZoom).coerceIn(1.0, maximumZoom)
        val newHalfSpanFraction = 0.5 / zoom
        centerFraction = (centerFraction - panX / plotWidth / zoom)
            .coerceIn(newHalfSpanFraction, 1.0 - newHalfSpanFraction)
    }
    val startVerticalZoom = rememberUpdatedState {
        val currentLayout = layout ?: return@rememberUpdatedState
        val currentViewport = LineGraphYViewport(currentLayout.minY, currentLayout.maxY)
        val center = visibleDataYCenter(
            points = allPoints,
            minX = visibleMinX,
            maxX = visibleMaxX,
            fallback = currentViewport.center,
        )
        verticalZoomCenter = center
        displayedYViewport = currentViewport.centeredOn(center)
    }
    val updateVerticalZoom = rememberUpdatedState<(Float) -> Unit> { gestureZoom ->
        val currentViewport = displayedYViewport ?: return@rememberUpdatedState
        val completeViewport = fullYViewport ?: return@rememberUpdatedState
        displayedYViewport = calculateLineGraphYViewport(
            current = currentViewport,
            complete = completeViewport,
            center = verticalZoomCenter,
            gestureZoom = gestureZoom.toDouble(),
        )
    }
    val finishVerticalZoom = rememberUpdatedState {
        displayedYViewport?.let {
            requestedYViewport = fullYViewport?.let { complete ->
                settleLineGraphYViewport(it, complete)
            } ?: it
            yLayoutRevision++
        }
    }

    LaunchedEffect(
        canvasSize,
        visibleMinX,
        visibleMaxX,
        lines,
        viewData.durationBasedRange,
        viewData.yRangeType,
        viewData.fixedYMin,
        viewData.fixedYMax,
        requestedYViewport,
        yLayoutRevision,
        maximumXLabelWidth,
        xLabelText,
        axisTextStyle,
    ) {
        if (canvasSize == IntSize.Zero) return@LaunchedEffect
        // Only the initial layout needs an empty/background frame before the reveal.
        // Yielding every viewport update makes axis movement lag behind the gesture.
        if (layout == null) yield()
        val startedAt = performanceLogger.startMeasurement()
        layout = calculateLineGraphLayout(
            width = canvasSize.width.toFloat(),
            height = canvasSize.height.toFloat(),
            visibleMinX = visibleMinX,
            visibleMaxX = visibleMaxX,
            points = allPoints,
            durationBasedRange = viewData.durationBasedRange,
            fixedYMin = viewData.fixedYMin.takeIf { viewData.yRangeType == YRangeType.FIXED },
            fixedYMax = viewData.fixedYMax.takeIf { viewData.yRangeType == YRangeType.FIXED },
            requestedYViewport = requestedYViewport,
            reservedXLabelWidth = maximumXLabelWidth,
            xLabelText = xLabelText,
            measureText = { text -> textMeasurer.measure(text, axisTextStyle).size },
            density = density.density,
        )
        if (fullYViewport == null) {
            layout?.let { fullYViewport = LineGraphYViewport(it.minY, it.maxY) }
        }
        if (requestedYViewport != null) displayedYViewport = null
        performanceLogger.recordLayout(startedAt)
    }
    LaunchedEffect(viewData, layout != null) {
        if (layout != null) {
            reveal.animateTo(1f, tween(REVEAL_DURATION_MILLIS))
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(graphHeight)
            .background(graphBackgroundColor)
            .onSizeChanged { canvasSize = it }
            .pointerInput(isInteractive, canvasSize, maximumZoom) {
                if (!isInteractive) return@pointerInput
                detectLineGraphPinchGestures(
                    onHorizontalTransform = { panX, gestureZoom ->
                        horizontalViewportTransform.value(panX, gestureZoom)
                    },
                    onVerticalStart = { startVerticalZoom.value() },
                    onVerticalTransform = { gestureZoom -> updateVerticalZoom.value(gestureZoom) },
                    onVerticalEnd = { finishVerticalZoom.value() },
                )
            }
            .pointerInput(isInteractive, canvasSize, maximumZoom) {
                if (!isInteractive) return@pointerInput
                detectHorizontalDragGestures { _, dragAmount ->
                    horizontalViewportTransform.value(dragAmount, 1f)
                }
            }
    ) {
        val baseLayout = layout ?: return@Canvas
        val currentLayout = displayedYViewport?.let { viewport ->
            baseLayout.copy(minY = viewport.minY, maxY = viewport.maxY)
        } ?: baseLayout
        val drawStartedAt = performanceLogger.startMeasurement()
        var visiblePointCount = 0
        val plot = currentLayout.plotRect
        val graphAlpha = reveal.value
        val revealedGridColor = gridColor.copy(alpha = gridColor.alpha * graphAlpha)
        val revealedAxisStyle = axisTextStyle.copy(
            color = axisTextStyle.color.copy(alpha = graphAlpha),
        )

        currentLayout.yTicks.forEach { tick ->
            val y = currentLayout.yToPixel(tick.value)
            if (y !in plot.top..plot.bottom) return@forEach
            drawLine(
                revealedGridColor,
                Offset(plot.left, y),
                Offset(plot.right, y),
                graphGridLineThickness.toPx(),
            )
            val measured = textMeasurer.measure(tick.label, axisTextStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = tick.label,
                style = revealedAxisStyle,
                topLeft = Offset(
                    plot.left - measured.size.width - yAxisLabelPadding.toPx(),
                    y - measured.size.height / 2f,
                ),
            )
        }
        drawLine(
            revealedGridColor,
            Offset(plot.left, plot.top),
            Offset(plot.left, plot.bottom),
            graphGridLineThickness.toPx(),
        )

        currentLayout.xTicks.forEach { tick ->
            val x = currentLayout.xToPixel(tick.epochMillis)
            drawLine(
                revealedGridColor,
                Offset(x, plot.top),
                Offset(x, plot.bottom + xAxisTickLength.toPx()),
                graphGridLineThickness.toPx(),
            )
            val measured = textMeasurer.measure(tick.label, axisTextStyle)
            val pivot = Offset(x, plot.bottom + xAxisLabelPadding.toPx())
            rotate(X_LABEL_ANGLE, pivot) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = tick.label,
                    style = revealedAxisStyle,
                    topLeft = Offset(pivot.x - measured.size.width, pivot.y),
                )
            }
        }

        clipRect(plot.left, plot.top, plot.right, plot.bottom) {
            timeMarker?.toInstant()?.toEpochMilli()?.let { markerMillis ->
                if (markerMillis in visibleMinX..visibleMaxX) {
                    val markerX = currentLayout.xToPixel(markerMillis)
                    drawLine(
                        markerColor.copy(alpha = graphAlpha),
                        Offset(markerX, plot.top),
                        Offset(markerX, plot.bottom),
                        timeMarkerWidth.toPx(),
                    )
                }
            }

            lines.forEach { line ->
                val visiblePoints = pointsForViewport(line.points, visibleMinX, visibleMaxX)
                if (visiblePoints.isEmpty()) return@forEach
                visiblePointCount += visiblePoints.size
                val color = getColor(line.color).copy(alpha = graphAlpha)
                if (line.pointStyle != LineGraphPointStyle.CIRCLES_ONLY && visiblePoints.size >= 2) {
                    val path = Path()
                    visiblePoints.forEachIndexed { index, point ->
                        val pointOffset = currentLayout.toPixel(point)
                        if (index == 0) path.moveTo(pointOffset.x, pointOffset.y)
                        else path.lineTo(pointOffset.x, pointOffset.y)
                    }
                    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = lineWidth.toPx(), cap = StrokeCap.Round,
                    ))
                }
                if (line.pointStyle != LineGraphPointStyle.NONE) {
                    visiblePoints.forEach { point ->
                        val pointOffset = currentLayout.toPixel(point)
                        drawCircle(color, vertexWidth.toPx() / 2f, pointOffset)
                        if (line.pointStyle == LineGraphPointStyle.CIRCLES_AND_NUMBERS) {
                            val label = formatLineGraphNumber(point.value)
                            val measured = textMeasurer.measure(label, pointTextStyle)
                            drawText(
                                textMeasurer,
                                label,
                                Offset(
                                    pointOffset.x - measured.size.width - pointLabelPadding.toPx(),
                                    pointOffset.y - measured.size.height,
                                ),
                                pointTextStyle.copy(color = pointTextStyle.color.copy(alpha = graphAlpha)),
                            )
                        }
                    }
                }
            }
        }
        performanceLogger.recordFirstDraw(
            startedAt = drawStartedAt,
            visiblePointCount = visiblePointCount,
            lineCount = lines.size,
            xTickCount = currentLayout.xTicks.size,
            yTickCount = currentLayout.yTicks.size,
        )
    }

    GraphLegend(
        items = lines.map { GraphLegendItem(color = getColor(it.color), label = it.name) }
    )
}

private suspend fun PointerInputScope.detectLineGraphPinchGestures(
    onHorizontalTransform: (panX: Float, zoom: Float) -> Unit,
    onVerticalStart: () -> Unit,
    onVerticalTransform: (zoom: Float) -> Unit,
    onVerticalEnd: () -> Unit,
) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    var axis: LineGraphPinchAxis? = null
    var verticalZoomFinished = false
    var initialHorizontalSeparation: Float? = null
    var initialVerticalSeparation: Float? = null
    do {
        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
        val pressed = event.changes.filter { it.pressed }
        if (pressed.size > 1) {
            val horizontalSeparation = abs(pressed[0].position.x - pressed[1].position.x)
            val verticalSeparation = abs(pressed[0].position.y - pressed[1].position.y)
            val initialHorizontal = initialHorizontalSeparation ?: horizontalSeparation.also {
                initialHorizontalSeparation = it
            }
            val initialVertical = initialVerticalSeparation ?: verticalSeparation.also {
                initialVerticalSeparation = it
            }
            if (axis == null) {
                val horizontalChange = abs(horizontalSeparation - initialHorizontal)
                val verticalChange = abs(verticalSeparation - initialVertical)
                if (max(horizontalChange, verticalChange) >= viewConfiguration.touchSlop) {
                    axis = lineGraphPinchAxis(horizontalChange, verticalChange)
                    if (axis == LineGraphPinchAxis.VERTICAL) onVerticalStart()
                }
            }
            val pan = event.calculatePan()
            val zoom = event.calculateZoom()
            when (axis) {
                LineGraphPinchAxis.HORIZONTAL -> {
                    if (pan.x != 0f || zoom != 1f) onHorizontalTransform(pan.x, zoom)
                }
                LineGraphPinchAxis.VERTICAL -> {
                    if (zoom != 1f) onVerticalTransform(zoom)
                }
                null -> Unit
            }
            event.changes.forEach { it.consume() }
        } else if (axis != null) {
            if (axis == LineGraphPinchAxis.VERTICAL && !verticalZoomFinished) {
                onVerticalEnd()
                verticalZoomFinished = true
            }
            event.changes.forEach { it.consume() }
        }
    } while (event.changes.any { it.pressed })
    if (axis == LineGraphPinchAxis.VERTICAL && !verticalZoomFinished) onVerticalEnd()
}

internal fun lineGraphPinchAxis(
    horizontalSeparationChange: Float,
    verticalSeparationChange: Float,
): LineGraphPinchAxis = if (verticalSeparationChange > horizontalSeparationChange) {
    LineGraphPinchAxis.VERTICAL
} else {
    LineGraphPinchAxis.HORIZONTAL
}

internal data class LineGraphLayout(
    val plotRect: Rect,
    val minX: Long,
    val maxX: Long,
    val minY: Double,
    val maxY: Double,
    val xTicks: List<XTick>,
    val yTicks: List<YTick>,
) {
    fun xToPixel(epochMillis: Long): Float = plotRect.left +
        ((epochMillis - minX).toDouble() / (maxX - minX).toDouble() * plotRect.width).toFloat()

    fun yToPixel(value: Double): Float = plotRect.bottom -
        ((value - minY) / (maxY - minY) * plotRect.height).toFloat()

    fun toPixel(point: LineGraphPoint) = Offset(
        xToPixel(point.timestamp.toInstant().toEpochMilli()),
        yToPixel(point.value),
    )
}

internal data class XTick(
    val epochMillis: Long,
    val label: String,
    val projectedWidth: Float,
    val projectedRightExtent: Float = 0f,
) {
    val projectedLeftExtent: Float get() = projectedWidth - projectedRightExtent
}
internal data class YTick(val value: Double, val label: String)

internal data class LineGraphViewport(val minX: Long, val maxX: Long)

internal data class LineGraphXLabelText(
    val months: List<String>,
    val weekdays: List<String>,
    val weekdayDayFormat: String,
    val dayMonthFormat: String,
    val monthYearFormat: String,
)

internal data class LineGraphYViewport(val minY: Double, val maxY: Double) {
    val span: Double get() = maxY - minY
    val center: Double get() = minY + span / 2.0

    fun centeredOn(newCenter: Double) = LineGraphYViewport(
        minY = newCenter - span / 2.0,
        maxY = newCenter + span / 2.0,
    )
}

internal fun calculateLineGraphYViewport(
    current: LineGraphYViewport,
    complete: LineGraphYViewport,
    center: Double,
    gestureZoom: Double,
): LineGraphYViewport {
    if (!gestureZoom.isFinite() || gestureZoom <= 0.0) return current
    val minimumSpan = min(complete.span, max(Math.ulp(center), Math.ulp(complete.span)) * 4.0)
    val newSpan = (current.span / gestureZoom).coerceIn(minimumSpan, complete.span)
    if (newSpan >= complete.span) return complete
    return LineGraphYViewport(
        minY = center - newSpan / 2.0,
        maxY = center + newSpan / 2.0,
    )
}

internal fun settleLineGraphYViewport(
    requested: LineGraphYViewport,
    complete: LineGraphYViewport,
): LineGraphYViewport = if (requested.span >= complete.span * 0.98) complete else requested

internal fun visibleDataYCenter(
    points: List<LineGraphPoint>,
    minX: Long,
    maxX: Long,
    fallback: Double,
): Double {
    var minimum = Double.POSITIVE_INFINITY
    var maximum = Double.NEGATIVE_INFINITY
    points.forEach { point ->
        if (point.timestamp.toInstant().toEpochMilli() in minX..maxX) {
            minimum = min(minimum, point.value)
            maximum = max(maximum, point.value)
        }
    }
    return if (minimum.isFinite() && maximum.isFinite()) minimum + (maximum - minimum) / 2.0
    else fallback
}

internal fun maximumLineGraphZoom(fullMinX: Long, fullMaxX: Long): Double =
    max(1.0, fullMaxX.toDouble() - fullMinX.toDouble())

internal fun calculateLineGraphViewport(
    fullMinX: Long,
    fullMaxX: Long,
    zoom: Double,
    centerFraction: Double,
): LineGraphViewport {
    val fullSpan = max(1.0, fullMaxX.toDouble() - fullMinX.toDouble())
    val constrainedZoom = zoom.coerceIn(1.0, fullSpan)
    val visibleSpan = max(1.0, fullSpan / constrainedZoom)
    val halfSpanFraction = 0.5 / constrainedZoom
    val constrainedCenter = centerFraction.coerceIn(halfSpanFraction, 1.0 - halfSpanFraction)
    val centerX = fullMinX + fullSpan * constrainedCenter
    val spanMillis = ceil(visibleSpan).toLong().coerceAtLeast(1L)
    val minX = (centerX - visibleSpan / 2.0).roundToLong()
        .coerceIn(fullMinX, fullMaxX - spanMillis)
    return LineGraphViewport(minX = minX, maxX = minX + spanMillis)
}

internal fun calculateLineGraphLayout(
    width: Float,
    height: Float,
    visibleMinX: Long,
    visibleMaxX: Long,
    points: List<LineGraphPoint>,
    durationBasedRange: Boolean,
    fixedYMin: Double?,
    fixedYMax: Double?,
    requestedYViewport: LineGraphYViewport? = null,
    reservedXLabelWidth: Float? = null,
    xLabelText: LineGraphXLabelText,
    measureText: (String) -> IntSize,
    density: Float,
): LineGraphLayout? {
    val rawYMin = requestedYViewport?.minY ?: fixedYMin ?: points.minOfOrNull { it.value } ?: return null
    val rawYMax = requestedYViewport?.maxY ?: fixedYMax ?: points.maxOfOrNull { it.value } ?: return null
    if (!rawYMin.isFinite() || !rawYMax.isFinite() || rawYMax < rawYMin) return null

    val initialRange = expandEqualRange(rawYMin, rawYMax)
    val approximateTickCount = (height / (APPROXIMATE_Y_TICK_SPACING_DP * density))
        .toInt()
        .coerceIn(MINIMUM_Y_TICK_COUNT, MAXIMUM_Y_TICK_COUNT)
    val yValues = calculateYTicks(
        initialRange.first,
        initialRange.second,
        approximateTickCount,
        requestedYViewport == null && fixedYMin != null && fixedYMax != null,
        durationBasedRange,
    )
    val yTicks = yValues.map { value ->
        YTick(value, if (durationBasedRange) formatTimeDuration(value.roundToLong()) else formatLineGraphNumber(value))
    }
    val yLabelSizes = yTicks.map { measureText(it.label) }
    val widestYLabel = yLabelSizes.maxOf { it.width }.toFloat()
    val labelHeight = yLabelSizes.maxOf { it.height }.toFloat()
    val yAxisLeft = widestYLabel + plotStartPadding.value * density
    val right = width - plotEndPadding.value * density
    val top = max(plotTopPadding.value * density, labelHeight / 2f)

    val allTimestamps = mutableListOf<Long>()
    var previousMillis: Long? = null
    points.forEach { point ->
        val millis = point.timestamp.toInstant().toEpochMilli()
        if (millis != previousMillis) {
            previousMillis = millis
            allTimestamps += millis
        }
    }

    val visibleSpan = visibleMaxX - visibleMinX
    val fullMinX = allTimestamps.first()
    val fullMaxX = allTimestamps.last()
    val labelFormat = visibleXLabelFormat(visibleSpan)
    val angleRadians = Math.toRadians(abs(X_LABEL_ANGLE).toDouble())
    val minimumGap = axisLabelMinimumGap.value * density
    val zoneId = ZoneId.systemDefault()
    val xLabelSizes = mutableMapOf<String, IntSize>()
    fun tickAt(epochMillis: Long): XTick {
        val label = formatLineGraphTimestamp(epochMillis, labelFormat, zoneId, xLabelText)
        val measured = xLabelSizes.getOrPut(label) { measureText(label) }
        return XTick(
            epochMillis = epochMillis,
            label = label,
            projectedWidth = projectedLabelWidth(measured, angleRadians),
            projectedRightExtent = projectedLabelRightExtent(measured, angleRadians),
        )
    }
    val startBoundaryTick = tickAt(visibleMinX)
    val endBoundaryTick = tickAt(visibleMaxX)
    val reservedStartLabelLeftExtent = reservedXLabelWidth
        ?.times(cos(angleRadians).toFloat())
        ?: 0f
    val left = max(
        max(yAxisLeft, startBoundaryTick.projectedLeftExtent),
        reservedStartLabelLeftExtent,
    )
    val preliminaryPlotWidth = right - left
    val representativeMillis = allTimestamps[allTimestamps.size / 2]
    val representativeLabel = formatLineGraphTimestamp(
        representativeMillis,
        labelFormat,
        zoneId,
        xLabelText,
    )
    val estimatedSize = xLabelSizes.getOrPut(representativeLabel) { measureText(representativeLabel) }
    val estimatedProjectedWidth = projectedLabelWidth(estimatedSize, angleRadians)
    val estimatedProjectedRightExtent = projectedLabelRightExtent(estimatedSize, angleRadians)
    val capacityProjectedLabelWidth = reservedXLabelWidth?.let { width ->
        (width * cos(angleRadians) + estimatedSize.height * sin(angleRadians)).toFloat()
    } ?: estimatedProjectedWidth
    val capacityPlotLeft = max(
        max(yAxisLeft, estimatedProjectedWidth - estimatedProjectedRightExtent),
        reservedStartLabelLeftExtent,
    )
    val estimatedMaximumTickCount = maximumLineGraphTickCount(
        plotWidth = right - capacityPlotLeft,
        maximumProjectedWidth = capacityProjectedLabelWidth,
        minimumGap = minimumGap,
    )
    val divisionCount = lineGraphTimeDivisionCount(
        fullSpan = fullMaxX - fullMinX,
        visibleSpan = visibleSpan,
        maximumTickCount = estimatedMaximumTickCount,
    )
    val dataTicks = lineGraphTimeTickCandidates(
        timestamps = allTimestamps,
        fullMinX = fullMinX,
        fullMaxX = fullMaxX,
        visibleMinX = visibleMinX,
        visibleMaxX = visibleMaxX,
        divisionCount = divisionCount,
    ).map(::tickAt)
    val coreTicks = (listOf(startBoundaryTick) + dataTicks)
        .distinctBy { it.epochMillis }
        .sortedBy { it.epochMillis }
    val xTicks = selectLineGraphXTicks(
        candidates = coreTicks + endBoundaryTick,
        minX = visibleMinX,
        maxX = visibleMaxX,
        plotLeft = left,
        plotWidth = preliminaryPlotWidth,
        minimumGap = minimumGap,
    )
    val maxLabelWidth = max(
        xTicks.maxOfOrNull { tick ->
            xLabelSizes.getValue(tick.label).width
        }?.toFloat() ?: 0f,
        reservedXLabelWidth ?: 0f,
    )
    val rotatedLabelHeight = (
        maxLabelWidth * sin(angleRadians) + labelHeight * cos(angleRadians)
        ).toFloat()
    val bottom = height - rotatedLabelHeight - plotBottomPadding.value * density
    if (right <= left || bottom <= top) return null
    val plot = Rect(left, top, right, bottom)
    return LineGraphLayout(
        plotRect = plot,
        minX = visibleMinX,
        maxX = visibleMaxX,
        minY = yValues.first(),
        maxY = yValues.last(),
        xTicks = xTicks,
        yTicks = yTicks,
    )
}

internal fun maximumLineGraphTickCount(
    plotWidth: Float,
    maximumProjectedWidth: Float,
    minimumGap: Float,
): Int = floor(plotWidth / max(1f, maximumProjectedWidth + minimumGap)).toInt().coerceAtLeast(1)

private fun projectedLabelWidth(size: IntSize, angleRadians: Double): Float =
    (size.width * cos(angleRadians) + size.height * sin(angleRadians)).toFloat()

private fun projectedLabelRightExtent(size: IntSize, angleRadians: Double): Float =
    (size.height * sin(angleRadians)).toFloat()

internal fun lineGraphTimeDivisionCount(
    fullSpan: Long,
    visibleSpan: Long,
    maximumTickCount: Int,
): Long {
    if (fullSpan <= 0L || visibleSpan <= 0L) return 1L
    val desiredDivisions = floor(
        maximumTickCount.coerceAtLeast(1) * fullSpan.toDouble() / visibleSpan
    ).toLong().coerceAtLeast(1L)
    var divisions = 1L
    while (divisions <= Long.MAX_VALUE / 2 && divisions * 2 <= desiredDivisions) {
        divisions *= 2
    }
    return divisions
}

internal fun lineGraphTimeTickCandidates(
    timestamps: List<Long>,
    fullMinX: Long,
    fullMaxX: Long,
    visibleMinX: Long,
    visibleMaxX: Long,
    divisionCount: Long,
): List<Long> {
    if (timestamps.isEmpty() || fullMaxX <= fullMinX || divisionCount < 1L) return emptyList()
    val fullSpan = fullMaxX - fullMinX
    val firstDivision = ceil(
        (visibleMinX - fullMinX).toDouble() / fullSpan * divisionCount
    ).toLong().coerceIn(0L, divisionCount)
    val lastDivision = floor(
        (visibleMaxX - fullMinX).toDouble() / fullSpan * divisionCount
    ).toLong().coerceIn(0L, divisionCount)
    if (firstDivision > lastDivision) return emptyList()

    return (firstDivision..lastDivision)
        .map { division ->
            fullMinX + (fullSpan.toDouble() * division / divisionCount).roundToLong()
        }
        .map { target -> timestamps.nearestValue(target) }
        .filter { it in visibleMinX..visibleMaxX }
        .distinct()
}

private fun List<Long>.nearestValue(target: Long): Long {
    val index = binarySearch(target)
    if (index >= 0) return this[index]
    val insertionIndex = -index - 1
    if (insertionIndex == 0) return first()
    if (insertionIndex == size) return last()
    val before = this[insertionIndex - 1]
    val after = this[insertionIndex]
    return if (target.toDouble() - before <= after.toDouble() - target) before else after
}

/** Greedily keeps timestamp labels whose measured, rotated bounds do not overlap. */
internal fun selectLineGraphXTicks(
    candidates: List<XTick>,
    minX: Long,
    maxX: Long,
    plotLeft: Float,
    plotWidth: Float,
    minimumGap: Float,
): List<XTick> {
    if (candidates.isEmpty()) return emptyList()
    val selected = mutableListOf<XTick>()
    var previousRight = Float.NEGATIVE_INFINITY
    fun horizontalBounds(tick: XTick): Pair<Float, Float> {
        val x = plotLeft + ((tick.epochMillis - minX).toDouble() / (maxX - minX) * plotWidth).toFloat()
        return x - tick.projectedLeftExtent to x + tick.projectedRightExtent
    }
    candidates
        .distinctBy { it.epochMillis }
        .sortedBy { it.epochMillis }
        .forEach { tick ->
            val (labelLeft, labelRight) = horizontalBounds(tick)
            if (labelLeft >= 0f && labelLeft >= previousRight + minimumGap) {
                selected += tick
                previousRight = labelRight
            }
        }
    return selected
}

internal fun calculateYTicks(
    rawMin: Double,
    rawMax: Double,
    targetCount: Int,
    fixed: Boolean,
    durationBasedRange: Boolean = false,
): List<Double> {
    val parameters = yAxisIntervalHelper.getYParameters(
        yMin = rawMin,
        yMax = rawMax,
        isDurationBasedRange = durationBasedRange,
        fixedBounds = fixed,
        approximateLineCount = targetCount,
    )
    val step = (parameters.boundsMax - parameters.boundsMin) / (parameters.subdivides - 1)
    return List(parameters.subdivides) { parameters.boundsMin + step * it }
        .let { it.dropLast(1) + parameters.boundsMax }
}

internal fun expandEqualRange(minValue: Double, maxValue: Double): Pair<Double, Double> {
    if (maxValue > minValue) return minValue to maxValue
    val padding = max(1.0, abs(minValue) * 0.1)
    return minValue - padding to maxValue + padding
}

private enum class LineGraphXLabelFormat { SECOND, MINUTE, WEEKDAY, DAY, MONTH }

private fun visibleXLabelFormat(durationMillis: Long): LineGraphXLabelFormat = when {
    Duration.ofMillis(durationMillis).toMinutes() < 5L -> LineGraphXLabelFormat.SECOND
    Duration.ofMillis(durationMillis).toDays() < 1L -> LineGraphXLabelFormat.MINUTE
    Duration.ofMillis(durationMillis).toDays() < 14L -> LineGraphXLabelFormat.WEEKDAY
    Duration.ofMillis(durationMillis).toDays() < 304L -> LineGraphXLabelFormat.DAY
    else -> LineGraphXLabelFormat.MONTH
}

internal fun maximumLineGraphXLabelWidth(
    durationMillis: Long,
    labelText: LineGraphXLabelText,
    measureText: (String) -> IntSize,
): Float {
    val candidates = when (visibleXLabelFormat(durationMillis)) {
        LineGraphXLabelFormat.SECOND -> listOf("88:88:88")
        LineGraphXLabelFormat.MINUTE -> listOf("88:88")
        LineGraphXLabelFormat.WEEKDAY -> labelText.weekdays.map {
            formatLineGraphLabel(labelText.weekdayDayFormat, it, "88")
        }
        LineGraphXLabelFormat.DAY -> labelText.months.map {
            formatLineGraphLabel(labelText.dayMonthFormat, "88", it)
        }
        LineGraphXLabelFormat.MONTH -> labelText.months.map {
            formatLineGraphLabel(labelText.monthYearFormat, it, "88")
        }
    }
    return candidates.maxOf { measureText(it).width }.toFloat()
}

private fun formatLineGraphTimestamp(
    epochMillis: Long,
    format: LineGraphXLabelFormat,
    zoneId: ZoneId,
    labelText: LineGraphXLabelText,
): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
    return when (format) {
        LineGraphXLabelFormat.SECOND -> dateTime.format(lineGraphSecondFormatter)
        LineGraphXLabelFormat.MINUTE -> dateTime.format(lineGraphMinuteFormatter)
        LineGraphXLabelFormat.WEEKDAY -> formatLineGraphLabel(
            labelText.weekdayDayFormat,
            labelText.weekdays[dateTime.dayOfWeek.value - 1],
            twoDigitString(dateTime.dayOfMonth),
        )
        LineGraphXLabelFormat.DAY -> formatLineGraphLabel(
            labelText.dayMonthFormat,
            twoDigitString(dateTime.dayOfMonth),
            labelText.months[dateTime.monthValue - 1],
        )
        LineGraphXLabelFormat.MONTH -> formatLineGraphLabel(
            labelText.monthYearFormat,
            labelText.months[dateTime.monthValue - 1],
            twoDigitString(Math.floorMod(dateTime.year, 100)),
        )
    }
}

private fun formatLineGraphLabel(format: String, first: String, second: String): String =
    String.format(format, first, second)

private fun twoDigitString(value: Int): String = value.toString().padStart(2, '0')

internal fun formatLineGraphTimestamp(
    epochMillis: Long,
    durationMillis: Long,
    zoneId: ZoneId,
    labelText: LineGraphXLabelText,
): String = formatLineGraphTimestamp(
    epochMillis,
    visibleXLabelFormat(durationMillis),
    zoneId,
    labelText,
)

internal fun pointsForViewport(
    points: List<LineGraphPoint>,
    minX: Long,
    maxX: Long,
): List<LineGraphPoint> {
    if (points.isEmpty()) return emptyList()
    val firstInside = points.indexOfFirst { it.timestamp.toInstant().toEpochMilli() >= minX }
        .let { if (it == -1) points.lastIndex else it }
    val lastInside = points.indexOfLast { it.timestamp.toInstant().toEpochMilli() <= maxX }
        .let { if (it == -1) 0 else it }
    val from = (min(firstInside, lastInside) - 1).coerceAtLeast(0)
    val to = (max(firstInside, lastInside) + 1).coerceAtMost(points.lastIndex)
    return points.subList(from, to + 1)
}

private fun formatLineGraphNumber(value: Double): String = synchronized(lineGraphNumberFormatter) {
    lineGraphNumberFormatter.format(value)
}

private class LineGraphPerformanceLogger(
    private val graphId: Long,
) {
    private var logged = false
    private var firstRenderWorkNanos = 0L
    private var inputPointCount = 0
    private var finitePointCount = 0
    private var mergedPointCount = 0

    fun now(): Long = SystemClock.elapsedRealtimeNanos()

    fun startMeasurement(): Long? = if (logged) null else now()

    fun recordPreparation(
        startedAt: Long,
        inputPointCount: Int,
        finitePointCount: Int,
    ) {
        firstRenderWorkNanos += elapsedNanos(startedAt)
        this.inputPointCount = inputPointCount
        this.finitePointCount = finitePointCount
    }

    fun recordPointPreparation(startedAt: Long, pointCount: Int) {
        firstRenderWorkNanos += elapsedNanos(startedAt)
        mergedPointCount = pointCount
    }

    fun recordLayout(startedAt: Long?) {
        if (startedAt != null) firstRenderWorkNanos += elapsedNanos(startedAt)
    }

    fun recordFirstDraw(
        startedAt: Long?,
        visiblePointCount: Int,
        lineCount: Int,
        xTickCount: Int,
        yTickCount: Int,
    ) {
        if (logged) return
        logged = true
        firstRenderWorkNanos += elapsedNanos(requireNotNull(startedAt))

        Timber.tag(PERFORMANCE_LOG_TAG).i(
            "graph=%d firstRenderWorkMs=%.3f lines=%d inputPoints=%d finitePoints=%d mergedPoints=%d visiblePoints=%d xTicks=%d yTicks=%d",
            graphId,
            firstRenderWorkNanos / 1_000_000.0,
            lineCount,
            inputPointCount,
            finitePointCount,
            mergedPointCount,
            visiblePointCount,
            xTickCount,
            yTickCount,
        )
    }

    private fun elapsedNanos(startedAt: Long) = now() - startedAt
}
