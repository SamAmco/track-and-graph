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
import com.samco.trackandgraph.base.database.DataSource
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.entity.DataPoint
import com.samco.trackandgraph.base.database.entity.GraphOrStat
import com.samco.trackandgraph.base.database.entity.PieChart
import com.samco.trackandgraph.functions.sampling.DataSamplerImpl
import com.samco.trackandgraph.functions.functions.DataClippingFunction
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PieChartDataFactory : ViewDataFactory<PieChart, IPieChartViewData>() {
    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IPieChartViewData {
        val pieChart = dataSource.getPieChartByGraphStatId(graphOrStat.id)
            ?: return object : IPieChartViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = GraphStatInitException(R.string.graph_stat_view_not_found)
            }
        return createViewData(dataSource, graphOrStat, pieChart, onDataSampled)
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        config: PieChart,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IPieChartViewData {
        return try {
            val plottingData = tryGetPlottableDataForPieChart(dataSource, config, onDataSampled)
                ?: return object : IPieChartViewData {
                    override val state = IGraphStatViewData.State.READY
                    override val graphOrStat = graphOrStat
                }
            val segments = getPieChartSegments(plottingData)
            val total = segments.sumOf { s -> s.value.toDouble() }
            val percentages = segments.map {
                Segment(it.title, (it.value.toDouble() / total) * 100f)
            }

            object : IPieChartViewData {
                override val segments = percentages
                override val state = IGraphStatViewData.State.READY
                override val graphOrStat = graphOrStat
            }
        } catch (throwable: Throwable) {
            object : IPieChartViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = throwable
            }
        }
    }

    private suspend fun tryGetPlottableDataForPieChart(
        dao: TrackAndGraphDatabaseDao,
        pieChart: PieChart,
        onDataSampled: (List<DataPoint>) -> Unit
    ): List<IDataPoint>? {
        val feature = withContext(Dispatchers.IO) {
            dao.getFeatureById(pieChart.featureId)
        }
        val dataSampler = DataSamplerImpl(dao)
        val dataSource = DataSource.FeatureDataSource(feature.id)
        val dataSample = DataClippingFunction(pieChart.endDate, pieChart.duration)
            .mapSample(dataSampler.getDataSampleForSource(dataSource))
        val dataPoints = dataSample
            .filter { it.label.isNotEmpty() }
            .toList()
        onDataSampled(dataSample.getRawDataPoints())
        return dataPoints.ifEmpty { null }
    }

    private fun getPieChartSegments(dataSample: List<IDataPoint>) =
        dataSample
            .groupingBy { dp -> dp.label }
            .eachCount()
            .map { b -> Segment(b.key, b.value) }

}