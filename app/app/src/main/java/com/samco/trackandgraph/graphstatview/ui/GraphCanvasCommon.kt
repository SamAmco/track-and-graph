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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.graphstatview.factories.helpers.DataDisplayIntervalHelper
import kotlinx.coroutines.yield
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal const val graphRevealDurationMillis = 450
private const val approximateGraphYTickSpacingDp = 32f
private const val minimumGraphYTickCount = 3
private const val maximumGraphYTickCount = 8
internal val graphPlotStartPadding = 14.dp
internal val graphPlotEndPadding = 8.dp
internal val graphPlotTopPadding = 8.dp
internal val graphPlotBottomPadding = 10.dp
internal val graphYAxisLabelPadding = 6.dp
internal val graphXAxisTickLength = 3.dp
internal val graphXAxisLabelPadding = 6.dp
internal val graphAxisLabelMinimumGap = 4.dp
internal val graphBarBorderThickness = 0.5.dp
private val graphYAxisIntervalHelper = DataDisplayIntervalHelper()

internal data class GraphXAxisTick<T>(
    val value: T,
    val label: String,
    val projectedWidth: Float,
    val projectedRightExtent: Float = 0f,
) {
    val projectedLeftExtent: Float get() = projectedWidth - projectedRightExtent
}

internal data class GraphYAxisTick(val value: Double, val label: String)

internal data class CategoricalGraphViewport(val minX: Double, val maxX: Double)

internal fun calculateCategoricalGraphViewport(
    bucketCount: Int,
    zoom: Double,
    centerFraction: Double,
    maximumZoom: Double = bucketCount.toDouble(),
): CategoricalGraphViewport {
    val count = bucketCount.coerceAtLeast(1).toDouble()
    val constrainedZoom = zoom.coerceIn(1.0, min(maximumZoom, count).coerceAtLeast(1.0))
    val visibleSpan = count / constrainedZoom
    val halfSpanFraction = 0.5 / constrainedZoom
    val constrainedCenter = centerFraction.coerceIn(halfSpanFraction, 1.0 - halfSpanFraction)
    val center = -0.5 + count * constrainedCenter
    return CategoricalGraphViewport(center - visibleSpan / 2.0, center + visibleSpan / 2.0)
}

internal suspend fun PointerInputScope.detectHorizontalGraphPinchGestures(
    onTransform: (panX: Float, zoom: Float) -> Unit,
) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    do {
        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
        if (event.changes.count { it.pressed } > 1) {
            val pan = event.calculatePan()
            val zoom = event.calculateZoom()
            if (pan.x != 0f || zoom != 1f) onTransform(pan.x, zoom)
            event.changes.forEach { it.consume() }
        }
    } while (event.changes.any { it.pressed })
}

@Composable
internal fun rememberGraphReveal(key: Any?, ready: Boolean): Animatable<Float, *> {
    val inspectionMode = LocalInspectionMode.current
    val reveal = remember(key, inspectionMode) { Animatable(if (inspectionMode) 1f else 0f) }
    LaunchedEffect(reveal, ready) {
        if (ready && reveal.value == 0f) {
            // Ensure the graph background gets one frame before content fades in.
            yield()
            reveal.animateTo(1f, tween(graphRevealDurationMillis))
        }
    }
    return reveal
}

internal fun graphRotatedLabelWidth(size: IntSize): Float {
    val radians = Math.toRadians(abs(graphXAxisLabelAngle).toDouble())
    return (size.width * cos(radians) + size.height * sin(radians)).toFloat()
}

internal fun graphRotatedLabelRightExtent(size: IntSize): Float {
    val radians = Math.toRadians(abs(graphXAxisLabelAngle).toDouble())
    return (size.height * sin(radians)).toFloat()
}

internal fun graphRotatedLabelHeight(maximumWidth: Float, labelHeight: Float): Float {
    val radians = Math.toRadians(abs(graphXAxisLabelAngle).toDouble())
    return (maximumWidth * sin(radians) + labelHeight * cos(radians)).toFloat()
}

/** Power-of-two bucket spacing shared by categorical charts such as bars and histograms. */
internal fun calculateCategoricalGraphLabelSpacing(
    visibleBucketCount: Int,
    maximumProjectedLabelWidth: Float,
    bucketWidth: Float,
    minimumGap: Float,
): Int {
    var densitySpacing = 1
    while (visibleBucketCount.toDouble() / densitySpacing > 10.0) densitySpacing *= 2
    if (bucketWidth <= 0f || !bucketWidth.isFinite()) return densitySpacing
    val widthSpacing = ceil((maximumProjectedLabelWidth + minimumGap) / bucketWidth)
        .toInt()
        .coerceAtLeast(1)
    return nextPowerOfTwo(max(densitySpacing, widthSpacing))
}

/** Greedily retains measured, rotated labels that fit within the available horizontal bounds. */
internal fun <T> selectGraphXAxisTicks(
    candidates: List<GraphXAxisTick<T>>,
    xToPixel: (T) -> Float,
    minimumX: Float,
    maximumX: Float,
    minimumGap: Float,
): List<GraphXAxisTick<T>> {
    val selected = mutableListOf<GraphXAxisTick<T>>()
    var previousRight = Float.NEGATIVE_INFINITY
    candidates.forEach { tick ->
        val x = xToPixel(tick.value)
        val left = x - tick.projectedLeftExtent
        val right = x + tick.projectedRightExtent
        if (left >= minimumX && right <= maximumX && left >= previousRight + minimumGap) {
            selected += tick
            previousRight = right
        }
    }
    return selected
}

internal fun approximateGraphYTickCount(height: Float, density: Float): Int =
    (height / (approximateGraphYTickSpacingDp * density))
        .toInt()
        .coerceIn(minimumGraphYTickCount, maximumGraphYTickCount)

internal fun calculateYTicks(
    rawMin: Double,
    rawMax: Double,
    targetCount: Int,
    fixed: Boolean,
    durationBasedRange: Boolean = false,
): List<Double> {
    val parameters = graphYAxisIntervalHelper.getYParameters(
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

private fun nextPowerOfTwo(value: Int): Int {
    var result = 1
    while (result < value && result <= Int.MAX_VALUE / 2) result *= 2
    return result
}

internal fun <T> DrawScope.drawGraphAxes(
    plot: Rect,
    xTicks: List<GraphXAxisTick<T>>,
    yTicks: List<GraphYAxisTick>,
    xToPixel: (T) -> Float,
    yToPixel: (Double) -> Float,
    textMeasurer: TextMeasurer,
    axisTextStyle: TextStyle,
    gridColor: Color,
    alpha: Float,
) {
    val revealedGridColor = gridColor.copy(alpha = gridColor.alpha * alpha)
    val revealedAxisStyle = axisTextStyle.copy(
        color = axisTextStyle.color.copy(alpha = axisTextStyle.color.alpha * alpha),
    )
    yTicks.forEach { tick ->
        val y = yToPixel(tick.value)
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
                plot.left - measured.size.width - graphYAxisLabelPadding.toPx(),
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
    xTicks.forEach { tick ->
        val x = xToPixel(tick.value)
        drawLine(
            revealedGridColor,
            Offset(x, plot.top),
            Offset(x, plot.bottom + graphXAxisTickLength.toPx()),
            graphGridLineThickness.toPx(),
        )
        val measured = textMeasurer.measure(tick.label, axisTextStyle)
        val pivot = Offset(x, plot.bottom + graphXAxisLabelPadding.toPx())
        rotate(graphXAxisLabelAngle, pivot) {
            drawText(
                textMeasurer = textMeasurer,
                text = tick.label,
                style = revealedAxisStyle,
                topLeft = Offset(pivot.x - measured.size.width, pivot.y),
            )
        }
    }
}

internal fun DrawScope.drawStackedGraphBars(
    plot: Rect,
    bucketRange: IntRange,
    valuesBySeries: List<List<Double>>,
    colors: List<Color>,
    xToPixel: (Double) -> Float,
    yToPixel: (Double) -> Float,
    alpha: Float,
    borderColor: Color,
    drawBorders: Boolean,
    highlightedBucket: Int? = null,
    highlightColor: Color = Color.Transparent,
) = clipRect(plot.left, plot.top, plot.right, plot.bottom) {
    val bucketWidth = xToPixel(0.5) - xToPixel(-0.5)
    bucketRange.forEach { bucket ->
        val centerX = xToPixel(bucket.toDouble())
        var positiveTotal = 0.0
        var negativeTotal = 0.0
        valuesBySeries.forEachIndexed { seriesIndex, values ->
            val value = values[bucket]
            val start = if (value >= 0.0) positiveTotal else negativeTotal
            val end = start + value
            if (value >= 0.0) positiveTotal = end else negativeTotal = end
            val top = min(yToPixel(start), yToPixel(end))
            val bottom = max(yToPixel(start), yToPixel(end))
            if (bottom > top) {
                val topLeft = Offset(centerX - bucketWidth / 2f, top)
                val size = Size(bucketWidth, bottom - top)
                drawRect(colors[seriesIndex].copy(alpha = alpha), topLeft, size)
                if (drawBorders) {
                    drawRect(
                        color = borderColor.copy(alpha = alpha),
                        topLeft = topLeft,
                        size = size,
                        style = Stroke(graphBarBorderThickness.toPx()),
                    )
                }
            }
        }
    }
    highlightedBucket?.let { bucket ->
        val centerX = xToPixel(bucket.toDouble())
        drawRect(
            color = highlightColor.copy(alpha = highlightColor.alpha * alpha),
            topLeft = Offset(centerX - bucketWidth / 2f, plot.top),
            size = Size(bucketWidth, plot.height),
        )
    }
}
