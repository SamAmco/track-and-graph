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
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.dto.IDataPoint
import com.samco.trackandgraph.database.entity.*
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeSinceViewData

class TimeSinceViewDataFactory : ViewDataFactory<TimeSinceLastStat, ITimeSinceViewData>() {
    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        onDataSampled: (List<IDataPoint>) -> Unit
    ): ITimeSinceViewData {
        val timeSinceStat = dataSource.getTimeSinceLastStatByGraphStatId(graphOrStat.id)
            ?: return object : ITimeSinceViewData {
                override val state: IGraphStatViewData.State
                    get() = IGraphStatViewData.State.ERROR
                override val graphOrStat: GraphOrStat
                    get() = graphOrStat
                override val error: GraphStatInitException?
                    get() = GraphStatInitException(R.string.graph_stat_view_not_found)

            }
        return createViewData(dataSource, graphOrStat, timeSinceStat, onDataSampled)
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        config: TimeSinceLastStat,
        onDataSampled: (List<IDataPoint>) -> Unit
    ): ITimeSinceViewData {
        val dataPoint = getLastDataPoint(dataSource, config)
        onDataSampled.invoke(dataPoint?.let { listOf(dataPoint) } ?: emptyList())
        return object : ITimeSinceViewData {
            override val lastDataPoint: IDataPoint?
                get() = dataPoint
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
        }
    }

    private fun getLastDataPoint(
        dao: TrackAndGraphDatabaseDao,
        timeSinceLastStat: TimeSinceLastStat
    ): IDataPoint? {
        //TODO need to revisit this
        val dataSampler = DataSamplerImpl(dao)
        val dataSource = DataSource.FeatureDataSource(timeSinceLastStat.featureId)
        val dataSample = dataSampler.getDataPointsForDataSource(dataSource)
        val first = dataSample.firstOrNull() ?: return null
        return when (first.dataType) {
            DataType.CONTINUOUS, DataType.DURATION -> {
                dataSampler.getLastDataPointBetween(
                    dataSource,
                    timeSinceLastStat.fromValue,
                    timeSinceLastStat.toValue
                )
            }
            else -> {
                dataSampler.getLastDataPointWithValue(
                    dataSource,
                    timeSinceLastStat.discreteValues
                )
            }
        }
    }
}