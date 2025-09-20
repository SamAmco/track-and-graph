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

import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.IDataPoint
import com.samco.trackandgraph.data.database.dto.PieChart
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.DataClippingFunction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PieChartDataFactory @Inject constructor(
    dataInteractor: DataInteractor,
    @IODispatcher ioDispatcher: CoroutineDispatcher
) : ViewDataFactory<PieChart, IPieChartViewData>(dataInteractor, ioDispatcher) {

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IPieChartViewData {
        val pieChart = dataInteractor.getPieChartByGraphStatId(graphOrStat.id)
            ?: return object : IPieChartViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = GraphStatInitException(R.string.graph_stat_view_not_found)
            }
        return createViewData(graphOrStat, pieChart, onDataSampled)
    }

    override suspend fun affectedBy(graphOrStatId: Long, featureId: Long): Boolean {
        return dataInteractor.getPieChartByGraphStatId(graphOrStatId)?.featureId == featureId
    }

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        config: PieChart,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IPieChartViewData {
        return try {
            val plottingData = tryGetPlottableDataForPieChart(config, onDataSampled)
                ?: return object : IPieChartViewData {
                    override val state = IGraphStatViewData.State.READY
                    override val graphOrStat = graphOrStat
                }
            val segments = getPieChartSegments(plottingData, config.sumByCount)
            val total = segments.sumOf { s -> s.second }
            val percentages = segments.map {
                IPieChartViewData.Segment(
                    title = it.first,
                    value = (it.second / total) * 100.0,
                    //Just use the default color behaviour for now
                    color = null
                )
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
        pieChart: PieChart,
        onDataSampled: (List<DataPoint>) -> Unit
    ): List<IDataPoint>? {
        val feature = withContext(Dispatchers.IO) {
            dataInteractor.getFeatureById(pieChart.featureId)
        } ?: return null
        val dataSample = DataClippingFunction(pieChart.endDate.toOffsetDateTime(), pieChart.sampleSize)
            .mapSample(dataInteractor.getDataSampleForFeatureId(feature.featureId))
        val dataPoints = dataSample.toList()
        onDataSampled(dataSample.getRawDataPoints())
        dataSample.dispose()
        return dataPoints.ifEmpty { null }
    }

    private fun getPieChartSegments(
        dataSample: List<IDataPoint>,
        sumByCount: Boolean
    ): List<Pair<String, Double>> = dataSample
        .groupingBy { dp -> dp.label }
        .aggregate<IDataPoint, String, Double> { _, accumulator, element, first ->
            when {
                first && sumByCount -> 1.0
                first && !sumByCount -> element.value
                !first && sumByCount -> accumulator!! + 1.0
                else -> accumulator!! + element.value
            }
        }
        .map { b -> Pair(b.key, b.value) }
        .sortedBy { s -> s.first }
}