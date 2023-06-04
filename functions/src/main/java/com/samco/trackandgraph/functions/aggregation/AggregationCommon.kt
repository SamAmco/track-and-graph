package com.samco.trackandgraph.functions.aggregation

import com.samco.trackandgraph.base.database.dto.IDataPoint


internal inline fun sumDataPoints(points: List<IDataPoint>): Double = points.sumOf { it.value }

internal inline fun averageDataPoints(points: List<IDataPoint>): Double = points.map { it.value }.average()

internal inline fun dataPointsLabel(points: List<IDataPoint>) = when {
    points.isEmpty() -> ""
    points.all { it.label == points[0].label } -> points[0].label
    else -> ""
}