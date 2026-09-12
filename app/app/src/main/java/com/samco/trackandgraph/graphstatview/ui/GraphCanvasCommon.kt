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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.yield
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal const val graphRevealDurationMillis = 450
internal val graphPlotStartPadding = 14.dp
internal val graphPlotEndPadding = 8.dp
internal val graphPlotTopPadding = 8.dp
internal val graphPlotBottomPadding = 10.dp
internal val graphYAxisLabelPadding = 6.dp
internal val graphXAxisTickLength = 3.dp
internal val graphXAxisLabelPadding = 6.dp
internal val graphAxisLabelMinimumGap = 4.dp

internal data class GraphXAxisTick<T>(
    val value: T,
    val label: String,
    val projectedWidth: Float,
    val projectedRightExtent: Float = 0f,
) {
    val projectedLeftExtent: Float get() = projectedWidth - projectedRightExtent
}

internal data class GraphYAxisTick(val value: Double, val label: String)

@Composable
internal fun rememberGraphReveal(key: Any?, ready: Boolean): Animatable<Float, *> {
    val reveal = remember(key) { Animatable(0f) }
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
