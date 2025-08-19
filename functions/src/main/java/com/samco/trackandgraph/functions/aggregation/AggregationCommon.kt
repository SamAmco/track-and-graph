package com.samco.trackandgraph.functions.aggregation

import com.samco.trackandgraph.data.database.dto.IDataPoint


internal fun sumDataPoints(points: List<IDataPoint>): Double = points.sumOf { it.value }

internal fun averageDataPoints(points: List<IDataPoint>): Double = points.map { it.value }.average()

internal fun dataPointsLabel(points: List<IDataPoint>) = when {
    points.isEmpty() -> ""
    points.all { it.label == points[0].label } -> points[0].label
    else -> ""
}