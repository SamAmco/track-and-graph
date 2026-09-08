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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.YRangeType
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import timber.log.Timber

private const val X_LABEL_ANGLE = -28f
private const val REVEAL_DURATION_MILLIS = 450
private const val PERFORMANCE_LOG_TAG = "LineGraphPerf"
private val lineWidth = 2.dp
private val vertexWidth = 6.dp

private val lineGraphSecondFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val lineGraphMinuteFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val lineGraphDayFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
private val lineGraphMonthFormatter = DateTimeFormatter.ofPattern("MMM’yy", Locale.ENGLISH)
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

    val textMeasurer = rememberTextMeasurer()
    val axisFont = remember { FontFamily(Font(R.font.roboto_mono)) }
    val axisTextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = axisFont,
        fontSize = 10.sp,
    )
    val pointTextStyle = axisTextStyle.copy(fontSize = 9.sp)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val markerColor = MaterialTheme.colorScheme.error
    val density = LocalDensity.current
    val graphHeight = graphHeightFor(graphViewMode, hasLegend = true)
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var layout by remember(viewData) { mutableStateOf<LineGraphLayout?>(null) }
    val reveal = remember(viewData) { Animatable(0f) }
    val viewportTransform = rememberUpdatedState<(Float, Float) -> Unit> { panX, gestureZoom ->
        val plotWidth = layout?.plotRect?.width ?: return@rememberUpdatedState
        zoom = (zoom * gestureZoom).coerceIn(1.0, maximumZoom)
        val newHalfSpanFraction = 0.5 / zoom
        centerFraction = (centerFraction - panX / plotWidth / zoom)
            .coerceIn(newHalfSpanFraction, 1.0 - newHalfSpanFraction)
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
            measureText = { text -> textMeasurer.measure(text, axisTextStyle).size },
            density = density.density,
        )
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
                detectLineGraphPinchGestures { panX, gestureZoom ->
                    viewportTransform.value(panX, gestureZoom)
                }
            }
            .pointerInput(isInteractive, canvasSize, maximumZoom) {
                if (!isInteractive) return@pointerInput
                detectHorizontalDragGestures { _, dragAmount ->
                    viewportTransform.value(dragAmount, 1f)
                }
            }
    ) {
        val currentLayout = layout ?: return@Canvas
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
            drawLine(revealedGridColor, Offset(plot.left, y), Offset(plot.right, y), 0.5.dp.toPx())
            val measured = textMeasurer.measure(tick.label, axisTextStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = tick.label,
                style = revealedAxisStyle,
                topLeft = Offset(plot.left - measured.size.width - 6.dp.toPx(), y - measured.size.height / 2f),
            )
        }
        drawLine(revealedGridColor, Offset(plot.left, plot.top), Offset(plot.left, plot.bottom), 0.5.dp.toPx())

        currentLayout.xTicks.forEach { tick ->
            val x = currentLayout.xToPixel(tick.epochMillis)
            drawLine(revealedGridColor, Offset(x, plot.top), Offset(x, plot.bottom + 3.dp.toPx()), 0.5.dp.toPx())
            val measured = textMeasurer.measure(tick.label, axisTextStyle)
            val pivot = Offset(x, plot.bottom + 6.dp.toPx())
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
                        3.dp.toPx(),
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
                                Offset(pointOffset.x - measured.size.width - 3.dp.toPx(), pointOffset.y - measured.size.height),
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
    onTransform: (panX: Float, zoom: Float) -> Unit,
) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    var transforming = false
    do {
        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
        if (event.changes.count { it.pressed } > 1) transforming = true
        if (transforming) {
            val pan = event.calculatePan()
            val zoom = event.calculateZoom()
            if (pan.x != 0f || zoom != 1f) onTransform(pan.x, zoom)
            event.changes.forEach { it.consume() }
        }
    } while (event.changes.any { it.pressed })
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

internal data class XTick(val epochMillis: Long, val label: String, val projectedWidth: Float)
internal data class YTick(val value: Double, val label: String)
private data class IndexedXTick(val index: Int, val tick: XTick)

internal data class LineGraphViewport(val minX: Long, val maxX: Long)

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
    measureText: (String) -> IntSize,
    density: Float,
): LineGraphLayout? {
    val visiblePoints = points.filter {
        it.timestamp.toInstant().toEpochMilli() in visibleMinX..visibleMaxX
    }.ifEmpty { points }
    val rawYMin = fixedYMin ?: visiblePoints.minOfOrNull { it.value } ?: return null
    val rawYMax = fixedYMax ?: visiblePoints.maxOfOrNull { it.value } ?: return null
    if (!rawYMin.isFinite() || !rawYMax.isFinite() || rawYMax < rawYMin) return null

    val initialRange = expandEqualRange(rawYMin, rawYMax)
    val approximateTickCount = (height / (32f * density)).toInt().coerceIn(3, 8)
    val yValues = calculateYTicks(
        initialRange.first,
        initialRange.second,
        approximateTickCount,
        fixedYMin != null && fixedYMax != null,
    )
    val yTicks = yValues.map { value ->
        YTick(value, if (durationBasedRange) formatTimeDuration(value.roundToLong()) else formatLineGraphNumber(value))
    }
    val yLabelSizes = yTicks.map { measureText(it.label) }
    val widestYLabel = yLabelSizes.maxOf { it.width }.toFloat()
    val labelHeight = yLabelSizes.maxOf { it.height }.toFloat()
    val left = widestYLabel + 14f * density
    val right = width - 8f * density
    val top = max(8f * density, labelHeight / 2f)

    val angleRadians = Math.toRadians(abs(X_LABEL_ANGLE).toDouble())
    val xLabelSizes = mutableMapOf<String, IntSize>()
    val candidates = mutableListOf<IndexedXTick>()
    var uniqueTimestampCount = 0
    var previousMillis: Long? = null
    points.forEach { point ->
        val millis = point.timestamp.toInstant().toEpochMilli()
        if (millis != previousMillis) {
            previousMillis = millis
            val index = uniqueTimestampCount++
            if (millis in visibleMinX..visibleMaxX) {
                val label = formatLineGraphTimestamp(
                    epochMillis = millis,
                    durationMillis = visibleMaxX - visibleMinX,
                    zoneId = ZoneId.systemDefault(),
                )
                val measured = xLabelSizes.getOrPut(label) { measureText(label) }
                candidates += IndexedXTick(
                    index = index,
                    tick = XTick(
                        epochMillis = millis,
                        label = label,
                        projectedWidth = (measured.width * cos(angleRadians) + measured.height * sin(angleRadians)).toFloat(),
                    ),
                )
            }
        }
    }
    val maxLabelWidth = xLabelSizes.values.maxOfOrNull { it.width }?.toFloat() ?: 0f
    val rotatedLabelHeight = (
        maxLabelWidth * sin(angleRadians) + labelHeight * cos(angleRadians)
        ).toFloat()
    val bottom = height - rotatedLabelHeight - 10f * density
    if (right <= left || bottom <= top) return null
    val plot = Rect(left, top, right, bottom)
    val minimumGap = 4f * density
    val maximumProjectedWidth = candidates.maxOfOrNull { it.tick.projectedWidth } ?: 0f
    val maximumTickCount = maximumLineGraphTickCount(
        plotWidth = plot.width,
        maximumProjectedWidth = maximumProjectedWidth,
        minimumGap = minimumGap,
    )
    val stride = anchoredLineGraphTickStride(
        totalTimestampCount = uniqueTimestampCount,
        visibleSpan = visibleMaxX - visibleMinX,
        fullSpan = points.last().timestamp.toInstant().toEpochMilli() -
            points.first().timestamp.toInstant().toEpochMilli(),
        maximumTickCount = maximumTickCount,
    )
    val anchoredCandidates = if (candidates.size <= maximumTickCount) {
        candidates.map { it.tick }
    } else {
        candidates.filter { it.index % stride == 0 }.map { it.tick }
    }
    val xTicks = selectLineGraphXTicks(
        candidates = anchoredCandidates,
        minX = visibleMinX,
        maxX = visibleMaxX,
        plotLeft = plot.left,
        plotWidth = plot.width,
        minimumGap = minimumGap,
    )
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

internal fun anchoredLineGraphTickStride(
    totalTimestampCount: Int,
    visibleSpan: Long,
    fullSpan: Long,
    maximumTickCount: Int,
): Int {
    if (totalTimestampCount <= 1 || fullSpan <= 0L) return 1
    val estimatedVisibleCount = ceil(
        totalTimestampCount * visibleSpan.toDouble() / fullSpan.toDouble()
    ).toInt().coerceIn(1, totalTimestampCount)
    val requiredStride = ceil(estimatedVisibleCount.toDouble() / maximumTickCount.coerceAtLeast(1))
        .toInt()
        .coerceAtLeast(1)
    var stride = 1
    while (stride < requiredStride && stride <= Int.MAX_VALUE / 2) stride *= 2
    return stride
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
    candidates.forEach { tick ->
        val x = plotLeft + ((tick.epochMillis - minX).toDouble() / (maxX - minX) * plotWidth).toFloat()
        val labelLeft = x - tick.projectedWidth
        if (labelLeft >= 0f && labelLeft >= previousRight + minimumGap) {
            selected += tick
            previousRight = x
        }
    }
    return selected
}

internal fun calculateYTicks(
    rawMin: Double,
    rawMax: Double,
    targetCount: Int,
    fixed: Boolean,
): List<Double> {
    if (fixed) {
        val step = (rawMax - rawMin) / (targetCount - 1)
        return List(targetCount) { rawMin + step * it }.let { it.dropLast(1) + rawMax }
    }
    val step = niceStep((rawMax - rawMin) / (targetCount - 1))
    val minValue = floor(rawMin / step) * step
    val maxValue = ceil(rawMax / step) * step
    val count = ((maxValue - minValue) / step).roundToLong().toInt() + 1
    return List(count) { minValue + step * it }.let { it.dropLast(1) + maxValue }
}

private fun niceStep(rawStep: Double): Double {
    val exponent = floor(log10(rawStep))
    val magnitude = 10.0.pow(exponent)
    val fraction = rawStep / magnitude
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 2.5 -> 2.5
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return niceFraction * magnitude
}

internal fun expandEqualRange(minValue: Double, maxValue: Double): Pair<Double, Double> {
    if (maxValue > minValue) return minValue to maxValue
    val padding = max(1.0, abs(minValue) * 0.1)
    return minValue - padding to maxValue + padding
}

private fun xFormatterFor(durationMillis: Long): DateTimeFormatter = when {
    Duration.ofMillis(durationMillis).toMinutes() < 5L -> lineGraphSecondFormatter
    Duration.ofMillis(durationMillis).toDays() < 1L -> lineGraphMinuteFormatter
    Duration.ofMillis(durationMillis).toDays() < 304L -> lineGraphDayFormatter
    else -> lineGraphMonthFormatter
}

internal fun formatLineGraphTimestamp(
    epochMillis: Long,
    durationMillis: Long,
    zoneId: ZoneId,
): String = Instant.ofEpochMilli(epochMillis)
    .atZone(zoneId)
    .format(xFormatterFor(durationMillis))

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
