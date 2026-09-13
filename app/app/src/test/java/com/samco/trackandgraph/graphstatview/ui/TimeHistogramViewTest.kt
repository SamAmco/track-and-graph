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
import com.samco.trackandgraph.TimeHistogramWindowData
import com.samco.trackandgraph.data.database.dto.TimeHistogramWindow
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeHistogramViewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.DayOfWeek

class TimeHistogramViewTest {
    private val labelText = TimeHistogramLabelText(
        months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    )

    @Test
    fun weekLabelsStartOnTheDayUsedToBuildTheBins() {
        assertEquals(
            listOf("Wed", "Thu", "Fri", "Sat", "Sun", "Mon", "Tue"),
            timeHistogramBucketLabels(
                TimeHistogramWindowData.getWindowData(TimeHistogramWindow.WEEK),
                DayOfWeek.WEDNESDAY,
                labelText,
            ),
        )
    }

    @Test
    fun yearLabelsUseLocalizedMonthNames() {
        assertEquals(
            labelText.months,
            timeHistogramBucketLabels(
                TimeHistogramWindowData.getWindowData(TimeHistogramWindow.YEAR),
                DayOfWeek.MONDAY,
                labelText,
            ),
        )
    }

    @Test
    fun monthHasARealBucketForTheThirtyFirstDay() {
        val labels = timeHistogramBucketLabels(
            TimeHistogramWindowData.getWindowData(TimeHistogramWindow.MONTH),
            DayOfWeek.MONDAY,
            labelText,
        )
        assertEquals(31, labels.size)
        assertEquals("31", labels.last())
    }

    @Test
    fun measuredCollisionDetectionRetainsAllWeekdaysWhenTheyFit() {
        val labels = labelText.weekdays
        val layout = calculateTimeHistogramLayout(
            width = 400f,
            height = 240f,
            bucketLabels = labels,
            viewport = calculateCategoricalGraphViewport(labels.size, 1.0, 0.5),
            axisTitle = "Days",
            maxY = 100.0,
            measureAxisText = { IntSize(it.length * 8, 12) },
            measureTitleText = { IntSize(it.length * 8, 16) },
            density = 1f,
        )
        assertEquals(labels, requireNotNull(layout).xTicks.map { it.label })
    }

    @Test
    fun measuredCollisionDetectionDropsLabelsThatDoNotFit() {
        val labels = labelText.months
        val layout = calculateTimeHistogramLayout(
            width = 140f,
            height = 240f,
            bucketLabels = labels,
            viewport = calculateCategoricalGraphViewport(labels.size, 1.0, 0.5),
            axisTitle = "Months",
            maxY = 100.0,
            measureAxisText = { IntSize(it.length * 10, 12) },
            measureTitleText = { IntSize(it.length * 10, 16) },
            density = 1f,
        )
        assertTrue(requireNotNull(layout).xTicks.size < labels.size)
    }

    @Test
    fun zoomingToOneBucketMakesItsLabelVisible() {
        val labels = List(31) { (it + 1).toString() }
        val layout = calculateTimeHistogramLayout(
            width = 140f,
            height = 240f,
            bucketLabels = labels,
            viewport = calculateCategoricalGraphViewport(labels.size, 31.0, 0.5),
            axisTitle = "Days",
            maxY = 100.0,
            measureAxisText = { IntSize(it.length * 10, 12) },
            measureTitleText = { IntSize(it.length * 10, 16) },
            density = 1f,
        )

        assertEquals(listOf("16"), requireNotNull(layout).xTicks.map { it.label })
    }

    @Test
    fun renderabilityRejectsMalformedOrNonFiniteSeries() {
        val window = TimeHistogramWindowData.getWindowData(TimeHistogramWindow.WEEK)
        assertTrue(
            isRenderableTimeHistogram(
                window,
                listOf(ITimeHistogramViewData.BarValue("", List(7) { 1.0 })),
                100.0,
            )
        )
        assertFalse(
            isRenderableTimeHistogram(
                window,
                listOf(ITimeHistogramViewData.BarValue("", List(6) { 1.0 })),
                100.0,
            )
        )
        assertFalse(
            isRenderableTimeHistogram(
                window,
                listOf(ITimeHistogramViewData.BarValue("", List(7) { Double.NaN })),
                100.0,
            )
        )
    }
}
