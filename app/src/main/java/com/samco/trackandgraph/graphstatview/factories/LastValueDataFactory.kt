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

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.LastValueStat
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.di.IODispatcher
import com.samco.trackandgraph.functions.functions.CompositeFunction
import com.samco.trackandgraph.functions.functions.DataSampleFunction
import com.samco.trackandgraph.functions.functions.FilterLabelFunction
import com.samco.trackandgraph.functions.functions.FilterValueFunction
import com.samco.trackandgraph.graphstatview.exceptions.GraphNotFoundException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILastValueViewData
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class LastValueDataFactory @Inject constructor(
    dataInteractor: DataInteractor,
    @IODispatcher ioDispatcher: CoroutineDispatcher
) : ViewDataFactory<LastValueStat, ILastValueViewData>(dataInteractor, ioDispatcher) {

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILastValueViewData {
        val lastValueStat = dataInteractor.getLastValueStatByGraphStatId(graphOrStat.id)
            ?: return graphNotFound(graphOrStat)
        return createViewData(graphOrStat, lastValueStat, onDataSampled)
    }

    override suspend fun affectedBy(graphOrStatId: Long, featureId: Long): Boolean {
        return dataInteractor.getLastValueStatByGraphStatId(graphOrStatId)?.featureId == featureId
    }

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        config: LastValueStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILastValueViewData {
        return try {
            val lastData = getLastDataPoint(
                dataInteractor,
                config,
                onDataSampled
            ) ?: return notEnoughData(graphOrStat)
            object : ILastValueViewData {
                override val state = IGraphStatViewData.State.READY
                override val graphOrStat = graphOrStat
                override val lastDataPoint = lastData.dataPoint
                override val isDuration = lastData.isDuration
            }
        } catch (throwable: Throwable) {
            object : ILastValueViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = throwable
                override val isDuration = false
            }
        }
    }

    private fun graphNotFound(graphOrStat: GraphOrStat) =
        object : ILastValueViewData {
            override val state = IGraphStatViewData.State.ERROR
            override val graphOrStat = graphOrStat
            override val error = GraphNotFoundException()
            override val isDuration = false
        }

    private fun notEnoughData(graphOrStat: GraphOrStat) =
        object : ILastValueViewData {
            override val state = IGraphStatViewData.State.READY
            override val graphOrStat = graphOrStat
            override val isDuration = false
        }

    private data class LastDataPointData(
        val dataPoint: DataPoint,
        val isDuration: Boolean
    )

    private suspend fun getLastDataPoint(
        dataInteractor: DataInteractor,
        config: LastValueStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): LastDataPointData? {
        val dataSample = dataInteractor.getDataSampleForFeatureId(config.featureId)

        val filters = mutableListOf<DataSampleFunction>()
        if (config.filterByLabels) filters.add(FilterLabelFunction(config.labels.toSet()))
        if (config.filterByRange) filters.add(FilterValueFunction(config.fromValue, config.toValue))

        val sampleFunc = CompositeFunction(filters)
        val sample = sampleFunc.mapSample(dataSample)
        val firstIDataPoint = sample.firstOrNull()
        val rawSample = sample.getRawDataPoints()
        val firstRawDataPoint = rawSample.firstOrNull()
        val note = if (firstIDataPoint?.timestamp == firstRawDataPoint?.timestamp) {
            firstRawDataPoint?.note ?: ""
        } else ""
        val isDuration = sample.dataSampleProperties.isDuration

        onDataSampled(rawSample)
        dataSample.dispose()
        return firstIDataPoint?.let {
            LastDataPointData(
                dataPoint = DataPoint(
                    timestamp = it.timestamp,
                    featureId = config.featureId,
                    value = it.value,
                    label = it.label,
                    note = note
                ),
                isDuration = isDuration
            )
        }
    }
}