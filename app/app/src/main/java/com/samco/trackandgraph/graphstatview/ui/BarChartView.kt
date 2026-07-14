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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.BarChartSeries
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import com.samco.trackandgraph.graphstatview.factories.viewdto.IBarChartViewData
import com.samco.trackandgraph.helpers.formatDayMonthYearHourMinute
import com.samco.trackandgraph.helpers.formatTimeDuration
import com.samco.trackandgraph.helpers.getDayMonthFormatter
import com.samco.trackandgraph.helpers.getMonthYearFormatter
import com.samco.trackandgraph.ui.ui.ColorCircle
import com.samco.trackandgraph.ui.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.ui.cardElevation
import com.samco.trackandgraph.ui.ui.cardPadding
import com.samco.trackandgraph.ui.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.TemporalAmount
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val barThickness = 16.dp

@Composable
fun BarChartView(
    modifier: Modifier = Modifier,
    viewData: IBarChartViewData,
    listMode: Boolean,
    timeMarker: OffsetDateTime? = null,
    graphViewMode: GraphViewMode,
    graphBackgroundColor: Color,
) = Box(modifier = modifier) {
    if (viewData.xDates.isEmpty() || viewData.bars.isEmpty()) {
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
        yMin = viewData.yMin,
        yMax = viewData.yMax,
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
    val extraDetails = remember(highlightedIndex, bars, durationBasedRange) {
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
                    label = "${series.label}: $displayedValue (${doubleToString(percentage, 1)}%)"
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
    ProvideGraphVicoTheme {
        Column(modifier = modifier) {
            val context = LocalContext.current
            val hasLegend = bars.size > 1
            val modelProducer = remember { CartesianChartModelProducer() }

            LaunchedEffect(modelProducer, bars) {
                modelProducer.runTransaction {
                    columnModel {
                        bars.forEach { series(it.values) }
                    }
                }
            }

            val borderFill = if (xDates.size < 60) {
                Fill(MaterialTheme.colorScheme.onSurface)
            } else {
                Fill.Transparent
            }
            val columns = bars.map { series ->
                rememberLineComponent(
                    fill = Fill(Color(getColorInt(series.color))),
                    thickness = barThickness,
                    strokeFill = borderFill,
                    strokeThickness = 0.5.dp,
                )
            }
            val columnLayer = rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(columns),
                columnCollectionSpacing = 0.dp,
                mergeMode = { ColumnCartesianLayer.MergeMode.Stacked },
                rangeProvider = CartesianLayerRangeProvider.fixed(
                    minX = -0.5,
                    maxX = xDates.lastIndex + 0.5,
                    minY = yMin,
                    maxY = yMax,
                ),
            )

            val xAxisFormatter = remember(context, xDates) { getXAxisFormatter(context, xDates) }
            val gridLine = rememberLineComponent(
                fill = Fill(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)),
                thickness = 0.5.dp,
            )
            val bottomAxis = HorizontalAxis.rememberBottom(
                line = gridLine,
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    xDates[value.roundToInt().coerceIn(xDates.indices)].format(xAxisFormatter)
                },
                tick = gridLine,
                guideline = gridLine,
                itemPlacer = remember(xDates.size) {
                    AdaptiveHorizontalAxisItemPlacer(xDates.size)
                },
            )
            val startAxis = VerticalAxis.rememberStart(
                line = gridLine,
                valueFormatter = CartesianValueFormatter { _, value, _ ->
                    if (durationBasedRange) formatTimeDuration(value.toLong())
                    else doubleToString(value)
                },
                tick = gridLine,
                guideline = gridLine,
                itemPlacer = VerticalAxis.ItemPlacer.count(
                    count = { yAxisSubdivides.coerceAtLeast(1) },
                ),
            )

            val interactionMarker = remember { object : CartesianMarker {} }
            val highlightDecoration = rememberHighlightDecoration(
                color = MaterialTheme.colorScheme.onSurface,
                barThickness = barThickness,
                highlightedIndex = highlightedIndex,
            )
            val markerVisibilityListener = remember(onHighlightedIndexChanged, xDates) {
                object : CartesianMarkerVisibilityListener {
                    override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                        onHighlightedIndexChanged(targets.firstOrNull()?.x?.roundToInt()?.takeIf {
                            it in xDates.indices
                        })
                    }

                    override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                        onShown(marker, targets)
                    }

                    override fun onHidden(marker: CartesianMarker) {
                        onHighlightedIndexChanged(null)
                    }
                }
            }

            val chart = rememberCartesianChart(
                columnLayer,
                startAxis = startAxis,
                bottomAxis = bottomAxis,
                marker = if (listMode) null else interactionMarker,
                markerVisibilityListener = markerVisibilityListener,
                decorations = listOf(highlightDecoration),
                markerController = CartesianMarkerController.rememberToggleOnTap(),
                getXStep = { _, _, _ -> 1.0 },
            )
            val scrollState = rememberVicoScrollState(scrollEnabled = !listMode)
            val zoomState = rememberVicoZoomState(
                zoomEnabled = !listMode,
                initialZoom = Zoom.Content,
            )
            val graphHeight = graphHeightFor(graphViewMode, hasLegend)

            CartesianChartHost(
                chart = chart,
                modelProducer = modelProducer,
                scrollState = scrollState,
                zoomState = zoomState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(graphHeight)
                    .background(graphBackgroundColor),
            )

            DialogInputSpacing()
            if (hasLegend) {
                GraphLegend(
                    items = bars.map { bar ->
                        GraphLegendItem(
                            color = getColor(bar.color),
                            label = bar.label.ifEmpty { context.getString(R.string.no_label) },
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun rememberHighlightDecoration(
    color: Color,
    barThickness: Dp,
    highlightedIndex: Int?,
): Decoration = remember(color, barThickness, highlightedIndex) {
    object : Decoration {
        private val paint = Paint().apply { this.color = color.copy(alpha = 0.2f) }

        override fun drawOverLayers(
            context: CartesianDrawingContext,
        ) {
            val selectedX = highlightedIndex?.toDouble() ?: return
            val xSpacing = context.layerDimensions.xSpacing
            if (xSpacing == 0f) return
            val xStep = context.ranges.xStep
            val fullRangeStart = context.ranges.minX -
                context.layerDimensions.startPadding / xSpacing * xStep
            val visibleRangeStart = fullRangeStart +
                context.layoutDirectionMultiplier * context.scroll / xSpacing * xStep
            val xOffset = (
                (selectedX - visibleRangeStart) /
                    xStep *
                    xSpacing
                ).toFloat()
            val centerX = if (context.isLtr) {
                context.layerBounds.left + xOffset
            } else {
                context.layerBounds.right - xOffset
            }
            val width = with(context) { barThickness.pixels } * context.zoom
            val left = max(centerX - width / 2f, context.layerBounds.left)
            val right = min(centerX + width / 2f, context.layerBounds.right)
            if (left >= right) return
            context.canvas.drawRect(
                Rect(
                    left = left,
                    top = context.layerBounds.top,
                    right = right,
                    bottom = context.layerBounds.bottom,
                ),
                paint,
            )
        }
    }
}

private fun calculateLabelSpacing(barCount: Int): Int {
    var spacing = 1
    while (barCount.toDouble() / spacing > 10.0) spacing *= 2
    return spacing
}

private class AdaptiveHorizontalAxisItemPlacer(
    barCount: Int,
) : HorizontalAxis.ItemPlacer by HorizontalAxis.ItemPlacer.aligned(
    spacing = { calculateLabelSpacing(barCount) },
    addExtremeLabelPadding = true,
) {
    override fun getLabelValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> {
        val visibleBarCount = ceil(visibleXRange.endInclusive - visibleXRange.start).toInt() + 1
        val spacing = calculateLabelSpacing(visibleBarCount)
        val first = ceil(visibleXRange.start / spacing).toInt() * spacing
        val last = floor(visibleXRange.endInclusive / spacing).toInt() * spacing
        if (first > last) return emptyList()
        return (first..last step spacing).map(Int::toDouble)
    }
}

private fun getXAxisFormatter(
    context: Context,
    xDates: List<ZonedDateTime>,
): DateTimeFormatter {
    val durationRange = Duration.between(xDates.first(), xDates.last())
    return when {
        durationRange.toMinutes() < 5L -> DateTimeFormatter.ofPattern("HH:mm:ss")
        durationRange.toDays() >= 304 -> getMonthYearFormatter(context)
        durationRange.toDays() >= 1 -> getDayMonthFormatter(context)
        else -> DateTimeFormatter.ofPattern("HH:mm")
    }
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
