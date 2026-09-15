/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.graphstatview.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphXAxisLayoutTest {

    @Test
    fun rotatedRectanglesNeedLessSpaceThanTheirProjectedBounds() {
        val metrics = GraphXAxisLabelMetrics(width = 60f, height = 12f)
        val anchorDistance = metrics.minimumAnchorDistanceTo(metrics, minimumGap = 4f)

        assertTrue(anchorDistance < metrics.projectedWidth + 4f)
        assertEquals(34.08f, anchorDistance, 0.01f)
    }

    @Test
    fun selectionAllowsProjectedBoundsToOverlapWhenRotatedRectanglesDoNot() {
        val metrics = GraphXAxisLabelMetrics(width = 60f, height = 12f)
        val candidates = listOf(
            GraphXAxisTick(0f, "first", metrics),
            GraphXAxisTick(35f, "second", metrics),
        )

        val selected = selectGraphXAxisTicks(
            candidates = candidates,
            xToPixel = { it },
            minimumX = Float.NEGATIVE_INFINITY,
            maximumX = Float.POSITIVE_INFINITY,
            minimumGap = 4f,
        )

        assertTrue(35f < metrics.projectedWidth + 4f)
        assertEquals(candidates, selected)
    }
}
