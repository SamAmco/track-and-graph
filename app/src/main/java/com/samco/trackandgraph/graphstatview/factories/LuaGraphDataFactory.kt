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
import com.samco.trackandgraph.base.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.base.database.sampling.RawDataSample
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.graphstatview.exceptions.GraphNotFoundException
import com.samco.trackandgraph.graphstatview.factories.helpers.DataPointLuaHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.ErrorLuaHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.LineGraphLuaHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.PieChartLuaHelper
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.helpers.TextLuaHelper
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.lua.LuaEngine
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Provider

class LuaGraphDataFactory @Inject constructor(
    private val luaEngine: Provider<LuaEngine>,
    private val pieChartLuaHelper: PieChartLuaHelper,
    private val textLuaHelper: TextLuaHelper,
    private val dataPointLuaHelper: DataPointLuaHelper,
    private val errorLuaHelper: ErrorLuaHelper,
    private val lineGraphLuaHelper: LineGraphLuaHelper,
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
    ): ILuaGraphViewData = coroutineScope {
        var dataSamples: Map<String, RawDataSample> = emptyMap()

        return@coroutineScope try {
            dataSamples = config.features
                .mapNotNull { lgf ->
                    dataInteractor
                        .getRawDataSampleForFeatureId(lgf.featureId)
                        ?.let { lgf.name to it }
                }
                .toMap()

            createViewData(
                dataSamples = dataSamples,
                graphOrStat = graphOrStat,
                config = config,
                onDataSampled = onDataSampled
            )
        } catch (t: Throwable) {
            errorLuaHelper(graphOrStat, t)
        } finally {
            dataSamples.values.forEach { it.dispose() }
        }
    }

    private fun createViewData(
        dataSamples: Map<String, RawDataSample>,
        graphOrStat: GraphOrStat,
        config: LuaGraphWithFeatures,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILuaGraphViewData {
        val luaEngineParams = LuaEngine.LuaGraphEngineParams(dataSources = dataSamples)

        val luaGraphResult = luaEngine.get().runLuaGraphScript(config.script, luaEngineParams)

        if (luaGraphResult.error != null) return errorLuaHelper(graphOrStat, luaGraphResult.error)

        onDataSampled(dataSamples.flatMap { it.value.getRawDataPoints() })

        return when (luaGraphResult.data) {
            is LuaGraphResultData.DataPointData -> dataPointLuaHelper(luaGraphResult.data, graphOrStat)
            is LuaGraphResultData.TextData -> textLuaHelper(luaGraphResult.data, graphOrStat)
            is LuaGraphResultData.PieChartData -> pieChartLuaHelper(luaGraphResult.data, graphOrStat)
            is LuaGraphResultData.LineGraphData ->
                lineGraphLuaHelper(luaGraphResult.data, graphOrStat) ?: noData(graphOrStat)
            null -> noData(graphOrStat)
        }
    }

    private fun noData(graphOrStat: GraphOrStat) =
        object : ILuaGraphViewData {
            override val wrapped: IGraphStatViewData? = null
            override val hasData: Boolean = false
            override val state = IGraphStatViewData.State.READY
            override val graphOrStat = graphOrStat
            override val error = null
        }

    private fun graphNotFound(graphOrStat: GraphOrStat) =
        object : ILuaGraphViewData {
            override val wrapped: IGraphStatViewData? = null
            override val state = IGraphStatViewData.State.ERROR
            override val graphOrStat = graphOrStat
            override val error = GraphNotFoundException()
        }
}
