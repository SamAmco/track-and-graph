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

import com.androidplot.Region
import com.androidplot.util.SeriesUtils
import com.androidplot.xy.FastXYSeries
import com.androidplot.xy.RectRegion
import com.androidplot.xy.StepMode
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.model.DataSource
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.constants.*
import com.samco.trackandgraph.base.database.dto.DurationPlottingMode
import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.dto.LineGraphAveraginModes
import com.samco.trackandgraph.base.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.base.database.entity.queryresponse.LineGraphWithFeatures
import com.samco.trackandgraph.base.database.entity.queryresponse.YRangeType
import com.samco.trackandgraph.base.database.entity.DataPoint
import com.samco.trackandgraph.base.database.entity.GraphOrStat
import com.samco.trackandgraph.base.database.entity.LineGraphFeature
import com.samco.trackandgraph.functions.sampling.DataSample
import com.samco.trackandgraph.functions.sampling.DataSamplerImpl
import com.samco.trackandgraph.functions.aggregation.GlobalAggregationPreferences
import com.samco.trackandgraph.functions.functions.*
import com.samco.trackandgraph.functions.helpers.TimeHelper
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import kotlinx.coroutines.*
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import kotlin.math.abs

class LineGraphDataFactory : ViewDataFactory<LineGraphWithFeatures, ILineGraphViewData>() {
    companion object {
        val instance = LineGraphDataFactory()
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        config: LineGraphWithFeatures,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILineGraphViewData {
        try {
            val endTime = config.endDate ?: OffsetDateTime.now()
            val plottableData = generatePlottingData(
                dataSource,
                config,
                endTime,
                onDataSampled
            )
            val hasPlottableData = plottableData
                .any { kvp -> kvp.value != null }

            val durationBasedRange = config.features
                .any { f -> f.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE }
            val (bounds, yAxisParameters) = getYAxisParameters(
                config,
                plottableData.values,
                durationBasedRange
            )

            return object : ILineGraphViewData {
                override val durationBasedRange = durationBasedRange
                override val yRangeType = config.yRangeType
                override val bounds = bounds
                override val hasPlottableData = hasPlottableData
                override val endTime = endTime
                override val plottableData = plottableData
                override val state = IGraphStatViewData.State.READY
                override val graphOrStat = graphOrStat
                override val yAxisRangeParameters = yAxisParameters
            }
        } catch (throwable: Throwable) {
            return object : ILineGraphViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = throwable
            }
        }
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILineGraphViewData {
        val lineGraph = dataSource.getLineGraphByGraphStatId(graphOrStat.id)
            ?: return object : ILineGraphViewData {
                override val state = IGraphStatViewData.State.ERROR
                override val graphOrStat = graphOrStat
                override val error = GraphStatInitException(R.string.graph_stat_view_not_found)
            }
        return createViewData(dataSource, graphOrStat, lineGraph, onDataSampled)
    }

    private suspend fun generatePlottingData(
        dataSource: TrackAndGraphDatabaseDao,
        lineGraph: LineGraphWithFeatures,
        endTime: OffsetDateTime,
        onDataSampled: (List<DataPoint>) -> Unit
    ): Map<LineGraphFeature, FastXYSeries?> = coroutineScope {
        withContext(Dispatchers.Default) {
            //Create all the data samples in parallel (this shouldn't actually take long but why not)
            val dataSamples = lineGraph.features.map { lgf ->
                async { Pair(lgf, tryGetPlottingData(dataSource, lineGraph, lgf)) }
            }.awaitAll()

            //Generate the actual plotting data for each sample. This is the part that will take longer
            // hence the parallelization
            val features = dataSamples.map { pair ->
                async {
                    //Calling toList on the data sample evaluates it and causes the whole pipeline
                    // to be processed
                    val dataPoints = pair.second.toList().asReversed()
                    val series = if (dataPoints.size >= 2) {
                        getXYSeriesFromDataPoints(dataPoints, endTime, pair.first)
                    } else null
                    pair.first to series
                }
            }.awaitAll().toMap()
            val rawDataPoints = dataSamples.map { it.second.getRawDataPoints() }.flatten()
            withContext(Dispatchers.Main) { onDataSampled(rawDataPoints) }
            return@withContext features
        }
    }

    private suspend fun tryGetPlottingData(
        dao: TrackAndGraphDatabaseDao,
        config: LineGraphWithFeatures,
        lineGraphFeature: LineGraphFeature
    ): DataSample {
        val movingAvDuration = movingAverageDurations[lineGraphFeature.averagingMode]
        val plottingPeriod = plottingModePeriods[lineGraphFeature.plottingMode]
        val rawDataSample = withContext(Dispatchers.IO) {
            val dataSampler = DataSamplerImpl(dao)
            val dataSource = DataSource.FeatureDataSource(lineGraphFeature.featureId)
            dataSampler.getDataSampleForSource(dataSource)
        }
        val clippingCalculator = DataClippingFunction(config.endDate, config.duration)

        val timeHelper = TimeHelper(GlobalAggregationPreferences)
        val aggregationCalculator = when (lineGraphFeature.plottingMode) {
            LineGraphPlottingModes.WHEN_TRACKED -> IdentityFunction()
            else -> CompositeFunction(
                DurationAggregationFunction(timeHelper, plottingPeriod!!),
                DataPaddingFunction(timeHelper, config.endDate, config.duration)
            )
        }
        val averageCalculator = when (lineGraphFeature.averagingMode) {
            LineGraphAveraginModes.NO_AVERAGING -> IdentityFunction()
            else -> MovingAverageFunction(movingAvDuration!!)
        }
        return CompositeFunction(aggregationCalculator, averageCalculator, clippingCalculator)
            .mapSample(rawDataSample)
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

        var yRegion = SeriesUtils.minMax(yValues)
        if (abs(yRegion.min.toDouble() - yRegion.max.toDouble()) < 0.1)
            yRegion = Region(yRegion.min, yRegion.min.toDouble() + 0.1)
        val xRegion = SeriesUtils.minMax(xValues)
        val rectRegion = RectRegion(xRegion.min, xRegion.max, yRegion.min, yRegion.max)

        return object : FastXYSeries {
            override fun minMax() = rectRegion
            override fun getX(index: Int): Number = xValues[index]
            override fun getY(index: Int): Number = yValues[index]
            override fun getTitle() = lineGraphFeature.name
            override fun size() = xValues.size
        }
    }

    private fun getYAxisParameters(
        lineGraph: LineGraphWithFeatures,
        series: Collection<FastXYSeries?>,
        timeBasedRange: Boolean
    ): Pair<RectRegion, Pair<StepMode, Double>> {
        val fixed = lineGraph.yRangeType == YRangeType.FIXED;

        val bounds = RectRegion()
        series.forEach { it?.let { bounds.union(it.minMax()) } }

        val (y_min, y_max) =
            if (fixed) Pair(lineGraph.yFrom, lineGraph.yTo)
            else Pair(bounds.minY, bounds.maxY)

        if (y_min == null || y_max == null) {
            return Pair(bounds, Pair(StepMode.SUBDIVIDE, 11.0))
        }

        val parameters = DataDisplayIntervalHelper()
            .getYParameters(y_min.toDouble(), y_max.toDouble(), timeBasedRange, fixed)

        bounds.minY = parameters.bounds_min
        bounds.maxY = parameters.bounds_max

        val intervalParameters = Pair(parameters.step_mode, parameters.n_intervals)

        return Pair(bounds, intervalParameters)
    }

}