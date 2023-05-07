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

import android.graphics.Color
import androidx.annotation.ColorRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.androidplot.pie.PieRenderer
import com.androidplot.pie.Segment
import com.androidplot.pie.SegmentFormatter
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.GraphPieChartBinding
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.ui.dataVisColorGenerator
import com.samco.trackandgraph.ui.dataVisColorList

private data class SegmentInfo(
    @ColorRes val color: Int,
    val segment: Segment,
    val label: String
)

@Composable
fun PieChartView(
    modifier: Modifier = Modifier,
    viewData: IPieChartViewData,
    graphHeight: Int? = null
) {
    if (viewData.segments.isNullOrEmpty()) {
        GraphErrorView(
            modifier = modifier,
            error = R.string.graph_stat_view_not_enough_data_graph
        )
    } else {
        val segCount = viewData.segments!!.size
        val segments = viewData.segments!!.mapIndexed { i, s ->
            val index = (dataVisColorGenerator * i) % dataVisColorList.size
            val color = dataVisColorList[index]
            val percentage = "%.1f".format(s.value.toDouble())
            val title = s.title.ifEmpty { stringResource(R.string.no_label) }
            var label = "$title ($percentage%)"

            if (segCount > dataVisColorList.size) {
                label = "$i: $title ($percentage%)"
                s.title = i.toString()
            }

            SegmentInfo(
                color = color,
                segment = s,
                label = label
            )
        }

        PieChartViewBody(
            modifier = modifier,
            segments = segments,
            graphHeight = graphHeight
        )
    }

}

@Composable
private fun PieChartViewBody(
    modifier: Modifier = Modifier,
    segments: List<SegmentInfo>,
    graphHeight: Int?
) = Column(modifier = modifier) {

    val context = LocalContext.current

    val smallLabelSize = context.resources.getDimension(R.dimen.small_label_size)
    val labelColor = colorResource(id = R.color.white).toArgb()

    AndroidViewBinding(factory = { inflater, parent, attachToParent ->
        val binding = GraphPieChartBinding.inflate(inflater, parent, attachToParent)

        binding.pieChart.clear()
        binding.pieChart.backgroundPaint.color = Color.TRANSPARENT

        segments.forEachIndexed { i, s ->

            val segForm = SegmentFormatter(context.getColor(s.color))
            if (segments.size > dataVisColorList.size) {
                segForm.labelPaint.textSize = smallLabelSize
                segForm.labelPaint.color = labelColor
            } else {
                segForm.labelPaint.color = Color.TRANSPARENT
            }

            binding.pieChart.addSegment(s.segment, segForm)
        }

        return@AndroidViewBinding binding
    }, update = {

        if (graphHeight != null) pieChart.layoutParams.height = graphHeight
        pieChart.redraw()
        pieChart.getRenderer(PieRenderer::class.java)
            .setDonutSize(0f, PieRenderer.DonutMode.PERCENT)
        pieChart.requestLayout()
    })

    GraphLegend(
        items = segments.map { GraphLegendItem(color = it.color, label = it.label) }
    )
}
