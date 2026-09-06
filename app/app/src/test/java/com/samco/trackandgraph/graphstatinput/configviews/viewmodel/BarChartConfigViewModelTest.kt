/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.graphstatinput.configviews.viewmodel

import com.samco.trackandgraph.data.database.dto.YRangeType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarChartConfigViewModelTest {

    @Test
    fun fixedYRangeMustHaveFinitePositiveMaximum() {
        assertTrue(isValidBarChartYRange(YRangeType.FIXED, 1.0))
        assertFalse(isValidBarChartYRange(YRangeType.FIXED, 0.0))
        assertFalse(isValidBarChartYRange(YRangeType.FIXED, -1.0))
        assertFalse(isValidBarChartYRange(YRangeType.FIXED, Double.NaN))
        assertFalse(isValidBarChartYRange(YRangeType.FIXED, Double.POSITIVE_INFINITY))
    }

    @Test
    fun dynamicYRangeDoesNotUseConfiguredMaximum() {
        assertTrue(isValidBarChartYRange(YRangeType.DYNAMIC, Double.NaN))
    }
}
