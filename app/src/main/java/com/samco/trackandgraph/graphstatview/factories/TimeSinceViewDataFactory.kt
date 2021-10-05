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
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.entity.*
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeSinceViewData

class TimeSinceViewDataFactory : ViewDataFactory<TimeSinceLastStat, ITimeSinceViewData>() {
    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPointInterface>) -> Unit
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
        onDataSampled: (List<DataPointInterface>) -> Unit
    ): ITimeSinceViewData {
        val dataPoint = getLastDataPoint(dataSource, config)
        onDataSampled.invoke(dataPoint?.let { listOf(dataPoint) } ?: emptyList())
        return object : ITimeSinceViewData {
            override val lastDataPoint: DataPointInterface?
                get() = dataPoint
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
        }
    }

    private fun getLastDataPoint(
        dataSource: TrackAndGraphDatabaseDao,
        timeSinceLastStat: TimeSinceLastStat
    ): DataPointInterface? {
        val feature = dataSource.getFeatureById(timeSinceLastStat.featureId)
        return when (feature.featureType) {
            FeatureType.CONTINUOUS, FeatureType.DURATION -> {
                dataSource.getLastDataPointBetween(
                    timeSinceLastStat.featureId,
                    timeSinceLastStat.fromValue,
                    timeSinceLastStat.toValue
                )
            }
            else -> {
                dataSource.getLastDataPointWithValue(
                    timeSinceLastStat.featureId,
                    timeSinceLastStat.discreteValues
                )
            }
        }
    }
}