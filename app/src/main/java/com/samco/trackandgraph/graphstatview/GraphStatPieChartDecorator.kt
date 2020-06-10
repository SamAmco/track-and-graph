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

package com.samco.trackandgraph.graphstatview

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import com.androidplot.pie.PieRenderer
import com.androidplot.pie.Segment
import com.androidplot.pie.SegmentFormatter
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.GraphOrStat
import com.samco.trackandgraph.database.PieChart
import com.samco.trackandgraph.database.dataVisColorGenerator
import com.samco.trackandgraph.database.dataVisColorList
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GraphStatPieChartDecorator(
    private val graphOrStat: GraphOrStat,
    private val pieChart: PieChart,
    private val onSampledDataCallback: SampleDataCallback?
) : IGraphStatViewDecorator {

    private var binding: GraphStatViewBinding? = null
    private var context: Context? = null
    private var graphStatView: IDecoratableGraphStatView? = null

    override suspend fun decorate(view: IDecoratableGraphStatView) {
        graphStatView = view
        binding = view.getBinding()
        context = view.getContext()

        binding!!.pieChart.visibility = View.INVISIBLE
        initHeader(binding!!, graphOrStat)
        initFromPieChartBody()
    }

    private suspend fun initFromPieChartBody() {
        binding!!.progressBar.visibility = View.VISIBLE
        val dataSample = tryGetPlottableDataForPieChart(pieChart)
            ?: throw GraphStatInitException(R.string.graph_stat_view_not_enough_data_graph)
        onSampledDataCallback?.invoke(dataSample.dataPoints)
        val segments = getPieChartSegments(dataSample)
        val total = withContext(Dispatchers.IO) {
            segments.sumByDouble { s -> s.value.toDouble() }
        }
        plotPieChartSegments(segments, total)
        binding!!.pieChart.redraw()
        binding!!.pieChart.getRenderer(PieRenderer::class.java)
            .setDonutSize(0f, PieRenderer.DonutMode.PERCENT)
        binding!!.pieChart.visibility = View.VISIBLE
        binding!!.progressBar.visibility = View.GONE
    }

    private suspend fun tryGetPlottableDataForPieChart(pieChart: PieChart): RawDataSample? {
        val dataSource = graphStatView!!.getDataSource()
        val feature = withContext(Dispatchers.IO) {
            dataSource.getFeatureById(pieChart.featureId)
        }
        val dataSample = sampleData(
            dataSource, feature.id, pieChart.duration,
            graphOrStat.endDate, null, null
        )
        return if (dataPlottable(dataSample)) dataSample else null
    }

    private fun plotPieChartSegments(segments: List<Segment>, total: Double) {
        segments.forEachIndexed { i, s ->
            val index = (dataVisColorGenerator * i) % dataVisColorList.size
            val colorId = dataVisColorList[index]
            val segForm = SegmentFormatter(ContextCompat.getColor(context!!, colorId))
            segForm.labelPaint.color = Color.TRANSPARENT
            val percentage = "%.1f".format((s.value.toDouble() / total) * 100f)
            inflateGraphLegendItem(binding!!, context!!, index, "${s.title} ($percentage%)")
            binding!!.pieChart.addSegment(s, segForm)
        }
    }

    private suspend fun getPieChartSegments(dataSample: RawDataSample) =
        withContext(Dispatchers.IO) {
            dataSample.dataPoints
                .drop(dataSample.plotFrom)
                .groupingBy { dp -> dp.label }
                .eachCount()
                .map { b -> Segment(b.key, b.value) }
        }
}