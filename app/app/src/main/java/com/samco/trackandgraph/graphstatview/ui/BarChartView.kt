/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph. If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.graphstatview.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.BarChartSeries
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import com.samco.trackandgraph.graphstatview.factories.viewdto.IBarChartViewData
import com.samco.trackandgraph.helpers.formatDayMonthYearHourMinute
import com.samco.trackandgraph.helpers.formatTimeDuration
import com.samco.trackandgraph.ui.ui.ColorCircle
import com.samco.trackandgraph.ui.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.ui.cardElevation
import com.samco.trackandgraph.ui.ui.cardPadding
import com.samco.trackandgraph.ui.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.TemporalAmount
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

private const val maximumBarChartZoom = 20.0

@Composable
fun BarChartView(
    modifier: Modifier = Modifier,
    viewData: IBarChartViewData,
    listMode: Boolean,
    timeMarker: OffsetDateTime? = null,
    graphViewMode: GraphViewMode,
    graphBackgroundColor: Color,
) = Box(modifier = modifier) {
    val yRange = getRenderableBarChartYRange(viewData.yMin, viewData.yMax)
    if (
        viewData.xDates.isEmpty() ||
        viewData.bars.isEmpty() ||
        yRange == null ||
        viewData.bars.any { series ->
            series.values.size != viewData.xDates.size || series.values.any { !it.isFinite() }
        }
    ) {
        GraphErrorView(error = R.string.graph_stat_view_not_enough_data_graph)
        return@Box
    }

    var highlightedIndex by remember(timeMarker, viewData.xDates, viewData.endTime) {
        mutableStateOf(timeMarker?.toBarIndex(viewData.xDates, viewData.endTime))
    }

    BarChartBodyView(
        xDates = viewData.xDates,
        bars = viewData.bars,
        durationBasedRange = viewData.durationBasedRange,
        yMin = yRange.first,
        yMax = yRange.second,
        yAxisSubdivides = viewData.yAxisSubdivides,
        listMode = listMode,
        highlightedIndex = highlightedIndex,
        onHighlightedIndexChanged = { highlightedIndex = it },
        graphViewMode = graphViewMode,
        graphBackgroundColor = graphBackgroundColor,
    )

    if (!listMode) {
        AnimatedContent(
            targetState = highlightedIndex,
            modifier = Modifier
                .wrapContentHeight(Alignment.Top)
                .align(Alignment.TopEnd)
                .padding(top = cardElevation, end = cardElevation),
            transitionSpec = {
                (
                    fadeIn() + scaleIn(transformOrigin = TransformOrigin(1f, 0f))
                    ) togetherWith (
                    fadeOut() + scaleOut(transformOrigin = TransformOrigin(1f, 0f))
                    ) using SizeTransform(clip = false)
            },
            contentKey = { it != null },
            label = "barChartDataOverlay",
        ) { index ->
            index?.let {
                BarChartDataOverlay(
                    context = LocalContext.current,
                    highlightedIndex = it,
                    xDates = viewData.xDates,
                    bars = viewData.bars,
                    barPeriod = viewData.barPeriod,
                    durationBasedRange = viewData.durationBasedRange,
                )
            }
        }
    }
}

/**
 * Returns finite, non-inverted bounds that the chart can safely use for coordinate calculations.
 *
 * A zero-length range would divide by zero while mapping values to pixels. Equal finite bounds are
 * expanded for compatibility with existing saved graphs; inverted and non-finite bounds are
 * rejected.
 */
internal fun getRenderableBarChartYRange(yMin: Double, yMax: Double): Pair<Double, Double>? {
    if (!yMin.isFinite() || !yMax.isFinite() || yMax < yMin) return null
    if (yMax > yMin) return yMin to yMax

    val expandedMax = yMin + max(1.0, kotlin.math.abs(yMin) * 0.1)
    return expandedMax.takeIf(Double::isFinite)?.let { yMin to it }
}

private fun OffsetDateTime.toBarIndex(
    xDates: List<ZonedDateTime>,
    endTime: ZonedDateTime,
): Int? {
    val zonedMarker = atZoneSameInstant(endTime.zone)
    val index = xDates.indexOfLast { zonedMarker.isAfter(it) } + 1
    return index.takeIf { it in xDates.indices }
}

internal fun doubleToString(value: Double, maxPlaces: Int = 3): String {
    if (!value.isFinite()) return value.toString()

    val scale = value.toBigDecimal().scale().coerceIn(0, maxPlaces.coerceAtLeast(0))
    return String.format("%.${scale}f", value)
}

@Composable
private fun BarChartDataOverlay(
    modifier: Modifier = Modifier,
    context: Context,
    highlightedIndex: Int,
    xDates: List<ZonedDateTime>,
    bars: List<BarChartSeries>,
    barPeriod: TemporalAmount,
    durationBasedRange: Boolean,
) = Surface(
    modifier = modifier
        .width(IntrinsicSize.Max),
    shape = MaterialTheme.shapes.small,
    shadowElevation = cardElevation,
) {
    val locale = LocalConfiguration.current.locales[0]
    val seriesBreakdownFormat = stringResource(R.string.bar_chart_series_breakdown_format)
    val total = remember(highlightedIndex, bars, durationBasedRange) {
        val totalValue = bars.sumOf { it.values[highlightedIndex] }
        if (durationBasedRange) formatTimeDuration(totalValue.toLong())
        else doubleToString(totalValue)
    }
    val fromText = remember(highlightedIndex, xDates, barPeriod) {
        formatDayMonthYearHourMinute(context, xDates[highlightedIndex].minus(barPeriod))
    }
    val toText = remember(highlightedIndex, xDates) {
        formatDayMonthYearHourMinute(context, xDates[highlightedIndex])
    }
    val extraDetails = remember(
        highlightedIndex,
        bars,
        durationBasedRange,
        locale,
        seriesBreakdownFormat,
    ) {
        val sum = bars.sumOf { it.values[highlightedIndex] }
        if (sum < 1e-6) {
            emptyList()
        } else {
            bars.map { series ->
                val value = series.values[highlightedIndex]
                val percentage = (value / sum) * 100.0
                val displayedValue = if (durationBasedRange) {
                    formatTimeDuration(value.toLong())
                } else {
                    doubleToString(value)
                }
                ExtraDetails(
                    color = series.color,
                    label = String.format(
                        locale,
                        seriesBreakdownFormat,
                        series.label,
                        displayedValue,
                        doubleToString(percentage, 1),
                    ),
                )
            }
        }
    }

    Column(modifier = Modifier.padding(cardPadding)) {
        Text(
            text = stringResource(id = R.string.from_formatted, fromText),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(id = R.string.to_formatted, toText),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(id = R.string.total_formatted, total),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (extraDetails.isNotEmpty()) BarChartDataOverlayExtraDetails(extraDetails)
    }
}

private data class ExtraDetails(
    val color: ColorSpec,
    val label: String,
)

@Composable
private fun BarChartDataOverlayExtraDetails(extraDetails: List<ExtraDetails>) {
    var expanded by remember { mutableStateOf(false) }

    DialogInputSpacing()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.info),
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier
                .size(inputSpacingLarge)
                .rotate(if (expanded) 180f else 0f),
        )
    }
    DialogInputSpacing()

    extraDetails.forEach { labelInfo ->
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Row {
                ColorCircle(
                    color = Color(getColorInt(labelInfo.color)),
                    size = graphLegendCircleSize,
                )
                HalfDialogInputSpacing()
                Text(
                    text = labelInfo.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = cardPadding),
                )
            }
        }
    }
}

@Composable
private fun BarChartBodyView(
    modifier: Modifier = Modifier,
    xDates: List<ZonedDateTime>,
    bars: List<BarChartSeries>,
    durationBasedRange: Boolean,
    yMin: Double,
    yMax: Double,
    yAxisSubdivides: Int,
    listMode: Boolean,
    highlightedIndex: Int?,
    onHighlightedIndexChanged: (Int?) -> Unit,
    graphViewMode: GraphViewMode,
    graphBackgroundColor: Color,
) {
    Column(modifier = modifier) {
        val hasLegend = bars.size > 1
        val noLabel = stringResource(R.string.no_label)
        val isInteractive = !listMode
        val maximumZoom = min(maximumBarChartZoom, xDates.size.toDouble()).coerceAtLeast(1.0)
        var zoom by remember(xDates) { mutableDoubleStateOf(1.0) }
        var centerFraction by remember(xDates) { mutableDoubleStateOf(0.5) }
        val viewport = calculateCategoricalGraphViewport(
            bucketCount = xDates.size,
            zoom = zoom,
            centerFraction = centerFraction,
            maximumZoom = maximumBarChartZoom,
        )
        val xLabelText = GraphXAxisLabelText(
            months = stringArrayResource(R.array.abbreviated_months).toList(),
            weekdays = stringArrayResource(R.array.abbreviated_weekdays).toList(),
            weekdayDayFormat = stringResource(R.string.compact_weekday_day_format),
            dayMonthFormat = stringResource(R.string.compact_day_month_format),
            monthYearFormat = stringResource(R.string.compact_month_year_format),
        )
        val textMeasurer = rememberTextMeasurer()
        val axisTextStyle = graphAxisTextStyle
        val density = LocalDensity.current
        val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = graphGridLineAlpha)
        val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        val barColors = bars.map { getColor(it.color) }
        val barSeriesValues = remember(bars) { bars.map { it.values } }
        val borderColor = MaterialTheme.colorScheme.onSurface
        val graphHeight = graphHeightFor(graphViewMode, hasLegend)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(graphHeight)
                .background(graphBackgroundColor),
        ) {
            val layout = remember(
                constraints,
                viewport,
                xDates,
                durationBasedRange,
                yMin,
                yMax,
                yAxisSubdivides,
                xLabelText,
                textMeasurer,
                axisTextStyle,
                density,
            ) {
                calculateBarChartLayout(
                    width = constraints.maxWidth.toFloat(),
                    height = constraints.maxHeight.toFloat(),
                    xDates = xDates,
                    viewport = viewport,
                    yMin = yMin,
                    yMax = yMax,
                    yAxisSubdivides = yAxisSubdivides,
                    durationBasedRange = durationBasedRange,
                    xLabelText = xLabelText,
                    measureText = { text -> textMeasurer.measure(text, axisTextStyle).size },
                    density = density.density,
                )
            }
            val reveal = rememberGraphReveal(xDates to bars, layout != null)
            val currentLayout = rememberUpdatedState(layout)
            val currentHighlightedIndex = rememberUpdatedState(highlightedIndex)
            val horizontalViewportTransform = rememberUpdatedState<(Float, Float) -> Unit>(
                newValue = { panX, gestureZoom ->
                    currentLayout.value?.plotRect?.width?.let { plotWidth ->
                        zoom = (zoom * gestureZoom).coerceIn(1.0, maximumZoom)
                        val halfSpanFraction = 0.5 / zoom
                        centerFraction = (centerFraction - panX / plotWidth / zoom)
                            .coerceIn(halfSpanFraction, 1.0 - halfSpanFraction)
                    }
                },
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isInteractive) {
                        if (!isInteractive) return@pointerInput
                        detectTapGestures { offset ->
                            currentLayout.value?.barIndexAt(offset)?.let { index ->
                                onHighlightedIndexChanged(
                                    index.takeUnless { it == currentHighlightedIndex.value }
                                )
                            }
                        }
                    }
                    .pointerInput(isInteractive, maximumZoom) {
                        if (!isInteractive) return@pointerInput
                        detectHorizontalGraphPinchGestures { panX, gestureZoom ->
                            horizontalViewportTransform.value(panX, gestureZoom)
                        }
                    }
                    .pointerInput(isInteractive, maximumZoom) {
                        if (!isInteractive) return@pointerInput
                        detectHorizontalDragGestures { _, dragAmount ->
                            horizontalViewportTransform.value(dragAmount, 1f)
                        }
                    },
            ) {
                val chartLayout = layout ?: return@Canvas
                val plot = chartLayout.plotRect
                val alpha = reveal.value
                drawGraphAxes(
                    plot = plot,
                    xTicks = chartLayout.xTicks,
                    yTicks = chartLayout.yTicks,
                    xToPixel = chartLayout::xToPixel,
                    yToPixel = chartLayout::yToPixel,
                    textMeasurer = textMeasurer,
                    axisTextStyle = axisTextStyle,
                    gridColor = gridColor,
                    alpha = alpha,
                )
                val firstBar = ceil(chartLayout.minX - 0.5).toInt().coerceAtLeast(0)
                val lastBar = floor(chartLayout.maxX + 0.5).toInt().coerceAtMost(xDates.lastIndex)
                if (firstBar <= lastBar) {
                    drawStackedGraphBars(
                        plot = plot,
                        bucketRange = firstBar..lastBar,
                        valuesBySeries = barSeriesValues,
                        colors = barColors,
                        xToPixel = chartLayout::xToPixel,
                        yToPixel = chartLayout::yToPixel,
                        alpha = alpha,
                        borderColor = borderColor,
                        drawBorders = xDates.size < 60,
                        highlightedBucket = highlightedIndex,
                        highlightColor = highlightColor,
                    )
                }
            }
        }

        DialogInputSpacing()
        if (hasLegend) {
            GraphLegend(
                items = bars.map { bar ->
                    GraphLegendItem(
                        color = getColor(bar.color),
                        label = bar.label.ifEmpty { noLabel },
                    )
                }
            )
        }
    }
}

internal data class BarChartLayout(
    val plotRect: Rect,
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
    val barCount: Int,
    val xTicks: List<GraphXAxisTick<Double>>,
    val yTicks: List<GraphYAxisTick>,
) {
    fun xToPixel(value: Double): Float = plotRect.left +
        ((value - minX) / (maxX - minX) * plotRect.width).toFloat()

    fun yToPixel(value: Double): Float = plotRect.bottom -
        ((value - minY) / (maxY - minY) * plotRect.height).toFloat()

    fun barIndexAt(offset: Offset): Int? {
        if (!plotRect.contains(offset)) return null
        val value = minX + (offset.x - plotRect.left) / plotRect.width * (maxX - minX)
        return floor(value + 0.5).toInt().takeIf { it in 0 until barCount }
    }
}

internal fun calculateBarChartLayout(
    width: Float,
    height: Float,
    xDates: List<ZonedDateTime>,
    viewport: CategoricalGraphViewport,
    yMin: Double,
    yMax: Double,
    yAxisSubdivides: Int,
    durationBasedRange: Boolean,
    xLabelText: GraphXAxisLabelText,
    measureText: (String) -> IntSize,
    density: Float,
): BarChartLayout? {
    if (width <= 0f || height <= 0f || xDates.isEmpty() || yMax <= yMin) return null
    val tickCount = yAxisSubdivides.coerceAtLeast(2)
    val yTicks = List(tickCount) { index ->
        val value = yMin + (yMax - yMin) * index / (tickCount - 1)
        GraphYAxisTick(
            value = value,
            label = if (durationBasedRange) formatTimeDuration(value.roundToLong()) else doubleToString(value),
        )
    }
    val yLabelSizes = yTicks.map { measureText(it.label) }
    val widestYLabel = yLabelSizes.maxOf { it.width }.toFloat()
    val labelHeight = yLabelSizes.maxOf { it.height }.toFloat()
    val right = width - graphPlotEndPadding.value * density
    val top = max(graphPlotTopPadding.value * density, labelHeight / 2f)
    val fullDuration = Duration.between(xDates.first(), xDates.last()).toMillis().coerceAtLeast(1L)
    val visibleDuration = (fullDuration * (viewport.maxX - viewport.minX) / xDates.size)
        .roundToLong().coerceAtLeast(1L)
    val format = graphXAxisLabelFormatForDuration(visibleDuration)
    val zoneId = ZoneId.systemDefault()
    val maximumLabelSize = graphXAxisLabelCandidates(format, xLabelText)
        .map(measureText)
        .maxBy { it.width }
    val firstVisibleIndex = ceil(viewport.minX).toInt().coerceAtLeast(0)
    val firstLabel = formatGraphXAxisTimestamp(
        xDates[firstVisibleIndex.coerceAtMost(xDates.lastIndex)].toInstant().toEpochMilli(),
        format,
        zoneId,
        xLabelText,
    )
    val firstLabelSize = measureText(firstLabel)
    val left = max(
        widestYLabel + graphPlotStartPadding.value * density,
        if (firstVisibleIndex == 0) {
            graphRotatedLabelWidth(firstLabelSize) - graphRotatedLabelRightExtent(firstLabelSize)
        } else 0f,
    )
    val plotWidth = right - left
    if (plotWidth <= 0f) return null
    val xSpacing = plotWidth / (viewport.maxX - viewport.minX).toFloat()
    val labelSpacing = calculateCategoricalGraphLabelSpacing(
        visibleBucketCount = ceil(viewport.maxX - viewport.minX).toInt(),
        maximumProjectedLabelWidth = graphRotatedLabelWidth(maximumLabelSize),
        bucketWidth = xSpacing,
        minimumGap = graphAxisLabelMinimumGap.value * density,
    )
    val firstTick = ceil(max(0.0, viewport.minX) / labelSpacing).toInt() * labelSpacing
    val lastTick = floor(min(xDates.lastIndex.toDouble(), viewport.maxX) / labelSpacing)
        .toInt() * labelSpacing
    val xTicks = if (firstTick > lastTick) emptyList() else {
        (firstTick..lastTick step labelSpacing).map { index ->
            val label = formatGraphXAxisTimestamp(
                xDates[index].toInstant().toEpochMilli(), format, zoneId, xLabelText,
            )
            val size = measureText(label)
            GraphXAxisTick(
                value = index.toDouble(),
                label = label,
                projectedWidth = graphRotatedLabelWidth(size),
                projectedRightExtent = graphRotatedLabelRightExtent(size),
            )
        }
    }
    val bottom = height - graphRotatedLabelHeight(
        maximumLabelSize.width.toFloat(),
        maximumLabelSize.height.toFloat(),
    ) - graphPlotBottomPadding.value * density
    if (bottom <= top) return null
    return BarChartLayout(
        plotRect = Rect(left, top, right, bottom),
        minX = viewport.minX,
        maxX = viewport.maxX,
        minY = yMin,
        maxY = yMax,
        barCount = xDates.size,
        xTicks = xTicks,
        yTicks = yTicks,
    )
}

@Preview(showBackground = true)
@Composable
private fun BarChartBodyViewPreview() {
    val end = ZonedDateTime.of(2026, 6, 8, 23, 59, 59, 0, ZoneOffset.UTC)
    TnGComposeTheme {
        BarChartBodyView(
            xDates = List(7) { index -> end.minusDays((6 - index).toLong()) },
            bars = listOf(
                BarChartSeries(
                    label = "Work",
                    values = listOf(2.0, 3.0, 4.0, 2.0, 5.0, 3.0, 4.0),
                    color = ColorSpec.ColorIndex(0),
                ),
                BarChartSeries(
                    label = "Personal",
                    values = listOf(1.0, 2.0, 1.0, 3.0, 2.0, 4.0, 2.0),
                    color = ColorSpec.ColorIndex(4),
                ),
            ),
            durationBasedRange = false,
            yMin = 0.0,
            yMax = 8.0,
            yAxisSubdivides = 9,
            listMode = true,
            highlightedIndex = null,
            onHighlightedIndexChanged = {},
            graphViewMode = GraphViewMode.ListMode,
            graphBackgroundColor = MaterialTheme.colorScheme.surface,
        )
    }
}
