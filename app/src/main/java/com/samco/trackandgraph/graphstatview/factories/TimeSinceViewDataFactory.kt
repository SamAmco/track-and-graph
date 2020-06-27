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
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.FeatureType
import com.samco.trackandgraph.database.entity.GraphOrStat
import com.samco.trackandgraph.database.entity.TimeSinceLastStat
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeSinceViewData
import org.threeten.bp.OffsetDateTime

class TimeSinceViewDataFactory(
    dataSource: TrackAndGraphDatabaseDao,
    graphOrStat: GraphOrStat
) : ViewDataFactory<ITimeSinceViewData>(
    dataSource, graphOrStat
) {
    fun createViewData(
        timeSinceStat: TimeSinceLastStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ITimeSinceViewData {
        val dataPoint = getLastDataPoint(dataSource, graphOrStat, timeSinceStat)
        onDataSampled.invoke(dataPoint?.let { listOf(dataPoint) } ?: emptyList())
        return object : ITimeSinceViewData {
            override val lastDataPoint: DataPoint?
                get() = dataPoint
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat
                get() = this@TimeSinceViewDataFactory.graphOrStat
            override val endDate: OffsetDateTime?
                get() = graphOrStat.endDate
        }
    }

    override suspend fun createViewData(onDataSampled: (List<DataPoint>) -> Unit): ITimeSinceViewData {
        val timeSinceStat = dataSource.getTimeSinceLastStatByGraphStatId(graphOrStat.id)
            ?: return object : ITimeSinceViewData {
                override val state: IGraphStatViewData.State
                    get() = IGraphStatViewData.State.ERROR
                override val graphOrStat: GraphOrStat
                    get() = this@TimeSinceViewDataFactory.graphOrStat
                override val error: GraphStatInitException?
                    get() = GraphStatInitException(R.string.graph_stat_view_not_found)

            }
        return createViewData(timeSinceStat, onDataSampled)
    }

    private fun getLastDataPoint(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        timeSinceLastStat: TimeSinceLastStat
    ): DataPoint? {
        val feature = dataSource.getFeatureById(timeSinceLastStat.featureId)
        val endDate = graphOrStat.endDate ?: OffsetDateTime.now()
        return when (feature.featureType) {
            FeatureType.CONTINUOUS, FeatureType.DURATION -> {
                dataSource.getLastDataPointBetween(
                    timeSinceLastStat.featureId,
                    timeSinceLastStat.fromValue,
                    timeSinceLastStat.toValue,
                    endDate
                )
            }
            else -> {
                dataSource.getLastDataPointWithValue(
                    timeSinceLastStat.featureId,
                    timeSinceLastStat.discreteValues,
                    endDate
                )
            }
        }
    }
}