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

import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.graphstatview.exceptions.GraphNotFoundException
import com.samco.trackandgraph.graphstatview.exceptions.NotEnoughDataException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILastValueData
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class LastValueDataFactory @Inject constructor(
    dataInteractor: DataInteractor,
    @IODispatcher ioDispatcher: CoroutineDispatcher,
    private val commonHelpers: DataFactoryCommonHelpers
) : ViewDataFactory<LastValueStat, ILastValueData>(dataInteractor, ioDispatcher) {

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILastValueData {
        val lastValueStat = dataInteractor.getLastValueStatByGraphStatId(graphOrStat.id)
            ?: return graphNotFound(graphOrStat)
        return createViewData(graphOrStat, lastValueStat, onDataSampled)
    }

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        config: LastValueStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILastValueData {
        return try {
            val dataPoint = commonHelpers.getLastDataPoint(
                dataInteractor = dataInteractor,
                featureId = config.featureId,
                filterByLabels = config.filterByLabels,
                labels = config.labels,
                filterByRange = config.filterByRange,
                fromValue = config.fromValue,
                toValue = config.toValue,
                onDataSampled = onDataSampled
            ) ?: return notEnoughData(graphOrStat)
            object : ILastValueData {
                override val state = IGraphStatViewData.State.READY
                override val graphOrStat = graphOrStat
                override val lastDataPoint = dataPoint
            }
        } catch (throwable: Throwable) {
            object : ILastValueData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = throwable
            }
        }
    }

    private fun graphNotFound(graphOrStat: GraphOrStat) =
        object : ILastValueData {
            override val state = IGraphStatViewData.State.ERROR
            override val graphOrStat = graphOrStat
            override val error = GraphNotFoundException()
        }

    private fun notEnoughData(graphOrStat: GraphOrStat) =
        object : ILastValueData {
            override val error = NotEnoughDataException(0)
            override val state = IGraphStatViewData.State.ERROR
            override val graphOrStat = graphOrStat
        }
}