/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatview.factories

import com.androidplot.pie.Segment
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.GraphOrStat
import com.samco.trackandgraph.database.entity.PieChart
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.statistics.DataSample
import com.samco.trackandgraph.statistics.sampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PieChartDataFactory(
    dataSource: TrackAndGraphDatabaseDao,
    graphOrStat: GraphOrStat
) : ViewDataFactory<PieChart, IPieChartViewData>(
    dataSource,
    graphOrStat
) {
    override suspend fun createViewData(config: PieChart, onDataSampled: (List<DataPoint>) -> Unit): IPieChartViewData {
        val plottingData = tryGetPlottableDataForPieChart(config)
            ?: return object : IPieChartViewData {
                override val state: IGraphStatViewData.State
                    get() = IGraphStatViewData.State.READY
                override val graphOrStat: GraphOrStat
                    get() = this@PieChartDataFactory.graphOrStat
                override val segments: List<Segment>?
                    get() = null
            }
        val segments = getPieChartSegments(plottingData)
        val total = segments.sumByDouble { s -> s.value.toDouble() }
        val percentages = segments.map {
            Segment(
                it.title,
                (it.value.toDouble() / total) * 100f
            )
        }

        onDataSampled(plottingData.dataPoints)

        return object : IPieChartViewData {
            override val segments: List<Segment>
                get() = percentages
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat
                get() = this@PieChartDataFactory.graphOrStat
        }
    }

    override suspend fun createViewData(onDataSampled: (List<DataPoint>) -> Unit): IPieChartViewData {
        val pieChart = dataSource.getPieChartByGraphStatId(graphOrStat.id)
            ?: return object : IPieChartViewData {
                override val state: IGraphStatViewData.State
                    get() = IGraphStatViewData.State.ERROR
                override val graphOrStat: GraphOrStat
                    get() = this@PieChartDataFactory.graphOrStat
                override val error: GraphStatInitException?
                    get() = GraphStatInitException(R.string.graph_stat_view_not_found)
            }
        return createViewData(pieChart, onDataSampled)
    }

    private suspend fun tryGetPlottableDataForPieChart(pieChart: PieChart): DataSample? {
        val feature = withContext(Dispatchers.IO) {
            dataSource.getFeatureById(pieChart.featureId)
        }
        val dataSample = sampleData(
            dataSource, feature.id, pieChart.duration,
            pieChart.endDate, null, null
        )
        return if (dataSample.dataPoints.isNotEmpty()) dataSample else null
    }

    private fun getPieChartSegments(dataSample: DataSample) =
        dataSample.dataPoints
            .groupingBy { dp -> dp.label }
            .eachCount()
            .map { b -> Segment(b.key, b.value) }
}