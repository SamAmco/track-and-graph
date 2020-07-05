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
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeHistogramViewData
import com.samco.trackandgraph.statistics.getHistogramBinsForSample
import com.samco.trackandgraph.statistics.getNextEndOfDuration
import com.samco.trackandgraph.statistics.sampleData

class TimeHistogramDataFactory : ViewDataFactory<TimeHistogram, ITimeHistogramViewData>() {
    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
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
        onDataSampled: (List<DataPoint>) -> Unit
    ): ITimeHistogramViewData {
        val discreteValue = getDiscreteValues(dataSource, config)
        val barValues = getBarValues(dataSource, config, onDataSampled)

        return object : ITimeHistogramViewData {
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
            override val window: TimeHistogramWindow?
                get() = config.window
            override val discreteValues: List<DiscreteValue>?
                get() = discreteValue
            override val barValues: List<Map<Int, Double>>?
                get() = barValues
        }
    }

    private fun getDiscreteValues(
        dataSource: TrackAndGraphDatabaseDao,
        config: TimeHistogram
    ): List<DiscreteValue>? {
        val feature = dataSource.tryGetFeatureByIdSync(config.featureId)
        return feature?.let {
            if (it.featureType == FeatureType.DISCRETE) it.discreteValues
            else null
        }
    }

    private suspend fun getBarValues(
        dataSource: TrackAndGraphDatabaseDao,
        config: TimeHistogram,
        onDataSampled: (List<DataPoint>) -> Unit
    ): List<Map<Int, Double>>? {
        val feature = dataSource.getFeatureById(config.featureId)
        val endTime = getNextEndOfDuration(config.window, config.endDate)
        val sample = sampleData(
            dataSource, config.featureId, config.duration,
            endTime, null, null
        )
        val barValues = getHistogramBinsForSample(
            sample,
            config.window,
            endTime,
            feature.featureType,
            config.sumByCount
        )
        onDataSampled(sample.dataPoints)
        return barValues
    }
}
