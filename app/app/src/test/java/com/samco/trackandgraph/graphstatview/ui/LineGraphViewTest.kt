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
import com.samco.trackandgraph.graphstatview.factories.viewdto.LineGraphPoint
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset

class LineGraphViewTest {

    @Test
    fun `dynamic y ticks use clear rounded intervals around the data`() {
        assertEquals(
            listOf(0.0, 2.5, 5.0, 7.5, 10.0),
            calculateYTicks(rawMin = 0.7, rawMax = 9.1, targetCount = 5, fixed = false),
        )
    }

    @Test
    fun `fixed y ticks preserve exact configured bounds`() {
        assertEquals(
            listOf(1.0, 4.0, 7.0, 10.0),
            calculateYTicks(rawMin = 1.0, rawMax = 10.0, targetCount = 4, fixed = true),
        )
    }

    @Test
    fun `fixed y ticks retain legacy whole number divisions`() {
        assertEquals(
            listOf(0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0),
            calculateYTicks(rawMin = 0.0, rawMax = 12.0, targetCount = 8, fixed = true),
        )
    }

    @Test
    fun `fixed thirty to fifty range prefers five unit intervals`() {
        assertEquals(
            listOf(30.0, 35.0, 40.0, 45.0, 50.0),
            calculateYTicks(rawMin = 30.0, rawMax = 50.0, targetCount = 6, fixed = true),
        )
    }

    @Test
    fun `fixed zero to one hundred range uses the same clean ticks at different heights`() {
        val points = listOf(point(0), point(100, 100.0))
        val listLayout = requireNotNull(
            layout(points, height = 220f, fixedYMin = 0.0, fixedYMax = 100.0)
        )
        val fullScreenLayout = requireNotNull(
            layout(points, height = 600f, fixedYMin = 0.0, fixedYMax = 100.0)
        )

        val expected = listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0)
        assertEquals(expected, listLayout.yTicks.map { it.value })
        assertEquals(expected, fullScreenLayout.yTicks.map { it.value })
    }

    @Test
    fun `duration y ticks use clock friendly legacy intervals`() {
        val ticks = calculateYTicks(
            rawMin = 1.0 * 60 * 60 + 53 * 60,
            rawMax = 8.0 * 60 * 60 + 25 * 60,
            targetCount = 8,
            fixed = false,
            durationBasedRange = true,
        )
        val intervals = ticks.zipWithNext { first, second -> second - first }

        assertTrue(intervals.isNotEmpty())
        assertTrue(intervals.all { it == intervals.first() })
        assertEquals(0.0, intervals.first() % (30 * 60))
    }

    @Test
    fun `dynamic y ticks cover negative crossing zero tiny and large ranges`() {
        listOf(
            -9.1 to -0.7,
            -2.1 to 7.2,
            0.00012 to 0.00019,
            1.2e12 to 9.8e12,
        ).forEach { (minimum, maximum) ->
            val ticks = calculateYTicks(
                rawMin = minimum,
                rawMax = maximum,
                targetCount = 5,
                fixed = false,
            )
            val tolerance = maxOf(kotlin.math.abs(minimum), kotlin.math.abs(maximum), 1.0) * 1e-12

            assertTrue("Ticks must all be finite for $minimum..$maximum", ticks.all(Double::isFinite))
            assertTrue("Ticks must be strictly increasing for $minimum..$maximum", ticks.zipWithNext().all {
                (first, second) -> first < second
            })
            assertTrue(
                "Ticks must cover the minimum for $minimum..$maximum",
                ticks.first() <= minimum + tolerance,
            )
            assertTrue(
                "Ticks must cover the maximum for $minimum..$maximum",
                ticks.last() >= maximum - tolerance,
            )
        }
    }

    @Test
    fun `equal y ranges expand to finite nonzero ranges`() {
        assertEquals(-1.0 to 1.0, expandEqualRange(0.0, 0.0))
        assertEquals(90.0 to 110.0, expandEqualRange(100.0, 100.0))
    }

    @Test
    fun `x ticks use actual data positions and reject overlapping labels`() {
        val selected = selectLineGraphXTicks(
            candidates = listOf(
                XTick(epochMillis = 0, label = "a", projectedWidth = 20f),
                XTick(epochMillis = 20, label = "b", projectedWidth = 20f),
                XTick(epochMillis = 30, label = "c", projectedWidth = 20f),
                XTick(epochMillis = 70, label = "d", projectedWidth = 20f),
                XTick(epochMillis = 100, label = "e", projectedWidth = 20f),
            ),
            minX = 0,
            maxX = 100,
            plotLeft = 30f,
            plotWidth = 100f,
            minimumGap = 5f,
        )

        assertEquals(listOf(0L, 30L, 70L, 100L), selected.map { it.epochMillis })
    }

    @Test
    fun `x ticks omit labels that would cross the left canvas edge`() {
        val selected = selectLineGraphXTicks(
            candidates = listOf(
                XTick(epochMillis = 0, label = "too wide", projectedWidth = 40f),
                XTick(epochMillis = 50, label = "fits", projectedWidth = 20f),
            ),
            minX = 0,
            maxX = 100,
            plotLeft = 30f,
            plotWidth = 100f,
            minimumGap = 5f,
        )

        assertEquals(listOf(50L), selected.map { it.epochMillis })
    }

    @Test
    fun `x tick selection handles empty and single candidate lists`() {
        assertTrue(
            selectLineGraphXTicks(emptyList(), 0, 100, 30f, 100f, 5f).isEmpty()
        )
        assertEquals(
            listOf(50L),
            selectLineGraphXTicks(
                candidates = listOf(XTick(50, "fits", 20f)),
                minX = 0,
                maxX = 100,
                plotLeft = 30f,
                plotWidth = 100f,
                minimumGap = 5f,
            ).map { it.epochMillis },
        )
    }

    @Test
    fun `maximum zoom stops at a ten second viewport`() {
        assertEquals(8_640.0, maximumLineGraphZoom(0L, 86_400_000L))

        val viewport = calculateLineGraphViewport(
            fullMinX = 0L,
            fullMaxX = 86_400_000L,
            zoom = Double.MAX_VALUE,
            centerFraction = 0.5,
        )

        assertEquals(10_000L, viewport.maxX - viewport.minX)
    }

    @Test
    fun `graphs shorter than ten seconds cannot zoom beyond their complete range`() {
        assertEquals(1.0, maximumLineGraphZoom(0L, 5_000L))

        val viewport = calculateLineGraphViewport(
            fullMinX = 0L,
            fullMaxX = 5_000L,
            zoom = Double.MAX_VALUE,
            centerFraction = 0.5,
        )

        assertEquals(LineGraphViewport(0L, 5_000L), viewport)
    }

    @Test
    fun `time tick layers refine by powers of two as the viewport zooms in`() {
        val wideDivisions = lineGraphTimeDivisionCount(
            fullSpan = 100,
            visibleSpan = 100,
            maximumTickCount = 4,
        )
        val zoomedDivisions = lineGraphTimeDivisionCount(
            fullSpan = 100,
            visibleSpan = 50,
            maximumTickCount = 4,
        )

        assertEquals(4L, wideDivisions)
        assertEquals(8L, zoomedDivisions)
        assertEquals(0L, zoomedDivisions % wideDivisions)
    }

    @Test
    fun `time tick layers snap to data and retain coarse ticks at finer resolutions`() {
        val timestamps = listOf(0L, 11L, 23L, 39L, 52L, 68L, 79L, 91L, 100L)
        val coarse = lineGraphTimeTickCandidates(timestamps, 0, 100, 0, 100, 4)
        val fine = lineGraphTimeTickCandidates(timestamps, 0, 100, 0, 100, 8)

        assertEquals(listOf(0L, 23L, 52L, 79L, 100L), coarse)
        assertTrue(fine.containsAll(coarse))
        assertTrue(fine.all(timestamps::contains))
    }

    @Test
    fun `panning retains anchored x ticks shared by both viewports`() {
        val points = (0L..160L step 10L).map { point(it, it.toDouble()) }
        val left = requireNotNull(
            layout(points, width = 180f, visibleMinX = 20L, visibleMaxX = 100L)
        )
        val right = requireNotNull(
            layout(points, width = 180f, visibleMinX = 40L, visibleMaxX = 120L)
        )
        val viewportBoundaries = setOf(left.minX, left.maxX, right.minX, right.maxX)

        assertEquals(
            left.xTicks.map { it.epochMillis }.filter { it in 40L..100L && it !in viewportBoundaries },
            right.xTicks.map { it.epochMillis }.filter { it in 40L..100L && it !in viewportBoundaries },
        )
        assertEquals(listOf(80L), left.xTicks.map { it.epochMillis }.filter { it !in viewportBoundaries })
    }

    @Test
    fun `zooming in retains coarse x ticks and adds finer ones`() {
        val points = (0L..160L step 10L).map { point(it, it.toDouble()) }
        val wide = requireNotNull(
            layout(points, width = 140f, visibleMinX = 0L, visibleMaxX = 160L)
        )
        val zoomed = requireNotNull(
            layout(points, width = 140f, visibleMinX = 40L, visibleMaxX = 120L)
        )
        val coarseTicksStillVisible = wide.xTicks.map { it.epochMillis }.filter { it in 40L..120L }

        assertTrue(zoomed.xTicks.size > coarseTicksStillVisible.size)
        assertTrue(zoomed.xTicks.map { it.epochMillis }.containsAll(coarseTicksStillVisible))
    }

    @Test
    fun `day and month labels use named unambiguous formats`() {
        val timestamp = OffsetDateTime.of(2026, 3, 3, 12, 0, 0, 0, ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        assertEquals(
            "03 Mar",
            formatTimestamp(timestamp, Duration.ofDays(30).toMillis(), ZoneOffset.UTC),
        )
        assertEquals(
            "Mar’26",
            formatTimestamp(timestamp, Duration.ofDays(365).toMillis(), ZoneOffset.UTC),
        )
    }

    @Test
    fun `september is always abbreviated to three characters`() {
        val timestamp = OffsetDateTime.of(2026, 9, 3, 12, 0, 0, 0, ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        assertEquals(
            "03 Sep",
            formatTimestamp(timestamp, Duration.ofDays(30).toMillis(), ZoneOffset.UTC),
        )
        assertEquals(
            "Sep’26",
            formatTimestamp(timestamp, Duration.ofDays(365).toMillis(), ZoneOffset.UTC),
        )
    }

    @Test
    fun `timestamp format changes at duration boundaries`() {
        val timestamp = OffsetDateTime.of(2026, 3, 3, 12, 34, 56, 0, ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        assertEquals(
            "12:34:56",
            formatTimestamp(timestamp, Duration.ofMinutes(5).minusMillis(1).toMillis(), ZoneOffset.UTC),
        )
        assertEquals(
            "12:34",
            formatTimestamp(timestamp, Duration.ofMinutes(5).toMillis(), ZoneOffset.UTC),
        )
        assertEquals(
            "12:34",
            formatTimestamp(timestamp, Duration.ofDays(1).minusMillis(1).toMillis(), ZoneOffset.UTC),
        )
        assertEquals(
            "Tue 03",
            formatTimestamp(timestamp, Duration.ofDays(1).toMillis(), ZoneOffset.UTC),
        )
        assertEquals(
            "Tue 03",
            formatTimestamp(timestamp, Duration.ofDays(14).minusMillis(1).toMillis(), ZoneOffset.UTC),
        )
        assertEquals(
            "03 Mar",
            formatTimestamp(timestamp, Duration.ofDays(14).toMillis(), ZoneOffset.UTC),
        )
        assertEquals(
            "03 Mar",
            formatTimestamp(timestamp, Duration.ofDays(304).minusMillis(1).toMillis(), ZoneOffset.UTC),
        )
        assertEquals(
            "Mar’26",
            formatTimestamp(timestamp, Duration.ofDays(304).toMillis(), ZoneOffset.UTC),
        )
    }

    @Test
    fun `timestamp labels use the supplied display timezone`() {
        val timestamp = OffsetDateTime.of(2026, 3, 3, 0, 30, 0, 0, ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        assertEquals(
            "02 Mar",
            formatTimestamp(timestamp, Duration.ofDays(30).toMillis(), ZoneOffset.ofHours(-1)),
        )
    }

    @Test
    fun `viewport points include one neighbour beyond each edge`() {
        val points = listOf(0L, 10L, 20L, 30L, 40L).map(::point)

        assertEquals(
            listOf(10L, 20L, 30L),
            pointsForViewport(points, minX = 15L, maxX = 25L).map(::epochMillis),
        )
        assertEquals(
            listOf(0L, 10L, 20L, 30L, 40L),
            pointsForViewport(points, minX = 10L, maxX = 30L).map(::epochMillis),
        )
        assertTrue(pointsForViewport(emptyList(), minX = 10L, maxX = 30L).isEmpty())
    }

    @Test
    fun `layout reserves measured width for y labels`() {
        val points = listOf(point(0, -10.0), point(100, 10.0))
        val narrow = layout(points, measureText = { IntSize(width = 10, height = 10) })
        val wide = layout(points, measureText = { IntSize(width = 80, height = 10) })

        assertEquals(70f, requireNotNull(wide).plotRect.left - requireNotNull(narrow).plotRect.left)
    }

    @Test
    fun `full screen layout can reserve stable maximum start label space`() {
        val points = listOf(point(0), point(100))
        val narrowStart = requireNotNull(
            layout(
                points = points,
                visibleMinX = 0,
                visibleMaxX = 80,
                reservedStartXLabelWidth = 75f,
            )
        )
        val wideStart = requireNotNull(
            layout(
                points = points,
                visibleMinX = 20,
                visibleMaxX = 100,
                reservedStartXLabelWidth = 75f,
            )
        )

        assertEquals(
            75f * kotlin.math.cos(Math.toRadians(28.0)).toFloat(),
            narrowStart.plotRect.left,
        )
        assertEquals(narrowStart.plotRect.left, wideStart.plotRect.left)
        assertEquals(narrowStart.plotRect.bottom, wideStart.plotRect.bottom)
    }

    @Test
    fun `maximum x label width checks every named month`() {
        val width = maximumLineGraphXLabelWidth(
            Duration.ofDays(30).toMillis(),
            englishXLabelText,
        ) { label ->
            IntSize(width = if (label.endsWith("May")) 80 else 30, height = 10)
        }

        assertEquals(80f, width)
    }

    @Test
    fun `start x label provides date context for finer tick formats`() {
        val timestamp = OffsetDateTime.of(2026, 3, 3, 12, 34, 56, 0, ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        listOf(
            Duration.ofMinutes(1).toMillis() to "03 Mar",
            Duration.ofHours(1).toMillis() to "03 Mar",
            Duration.ofDays(7).toMillis() to "03 Mar",
            Duration.ofDays(30).toMillis() to "Mar’26",
            Duration.ofDays(365).toMillis() to "Mar’26",
        ).forEach { (duration, expected) ->
            assertEquals(
                expected,
                formatGraphXAxisStartTimestamp(timestamp, duration, ZoneOffset.UTC, englishXLabelText),
            )
        }
    }

    @Test
    fun `only the pinned start tick uses the contextual format`() {
        val start = OffsetDateTime.of(2026, 3, 3, 12, 0, 0, 0, ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val end = start + Duration.ofHours(1).toMillis()
        val layout = requireNotNull(
            layout(
                points = (0L..6L).map { point(start + Duration.ofMinutes(it * 10).toMillis()) },
                width = 900f,
                visibleMinX = start,
                visibleMaxX = end,
                measureText = { IntSize(width = 20, height = 10) },
            )
        )

        assertEquals("03 Mar", layout.xTicks.first().label)
        assertTrue(layout.xTicks.drop(1).all { ':' in it.label })
    }

    @Test
    fun `maximum start x label width uses its contextual format`() {
        val width = maximumLineGraphStartXLabelWidth(
            Duration.ofDays(30).toMillis(),
            englishXLabelText,
        ) { label ->
            IntSize(width = if (label == "Sep’88") 80 else 30, height = 10)
        }

        assertEquals(80f, width)
    }

    @Test
    fun `weekday labels and maximum width use supplied localized text`() {
        val germanLabels = englishXLabelText.copy(
            weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"),
        )
        val timestamp = OffsetDateTime.of(2026, 3, 3, 12, 0, 0, 0, ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        assertEquals(
            "Di 03",
            formatGraphXAxisTimestamp(
                timestamp,
                Duration.ofDays(7).toMillis(),
                ZoneOffset.UTC,
                germanLabels,
            ),
        )
        assertEquals(
            50f,
            maximumLineGraphXLabelWidth(
                Duration.ofDays(7).toMillis(),
                germanLabels,
            ) { label -> IntSize(if (label.startsWith("Mi")) 50 else 20, 10) },
        )
    }

    @Test
    fun `reserved maximum tick label width controls x tick spacing`() {
        val points = (0L..100L step 5L).map(::point)
        val compact = requireNotNull(
            layout(points, width = 400f, measureText = { IntSize(10, 10) })
        )
        val reserved = requireNotNull(
            layout(
                points,
                width = 400f,
                maximumXTickLabelWidth = 100f,
                measureText = { IntSize(10, 10) },
            )
        )

        assertTrue(reserved.xTicks.size < compact.xTicks.size)
    }

    @Test
    fun `layout expands an all zero dynamic range`() {
        val layout = requireNotNull(layout(listOf(point(0), point(100))))

        assertTrue(layout.minY.isFinite())
        assertTrue(layout.maxY.isFinite())
        assertTrue(layout.minY < 0.0)
        assertTrue(layout.maxY > 0.0)
    }

    @Test
    fun `horizontal viewport changes do not change dynamic y layout`() {
        val points = listOf(point(0, 100.0), point(50, 1.0), point(100, 2.0))
        val left = requireNotNull(
            layout(
                points = points,
                visibleMinX = 0,
                visibleMaxX = 60,
            )
        )
        val right = requireNotNull(
            layout(
                points = points,
                visibleMinX = 40,
                visibleMaxX = 100,
            )
        )

        assertEquals(left.minY, right.minY)
        assertEquals(left.maxY, right.maxY)
        assertEquals(left.yTicks, right.yTicks)
        assertEquals(left.plotRect.left, right.plotRect.left)
        assertTrue(left.minY <= 1.0)
        assertTrue(left.maxY >= 100.0)
    }

    @Test
    fun `vertical zoom changes span around the visible data center`() {
        val zoomed = calculateLineGraphYViewport(
            current = LineGraphYViewport(0.0, 100.0),
            complete = LineGraphYViewport(0.0, 100.0),
            center = 40.0,
            gestureZoom = 2.0,
        )

        assertEquals(LineGraphYViewport(15.0, 65.0), zoomed)
    }

    @Test
    fun `vertical zoom out stops at the complete y span`() {
        val zoomed = calculateLineGraphYViewport(
            current = LineGraphYViewport(25.0, 75.0),
            complete = LineGraphYViewport(0.0, 100.0),
            center = 40.0,
            gestureZoom = 0.1,
        )

        assertEquals(LineGraphYViewport(0.0, 100.0), zoomed)
    }

    @Test
    fun `numeric vertical zoom stops at three decimal display precision`() {
        val zoomed = calculateLineGraphYViewport(
            current = LineGraphYViewport(0.0, 100.0),
            complete = LineGraphYViewport(0.0, 100.0),
            center = 50.0,
            gestureZoom = Double.MAX_VALUE,
        )

        assertEquals(0.01, zoomed.span, 1e-12)
    }

    @Test
    fun `duration vertical zoom stops at whole second display precision`() {
        val zoomed = calculateLineGraphYViewport(
            current = LineGraphYViewport(0.0, 100.0),
            complete = LineGraphYViewport(0.0, 100.0),
            center = 50.0,
            gestureZoom = Double.MAX_VALUE,
            minimumSpan = 10.0,
        )

        assertEquals(10.0, zoomed.span, 1e-12)
    }

    @Test
    fun `minimum vertical zoom ranges retain distinct y labels`() {
        val numericLayout = requireNotNull(
            layout(
                points = listOf(point(0, 0.0), point(100, 100.0)),
                requestedYViewport = LineGraphYViewport(49.995, 50.005),
            )
        )
        val durationLayout = requireNotNull(
            layout(
                points = listOf(point(0, 0.0), point(100, 100.0)),
                requestedYViewport = LineGraphYViewport(45.0, 55.0),
                durationBasedRange = true,
            )
        )

        assertEquals(numericLayout.yTicks.size, numericLayout.yTicks.map { it.label }.distinct().size)
        assertEquals(durationLayout.yTicks.size, durationLayout.yTicks.map { it.label }.distinct().size)
    }

    @Test
    fun `vertical zoom settles on the complete viewport when nearly zoomed out`() {
        val complete = LineGraphYViewport(0.0, 1.0)

        assertEquals(
            complete,
            settleLineGraphYViewport(LineGraphYViewport(0.001, 0.999), complete),
        )
        assertEquals(
            LineGraphYViewport(0.03, 0.97),
            settleLineGraphYViewport(LineGraphYViewport(0.03, 0.97), complete),
        )
    }

    @Test
    fun `vertical zoom settles on clear rounded y bounds`() {
        val layout = requireNotNull(
            layout(
                points = listOf(point(0), point(100, 10.0)),
                fixedYMin = 0.0,
                fixedYMax = 10.0,
                requestedYViewport = LineGraphYViewport(0.0, 1.9),
            )
        )

        assertEquals(0.0, layout.minY)
        assertEquals(2.0, layout.maxY)
        assertEquals(listOf(0.0, 0.5, 1.0, 1.5, 2.0), layout.yTicks.map { it.value })
    }

    @Test
    fun `vertical zoom centers on finite data in the horizontal viewport`() {
        val points = listOf(point(0, 100.0), point(50, 20.0), point(100, 40.0))

        assertEquals(30.0, visibleDataYCenter(points, 40, 100, fallback = 7.0))
        assertEquals(7.0, visibleDataYCenter(points, 10, 20, fallback = 7.0))
    }

    @Test
    fun `pinch locks to the axis with the larger separation change`() {
        assertEquals(LineGraphPinchAxis.HORIZONTAL, lineGraphPinchAxis(20f, 5f))
        assertEquals(LineGraphPinchAxis.VERTICAL, lineGraphPinchAxis(5f, 20f))
        assertEquals(LineGraphPinchAxis.HORIZONTAL, lineGraphPinchAxis(10f, 10f))
    }

    @Test
    fun `layout rejects invalid fixed bounds and insufficient width`() {
        val points = listOf(point(0), point(100, 1.0))

        assertNull(layout(points, fixedYMin = 10.0, fixedYMax = 1.0))
        assertNull(layout(points, fixedYMin = Double.NaN, fixedYMax = 1.0))
        assertNull(
            layout(
                points = points,
                width = 40f,
                measureText = { IntSize(width = 40, height = 10) },
            )
        )
    }

    @Test
    fun `layout x ticks are unique actual data timestamps`() {
        val points = listOf(point(0), point(50), point(50, 2.0), point(100))
        val layout = requireNotNull(
            layout(points, measureText = { IntSize(width = 1, height = 10) })
        )

        assertEquals(listOf(0L, 50L, 100L), layout.xTicks.map { it.epochMillis })
    }

    @Test
    fun `layout pins boundary labels to the viewport when both fit`() {
        val points = listOf(0L, 20L, 40L, 60L, 70L, 100L).map(::point)
        val layout = requireNotNull(
            layout(
                points = points,
                width = 900f,
                visibleMinX = 5L,
                visibleMaxX = 95L,
                measureText = ::measureWideXLabel,
            )
        )

        assertEquals(5L, layout.xTicks.first().epochMillis)
        assertEquals(95L, layout.xTicks.last().epochMillis)
        assertEquals(layout.xTicks.first().projectedLeftExtent, layout.plotRect.left)
        assertTrue(layout.plotRect.left < layout.xTicks.first().projectedWidth)
    }

    @Test
    fun `layout retains the first boundary label and omits an overlapping end label`() {
        val points = listOf(0L, 80L, 100L).map(::point)
        val layout = requireNotNull(
            layout(
                points = points,
                width = 300f,
                visibleMinX = 5L,
                visibleMaxX = 95L,
                measureText = ::measureWideXLabel,
            )
        )

        assertEquals(5L, layout.xTicks.first().epochMillis)
        assertTrue(95L !in layout.xTicks.map { it.epochMillis })
    }

    @Test
    fun `first label follows the viewport bound while panning`() {
        val points = (0L..160L step 10L).map(::point)
        val layout = requireNotNull(
            layout(
                points = points,
                width = 300f,
                visibleMinX = 45L,
                visibleMaxX = 125L,
                measureText = ::measureWideXLabel,
            )
        )

        assertEquals(45L, layout.xTicks.first().epochMillis)
    }

    @Test
    fun `layout measures repeated x labels only once`() {
        val measurementCounts = mutableMapOf<String, Int>()
        val repeatedLabel = formatTimestamp(
            epochMillis = 0,
            durationMillis = 100,
            zoneId = ZoneId.systemDefault(),
        )

        layout(
            points = listOf(point(0), point(100, 1.0)),
            measureText = { label ->
                measurementCounts[label] = measurementCounts.getOrDefault(label, 0) + 1
                IntSize(width = 1, height = 10)
            },
        )

        assertEquals(1, measurementCounts[repeatedLabel])
    }

    @Test
    fun `small x tick sets do not perform extra estimate measurements`() {
        val measuredLabels = mutableListOf<String>()

        layout(
            points = listOf(point(0), point(1_000), point(2_000)),
            visibleMaxX = 2_000,
            measureText = { label ->
                measuredLabels += label
                IntSize(width = 30, height = 10)
            },
        )

        val measuredTimeLabels = measuredLabels.filter { it.matches(Regex("\\d{2}:\\d{2}:\\d{2}")) }
        assertEquals(3, measuredTimeLabels.size)
    }

    @Test
    fun `dense x tick sets measure only the selected time layer`() {
        val end = Duration.ofHours(23).toMillis()
        val measuredLabels = mutableListOf<String>()
        val points = List(5_000) { index ->
            point(end * index / 4_999, index.toDouble())
        }

        layout(
            points = points,
            visibleMaxX = end,
            measureText = { label ->
                measuredLabels += label
                IntSize(width = 40, height = 10)
            },
        )

        val measuredTimeLabels = measuredLabels.filter { label ->
            label.matches(Regex("\\d{2}:\\d{2}"))
        }
        assertTrue("Measured ${measuredTimeLabels.size} X labels", measuredTimeLabels.size <= 16)
    }

    private fun layout(
        points: List<LineGraphPoint>,
        width: Float = 400f,
        height: Float = 220f,
        visibleMinX: Long = 0L,
        visibleMaxX: Long = 100L,
        durationBasedRange: Boolean = false,
        fixedYMin: Double? = null,
        fixedYMax: Double? = null,
        requestedYViewport: LineGraphYViewport? = null,
        maximumXTickLabelWidth: Float? = null,
        reservedStartXLabelWidth: Float? = null,
        measureText: (String) -> IntSize = { IntSize(width = 30, height = 10) },
    ) = calculateLineGraphLayout(
        width = width,
        height = height,
        visibleMinX = visibleMinX,
        visibleMaxX = visibleMaxX,
        points = points,
        durationBasedRange = durationBasedRange,
        fixedYMin = fixedYMin,
        fixedYMax = fixedYMax,
        requestedYViewport = requestedYViewport,
        maximumXTickLabelWidth = maximumXTickLabelWidth,
        reservedStartXLabelWidth = reservedStartXLabelWidth,
        xLabelText = englishXLabelText,
        measureText = measureText,
        density = 1f,
    )

    private fun point(epochMillis: Long, value: Double = 0.0) = LineGraphPoint(
        timestamp = Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC),
        value = value,
    )

    private fun epochMillis(point: LineGraphPoint) = point.timestamp.toInstant().toEpochMilli()

    private fun formatTimestamp(
        epochMillis: Long,
        durationMillis: Long,
        zoneId: ZoneId,
    ) = formatGraphXAxisTimestamp(epochMillis, durationMillis, zoneId, englishXLabelText)

    private val englishXLabelText = GraphXAxisLabelText(
        months = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        ),
        weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
        weekdayDayFormat = "%1\$s %2\$s",
        dayMonthFormat = "%1\$s %2\$s",
        monthYearFormat = "%1\$s’%2\$s",
    )

    private fun measureWideXLabel(label: String) = IntSize(
        width = if (':' in label || label.any(Char::isLetter)) 80 else 10,
        height = 20,
    )
}
