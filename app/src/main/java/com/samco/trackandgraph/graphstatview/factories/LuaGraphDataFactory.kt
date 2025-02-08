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

import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.base.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.graphstatview.exceptions.GraphNotFoundException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILastValueViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import kotlinx.coroutines.CoroutineDispatcher
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

class LuaGraphDataFactory @Inject constructor(
    dataInteractor: DataInteractor,
    @IODispatcher ioDispatcher: CoroutineDispatcher
) : ViewDataFactory<LuaGraphWithFeatures, ILuaGraphViewData>(dataInteractor, ioDispatcher) {
    override suspend fun createViewData(graphOrStat: GraphOrStat, onDataSampled: (List<DataPoint>) -> Unit): ILuaGraphViewData {
        val luaGraph = dataInteractor.getLuaGraphByGraphStatId(graphOrStat.id)
            ?: return graphNotFound(graphOrStat)
        return createViewData(graphOrStat, luaGraph, onDataSampled)
    }

    override suspend fun affectedBy(graphOrStatId: Long, featureId: Long): Boolean {
        return dataInteractor.getLuaGraphByGraphStatId(graphOrStatId)?.features
            ?.any { it.featureId == featureId } ?: false
    }

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        config: LuaGraphWithFeatures,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILuaGraphViewData {
        return object : ILuaGraphViewData {
            override val wrapped: IGraphStatViewData = dataPoint(graphOrStat)
            override val state: IGraphStatViewData.State = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat = graphOrStat
        }
    }

    private fun dataPoint(graphOrStat: GraphOrStat): ILastValueViewData {
        return object : ILastValueViewData {
            override val isDuration: Boolean = false
            override val state = IGraphStatViewData.State.READY
            override val graphOrStat = graphOrStat.copy(
                type = GraphStatType.LAST_VALUE
            )
            override val lastDataPoint: DataPoint = DataPoint(
                timestamp = OffsetDateTime.now(),
                featureId = 0,
                value = 0.0,
                label = "label",
                note = "note"
            )
        }
    }

    private fun graphNotFound(graphOrStat: GraphOrStat) =
        object : ILuaGraphViewData {
            override val wrapped: IGraphStatViewData? = null
            override val state = IGraphStatViewData.State.ERROR
            override val graphOrStat = graphOrStat
            override val error = GraphNotFoundException()
        }
}