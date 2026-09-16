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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val graphXAxisLabelAngleRadians =
    Math.toRadians(abs(graphXAxisLabelAngle).toDouble())
private val graphXAxisLabelAngleCosine = cos(graphXAxisLabelAngleRadians).toFloat()
private val graphXAxisLabelAngleSine = sin(graphXAxisLabelAngleRadians).toFloat()

internal data class GraphXAxisLabelMetrics(
    val width: Float,
    val height: Float,
) {
    constructor(size: IntSize) : this(size.width.toFloat(), size.height.toFloat())

    val projectedLeftExtent: Float = width * graphXAxisLabelAngleCosine
    val projectedRightExtent: Float = height * graphXAxisLabelAngleSine
    val projectedWidth: Float = projectedLeftExtent + projectedRightExtent
    val projectedHeight: Float =
        width * graphXAxisLabelAngleSine + height * graphXAxisLabelAngleCosine

    /**
     * Horizontal distance between the labels' right-edge anchors that separates their rotated
     * rectangles. The rectangles are disjoint once either their text axes or height axes separate.
     */
    fun minimumAnchorDistanceTo(next: GraphXAxisLabelMetrics): Float {
        val distanceAlongText = if (graphXAxisLabelAngleCosine > 0f) {
            next.width / graphXAxisLabelAngleCosine
        } else Float.POSITIVE_INFINITY
        val distanceAcrossHeight = if (graphXAxisLabelAngleSine > 0f) {
            height / graphXAxisLabelAngleSine
        } else Float.POSITIVE_INFINITY
        return minOf(distanceAlongText, distanceAcrossHeight)
    }
}

internal data class GraphXAxisTick<T>(
    val value: T,
    val label: String,
    val metrics: GraphXAxisLabelMetrics,
) {
    val projectedLeftExtent: Float get() = metrics.projectedLeftExtent
    val projectedRightExtent: Float get() = metrics.projectedRightExtent
}

internal fun graphRotatedLabelWidth(size: IntSize): Float {
    return GraphXAxisLabelMetrics(size).projectedWidth
}

internal fun graphRotatedLabelRightExtent(size: IntSize): Float {
    return GraphXAxisLabelMetrics(size).projectedRightExtent
}

internal fun graphRotatedLabelHeight(maximumWidth: Float, labelHeight: Float): Float {
    return GraphXAxisLabelMetrics(maximumWidth, labelHeight).projectedHeight
}

/** Power-of-two bucket spacing shared by categorical charts such as bars and histograms. */
internal fun calculateCategoricalGraphLabelSpacing(
    visibleBucketCount: Int,
    maximumLabelMetrics: GraphXAxisLabelMetrics,
    bucketWidth: Float,
): Int {
    var densitySpacing = 1
    while (visibleBucketCount.toDouble() / densitySpacing > 10.0) densitySpacing *= 2
    if (bucketWidth <= 0f || !bucketWidth.isFinite()) return densitySpacing
    val minimumAnchorDistance = maximumLabelMetrics.minimumAnchorDistanceTo(maximumLabelMetrics)
    val widthSpacing = ceil(minimumAnchorDistance / bucketWidth)
        .toInt()
        .coerceAtLeast(1)
    return nextPowerOfTwo(max(densitySpacing, widthSpacing))
}

/** Greedily retains measured labels whose rotated rectangles fit without colliding. */
internal fun <T> selectGraphXAxisTicks(
    candidates: List<GraphXAxisTick<T>>,
    xToPixel: (T) -> Float,
    minimumX: Float,
    maximumX: Float,
): List<GraphXAxisTick<T>> {
    val selected = mutableListOf<GraphXAxisTick<T>>()
    var previousTick: GraphXAxisTick<T>? = null
    var previousX = 0f
    candidates.forEach { tick ->
        val x = xToPixel(tick.value)
        val left = x - tick.projectedLeftExtent
        val right = x + tick.projectedRightExtent
        val minimumAnchorDistance = previousTick?.metrics?.minimumAnchorDistanceTo(tick.metrics) ?: 0f
        if (
            left >= minimumX && right <= maximumX &&
            x - previousX >= minimumAnchorDistance
        ) {
            selected += tick
            previousTick = tick
            previousX = x
        }
    }
    return selected
}

private fun nextPowerOfTwo(value: Int): Int {
    var result = 1
    while (result < value && result <= Int.MAX_VALUE / 2) result *= 2
    return result
}
