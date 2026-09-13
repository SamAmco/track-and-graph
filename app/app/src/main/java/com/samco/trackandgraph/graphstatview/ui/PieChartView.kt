/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.ui.dataVisColorGenerator
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.ui.ui.DialogInputSpacing
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import java.util.Locale

private val pieChartPadding = 8.dp
private val pieArcLabelPadding = 4.dp

internal data class PieSegmentInfo(
    val value: Double,
    val color: Color,
    val legendLabel: String,
    val arcLabel: String?,
)

@Composable
fun PieChartView(
    modifier: Modifier = Modifier,
    viewData: IPieChartViewData,
    graphViewMode: GraphViewMode,
    graphBackgroundColor: Color,
) {
    val segments = viewData.segments
    if (!hasRenderablePieSegments(segments)) {
        GraphErrorView(
            modifier = modifier,
            error = R.string.graph_stat_view_not_enough_data_graph,
        )
        return
    }

    val locale = LocalConfiguration.current.locales[0]
    val noLabel = stringResource(R.string.no_label)
    val legendFormat = stringResource(R.string.pie_chart_legend_format)
    val indexedLegendFormat = stringResource(R.string.pie_chart_indexed_legend_format)
    val segmentInfos = remember(
        segments,
        locale,
        noLabel,
        legendFormat,
        indexedLegendFormat,
    ) {
        createPieSegmentInfos(
            segments = segments!!,
            locale = locale,
            noLabel = noLabel,
            legendFormat = legendFormat,
            indexedLegendFormat = indexedLegendFormat,
        )
    }
    PieChartViewBody(
        modifier = modifier,
        segments = segmentInfos,
        graphViewMode = graphViewMode,
        graphBackgroundColor = graphBackgroundColor,
    )
}

internal fun hasRenderablePieSegments(segments: List<IPieChartViewData.Segment>?): Boolean =
    !segments.isNullOrEmpty() &&
        segments.all { it.value.isFinite() && it.value >= 0.0 } &&
        segments.sumOf { it.value }.let { it.isFinite() && it > 0.0 }

internal fun createPieSegmentInfos(
    segments: List<IPieChartViewData.Segment>,
    locale: Locale,
    noLabel: String,
    legendFormat: String,
    indexedLegendFormat: String,
): List<PieSegmentInfo> {
    val showArcLabels = shouldLabelPieSegments(segments.size)
    val colors = resolvePieSegmentColors(segments)
    return segments.mapIndexed { index, segment ->
        val title = segment.title.ifEmpty { noLabel }
        val percentage = String.format(locale, "%.1f", segment.value)
        PieSegmentInfo(
            value = segment.value,
            color = colors[index],
            legendLabel = if (showArcLabels) {
                String.format(
                    locale,
                    indexedLegendFormat,
                    index,
                    title,
                    percentage,
                )
            } else {
                String.format(locale, legendFormat, title, percentage)
            },
            arcLabel = index.toString().takeIf { showArcLabels },
        )
    }
}

internal fun shouldLabelPieSegments(segmentCount: Int): Boolean =
    segmentCount > dataVisColorList.size

internal fun resolvePieSegmentColors(
    segments: List<IPieChartViewData.Segment>,
): List<Color> {
    var lastColorIndex: Int? = null
    val colors = segments.mapIndexed { index, segment ->
        when (val colorSpec = segment.color) {
            is ColorSpec.ColorIndex -> {
                lastColorIndex = colorSpec.index
                dataVisColorList[Math.floorMod(colorSpec.index, dataVisColorList.size)]
            }

            is ColorSpec.ColorValue -> Color(colorSpec.value)
            null -> {
                val sequenceIndex = lastColorIndex?.plus(1) ?: index
                lastColorIndex = sequenceIndex
                automaticPieColor(sequenceIndex)
            }
        }
    }.toMutableList()

    avoidMatchingPieColorsAtWrapBoundary(segments, colors, lastColorIndex)
    return colors
}

private fun automaticPieColor(sequenceIndex: Int): Color =
    dataVisColorList[Math.floorMod(
        sequenceIndex * dataVisColorGenerator,
        dataVisColorList.size,
    )]

private fun avoidMatchingPieColorsAtWrapBoundary(
    segments: List<IPieChartViewData.Segment>,
    colors: MutableList<Color>,
    lastColorIndex: Int?,
) {
    if (colors.size < 2 || colors.first() != colors.last() || dataVisColorList.size < 2) return

    val endpoint = when {
        segments.last().color == null -> colors.lastIndex
        segments.first().color == null -> 0
        else -> return
    }
    val adjacentColor = colors[if (endpoint == 0) 1 else colors.lastIndex - 1]
    val matchingBoundaryColor = colors[if (endpoint == 0) colors.lastIndex else 0]
    val startSequenceIndex = if (endpoint == 0) 0 else (lastColorIndex ?: colors.lastIndex) + 1
    val candidates = List(dataVisColorList.size) { offset ->
        automaticPieColor(startSequenceIndex + offset)
    }
    colors[endpoint] = candidates.firstOrNull {
        it != adjacentColor && it != matchingBoundaryColor
    } ?: candidates.first { it != matchingBoundaryColor }
}

@Composable
private fun PieChartViewBody(
    modifier: Modifier,
    segments: List<PieSegmentInfo>,
    graphViewMode: GraphViewMode,
    graphBackgroundColor: Color,
) = Column(modifier = modifier) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = graphAxisTextStyle
    val reveal = rememberGraphReveal(segments, ready = true)
    val total = remember(segments) { segments.sumOf { it.value } }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(graphHeightFor(graphViewMode, hasLegend = true))
            .background(graphBackgroundColor),
    ) {
        val radius = (min(size.width, size.height) / 2f - pieChartPadding.toPx())
            .coerceAtLeast(0f)
        if (radius == 0f) return@Canvas
        val topLeft = Offset(size.width / 2f - radius, size.height / 2f - radius)
        val diameter = radius * 2f
        var startAngle = -90f
        segments.forEach { segment ->
            val sweepAngle = (segment.value / total * 360.0).toFloat()
            if (sweepAngle > 0f) {
                drawArc(
                    color = segment.color.copy(alpha = reveal.value),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                )
                segment.arcLabel?.let { label ->
                    drawPieArcLabel(
                        label = label,
                        midAngleDegrees = startAngle + sweepAngle / 2f,
                        center = center,
                        radius = radius,
                        textMeasurer = textMeasurer,
                        textStyle = labelStyle,
                        segmentColor = segment.color,
                        alpha = reveal.value,
                    )
                }
            }
            startAngle += sweepAngle
        }
    }

    DialogInputSpacing()
    GraphLegend(
        items = segments.map { GraphLegendItem(color = it.color, label = it.legendLabel) },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPieArcLabel(
    label: String,
    midAngleDegrees: Float,
    center: Offset,
    radius: Float,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    segmentColor: Color,
    alpha: Float,
) {
    val measured = textMeasurer.measure(label, textStyle)
    val labelCenter = calculatePieArcLabelCenter(
        center = center,
        radius = radius,
        angleDegrees = midAngleDegrees,
        labelSize = measured.size,
        padding = pieArcLabelPadding.toPx(),
    )
    val textColor = if (segmentColor.luminance() > 0.5f) Color.Black else Color.White
    drawText(
        textMeasurer = textMeasurer,
        text = label,
        style = textStyle.copy(color = textColor.copy(alpha = alpha)),
        topLeft = Offset(
            labelCenter.x - measured.size.width / 2f,
            labelCenter.y - measured.size.height / 2f,
        ),
    )
}

internal fun calculatePieArcLabelCenter(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
    labelSize: IntSize,
    padding: Float,
): Offset {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val xDirection = cos(radians).toFloat()
    val yDirection = sin(radians).toFloat()
    val labelRadialExtent = abs(xDirection) * labelSize.width / 2f +
        abs(yDirection) * labelSize.height / 2f
    val labelRadius = (radius - padding - labelRadialExtent).coerceAtLeast(0f)
    return Offset(
        x = center.x + xDirection * labelRadius,
        y = center.y + yDirection * labelRadius,
    )
}
