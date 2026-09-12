/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatview.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarChartViewTest {

    @Test
    fun doubleToStringClampsNegativeScaleForExponentValues() {
        assertEquals("10000000", doubleToString(1.0E7))
    }

    @Test
    fun doubleToStringLimitsDecimalPlaces() {
        assertEquals("123.457", doubleToString(123.4567))
        assertEquals("12.3", doubleToString(12.34, maxPlaces = 1))
    }

    @Test
    fun doubleToStringHandlesNonFiniteValues() {
        assertEquals("NaN", doubleToString(Double.NaN))
        assertEquals("Infinity", doubleToString(Double.POSITIVE_INFINITY))
    }

    @Test
    fun zeroLengthYRangeIsExpanded() {
        assertEquals(0.0 to 1.0, getRenderableBarChartYRange(0.0, 0.0))
    }

    @Test
    fun invalidYRangesAreRejected() {
        assertNull(getRenderableBarChartYRange(Double.NaN, 1.0))
        assertNull(getRenderableBarChartYRange(0.0, Double.POSITIVE_INFINITY))
        assertNull(getRenderableBarChartYRange(1.0, 0.0))
    }

    @Test
    fun xLabelSpacingRemainsPowerOfTwoWhenMeasuredLabelsNeedMoreRoom() {
        assertEquals(
            8,
            calculateCategoricalGraphLabelSpacing(
                visibleBucketCount = 26,
                maximumProjectedLabelWidth = 120f,
                bucketWidth = 25f,
                minimumGap = 4f,
            ),
        )
    }

    @Test
    fun xLabelSpacingRetainsApproximateTenLabelLimitWhenLabelsFit() {
        assertEquals(
            4,
            calculateCategoricalGraphLabelSpacing(
                visibleBucketCount = 26,
                maximumProjectedLabelWidth = 60f,
                bucketWidth = 25f,
                minimumGap = 4f,
            ),
        )
    }

    @Test
    fun barViewportInitiallyIncludesEveryBucketEdge() {
        assertEquals(BarChartViewport(-0.5, 25.5), calculateBarChartViewport(26, 1.0, 0.5))
    }

    @Test
    fun barViewportClampsZoomAndPanToBucketEdges() {
        assertEquals(BarChartViewport(-0.5, 0.5), calculateBarChartViewport(4, 99.0, -1.0))
        assertEquals(BarChartViewport(2.5, 3.5), calculateBarChartViewport(4, 4.0, 2.0))
    }
}
