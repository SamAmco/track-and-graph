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
import com.samco.trackandgraph.database.DataSamplerImpl
import com.samco.trackandgraph.database.DataSource
import com.samco.trackandgraph.functionslib.GlobalAggregationPreferences
import com.samco.trackandgraph.functionslib.TimeHelper
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.dto.IDataPoint
import com.samco.trackandgraph.database.entity.*
import com.samco.trackandgraph.functionslib.DataClippingFunction
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeHistogramViewData
import org.threeten.bp.OffsetDateTime
import kotlin.math.min

class TimeHistogramDataFactory : ViewDataFactory<TimeHistogram, ITimeHistogramViewData>() {
    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        onDataSampled: (List<IDataPoint>) -> Unit
    ): ITimeHistogramViewData {
        val timeHistogram = dataSource.getTimeHistogramByGraphStatId(graphOrStat.id)
            ?: return object : ITimeHistogramViewData {
                override val state: IGraphStatViewData.State
                    get() = IGraphStatViewData.State.ERROR
                override val graphOrStat: GraphOrStat
                    get() = graphOrStat
                override val error: GraphStatInitException?
                    get() = GraphStatInitException(R.string.graph_stat_view_not_found)
            }
        return createViewData(dataSource, graphOrStat, timeHistogram, onDataSampled)
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        config: TimeHistogram,
        onDataSampled: (List<IDataPoint>) -> Unit
    ): ITimeHistogramViewData {
        val discreteValue = getDiscreteValues(dataSource, config) ?: listOf(DiscreteValue(0, ""))
        val timeHistogramDataHelper =
            TimeHistogramDataHelper(TimeHelper(GlobalAggregationPreferences))
        val barValues =
            getBarValues(dataSource, config, config.endDate, onDataSampled, timeHistogramDataHelper)
        val largestBin = timeHistogramDataHelper.getLargestBin(barValues?.values?.toList())
        val maxDisplayHeight = largestBin?.let {
            min(
                it.times(10.0).toInt().plus(1).div(10.0),
                1.0
            )
        }

        return object : ITimeHistogramViewData {
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
            override val window: TimeHistogramWindow?
                get() = config.window
            override val discreteValues: List<DiscreteValue>?
                get() = discreteValue
            override val barValues: Map<Int, List<Double>>?
                get() = barValues
            override val maxDisplayHeight: Double?
                get() = maxDisplayHeight
        }
    }

    private fun getDiscreteValues(
        dataSource: TrackAndGraphDatabaseDao,
        config: TimeHistogram
    ): List<DiscreteValue>? {
        val feature = dataSource.tryGetFeatureByIdSync(config.featureId)
        return feature?.let {
            if (it.featureType == DataType.DISCRETE) it.discreteValues
            else null
        }
    }

    private suspend fun getBarValues(
        dao: TrackAndGraphDatabaseDao,
        config: TimeHistogram,
        endDate: OffsetDateTime?,
        onDataSampled: (List<IDataPoint>) -> Unit,
        timeHistogramDataHelper: TimeHistogramDataHelper
    ): Map<Int, List<Double>>? {
        val feature = dao.getFeatureById(config.featureId)
        val dataSampler = DataSamplerImpl(dao)
        val dataSource = DataSource.FeatureDataSource(config.featureId)
        val sample = dataSampler.getDataPointsForDataSource(dataSource)
        val dataPoints = DataClippingFunction(config.endDate, config.duration)
            .mapSample(sample)
            .toList()
        val barValues = timeHistogramDataHelper.getHistogramBinsForSample(
            dataPoints,
            config.window,
            feature,
            config.sumByCount
        )
        onDataSampled(dataPoints)
        return barValues
    }
}
