/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.ui.dataVisColorList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PieChartViewTest {

    @Test
    fun rejectsInvalidOrEmptySegments() {
        assertFalse(hasRenderablePieSegments(null))
        assertFalse(hasRenderablePieSegments(emptyList()))
        assertFalse(hasRenderablePieSegments(listOf(segment(0.0))))
        assertFalse(hasRenderablePieSegments(listOf(segment(-1.0))))
        assertFalse(hasRenderablePieSegments(listOf(segment(Double.NaN))))
    }

    @Test
    fun acceptsFiniteSegmentsWithPositiveTotal() {
        assertTrue(hasRenderablePieSegments(listOf(segment(0.0), segment(1.0))))
    }

    @Test
    fun automaticColorsNeverMatchAcrossTheWrapBoundary() {
        (2..dataVisColorList.size * 4).forEach { segmentCount ->
            val colors = resolvePieSegmentColors(List(segmentCount) { segment(1.0) })

            assertEquals(segmentCount, colors.size)
            assertTrue(colors.zipWithNext().all { (first, second) -> first != second })
            assertTrue(colors.first() != colors.last())
        }

        assertTrue(shouldLabelPieSegments(dataVisColorList.size + 1))
        assertFalse(shouldLabelPieSegments(dataVisColorList.size))
    }

    @Test
    fun arcLabelSitsJustInsideOuterArc() {
        assertEquals(
            Offset(185f, 100f),
            calculatePieArcLabelCenter(
                center = Offset(100f, 100f),
                radius = 100f,
                angleDegrees = 0f,
                labelSize = IntSize(20, 10),
                padding = 5f,
            ),
        )
        assertEquals(
            Offset(100f, 190f),
            calculatePieArcLabelCenter(
                center = Offset(100f, 100f),
                radius = 100f,
                angleDegrees = 90f,
                labelSize = IntSize(20, 10),
                padding = 5f,
            ),
        )
    }

    private fun segment(value: Double) = IPieChartViewData.Segment(
        value = value,
        title = "",
        color = null,
    )
}
