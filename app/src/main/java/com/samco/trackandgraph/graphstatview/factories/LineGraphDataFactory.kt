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
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.database.dto.YRangeType
import com.samco.trackandgraph.database.entity.*
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.statistics.*
import kotlinx.coroutines.yield
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

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
        val endTime = config.endDate ?: OffsetDateTime.now()
        val allReferencedDataPoints = mutableListOf<DataPoint>()
        val plottableData = generatePlottingData(
            dataSource,
            config,
            allReferencedDataPoints,
            endTime
        )
        val hasPlottableData = plottableData.any { kvp -> kvp.value != null }
        val bounds = getBounds(config, plottableData.values)
        val durationBasedRange =
            config.features.any { f -> f.durationPlottingMode == DurationPlottingMode.DURATION_IF_POSSIBLE }

        onDataSampled(allReferencedDataPoints)

        return object : ILineGraphViewData {
            override val durationBasedRange: Boolean
                get() = durationBasedRange
            override val yRangeType: YRangeType
                get() = config.yRangeType
            override val bounds: RectRegion
                get() = bounds
            override val hasPlottableData: Boolean
                get() = hasPlottableData
            override val endTime: OffsetDateTime
                get() = endTime
            override val plottableData: Map<LineGraphFeature, FastXYSeries?>
                get() = plottableData
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
        }
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): ILineGraphViewData {
        val lineGraph = dataSource.getLineGraphByGraphStatId(graphOrStat.id)
            ?: return object : ILineGraphViewData {
                override val state: IGraphStatViewData.State
                    get() = IGraphStatViewData.State.ERROR
                override val graphOrStat: GraphOrStat
                    get() = graphOrStat
                override val error: GraphStatInitException?
                    get() = GraphStatInitException(R.string.graph_stat_view_not_found)
            }
        return createViewData(dataSource, graphOrStat, lineGraph, onDataSampled)
    }

    private suspend fun generatePlottingData(
        dataSource: TrackAndGraphDatabaseDao,
        lineGraph: LineGraphWithFeatures,
        allReferencedDataPoints: MutableList<DataPoint>,
        endTime: OffsetDateTime
    ): Map<LineGraphFeature, FastXYSeries?> {
        return lineGraph.features.map { lgf ->
            yield()
            val plottingData =
                tryGetPlottingData(dataSource, lineGraph, allReferencedDataPoints, lgf)
            lgf to plottingData?.let { getXYSeriesFromDataSample(it, endTime, lgf) }
        }.toMap()
    }

    //TODO can we get more efficiency by running some of this on the default dispatcher?
    private suspend fun tryGetPlottingData(
        dataSource: TrackAndGraphDatabaseDao,
        lineGraph: LineGraphWithFeatures,
        allReferencedDataPoints: MutableList<DataPoint>,
        lineGraphFeature: LineGraphFeature
    ): DataSample? {
        val movingAvDuration = movingAverageDurations[lineGraphFeature.averagingMode]
        val plottingPeriod = plottingModePeriods[lineGraphFeature.plottingMode]
        val rawDataSample =
            sampleData(
                dataSource,
                lineGraphFeature.featureId,
                lineGraph.duration,
                lineGraph.endDate,
                movingAvDuration,
                plottingPeriod
            )
        val visibleSection =
            clipDataSample(
                rawDataSample,
                lineGraph.endDate,
                lineGraph.duration
            )
        allReferencedDataPoints.addAll(visibleSection.dataPoints)
        val plotTotalData = when (lineGraphFeature.plottingMode) {
            LineGraphPlottingModes.WHEN_TRACKED -> rawDataSample
            else -> calculateDurationAccumulatedValues(
                rawDataSample,
                lineGraphFeature.featureId,
                //We have to add movingAvDuration if it exists to make sure we're going back far enough
                // to get correct averaging
                lineGraph.duration?.plus(movingAvDuration ?: Duration.ZERO),
                lineGraph.endDate,
                plottingPeriod!!
            )
        }
        val averagedData = when (lineGraphFeature.averagingMode) {
            LineGraphAveraginModes.NO_AVERAGING -> plotTotalData
            else -> calculateMovingAverages(
                plotTotalData,
                movingAvDuration!!
            )
        }
        val plottingData =
            clipDataSample(
                averagedData,
                lineGraph.endDate,
                lineGraph.duration
            )

        return if (plottingData.dataPoints.size >= 2) plottingData else null
    }

    private suspend fun getBounds(
        lineGraph: LineGraphWithFeatures,
        series: Collection<FastXYSeries?>
    ): RectRegion {
        val bounds = RectRegion()
        series.forEach { it?.let { bounds.union(it.minMax()) } }
        yield()
        if (lineGraph.yRangeType == YRangeType.FIXED) {
            bounds.minY = lineGraph.yFrom
            bounds.maxY = lineGraph.yTo
        }
        return bounds
    }
}