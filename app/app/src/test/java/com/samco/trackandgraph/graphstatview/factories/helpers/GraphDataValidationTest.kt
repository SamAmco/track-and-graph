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
import com.samco.trackandgraph.graphstatview.factories.viewdto.BarChartSeries
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GraphDataValidationTest {

    @Test
    fun nonFiniteValuesReturnSpecificError() {
        val error = validateFiniteGraphValues(listOf(1.0, Double.NaN))

        assertEquals(R.string.graph_non_finite_data_error, error?.errorTextId)
        assertNull(validateFiniteGraphValues(listOf(-1.0, 0.0, 1.0)))
    }

    @Test
    fun invalidYRangesReturnSpecificError() {
        assertEquals(
            R.string.graph_invalid_y_range_error,
            validateGraphYRange(2.0, 1.0)?.errorTextId,
        )
        assertEquals(
            R.string.graph_invalid_y_range_error,
            validateOptionalGraphYRange(null, 1.0)?.errorTextId,
        )
        assertNull(validateGraphYRange(-1.0, 1.0))
        assertNull(validateOptionalGraphYRange(null, null))
    }

    @Test
    fun inconsistentSeriesReturnSpecificError() {
        val series = listOf(
            BarChartSeries("A", listOf(1.0), ColorSpec.ColorIndex(0)),
        )

        assertEquals(
            R.string.graph_inconsistent_series_error,
            validateBarChartSeries(series, bucketCount = 2)?.errorTextId,
        )
    }
}
