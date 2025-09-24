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
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.sampling.RawDataSample
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.exceptions.GraphNotFoundException
import com.samco.trackandgraph.graphstatview.factories.helpers.DataPointLuaHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.ErrorLuaHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.LineGraphLuaHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.PieChartLuaHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.TextLuaHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.TimeBarchartLuaHelper
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.LuaEngineSettingsProvider
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class LuaGraphDataFactory @Inject constructor(
    private val luaEngine: LuaEngine,
    private val pieChartLuaHelper: PieChartLuaHelper,
    private val textLuaHelper: TextLuaHelper,
    private val dataPointLuaHelper: DataPointLuaHelper,
    private val errorLuaHelper: ErrorLuaHelper,
    private val lineGraphLuaHelper: LineGraphLuaHelper,
    private val timeBarChartLuaHelper: TimeBarchartLuaHelper,
    private val luaEngineSettingsProvider: LuaEngineSettingsProvider,
    dataInteractor: DataInteractor,
    dataSampler: DataSampler,
    @IODispatcher ioDispatcher: CoroutineDispatcher
) : ViewDataFactory<LuaGraphWithFeatures, ILuaGraphViewData>(
    dataInteractor,
    dataSampler,
    ioDispatcher
) {
    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILuaGraphViewData {
        val luaGraph = dataInteractor.getLuaGraphByGraphStatId(graphOrStat.id)
            ?: return graphNotFound(graphOrStat)
        return createViewData(graphOrStat, luaGraph, onDataSampled)
    }

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        config: LuaGraphWithFeatures,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILuaGraphViewData = coroutineScope {
        if (!luaEngineSettingsProvider.settings.enabled) {
            return@coroutineScope luaEngineDisabled(graphOrStat)
        }

        var dataSamples: Map<String, RawDataSample> = emptyMap()

        return@coroutineScope try {
            dataSamples = config.features
                .mapNotNull { lgf ->
                    dataSampler
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

        val luaGraphResult = luaEngine.runLuaGraphScript(config.script, luaEngineParams)
        val error = luaGraphResult.error

        if (error != null) return errorLuaHelper(graphOrStat, error)

        onDataSampled(dataSamples.flatMap { it.value.getRawDataPoints() })

        return when (val data = luaGraphResult.data) {
            is LuaGraphResultData.DataPointData -> dataPointLuaHelper(data, graphOrStat)
            is LuaGraphResultData.TextData -> textLuaHelper(data, graphOrStat)
            is LuaGraphResultData.PieChartData -> pieChartLuaHelper(data, graphOrStat)
            is LuaGraphResultData.TimeBarChartData -> timeBarChartLuaHelper(data, graphOrStat)
            is LuaGraphResultData.LineGraphData ->
                lineGraphLuaHelper(data, graphOrStat) ?: noData(graphOrStat)

            null -> noData(graphOrStat)
        }
    }

    private fun luaEngineDisabled(graphOrStat: GraphOrStat) =
        object : ILuaGraphViewData {
            override val wrapped: IGraphStatViewData? = null
            override val hasData: Boolean = false
            override val state = IGraphStatViewData.State.ERROR
            override val graphOrStat = graphOrStat
            override val error = GraphStatInitException(R.string.lua_engine_disabled)
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
