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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.TimeHistogramWindowData
import com.samco.trackandgraph.data.database.dto.TimeHistogramWindow
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeHistogramViewData
import com.samco.trackandgraph.ui.dataVisColorGenerator
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.ui.DialogInputSpacing
import org.threeten.bp.DayOfWeek
import java.text.DecimalFormat
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private val histogramAxisTitlePadding = 4.dp
private val histogramNumberFormatter = DecimalFormat("#,##0.#")

@Composable
fun TimeHistogramView(
    modifier: Modifier = Modifier,
    viewData: ITimeHistogramViewData,
    graphViewMode: GraphViewMode,
    graphBackgroundColor: Color,
) {
    val barValues = viewData.barValues
    if (!isRenderableTimeHistogram(viewData.window, barValues, viewData.maxDisplayHeight)) {
        GraphErrorView(
            modifier = modifier,
            error = R.string.graph_stat_view_not_enough_data_graph,
        )
        return
    }

    TimeHistogramBodyView(
        modifier = modifier,
        window = viewData.window,
        firstDayOfWeek = viewData.firstDayOfWeek,
        barValues = requireNotNull(barValues),
        maxDisplayHeight = viewData.maxDisplayHeight,
        graphViewMode = graphViewMode,
        graphBackgroundColor = graphBackgroundColor,
    )
}

internal fun isRenderableTimeHistogram(
    window: TimeHistogramWindowData,
    barValues: List<ITimeHistogramViewData.BarValue>?,
    maxDisplayHeight: Double,
): Boolean = !barValues.isNullOrEmpty() &&
    maxDisplayHeight.isFinite() &&
    maxDisplayHeight > 0.0 &&
    barValues.all { series ->
        series.values.size == window.numBins && series.values.all(Double::isFinite)
    }

@Composable
private fun TimeHistogramBodyView(
    modifier: Modifier,
    window: TimeHistogramWindowData,
    firstDayOfWeek: DayOfWeek,
    barValues: List<ITimeHistogramViewData.BarValue>,
    maxDisplayHeight: Double,
    graphViewMode: GraphViewMode,
    graphBackgroundColor: Color,
) = Column(modifier = modifier) {
    val hasLegend = barValues.size > 1
    val noLabel = stringResource(R.string.no_label)
    val labelText = TimeHistogramLabelText(
        months = stringArrayResource(R.array.abbreviated_months).toList(),
        weekdays = stringArrayResource(R.array.abbreviated_weekdays).toList(),
    )
    val bucketLabels = remember(window.window, firstDayOfWeek, labelText) {
        timeHistogramBucketLabels(window, firstDayOfWeek, labelText)
    }
    val axisTitle = timeHistogramAxisTitle(window.window)
    val colors = barValues.indices.map { index ->
        dataVisColorList[(index * dataVisColorGenerator) % dataVisColorList.size]
    }
    val seriesValues = remember(barValues) { barValues.map { it.values } }
    val axisTextStyle = graphAxisTextStyle
    val axisTitleTextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = graphGridLineAlpha)
    val borderColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val isInteractive = graphViewMode is GraphViewMode.FullScreenMode
    var zoom by remember(window) { mutableDoubleStateOf(1.0) }
    var centerFraction by remember(window) { mutableDoubleStateOf(0.5) }
    val maximumZoom = bucketLabels.size.toDouble().coerceAtLeast(1.0)
    val viewport = calculateCategoricalGraphViewport(
        bucketCount = bucketLabels.size,
        zoom = zoom,
        centerFraction = centerFraction,
    )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(graphHeightFor(graphViewMode, hasLegend))
            .background(graphBackgroundColor),
    ) {
        val layout = remember(
            constraints,
            bucketLabels,
            axisTitle,
            viewport,
            maxDisplayHeight,
            textMeasurer,
            axisTextStyle,
            axisTitleTextStyle,
            density,
        ) {
            calculateTimeHistogramLayout(
                width = constraints.maxWidth.toFloat(),
                height = constraints.maxHeight.toFloat(),
                bucketLabels = bucketLabels,
                viewport = viewport,
                axisTitle = axisTitle,
                maxY = maxDisplayHeight,
                measureAxisText = { text -> textMeasurer.measure(text, axisTextStyle).size },
                measureTitleText = { text -> textMeasurer.measure(text, axisTitleTextStyle).size },
                density = density.density,
            )
        }
        val reveal = rememberGraphReveal(window to barValues, layout != null)
        val currentLayout = rememberUpdatedState(layout)
        val horizontalViewportTransform = rememberUpdatedState<(Float, Float) -> Unit> { panX, gestureZoom ->
            currentLayout.value?.plotRect?.width?.let { plotWidth ->
                zoom = (zoom * gestureZoom).coerceIn(1.0, maximumZoom)
                val halfSpanFraction = 0.5 / zoom
                centerFraction = (centerFraction - panX / plotWidth / zoom)
                    .coerceIn(halfSpanFraction, 1.0 - halfSpanFraction)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
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
            val histogramLayout = layout ?: return@Canvas
            val alpha = reveal.value
            drawGraphAxes(
                plot = histogramLayout.plotRect,
                xTicks = histogramLayout.xTicks,
                yTicks = histogramLayout.yTicks,
                xToPixel = histogramLayout::xToPixel,
                yToPixel = histogramLayout::yToPixel,
                textMeasurer = textMeasurer,
                axisTextStyle = axisTextStyle,
                gridColor = gridColor,
                alpha = alpha,
            )
            val firstBar = ceil(histogramLayout.minX - 0.5).toInt().coerceAtLeast(0)
            val lastBar = floor(histogramLayout.maxX + 0.5).toInt().coerceAtMost(bucketLabels.lastIndex)
            if (firstBar <= lastBar) {
                drawStackedGraphBars(
                    plot = histogramLayout.plotRect,
                    bucketRange = firstBar..lastBar,
                    valuesBySeries = seriesValues,
                    colors = colors,
                    xToPixel = histogramLayout::xToPixel,
                    yToPixel = histogramLayout::yToPixel,
                    alpha = alpha,
                    borderColor = borderColor,
                    drawBorders = true,
                )
            }
            val titleSize = textMeasurer.measure(axisTitle, axisTitleTextStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = axisTitle,
                style = axisTitleTextStyle.copy(
                    color = axisTitleTextStyle.color.copy(alpha = alpha),
                ),
                topLeft = Offset(
                    histogramLayout.plotRect.center.x - titleSize.size.width / 2f,
                    histogramLayout.axisTitleTop,
                ),
            )
        }
    }

    DialogInputSpacing()
    if (hasLegend) {
        GraphLegend(
            items = barValues.mapIndexed { index, bar ->
                GraphLegendItem(
                    color = colors[index],
                    label = bar.label.ifEmpty { noLabel },
                )
            },
        )
    }
}

@Composable
private fun timeHistogramAxisTitle(window: TimeHistogramWindow): String = stringResource(
    when (window) {
        TimeHistogramWindow.HOUR -> R.string.minutes
        TimeHistogramWindow.DAY -> R.string.hours
        TimeHistogramWindow.WEEK, TimeHistogramWindow.MONTH -> R.string.days
        TimeHistogramWindow.THREE_MONTHS, TimeHistogramWindow.SIX_MONTHS -> R.string.weeks
        TimeHistogramWindow.YEAR -> R.string.months
    }
)

internal data class TimeHistogramLabelText(
    val months: List<String>,
    val weekdays: List<String>,
)

internal fun timeHistogramBucketLabels(
    window: TimeHistogramWindowData,
    firstDayOfWeek: DayOfWeek,
    labelText: TimeHistogramLabelText,
): List<String> = when (window.window) {
    TimeHistogramWindow.HOUR, TimeHistogramWindow.DAY ->
        List(window.numBins) { it.toString() }

    TimeHistogramWindow.WEEK -> List(window.numBins) { offset ->
        labelText.weekdays[(firstDayOfWeek.value - 1 + offset) % labelText.weekdays.size]
    }

    TimeHistogramWindow.MONTH,
    TimeHistogramWindow.THREE_MONTHS,
    TimeHistogramWindow.SIX_MONTHS -> List(window.numBins) { (it + 1).toString() }

    TimeHistogramWindow.YEAR -> labelText.months.take(window.numBins)
}

internal data class TimeHistogramLayout(
    val plotRect: Rect,
    val minX: Double,
    val maxX: Double,
    val maxY: Double,
    val xTicks: List<GraphXAxisTick<Double>>,
    val yTicks: List<GraphYAxisTick>,
    val axisTitleTop: Float,
) {
    fun xToPixel(value: Double): Float = plotRect.left +
        ((value - minX) / (maxX - minX) * plotRect.width).toFloat()

    fun yToPixel(value: Double): Float = plotRect.bottom -
        (value / maxY * plotRect.height).toFloat()
}

internal fun calculateTimeHistogramLayout(
    width: Float,
    height: Float,
    bucketLabels: List<String>,
    viewport: CategoricalGraphViewport,
    axisTitle: String,
    maxY: Double,
    measureAxisText: (String) -> IntSize,
    measureTitleText: (String) -> IntSize,
    density: Float,
): TimeHistogramLayout? {
    if (
        width <= 0f || height <= 0f || bucketLabels.isEmpty() ||
        !maxY.isFinite() || maxY <= 0.0 ||
        !viewport.minX.isFinite() || !viewport.maxX.isFinite() || viewport.maxX <= viewport.minX
    ) {
        return null
    }
    val yValues = calculateYTicks(
        rawMin = 0.0,
        rawMax = maxY,
        targetCount = approximateGraphYTickCount(height, density),
        fixed = true,
    )
    val yTicks = yValues.map { GraphYAxisTick(it, formatHistogramNumber(it)) }
    val yLabelSizes = yTicks.map { measureAxisText(it.label) }
    val widestYLabel = yLabelSizes.maxOf { it.width }.toFloat()
    val labelHeight = yLabelSizes.maxOf { it.height }.toFloat()
    val measuredBucketLabels = bucketLabels.map(measureAxisText)
    val maximumLabelSize = measuredBucketLabels.maxBy { it.width }
    val firstLabelSize = measuredBucketLabels.first()
    val left = max(
        widestYLabel + graphPlotStartPadding.value * density,
        if (viewport.minX <= -0.5) {
            graphRotatedLabelWidth(firstLabelSize) - graphRotatedLabelRightExtent(firstLabelSize)
        } else 0f,
    )
    val right = width - graphPlotEndPadding.value * density
    val top = max(graphPlotTopPadding.value * density, labelHeight / 2f)
    val rotatedLabelHeight = graphRotatedLabelHeight(
        maximumLabelSize.width.toFloat(),
        maximumLabelSize.height.toFloat(),
    )
    val axisTitleSize = measureTitleText(axisTitle)
    val bottom = height - rotatedLabelHeight - axisTitleSize.height -
        graphXAxisLabelPadding.value * density - histogramAxisTitlePadding.value * density -
        graphPlotBottomPadding.value * density
    if (right <= left || bottom <= top) return null
    val plot = Rect(left, top, right, bottom)
    fun xToPixel(index: Double): Float = plot.left +
        ((index - viewport.minX) / (viewport.maxX - viewport.minX) * plot.width).toFloat()
    val bucketWidth = plot.width / (viewport.maxX - viewport.minX).toFloat()
    val labelSpacing = calculateTimeHistogramLabelSpacing(
        visibleBucketCount = ceil(viewport.maxX - viewport.minX).toInt(),
        maximumLabelMetrics = GraphXAxisLabelMetrics(maximumLabelSize),
        bucketWidth = bucketWidth,
    )
    val firstTick = ceil(max(0.0, viewport.minX) / labelSpacing).toInt() * labelSpacing
    val lastTick = floor(min(bucketLabels.lastIndex.toDouble(), viewport.maxX) / labelSpacing)
        .toInt() * labelSpacing
    val candidates = if (firstTick > lastTick) emptyList() else {
        (firstTick..lastTick step labelSpacing)
    }
        .map { index ->
            val label = bucketLabels[index]
            val size = measuredBucketLabels[index]
            GraphXAxisTick(
                value = index.toDouble(),
                label = label,
                metrics = GraphXAxisLabelMetrics(size),
            )
        }
    val xTicks = selectGraphXAxisTicks(
        candidates = candidates,
        xToPixel = ::xToPixel,
        minimumX = 0f,
        maximumX = width,
    )
    return TimeHistogramLayout(
        plotRect = plot,
        minX = viewport.minX,
        maxX = viewport.maxX,
        maxY = maxY,
        xTicks = xTicks,
        yTicks = yTicks,
        axisTitleTop = bottom + graphXAxisLabelPadding.value * density + rotatedLabelHeight +
            histogramAxisTitlePadding.value * density,
    )
}

internal fun calculateTimeHistogramLabelSpacing(
    visibleBucketCount: Int,
    maximumLabelMetrics: GraphXAxisLabelMetrics,
    bucketWidth: Float,
): Int {
    val densitySpacing = ceil(visibleBucketCount.coerceAtLeast(1) / 12.0).toInt()
    val collisionSpacing = if (bucketWidth > 0f && bucketWidth.isFinite()) {
        ceil(
            maximumLabelMetrics.minimumAnchorDistanceTo(maximumLabelMetrics) /
                bucketWidth
        ).toInt()
    } else 1
    val requiredSpacing = max(1, max(densitySpacing, collisionSpacing))
    var scale = 1
    while (requiredSpacing > scale * 5) scale *= 10
    return when {
        requiredSpacing <= scale -> scale
        requiredSpacing <= scale * 2 -> scale * 2
        else -> scale * 5
    }
}

internal fun formatHistogramNumber(value: Double): String = synchronized(histogramNumberFormatter) {
    histogramNumberFormatter.format(value)
}

@Preview(showBackground = true)
@Composable
private fun TimeHistogramBodyViewPreview() {
    TnGComposeTheme {
        TimeHistogramBodyView(
            modifier = Modifier,
            window = TimeHistogramWindowData.getWindowData(TimeHistogramWindow.WEEK),
            firstDayOfWeek = DayOfWeek.MONDAY,
            barValues = listOf(
                ITimeHistogramViewData.BarValue(
                    label = "Work",
                    values = listOf(20.0, 10.0, 30.0, 20.0, 35.0, 15.0, 5.0),
                ),
                ITimeHistogramViewData.BarValue(
                    label = "Personal",
                    values = listOf(10.0, 20.0, 15.0, 10.0, 20.0, 25.0, 35.0),
                ),
            ),
            maxDisplayHeight = 60.0,
            graphViewMode = GraphViewMode.ListMode,
            graphBackgroundColor = MaterialTheme.colorScheme.surface,
        )
    }
}
