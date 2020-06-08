/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatview

import android.content.Context
import com.androidplot.Region
import com.androidplot.util.SeriesUtils
import com.androidplot.xy.FastXYSeries
import com.androidplot.xy.RectRegion
import com.samco.trackandgraph.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.ui.GraphLegendItemView
import kotlinx.coroutines.yield
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.TemporalAmount
import org.threeten.bp.temporal.WeekFields
import java.util.*
import kotlin.math.abs

//TODO The functions in this class should probably all have javadoc and unit tests as many of them
// contain functionality which is not easy to decipher at a glance

class RawDataSample(val dataPoints: List<DataPoint>, val plotFrom: Int)

class SampleDataCallback(val callback: (List<DataPoint>) -> Unit) : (List<DataPoint>) -> Unit {
    override fun invoke(dataPoints: List<DataPoint>) {
        callback.invoke(dataPoints)
    }
}

internal suspend fun sampleData(
    dataSource: TrackAndGraphDatabaseDao, featureId: Long, sampleDuration: Duration?,
    averagingDuration: Duration?, plotTotalTime: TemporalAmount?
): RawDataSample {
    return withContext(Dispatchers.IO) {
        if (sampleDuration == null) RawDataSample(
            dataSource.getDataPointsForFeatureAscSync(featureId),
            0
        )
        else {
            val latest = getLatestTimeOrNowForFeature(dataSource, featureId)
            val startDate = latest.minus(sampleDuration)
            val plottingDuration =
                plotTotalTime?.let { Duration.between(latest, latest.plus(plotTotalTime)) }
            val maxSampleDuration = listOf(
                sampleDuration,
                averagingDuration?.plus(sampleDuration),
                plottingDuration?.plus(sampleDuration)
            ).maxBy { d -> d ?: Duration.ZERO }
            val minSampleDate = latest.minus(maxSampleDuration)
            val dataPoints =
                dataSource.getDataPointsForFeatureBetweenAscSync(featureId, minSampleDate, latest)
            val startIndex = dataPoints.indexOfFirst { dp -> dp.timestamp.isAfter(startDate) }
            RawDataSample(dataPoints, startIndex)
        }
    }
}

internal fun getLatestTimeOrNowForFeature(
    dataSource: TrackAndGraphDatabaseDao,
    featureId: Long
): OffsetDateTime {
    val lastDataPointList = dataSource.getLastDataPointForFeatureSync(featureId)
    val now = OffsetDateTime.now()
    val latest = lastDataPointList.firstOrNull()?.timestamp?.plusSeconds(1)
    return listOfNotNull(now, latest).max()!!
}

internal fun initHeader(binding: GraphStatViewBinding?, graphOrStat: GraphOrStat?) {
    val headerText = graphOrStat?.name ?: ""
    binding?.headerText?.text = headerText
}

internal fun dataPlottable(
    rawData: RawDataSample,
    minDataPoints: Int = 1
): Boolean {
    return rawData.plotFrom >= 0 && rawData.dataPoints.size - rawData.plotFrom >= minDataPoints
}

internal fun inflateGraphLegendItem(
    binding: GraphStatViewBinding, context: Context,
    colorIndex: Int, label: String
) {
    val colorId = dataVisColorList[colorIndex]
    binding.legendFlexboxLayout.addView(
        GraphLegendItemView(
            context,
            colorId,
            label
        )
    )
}

internal suspend fun calculateDurationAccumulatedValues(
    rawData: RawDataSample,
    featureId: Long,
    plotTotalTime: TemporalAmount
): RawDataSample {
    var plotFrom = 0
    var foundPlotFrom = false
    val newData = mutableListOf<DataPoint>()
    val firstDataPointTime = if (rawData.dataPoints.isEmpty()) {
        OffsetDateTime.now()
    } else rawData.dataPoints[0].timestamp
    var currentTimeStamp = findFirstPlotDateTime(firstDataPointTime, plotTotalTime)
    val latest = getNowOrLatest(rawData)
    var index = 0
    while (currentTimeStamp.isBefore(latest)) {
        currentTimeStamp = currentTimeStamp.with { ld -> ld.plus(plotTotalTime) }
        val points = rawData.dataPoints.drop(index)
            .takeWhile { dp -> dp.timestamp.isBefore(currentTimeStamp) }
        val total = points.sumByDouble { dp -> dp.value }
        index += points.size
        if (index > rawData.plotFrom && !foundPlotFrom) {
            plotFrom = newData.size
            foundPlotFrom = true
        }
        newData.add(DataPoint(currentTimeStamp, featureId, total, "", ""))
        yield()
    }
    if (newData.size == 1) {
        val newPointTime = findFirstPlotDateTime(firstDataPointTime, plotTotalTime)
        newData.add(0, DataPoint(newPointTime, featureId, 0.0, "", ""))
    }
    return RawDataSample(newData, plotFrom)
}

internal fun getNowOrLatest(rawData: RawDataSample): OffsetDateTime {
    val now = OffsetDateTime.now()
    if (rawData.dataPoints.isEmpty()) return now
    val latest = rawData.dataPoints.last().timestamp
    return if (latest > now) latest else now
}

internal fun findFirstPlotDateTime(
    startDateTime: OffsetDateTime,
    plotTotalTime: TemporalAmount
): OffsetDateTime {
    return when (plotTotalTime) {
        is Duration -> {
            //For now we assume the duration is 1 hour for simplicity since this is the
            // only available duration option anyway
            startDateTime.withMinute(0).withSecond(0)
                .withNano(0).minusNanos(1)
        }
        is Period -> {
            var dt = startDateTime
            val minusAWeek = plotTotalTime.minus(Period.ofWeeks(1))
            val minusAMonth = plotTotalTime.minus(Period.ofMonths(1))
            val minusAYear = plotTotalTime.minus(Period.ofYears(1))
            if (minusAYear.days >= 0 && !minusAYear.isNegative) {
                dt = startDateTime.withDayOfYear(1)
            } else if (minusAMonth.days >= 0 && !minusAMonth.isNegative) {
                dt = startDateTime.withDayOfMonth(1)
            } else if (minusAWeek.days >= 0 && !minusAWeek.isNegative) {
                dt = startDateTime.with(
                    TemporalAdjusters.previousOrSame(
                        WeekFields.of(Locale.getDefault()).firstDayOfWeek
                    )
                )
            }
            dt.withHour(0).withMinute(0).withSecond(0).minusSeconds(1)
        }
        else -> startDateTime
    }
}

internal suspend fun calculateMovingAverage(
    rawData: RawDataSample,
    movingAvDuration: Duration
): List<Double> {
    return rawData.dataPoints
        .drop(rawData.plotFrom)
        .mapIndexed { index, dataPoint ->
            val inRange = mutableListOf(dataPoint)
            var i = rawData.plotFrom + index - 1
            while (i > 0 && Duration.between(
                    rawData.dataPoints[i].timestamp,
                    dataPoint.timestamp
                ) <= movingAvDuration
            ) {
                inRange.add(rawData.dataPoints[i])
                i--
            }
            yield()
            inRange.sumByDouble { dp -> dp.value } / inRange.size.toDouble()
        }
}

internal suspend fun getXYSeriesFromRawDataSample(
    rawData: RawDataSample,
    endTime: OffsetDateTime,
    lineGraphFeature: LineGraphFeature
): FastXYSeries {
    val yValues = when (lineGraphFeature.averagingMode) {
        LineGraphAveraginModes.NO_AVERAGING -> rawData.dataPoints
            .drop(rawData.plotFrom)
            .map { dp -> dp.value }
        else -> calculateMovingAverage(
            rawData,
            movingAverageDurations[lineGraphFeature.averagingMode]!!
        )
    }.map { v -> (v * lineGraphFeature.scale) + lineGraphFeature.offset }

    val xValues = rawData.dataPoints.drop(rawData.plotFrom).map { dp ->
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
        override fun size() = rawData.dataPoints.size - rawData.plotFrom
    }
}

