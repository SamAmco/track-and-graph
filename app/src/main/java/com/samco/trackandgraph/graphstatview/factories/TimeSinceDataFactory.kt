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

import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.TimeSinceLastStat
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.di.IODispatcher
import com.samco.trackandgraph.functions.functions.CompositeFunction
import com.samco.trackandgraph.functions.functions.DataSampleFunction
import com.samco.trackandgraph.functions.functions.FilterLabelFunction
import com.samco.trackandgraph.functions.functions.FilterValueFunction
import com.samco.trackandgraph.graphstatview.exceptions.GraphNotFoundException
import com.samco.trackandgraph.graphstatview.exceptions.NotEnoughDataException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeSinceViewData
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class TimeSinceDataFactory @Inject constructor(
    dataInteractor: DataInteractor,
    @IODispatcher ioDispatcher: CoroutineDispatcher
) : ViewDataFactory<TimeSinceLastStat, ITimeSinceViewData>(dataInteractor, ioDispatcher) {
    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ITimeSinceViewData {
        val timeSinceStat = dataInteractor.getTimeSinceLastStatByGraphStatId(graphOrStat.id)
            ?: return graphNotFound(graphOrStat)
        return createViewData(graphOrStat, timeSinceStat, onDataSampled)
    }

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        config: TimeSinceLastStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ITimeSinceViewData {
        return try {
            val dataPoint = getLastDataPoint(config, onDataSampled)
                ?: return notEnoughData(graphOrStat, 0)
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

    private fun graphNotFound(graphOrStat: GraphOrStat) =
        object : ITimeSinceViewData {
            override val state = IGraphStatViewData.State.ERROR
            override val graphOrStat = graphOrStat
            override val error = GraphNotFoundException()
        }

    private fun notEnoughData(graphOrStat: GraphOrStat, numDataPoints: Int) =
        object : ITimeSinceViewData {
            override val error = NotEnoughDataException(numDataPoints)
            override val state = IGraphStatViewData.State.ERROR
            override val graphOrStat = graphOrStat
        }

    private suspend fun getLastDataPoint(
        config: TimeSinceLastStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IDataPoint? {
        val dataSample = dataInteractor.getDataSampleForFeatureId(config.featureId)

        val filters = mutableListOf<DataSampleFunction>()
        if (config.filterByLabels) filters.add(FilterLabelFunction(config.labels.toSet()))
        if (config.filterByRange) filters.add(FilterValueFunction(config.fromValue, config.toValue))

        val sample = CompositeFunction(filters).mapSample(dataSample)
        val first = sample.firstOrNull()

        onDataSampled(sample.getRawDataPoints())
        return first
    }
}