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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatview.ui

import android.graphics.Color as GColor
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.androidplot.pie.PieRenderer
import com.androidplot.pie.Segment
import com.androidplot.pie.SegmentFormatter
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.GraphPieChartBinding
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.ui.dataVisColorGenerator
import com.samco.trackandgraph.ui.dataVisColorList

private data class SegmentInfo(
    val color: Color,
    val segment: Segment,
    val label: String
)

@Composable
fun PieChartView(
    modifier: Modifier = Modifier,
    viewData: IPieChartViewData,
    graphViewMode: GraphViewMode,
) {
    val segments = viewData.segments
    if (segments.isNullOrEmpty()) {
        GraphErrorView(
            modifier = modifier,
            error = R.string.graph_stat_view_not_enough_data_graph
        )
    } else {
        val segmentInfos = mutableListOf<SegmentInfo>()
        var lastColorIndex: Int? = null

        for (index in segments.indices) {
            val segment = segments[index]

            val color = when (segment.color) {
                is ColorSpec.ColorIndex -> {
                    lastColorIndex = segment.color.index
                    dataVisColorList[segment.color.index]
                }

                is ColorSpec.ColorValue -> Color(segment.color.value)

                null -> {
                    if (lastColorIndex != null) {
                        val colorIndex = (dataVisColorGenerator * ++lastColorIndex) % dataVisColorList.size
                        dataVisColorList[colorIndex]
                    } else {
                        lastColorIndex = index
                        dataVisColorList[index]
                    }
                }
            }

            val percentage = "%.1f".format(segment.value)
            val title = segment.title.ifEmpty { stringResource(R.string.no_label) }

            val label = when {
                segments.size > dataVisColorList.size -> "$index: $title ($percentage%)"
                else -> "$title ($percentage%)"
            }

            val segmentTitle = when {
                segments.size > dataVisColorList.size -> index.toString()
                else -> title
            }

            val androidPlotSegment = Segment(segmentTitle, segment.value)

            segmentInfos.add(
                SegmentInfo(
                    color = color,
                    segment = androidPlotSegment,
                    label = label
                )
            )
        }

        PieChartViewBody(
            modifier = modifier,
            segments = segmentInfos,
            graphViewMode = graphViewMode,
        )
    }
}

@Composable
private fun PieChartViewBody(
    modifier: Modifier = Modifier,
    segments: List<SegmentInfo>,
    graphViewMode: GraphViewMode,
) = Column(modifier = modifier) {

    val context = LocalContext.current

    val smallLabelSize = context.resources.getDimension(R.dimen.small_label_size)
    val labelColor = Color.White.toArgb()

    AndroidViewBinding(factory = { inflater, parent, attachToParent ->
        val binding = GraphPieChartBinding.inflate(inflater, parent, attachToParent)

        binding.pieChart.clear()
        binding.pieChart.backgroundPaint.color = GColor.TRANSPARENT

        segments.forEachIndexed { i, s ->

            val segForm = SegmentFormatter(s.color.toArgb())
            if (segments.size > dataVisColorList.size) {
                segForm.labelPaint.textSize = smallLabelSize
                segForm.labelPaint.color = labelColor
            } else {
                segForm.labelPaint.color = GColor.TRANSPARENT
            }

            binding.pieChart.addSegment(s.segment, segForm)
        }

        return@AndroidViewBinding binding
    }, update = {
        setGraphHeight(
            graphView = pieChart,
            graphViewMode = graphViewMode,
            hasLegend = true,
        )
        pieChart.redraw()
        pieChart.getRenderer(PieRenderer::class.java)
            .setDonutSize(0f, PieRenderer.DonutMode.PERCENT)
        pieChart.requestLayout()
    })

    GraphLegend(
        items = segments.map {
            val label = it.label
                .ifEmpty { context.getString(R.string.no_label) }
            GraphLegendItem(color = it.color, label = label)
        }
    )
}