/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 */

package com.samco.trackandgraph.graphstatview.factories.helpers

import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.BarChartSeries

internal fun validateFiniteGraphValues(values: Iterable<Double>): GraphStatInitException? =
    if (values.any { !it.isFinite() }) {
        GraphStatInitException(R.string.graph_non_finite_data_error)
    } else {
        null
    }

internal fun validateGraphYRange(yMin: Double, yMax: Double): GraphStatInitException? =
    if (!yMin.isFinite() || !yMax.isFinite() || yMax < yMin) {
        GraphStatInitException(R.string.graph_invalid_y_range_error)
    } else {
        null
    }

internal fun validateOptionalGraphYRange(
    yMin: Double?,
    yMax: Double?,
): GraphStatInitException? = when {
    (yMin == null) != (yMax == null) -> GraphStatInitException(R.string.graph_invalid_y_range_error)
    yMin != null && yMax != null -> validateGraphYRange(yMin, yMax)
    else -> null
}

internal fun validateBarChartSeries(
    series: List<BarChartSeries>,
    bucketCount: Int,
): GraphStatInitException? = validateGraphSeries(
    valuesBySeries = series.map { it.values },
    bucketCount = bucketCount,
)

internal fun validateGraphSeries(
    valuesBySeries: List<List<Double>>,
    bucketCount: Int,
): GraphStatInitException? = when {
    valuesBySeries.any { it.size != bucketCount } ->
        GraphStatInitException(R.string.graph_inconsistent_series_error)
    else -> validateFiniteGraphValues(valuesBySeries.asSequence().flatten().asIterable())
}
