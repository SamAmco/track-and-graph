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
import com.samco.trackandgraph.data.database.dto.DurationPlottingMode
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.IDataPoint
import com.samco.trackandgraph.data.database.dto.LineGraphAveragingModes
import com.samco.trackandgraph.data.database.dto.LineGraphFeature
import com.samco.trackandgraph.data.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.data.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.di.DefaultDispatcher
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.LuaVMLock
import com.samco.trackandgraph.data.sampling.DataSample
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.exceptions.LuaEngineDisabledGraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.Line
import com.samco.trackandgraph.graphstatview.factories.viewdto.LineGraphPoint
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.CompositeFunction
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.DataClippingFunction
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.DataPaddingFunction
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.DurationAggregationFunction
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.IdentityFunction
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.MovingAverageFunction
import com.samco.trackandgraph.graphstatview.functions.helpers.TimeHelper
import com.samco.trackandgraph.movingAverageDurations
import com.samco.trackandgraph.plottingModePeriods
import com.samco.trackandgraph.data.lua.dto.LuaEngineDisabledException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import java.util.Collections
import javax.inject.Inject

class LineGraphDataFactory @Inject constructor(
    dataInteractor: DataInteractor,
    dataSampler: DataSampler,
    @IODispatcher ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val timeHelper: TimeHelper,
    private val luaEngine: LuaEngine,
) : ViewDataFactory<LineGraphWithFeatures, ILineGraphViewData>(
    dataInteractor,
    dataSampler,
    ioDispatcher
) {

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        config: LineGraphWithFeatures,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILineGraphViewData = withContext(defaultDispatcher) {
        val disposables = Collections.synchronizedList(mutableListOf<DataSample>())
        var vmLock: LuaVMLock? = null
        try {
            vmLock = acquireLuaVMIfAvailable()
            val dataSamples = config.features.map { lgf ->
                val dataSample = dataSampler.getDataSampleForFeatureId(
                    featureId = lgf.featureId,
                    vmLock = vmLock
                )
                disposables.add(dataSample)
                Pair(lgf, tryGetPlottingData(dataSample, config, lgf))
            }

            val plottableData = generatePlottingData(dataSamples, config, onDataSampled)
            val hasPlottableData = plottableData.lines.any { it.points.size >= 2 }

            // Only show a duration based range if the user selected duration for a line graph
            // feature, and that feature actually says it is a duration still (this could have
            // changed since they created the graph).
            val durationBasedRange = dataSamples.any { pair ->
                pair.first.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE
                        && pair.second.dataSampleProperties.isDuration
            }
            return@withContext object : ILineGraphViewData {
                override val durationBasedRange = durationBasedRange
                override val yRangeType = config.yRangeType
                override val fixedYMin = config.yFrom.takeIf { config.yRangeType == YRangeType.FIXED }
                override val fixedYMax = config.yTo.takeIf { config.yRangeType == YRangeType.FIXED }
                override val hasPlottableData = hasPlottableData
                override val endTime = plottableData.endTime
                override val lines = plottableData.lines
                override val state = IGraphStatViewData.State.READY
                override val graphOrStat = graphOrStat
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            throwable.printStackTrace()
            return@withContext object : ILineGraphViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = if (throwable is LuaEngineDisabledException) {
                    LuaEngineDisabledGraphStatInitException()
                } else {
                    throwable
                }
            }
        } finally {
            disposables.forEach { it.dispose() }
            vmLock?.let { luaEngine.releaseVM(it) }
        }
    }

    private suspend fun acquireLuaVMIfAvailable(): LuaVMLock? {
        return try {
            luaEngine.acquireVM()
        } catch (throwable: LuaEngineDisabledException) {
            null
        }
    }

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILineGraphViewData {
        val lineGraph = dataInteractor.getLineGraphByGraphStatId(graphOrStat.id)
            ?: return object : ILineGraphViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = GraphStatInitException(R.string.graph_stat_view_not_found)
            }
        return createViewData(graphOrStat, lineGraph, onDataSampled)
    }

    private data class PlottingData(
        val lines: List<Line>,
        val endTime: OffsetDateTime
    )

    private suspend fun generatePlottingData(
        dataSamples: List<Pair<LineGraphFeature, DataSample>>,
        lineGraph: LineGraphWithFeatures,
        onDataSampled: (List<DataPoint>) -> Unit
    ): PlottingData = withContext(defaultDispatcher) {

        //Get the end time of the graph. If not specified it's the time of the last data
        // point of any of the features
        val endTime = lineGraph.endDate.toOffsetDateTime() ?: dataSamples
            .mapNotNull { it.second.firstOrNull() }
            .maxOfOrNull { it.timestamp }
        ?: OffsetDateTime.now()

        //Generate the actual plotting data for each sample.
        val coroutineContext = currentCoroutineContext()
        val features = dataSamples.map { pair ->
            coroutineContext.ensureActive()
            val feature = pair.first
            val clippedSample = DataClippingFunction(endTime, lineGraph.sampleSize)
                .mapSample(pair.second)

            //Calling toList on the data sample evaluates it and causes the whole pipeline
            // to be processed
            val dataPoints = clippedSample.toListCancellable().asReversed()

            val points = if (dataPoints.size >= 2) {
                getLinePoints(dataPoints, pair.first)
            } else emptyList()

            Line(
                name = feature.name,
                color = ColorSpec.ColorIndex(feature.colorIndex),
                pointStyle = feature.pointStyle,
                points = points
            )
        }

        val rawDataPoints = dataSamples.flatMap {
            coroutineContext.ensureActive()
            it.second.getRawDataPoints()
        }

        onDataSampled(rawDataPoints)

        return@withContext PlottingData(features, endTime)
    }

    private suspend fun tryGetPlottingData(
        dataSample: DataSample,
        config: LineGraphWithFeatures,
        lineGraphFeature: LineGraphFeature
    ): DataSample {
        val movingAvDuration = movingAverageDurations[lineGraphFeature.averagingMode]
        val plottingPeriod = plottingModePeriods[lineGraphFeature.plottingMode]

        val aggregationCalculator = when (lineGraphFeature.plottingMode) {
            LineGraphPlottingModes.WHEN_TRACKED -> IdentityFunction()
            LineGraphPlottingModes.GENERATE_HOURLY_TOTALS,
            LineGraphPlottingModes.GENERATE_DAILY_TOTALS,
            LineGraphPlottingModes.GENERATE_WEEKLY_TOTALS,
            LineGraphPlottingModes.GENERATE_MONTHLY_TOTALS,
            LineGraphPlottingModes.GENERATE_YEARLY_TOTALS -> CompositeFunction(
                DurationAggregationFunction(timeHelper, plottingPeriod!!),
                DataPaddingFunction(
                    timeHelper = timeHelper,
                    endTime = config.endDate.toOffsetDateTime(
                        fallback = getLastDataPointTimestamp(dataSample)
                    ),
                    duration = config.sampleSize
                )
            )
        }
        val averageCalculator = when (lineGraphFeature.averagingMode) {
            LineGraphAveragingModes.NO_AVERAGING -> IdentityFunction()
            else -> MovingAverageFunction(movingAvDuration!!)
        }
        return CompositeFunction(aggregationCalculator, averageCalculator).mapSample(dataSample)
    }

    private fun getLastDataPointTimestamp(rawDataSample: DataSample): OffsetDateTime {
        val dataPeekIterator = rawDataSample.iterator()
        return if (!dataPeekIterator.hasNext()) OffsetDateTime.now()
        else dataPeekIterator.next().timestamp
    }

    private suspend fun getLinePoints(
        dataSample: List<IDataPoint>,
        lineGraphFeature: LineGraphFeature
    ): List<LineGraphPoint> {
        val scale = lineGraphFeature.scale
        val offset = lineGraphFeature.offset
        val durationDivisor = when (lineGraphFeature.durationPlottingMode) {
            DurationPlottingMode.HOURS -> 3600.0
            DurationPlottingMode.MINUTES -> 60.0
            else -> 1.0
        }

        val coroutineContext = currentCoroutineContext()
        return dataSample.mapIndexed { index, dp ->
            if (index % CANCELLATION_CHECK_INTERVAL == 0) coroutineContext.ensureActive()
            LineGraphPoint(
                timestamp = dp.timestamp,
                value = (dp.value * scale / durationDivisor) + offset,
            )
        }.also { coroutineContext.ensureActive() }
    }

    private suspend fun DataSample.toListCancellable(): List<IDataPoint> {
        val coroutineContext = currentCoroutineContext()
        val result = mutableListOf<IDataPoint>()
        val iterator = iterator()
        var index = 0
        while (iterator.hasNext()) {
            if (index % CANCELLATION_CHECK_INTERVAL == 0) coroutineContext.ensureActive()
            result.add(iterator.next())
            index++
        }
        coroutineContext.ensureActive()
        return result
    }

    companion object {
        private const val CANCELLATION_CHECK_INTERVAL = 128
    }
}
