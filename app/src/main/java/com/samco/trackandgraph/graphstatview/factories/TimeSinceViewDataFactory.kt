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
import com.samco.trackandgraph.functionslib.FilterLabelFunction
import com.samco.trackandgraph.functionslib.FilterValueFunction
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeSinceViewData

class TimeSinceViewDataFactory : ViewDataFactory<TimeSinceLastStat, ITimeSinceViewData>() {
    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ITimeSinceViewData {
        val timeSinceStat = dataSource.getTimeSinceLastStatByGraphStatId(graphOrStat.id)
            ?: return object : ITimeSinceViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = GraphStatInitException(R.string.graph_stat_view_not_found)

            }
        return createViewData(dataSource, graphOrStat, timeSinceStat, onDataSampled)
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        config: TimeSinceLastStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ITimeSinceViewData {
        return try {
            val dataPoint = getLastDataPoint(dataSource, config, onDataSampled)
            object : ITimeSinceViewData {
                override val lastDataPoint = dataPoint
                override val state = IGraphStatViewData.State.READY
                override val graphOrStat = graphOrStat
            }
        } catch (throwable: Throwable) {
            object : ITimeSinceViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = throwable
            }
        }
    }

    private suspend fun getLastDataPoint(
        dao: TrackAndGraphDatabaseDao,
        config: TimeSinceLastStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IDataPoint? {
        val dataSampler = DataSamplerImpl(dao)
        val dataSource = DataSource.FeatureDataSource(config.featureId)
        val dataSample = dataSampler.getDataPointsForDataSource(dataSource)
        val filterFunction =
            if (config.labels.isNullOrEmpty()) FilterValueFunction(config.fromValue, config.toValue)
            else FilterLabelFunction(config.labels.toSet())
        val sample = filterFunction.mapSample(dataSample)
        val first = sample.firstOrNull()
        onDataSampled(sample.getRawDataPoints())
        return first
    }
}