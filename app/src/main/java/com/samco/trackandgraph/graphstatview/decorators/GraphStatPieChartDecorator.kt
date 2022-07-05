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

package com.samco.trackandgraph.graphstatview.decorators

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat.getColor
import com.androidplot.pie.PieRenderer
import com.androidplot.pie.Segment
import com.androidplot.pie.SegmentFormatter
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.graphstatview.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.ui.dataVisColorGenerator
import com.samco.trackandgraph.ui.dataVisColorList
import org.threeten.bp.OffsetDateTime

class GraphStatPieChartDecorator(listMode: Boolean) :
    GraphStatViewDecorator<IPieChartViewData>(listMode) {

    private var binding: GraphStatViewBinding? = null
    private var context: Context? = null
    private var data: IPieChartViewData? = null

    override suspend fun decorate(
        view: IDecoratableGraphStatView,
        data: IPieChartViewData
    ) {
        this.data = data
        binding = view.getBinding()
        context = view.getContext()

        initFromPieChartBody()
    }

    override fun setTimeMarker(time: OffsetDateTime) {}

    private fun initFromPieChartBody() {
        binding!!.pieChart.visibility = View.INVISIBLE
        binding!!.progressBar.visibility = View.VISIBLE

        val segments = data!!.segments
            ?: throw GraphStatInitException(
                R.string.graph_stat_view_not_enough_data_graph
            )
        plotPieChartSegments(segments)
        binding!!.pieChart.redraw()
        binding!!.pieChart.getRenderer(PieRenderer::class.java)
            .setDonutSize(0f, PieRenderer.DonutMode.PERCENT)

        binding!!.pieChart.visibility = View.VISIBLE
        binding!!.progressBar.visibility = View.GONE
    }

    private fun plotPieChartSegments(segments: List<Segment>) {
        segments.forEachIndexed { i, s ->
            val index = (dataVisColorGenerator * i) % dataVisColorList.size
            val color = getColor(context!!, dataVisColorList[index])
            val percentage = "%.1f".format(s.value.toDouble())
            var label = "${s.title} ($percentage%)"

            val segForm = SegmentFormatter(color)
            if (segments.size > dataVisColorList.size) {
                label = "$i: ${s.title} ($percentage%)"
                s.title = i.toString()
                segForm.labelPaint.textSize = context!!.resources.getDimension(R.dimen.small_label_size)
                segForm.labelPaint.color = getColor(context!!, R.color.white)
            } else {
                segForm.labelPaint.color = Color.TRANSPARENT
            }
            binding!!.pieChart.addSegment(s, segForm)
            inflateGraphLegendItem(binding!!, context!!, color, label)
        }
    }
}