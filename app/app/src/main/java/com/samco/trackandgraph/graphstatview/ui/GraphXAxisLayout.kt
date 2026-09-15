/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal val graphAxisLabelMinimumGap = 4.dp

internal data class GraphXAxisTick<T>(
    val value: T,
    val label: String,
    val projectedWidth: Float,
    val projectedRightExtent: Float = 0f,
) {
    val projectedLeftExtent: Float get() = projectedWidth - projectedRightExtent
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

private fun nextPowerOfTwo(value: Int): Int {
    var result = 1
    while (result < value && result <= Int.MAX_VALUE / 2) result *= 2
    return result
}
