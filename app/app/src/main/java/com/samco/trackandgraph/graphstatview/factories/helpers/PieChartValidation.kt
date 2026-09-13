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

internal fun validatePieChartSegments(values: Iterable<Double>): GraphStatInitException? {
    validateFiniteGraphValues(values)?.let { return it }
    var hasPositive = false
    var hasNegative = false
    values.forEach { value ->
        if (value > 0.0) hasPositive = true
        if (value < 0.0) hasNegative = true
    }
    return if (hasPositive && hasNegative) {
        GraphStatInitException(R.string.pie_chart_mixed_signs_error)
    } else {
        null
    }
}
