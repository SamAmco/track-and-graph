/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
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

private const val X_LABEL_ANGLE = -28f
private const val REVEAL_DURATION_MILLIS = 450
private const val MAX_ZOOM = 8.0
private val lineWidth = 2.dp
private val vertexWidth = 6.dp

private val lineGraphSecondFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val lineGraphMinuteFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val lineGraphDayFormatter = DateTimeFormatter.ofPattern("dd MMM")
private val lineGraphMonthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
private val lineGraphNumberFormatter = DecimalFormat("#,##0.###")

@Composable
fun LineGraphView(
    modifier: Modifier = Modifier,
    viewData: ILineGraphViewData,
    graphViewMode: GraphViewMode,
    graphBackgroundColor: Color,
    timeMarker: OffsetDateTime? = null,
) {
    val renderableLines = remember(viewData.lines) {
        viewData.lines.map { line ->
            line.copy(points = line.points.filter { point -> point.value.isFinite() })
        }
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
) = Column(modifier = modifier) {
    val allPoints = remember(lines) { lines.flatMap { it.points }.sortedBy { it.timestamp } }
    val allTimes = remember(allPoints) {
        allPoints.map { it.timestamp.toInstant().toEpochMilli() }.distinct().sorted()
    }
    val fullMinX = allTimes.first()
    val fullMaxX = allTimes.last().let { if (it == fullMinX) it + 1L else it }
    val isInteractive = graphViewMode is GraphViewMode.FullScreenMode
    var zoom by remember(viewData) { mutableDoubleStateOf(1.0) }
    var centerFraction by remember(viewData) { mutableDoubleStateOf(0.5) }
    val visibleSpan = max(1.0, (fullMaxX - fullMinX).toDouble() / zoom)
    val halfSpanFraction = 0.5 / zoom
    val constrainedCenterFraction = centerFraction.coerceIn(halfSpanFraction, 1.0 - halfSpanFraction)
    val centerX = fullMinX + (fullMaxX - fullMinX) * constrainedCenterFraction
    val visibleMinX = (centerX - visibleSpan / 2.0).roundToLong()
    val visibleMaxX = (centerX + visibleSpan / 2.0).roundToLong()

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
        // Let the empty/background frame render before doing text measurement and revealing the plot.
        yield()
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
            .pointerInput(isInteractive, canvasSize) {
                if (!isInteractive) return@pointerInput
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val plotWidth = layout?.plotRect?.width ?: return@detectTransformGestures
                    zoom = (zoom * gestureZoom).coerceIn(1.0, MAX_ZOOM)
                    val newHalfSpanFraction = 0.5 / zoom
                    centerFraction = (centerFraction - pan.x / plotWidth / zoom)
                        .coerceIn(newHalfSpanFraction, 1.0 - newHalfSpanFraction)
                }
            }
    ) {
        val currentLayout = layout ?: return@Canvas
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
    }

    GraphLegend(
        items = lines.map { GraphLegendItem(color = getColor(it.color), label = it.name) }
    )
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
    val widestYLabel = yTicks.maxOf { measureText(it.label).width }.toFloat()
    val labelHeight = yTicks.maxOf { measureText(it.label).height }.toFloat()
    val left = widestYLabel + 14f * density
    val right = width - 8f * density
    val top = max(8f * density, labelHeight / 2f)

    val angleRadians = Math.toRadians(abs(X_LABEL_ANGLE).toDouble())
    val candidates = points.asSequence()
        .map { it.timestamp.toInstant().toEpochMilli() }
        .filter { it in visibleMinX..visibleMaxX }
        .distinct()
        .sorted()
        .map { millis ->
            val label = formatLineGraphTimestamp(
                epochMillis = millis,
                durationMillis = visibleMaxX - visibleMinX,
                zoneId = ZoneId.systemDefault(),
            )
            val measured = measureText(label)
            XTick(
                epochMillis = millis,
                label = label,
                projectedWidth = (measured.width * cos(angleRadians) + measured.height * sin(angleRadians)).toFloat(),
            )
        }
        .toList()
    val maxLabelWidth = candidates.maxOfOrNull { measureText(it.label).width }?.toFloat() ?: 0f
    val rotatedLabelHeight = (
        maxLabelWidth * sin(angleRadians) + labelHeight * cos(angleRadians)
        ).toFloat()
    val bottom = height - rotatedLabelHeight - 10f * density
    if (right <= left || bottom <= top) return null
    val plot = Rect(left, top, right, bottom)
    val xTicks = selectLineGraphXTicks(
        candidates = candidates,
        minX = visibleMinX,
        maxX = visibleMaxX,
        plotLeft = plot.left,
        plotWidth = plot.width,
        minimumGap = 4f * density,
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
