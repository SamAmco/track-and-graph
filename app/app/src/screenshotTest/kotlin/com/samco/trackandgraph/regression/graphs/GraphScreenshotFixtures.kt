/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.regression.graphs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.samco.trackandgraph.TimeHistogramWindowData
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.TimeHistogramWindow
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.graphstatview.factories.viewdto.BarChartSeries
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import com.samco.trackandgraph.graphstatview.factories.viewdto.IBarChartViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeHistogramViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.Line
import com.samco.trackandgraph.graphstatview.factories.viewdto.LineGraphPoint
import com.samco.trackandgraph.graphstatview.ui.BarChartView
import com.samco.trackandgraph.graphstatview.ui.GraphViewMode
import com.samco.trackandgraph.graphstatview.ui.LineGraphView
import com.samco.trackandgraph.graphstatview.ui.PieChartView
import com.samco.trackandgraph.graphstatview.ui.TimeHistogramView
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.TemporalAmount
import kotlin.math.sin

private val snapshotEndTime = OffsetDateTime.of(2026, 7, 19, 12, 0, 0, 0, ZoneOffset.UTC)
private val snapshotEndZonedTime = snapshotEndTime.toZonedDateTime()

internal val lineGraphCases: List<ILineGraphViewData> = listOf(
    lineGraphData(line(values = listOf(1.0, 2.5, 2.0, 4.0, 5.5, 5.0, 7.0))),
    lineGraphData(
        line("A", 0, listOf(1.0, 2.0, 4.0, 3.0, 5.0, 6.0, 5.0), LineGraphPointStyle.CIRCLES),
        line("B", 8, listOf(5.0, 4.0, 3.5, 4.5, 3.0, 2.0, 1.0), LineGraphPointStyle.CIRCLES_AND_NUMBERS),
    ),
    lineGraphData(line(values = listOf(-8.0, -3.0, 2.0, -1.0, 6.0, 4.0, 10.0))),
    lineGraphData(
        line(values = listOf(600.0, 1_800.0, 3_600.0, 2_700.0, 5_400.0, 7_200.0)),
        durationBasedRange = true,
    ),
    lineGraphData(
        line(values = waveValues(80, amplitude = 25.0, offset = 30.0), spacing = Duration.ofDays(14)),
    ),
    lineGraphData(
        line(values = waveValues(24, amplitude = 3.0, offset = 4.0), spacing = Duration.ofHours(1)),
    ),
    lineGraphData(
        line(values = listOf(0.012, 0.018, 0.015, 0.024, 0.021, 0.029)),
        yRangeType = YRangeType.FIXED,
        fixedYMin = 0.0,
        fixedYMax = 0.03,
    ),
    lineGraphData(
        line(values = listOf(950_000.0, 1_200_000.0), spacing = Duration.ofDays(365)),
    ),
)

internal val barChartCases: List<IBarChartViewData> = listOf(
    barChartData(values = listOf(listOf(2.0, 4.0, 3.0, 6.0, 5.0, 8.0, 7.0))),
    barChartData(
        values = listOf(
            listOf(2.0, 4.0, 3.0, 5.0, 4.0, 6.0, 5.0),
            listOf(1.0, 2.0, 2.0, 1.0, 3.0, 2.0, 4.0),
            listOf(2.0, 1.0, 3.0, 2.0, 1.0, 2.0, 1.0),
        ),
    ),
    barChartData(values = listOf(waveValues(26, 6.0, 8.0)), period = Period.ofWeeks(1)),
    barChartData(values = listOf(waveValues(24, 10.0, 12.0)), period = Period.ofMonths(1)),
    barChartData(values = listOf(listOf(-5.0, 3.0, -2.0, 7.0, -4.0, 6.0, 2.0))),
    barChartData(
        values = listOf(listOf(900.0, 1_800.0, 3_600.0, 5_400.0, 7_200.0, 4_500.0, 2_700.0)),
        durationBasedRange = true,
    ),
    barChartData(values = listOf(listOf(0.01, 0.03, 0.02, 0.04, 0.025, 0.05))),
    barChartData(values = listOf(listOf(42.0))),
    barChartData(
        values = List(100) { listOf(1.0) },
        numberedLabels = true,
    ),
)

internal val histogramCases: List<ITimeHistogramViewData> = TimeHistogramWindow.entries.map { window ->
    val count = TimeHistogramWindowData.getWindowData(window).numBins
    val primary = List(count) { index -> 8.0 + ((index * 7) % 19) }
    val series = if (window in setOf(
            TimeHistogramWindow.WEEK,
            TimeHistogramWindow.MONTH,
            TimeHistogramWindow.SIX_MONTHS,
        )
    ) {
        listOf(
            ITimeHistogramViewData.BarValue("A", primary),
            ITimeHistogramViewData.BarValue("", List(count) { index -> 3.0 + ((index * 5) % 11) }),
        )
    } else {
        listOf(ITimeHistogramViewData.BarValue("", primary))
    }
    histogramData(window, series)
}

internal val pieChartCases: List<IPieChartViewData> = listOf(
    pieChartData(100.0),
    pieChartData(62.5, 37.5),
    pieChartData(40.0, 30.0, 20.0, 10.0),
    pieChartData(28.0, 22.0, 17.0, 13.0, 9.0, 6.0, 3.0, 2.0),
    pieChartData(*DoubleArray(13) { if (it == 12) 4.0 else 8.0 }),
    pieChartData(55.0, 25.0, 15.0, 5.0, explicitColors = true),
    pieChartData(50.0, 30.0, 20.0, emptyTitles = true),
    pieChartData(70.0, 10.0, 7.0, 5.0, 3.0, 2.0, 1.5, 1.0, 0.5),
)

@Composable
internal fun LineGraphSnapshot(index: Int) = GraphSnapshotFrame {
    LineGraphView(
        modifier = Modifier.fillMaxWidth(),
        viewData = lineGraphCases[index],
        graphViewMode = GraphViewMode.ListMode,
        graphBackgroundColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
internal fun BarChartSnapshot(index: Int) = GraphSnapshotFrame {
    BarChartView(
        modifier = Modifier.fillMaxWidth(),
        viewData = barChartCases[index],
        listMode = true,
        graphViewMode = GraphViewMode.ListMode,
        graphBackgroundColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
internal fun HistogramSnapshot(index: Int) = GraphSnapshotFrame {
    TimeHistogramView(
        modifier = Modifier.fillMaxWidth(),
        viewData = histogramCases[index],
        graphViewMode = GraphViewMode.ListMode,
        graphBackgroundColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
internal fun PieChartSnapshot(index: Int) = GraphSnapshotFrame {
    PieChartView(
        modifier = Modifier.fillMaxWidth(),
        viewData = pieChartCases[index],
        graphViewMode = GraphViewMode.ListMode,
        graphBackgroundColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun GraphSnapshotFrame(content: @Composable () -> Unit) {
    TnGComposeTheme {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            content = content,
        )
    }
}

private fun lineGraphData(
    vararg lines: Line,
    durationBasedRange: Boolean = false,
    yRangeType: YRangeType = YRangeType.DYNAMIC,
    fixedYMin: Double? = null,
    fixedYMax: Double? = null,
): ILineGraphViewData = object : ILineGraphViewData {
    override val state = IGraphStatViewData.State.READY
    override val graphOrStat = graphOrStat(GraphStatType.LINE_GRAPH)
    override val durationBasedRange = durationBasedRange
    override val yRangeType = yRangeType
    override val fixedYMin = fixedYMin
    override val fixedYMax = fixedYMax
    override val hasPlottableData = true
    override val endTime = snapshotEndTime
    override val lines = lines.toList()
}

private fun line(
    name: String = "A",
    colorIndex: Int = 0,
    values: List<Double>,
    pointStyle: LineGraphPointStyle = LineGraphPointStyle.NONE,
    spacing: Duration = Duration.ofDays(7),
): Line {
    val points = values.mapIndexed { index, value ->
        LineGraphPoint(
            timestamp = snapshotEndTime.minus(spacing.multipliedBy((values.lastIndex - index).toLong())),
            value = value,
        )
    }
    return Line(name, ColorSpec.ColorIndex(colorIndex), pointStyle, points)
}

private fun barChartData(
    values: List<List<Double>>,
    period: TemporalAmount = Period.ofDays(1),
    durationBasedRange: Boolean = false,
    numberedLabels: Boolean = false,
): IBarChartViewData {
    val count = values.first().size
    val dates = List(count) { index ->
        var date = snapshotEndZonedTime
        repeat(count - index) { date = date.minus(period) }
        date
    }
    val minimum = values.flatten().minOrNull() ?: 0.0
    val maximum = (0 until count).maxOf { bucket -> values.sumOf { it[bucket] } }
    val yMin = minOf(0.0, minimum) * 1.15
    val yMax = maxOf(1.0, maximum * 1.15)
    return object : IBarChartViewData {
        override val state = IGraphStatViewData.State.READY
        override val graphOrStat = graphOrStat(GraphStatType.BAR_CHART)
        override val xDates = dates
        override val bars = values.mapIndexed { index, series ->
            BarChartSeries(
                label = if (numberedLabels) "Series ${index + 1}"
                else if (index == 0) "" else ('A' + index).toString(),
                values = series,
                color = ColorSpec.ColorIndex((index * 4) % dataVisColorList.size),
            )
        }
        override val durationBasedRange = durationBasedRange
        override val endTime = snapshotEndZonedTime
        override val yMin = yMin
        override val yMax = yMax
        override val yAxisSubdivides = 6
        override val barPeriod = period
    }
}

private fun histogramData(
    window: TimeHistogramWindow,
    series: List<ITimeHistogramViewData.BarValue>,
): ITimeHistogramViewData = object : ITimeHistogramViewData {
    override val state = IGraphStatViewData.State.READY
    override val graphOrStat = graphOrStat(GraphStatType.TIME_HISTOGRAM)
    override val window = TimeHistogramWindowData.getWindowData(window)
    override val barValues = series
    override val maxDisplayHeight = series.first().values.indices
        .maxOf { bucket -> series.sumOf { it.values[bucket] } } * 1.15
    override val firstDayOfWeek = DayOfWeek.MONDAY
}

private fun pieChartData(
    vararg values: Double,
    explicitColors: Boolean = false,
    emptyTitles: Boolean = false,
): IPieChartViewData = object : IPieChartViewData {
    override val state = IGraphStatViewData.State.READY
    override val graphOrStat = graphOrStat(GraphStatType.PIE_CHART)
    override val segments = values.mapIndexed { index, value ->
        IPieChartViewData.Segment(
            value = value,
            title = when {
                emptyTitles -> ""
                else -> ('A' + index).toString()
            },
            color = if (explicitColors) ColorSpec.ColorIndex(index * 3) else null,
        )
    }
}

private fun graphOrStat(type: GraphStatType) = GraphOrStat(
    id = type.ordinal.toLong(),
    name = "",
    type = type,
    unique = true,
)

private fun waveValues(count: Int, amplitude: Double, offset: Double): List<Double> =
    List(count) { index -> offset + amplitude * sin(index * Math.PI / 5.0) }
