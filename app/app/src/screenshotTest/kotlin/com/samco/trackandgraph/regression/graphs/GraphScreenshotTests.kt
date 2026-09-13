/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.regression.graphs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

private const val GRAPH_CARD_CONTENT_WIDTH_DP = 380

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@Preview(name = "English", locale = "en-rGB", widthDp = GRAPH_CARD_CONTENT_WIDTH_DP)
private annotation class GraphPreview

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@Preview(name = "English", locale = "en-rGB", widthDp = GRAPH_CARD_CONTENT_WIDTH_DP)
@Preview(name = "German", locale = "de-rDE", widthDp = GRAPH_CARD_CONTENT_WIDTH_DP)
@Preview(name = "Spanish", locale = "es-rES", widthDp = GRAPH_CARD_CONTENT_WIDTH_DP)
@Preview(name = "French", locale = "fr-rFR", widthDp = GRAPH_CARD_CONTENT_WIDTH_DP)
private annotation class GraphLocalesPreview

@PreviewTest @GraphLocalesPreview @Composable
fun LineNumericTrend() = LineGraphSnapshot(0)

@PreviewTest @GraphPreview @Composable
fun LineMultipleSeriesAndPointStyles() = LineGraphSnapshot(1)

@PreviewTest @GraphPreview @Composable
fun LineNegativeAndPositiveValues() = LineGraphSnapshot(2)

@PreviewTest @GraphPreview @Composable
fun LineDurationAxis() = LineGraphSnapshot(3)

@PreviewTest @GraphLocalesPreview @Composable
fun LineDenseLongRange() = LineGraphSnapshot(4)

@PreviewTest @GraphLocalesPreview @Composable
fun LineHourlyRange() = LineGraphSnapshot(5)

@PreviewTest @GraphPreview @Composable
fun LineSmallDecimalRange() = LineGraphSnapshot(6)

@PreviewTest @GraphPreview @Composable
fun LineTwoPointsLargeValues() = LineGraphSnapshot(7)

@PreviewTest @GraphPreview @Composable
fun BarDailySingleSeries() = BarChartSnapshot(0)

@PreviewTest @GraphLocalesPreview @Composable
fun BarDailyStackedSeries() = BarChartSnapshot(1)

@PreviewTest @GraphLocalesPreview @Composable
fun BarDenseWeeklyRange() = BarChartSnapshot(2)

@PreviewTest @GraphLocalesPreview @Composable
fun BarLongMonthlyRange() = BarChartSnapshot(3)

@PreviewTest @GraphPreview @Composable
fun BarNegativeAndPositiveValues() = BarChartSnapshot(4)

@PreviewTest @GraphPreview @Composable
fun BarDurationAxis() = BarChartSnapshot(5)

@PreviewTest @GraphPreview @Composable
fun BarSmallDecimalRange() = BarChartSnapshot(6)

@PreviewTest @GraphPreview @Composable
fun BarSingleBucket() = BarChartSnapshot(7)

@PreviewTest @GraphPreview @Composable
fun BarHundredSeriesLegend() = BarChartSnapshot(8)

@PreviewTest @GraphLocalesPreview @Composable
fun HistogramHourWindow() = HistogramSnapshot(0)

@PreviewTest @GraphLocalesPreview @Composable
fun HistogramDayWindow() = HistogramSnapshot(1)

@PreviewTest @GraphLocalesPreview @Composable
fun HistogramWeekWindowStacked() = HistogramSnapshot(2)

@PreviewTest @GraphLocalesPreview @Composable
fun HistogramMonthWindowStacked() = HistogramSnapshot(3)

@PreviewTest @GraphLocalesPreview @Composable
fun HistogramThreeMonthWindow() = HistogramSnapshot(4)

@PreviewTest @GraphLocalesPreview @Composable
fun HistogramSixMonthWindowStacked() = HistogramSnapshot(5)

@PreviewTest @GraphLocalesPreview @Composable
fun HistogramYearWindow() = HistogramSnapshot(6)

@PreviewTest @GraphPreview @Composable
fun PieSingleSegment() = PieChartSnapshot(0)

@PreviewTest @GraphPreview @Composable
fun PieTwoSegments() = PieChartSnapshot(1)

@PreviewTest @GraphPreview @Composable
fun PieFourSegments() = PieChartSnapshot(2)

@PreviewTest @GraphPreview @Composable
fun PieEightSegments() = PieChartSnapshot(3)

@PreviewTest @GraphPreview @Composable
fun PieRepeatingColorsWithArcLabels() = PieChartSnapshot(4)

@PreviewTest @GraphPreview @Composable
fun PieExplicitColors() = PieChartSnapshot(5)

@PreviewTest @GraphLocalesPreview @Composable
fun PieLocalizedEmptyLabels() = PieChartSnapshot(6)

@PreviewTest @GraphPreview @Composable
fun PieTinySegments() = PieChartSnapshot(7)
