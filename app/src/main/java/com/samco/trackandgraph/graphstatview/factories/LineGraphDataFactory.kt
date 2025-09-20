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

import com.androidplot.xy.FastXYSeries
import com.androidplot.xy.RectRegion
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.DurationPlottingMode
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.IDataPoint
import com.samco.trackandgraph.data.database.dto.LineGraphAveraginModes
import com.samco.trackandgraph.data.database.dto.LineGraphFeature
import com.samco.trackandgraph.data.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.data.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.di.DefaultDispatcher
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.sampling.DataSample
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.helpers.AndroidPlotSeriesHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.DataDisplayIntervalHelper
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.Line
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.CompositeFunction
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.DataClippingFunction
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.DataPaddingFunction
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.DurationAggregationFunction
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.IdentityFunction
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.MovingAverageFunction
import com.samco.trackandgraph.graphstatview.functions.helpers.TimeHelper
import com.samco.trackandgraph.movingAverageDurations
import com.samco.trackandgraph.plottingModePeriods
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

class LineGraphDataFactory @Inject constructor(
    dataInteractor: DataInteractor,
    dataSampler: DataSampler,
    private val androidPlotSeriesHelper: AndroidPlotSeriesHelper,
    private val dataDisplayIntervalHelper: DataDisplayIntervalHelper,
    @IODispatcher ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val timeHelper: TimeHelper,
) : ViewDataFactory<LineGraphWithFeatures, ILineGraphViewData>(dataInteractor, dataSampler, ioDispatcher) {

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        config: LineGraphWithFeatures,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILineGraphViewData {
        try {
            val plottableData = generatePlottingData(config, onDataSampled)
            val hasPlottableData = plottableData.lines.any { it.line != null }

            val durationBasedRange = config.features
                .any { f -> f.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE }
            val (bounds, yAxisParameters) = getYAxisParameters(
                config,
                plottableData.lines.map { it.line },
                durationBasedRange
            )

            return object : ILineGraphViewData {
                override val durationBasedRange = durationBasedRange
                override val yRangeType = config.yRangeType
                override val bounds = bounds
                override val hasPlottableData = hasPlottableData
                override val endTime = plottableData.endTime
                override val lines = plottableData.lines
                override val state = IGraphStatViewData.State.READY
                override val graphOrStat = graphOrStat
                override val yAxisSubdivides = yAxisParameters
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            return object : ILineGraphViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = throwable
            }
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

    override suspend fun affectedBy(graphOrStatId: Long, featureId: Long): Boolean {
        return dataInteractor.getLineGraphByGraphStatId(graphOrStatId)?.features
            ?.any { it.featureId == featureId } ?: false
    }

    private data class PlottingData(
        val lines: List<Line>,
        val endTime: OffsetDateTime
    )

    private suspend fun generatePlottingData(
        lineGraph: LineGraphWithFeatures,
        onDataSampled: (List<DataPoint>) -> Unit
    ): PlottingData = coroutineScope {
        withContext(defaultDispatcher) {
            //Create all the data samples in parallel (this shouldn't actually take long but why not)
            val dataSamples = lineGraph.features.map { lgf ->
                async { Pair(lgf, tryGetPlottingData(lineGraph, lgf)) }
            }.awaitAll()

            //Get the end time of the graph. If not specified it's the time of the last data
            // point of any of the features
            val endTime = lineGraph.endDate.toOffsetDateTime() ?: dataSamples
                .mapNotNull { it.second.firstOrNull() }
                .maxOfOrNull { it.timestamp }
            ?: OffsetDateTime.now()

            //Generate the actual plotting data for each sample. This is the part that will take longer
            // hence the parallelization
            val features = dataSamples.map { pair ->
                async {
                    val feature = pair.first
                    val clippedSample = DataClippingFunction(endTime, lineGraph.sampleSize)
                        .mapSample(pair.second)

                    //Calling toList on the data sample evaluates it and causes the whole pipeline
                    // to be processed
                    val dataPoints = clippedSample.toList().asReversed()

                    val series = if (dataPoints.size >= 2) {
                        getXYSeriesFromDataPoints(dataPoints, endTime, pair.first)
                    } else null

                    Line(
                        name = feature.name,
                        color = ColorSpec.ColorIndex(feature.colorIndex),
                        pointStyle = feature.pointStyle,
                        line = series
                    )
                }
            }.awaitAll()

            val rawDataPoints = dataSamples
                .map { it.second.getRawDataPoints() }
                .flatten()
            onDataSampled(rawDataPoints)
            dataSamples.forEach { it.second.dispose() }

            return@withContext PlottingData(
                features,
                endTime
            )
        }
    }

    private suspend fun tryGetPlottingData(
        config: LineGraphWithFeatures,
        lineGraphFeature: LineGraphFeature
    ): DataSample {
        val movingAvDuration = movingAverageDurations[lineGraphFeature.averagingMode]
        val plottingPeriod = plottingModePeriods[lineGraphFeature.plottingMode]
        val rawDataSample = withContext(ioDispatcher) {
            dataSampler.getDataSampleForFeatureId(lineGraphFeature.featureId)
        }

        val aggregationCalculator = when (lineGraphFeature.plottingMode) {
            LineGraphPlottingModes.WHEN_TRACKED -> IdentityFunction()
            else -> CompositeFunction(
                DurationAggregationFunction(timeHelper, plottingPeriod!!),
                DataPaddingFunction(
                    timeHelper = timeHelper,
                    endTime = config.endDate.toOffsetDateTime(
                        fallback = getLastDataPointTimestamp(rawDataSample)
                    ),
                    duration = config.sampleSize
                )
            )
        }
        val averageCalculator = when (lineGraphFeature.averagingMode) {
            LineGraphAveraginModes.NO_AVERAGING -> IdentityFunction()
            else -> MovingAverageFunction(movingAvDuration!!)
        }
        return CompositeFunction(aggregationCalculator, averageCalculator)
            .mapSample(rawDataSample)
    }

    private fun getLastDataPointTimestamp(rawDataSample: DataSample): OffsetDateTime {
        val dataPeekIterator = rawDataSample.iterator()
        return if (!dataPeekIterator.hasNext()) OffsetDateTime.now()
        else dataPeekIterator.next().timestamp
    }

    private fun getXYSeriesFromDataPoints(
        dataSample: List<IDataPoint>,
        endTime: OffsetDateTime,
        lineGraphFeature: LineGraphFeature
    ): FastXYSeries {
        val scale = lineGraphFeature.scale
        val offset = lineGraphFeature.offset
        val durationDivisor = when (lineGraphFeature.durationPlottingMode) {
            DurationPlottingMode.HOURS -> 3600.0
            DurationPlottingMode.MINUTES -> 60.0
            else -> 1.0
        }

        val yValues = dataSample.map { dp ->
            (dp.value * scale / durationDivisor) + offset
        }
        val xValues = dataSample.map { dp ->
            Duration.between(endTime, dp.timestamp).toMillis()
        }

        return androidPlotSeriesHelper.getFastXYSeries(
            name = lineGraphFeature.name,
            xValues = xValues,
            yValues = yValues,
        )
    }

    private data class YAxisParams(
        val bounds: RectRegion,
        val subdivides: Int,
    )

    private fun getYAxisParameters(
        lineGraph: LineGraphWithFeatures,
        series: Collection<FastXYSeries?>,
        timeBasedRange: Boolean
    ): YAxisParams {
        val fixed = lineGraph.yRangeType == YRangeType.FIXED

        val bounds = RectRegion()
        series.forEach { it?.let { bounds.union(it.minMax()) } }

        val (yMin, yMax) =
            if (fixed) Pair(lineGraph.yFrom, lineGraph.yTo)
            else Pair(bounds.minY, bounds.maxY)

        if (yMin == null || yMax == null) return YAxisParams(bounds, 11)

        val parameters = dataDisplayIntervalHelper
            .getYParameters(yMin.toDouble(), yMax.toDouble(), timeBasedRange, fixed)

        bounds.minY = parameters.boundsMin
        bounds.maxY = parameters.boundsMax

        return YAxisParams(bounds, parameters.subdivides)
    }
}