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

package com.samco.trackandgraph.statistics

import com.androidplot.Region
import com.androidplot.util.SeriesUtils
import com.androidplot.xy.FastXYSeries
import com.androidplot.xy.RectRegion
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.TemporalAmount
import kotlin.math.*

class DataSample(val dataPoints: List<DataPointInterface>)

/**
 * This function will call the dataSource to get a sample of the data points with the given featureId.
 * The end date of this sample is calculated as:
 *  - If an endDate is given then it is used
 *  - If an endDate is not given then the last data point tracked or the current date/time is used (which ever is later)
 * The start date of this sample is calculated as:
 *  - The beginning of time if no sampleDuration is provided
 *  - If a sampleDuration is provided then it is the end date minus the sample duration. However if there is
 *      a plotTotalTime or averagingDuration provided as well then which ever of the two is larger will be added
 *      to the sampleDuration before it is subtracted from the end date such that all relevant information
 *      is contained in the sample.
 *
 * Note: No actual averaging or totalling is performed by this function, it just collects all relevant data.
 */
internal suspend fun sampleData(
    dataSource: TrackAndGraphDatabaseDao, featureId: Long, sampleDuration: Duration?,
    endDate: OffsetDateTime?, averagingDuration: Duration?, plotTotalTime: TemporalAmount?
): DataSample {
    return withContext(Dispatchers.IO) {
        if (sampleDuration == null && endDate == null) DataSample(
            dataSource.getDataPointsForFeatureAscSync(featureId)
        )
        else {
            val latest = endDate ?: getLastTrackedTimeOrNow(
                dataSource,
                featureId
            )
            val plottingDuration =
                plotTotalTime?.let { Duration.between(latest, latest.plus(plotTotalTime)) }
            val minSampleDate = sampleDuration?.let {
                val possibleLongestDurations = listOf(
                    sampleDuration,
                    averagingDuration?.plus(sampleDuration),
                    plottingDuration?.plus(sampleDuration)
                )
                latest.minus(possibleLongestDurations.maxBy { d -> d ?: Duration.ZERO })
            } ?: OffsetDateTime.MIN
            val dataPoints =
                dataSource.getDataPointsForFeatureBetweenAscSync(featureId, minSampleDate, latest)
            DataSample(
                dataPoints
            )
        }
    }
}

private fun getLastTrackedTimeOrNow(
    dataSource: TrackAndGraphDatabaseDao,
    featureId: Long
): OffsetDateTime {
    val lastDataPointList = dataSource.getLastDataPointForFeatureSync(featureId)
    val now = OffsetDateTime.now()
    val latest = lastDataPointList.firstOrNull()?.timestamp?.plusSeconds(1)
    return listOfNotNull(now, latest).max()!!
}


/**
 * Return all the data points in the sample that lie within the sampleDuration leading up to the endTime.
 * If the sampleDuration is null then all data points leading up to the end time will be returned.
 * If the endTime is null then all data points within the sampleDuration leading up to the last data point
 * will be returned. If both the sampleDuration and endTime are null then the whole list will be returned.
 */
internal fun clipDataSample(
    dataSample: DataSample,
    endTime: OffsetDateTime?,
    sampleDuration: Duration?
): DataSample {
    if (dataSample.dataPoints.isEmpty()) return dataSample

    var newDataPoints = dataSample.dataPoints
    if (endTime != null) {
        val lastIndex = newDataPoints.indexOfLast { dp -> dp.timestamp <= endTime }
        newDataPoints = newDataPoints.take(lastIndex + 1)
    }
    if (sampleDuration != null) {
        val endOfDuration = endTime ?: dataSample.dataPoints.last().timestamp
        val startTime = endOfDuration.minus(sampleDuration)
        val firstIndex = newDataPoints.indexOfFirst { dp -> dp.timestamp >= startTime }
        newDataPoints = if (firstIndex < 0) emptyList() else newDataPoints.drop(firstIndex)
    }
    return DataSample(newDataPoints)
}

internal fun getXYSeriesFromDataSample(
    dataSample: DataSample,
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
    val yValues = dataSample.dataPoints.map { dp ->
        (dp.value * scale / durationDivisor) + offset
    }
    val xValues =
        dataSample.dataPoints.map { dp -> Duration.between(endTime, dp.timestamp).toMillis() }

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
        override fun size() = dataSample.dataPoints.size
    }
}

/**
 * Get the end of the window with respect to the given endDate. For example if the window
 * size is a week and the endDate is on a Wednesday then the returned date/time will be
 * 00:00 on the following monday.
 *
 * If the endDate is null then the current date/time is used in its place
 */
internal fun getNextEndOfWindow(
    window: TimeHistogramWindow,
    endDate: OffsetDateTime?,
    firstDayOfWeek: DayOfWeek?
): OffsetDateTime {
    val end = endDate ?: OffsetDateTime.now()
    return findBeginningOfTemporal(
        end.plus(window.period),
        window.period,
        firstDayOfWeek
    )
}

/**
 * This function essentially loops over the data sample and puts every input data point into a bin
 * depending on where its timestamp falls within the given input window. For example if the window
 * represents a week then each data point may be put into one of 7 bins depending on which day of the
 * week it was tracked.
 *
 * A map is generated with a data structure similar to a Matrix whereby each value in the map is a list of the
 * same length. The keys in the map are the integer values of the discrete values of the {@param feature}
 * or just {0} if the feature is not Discrete. The length of the lists is the number of bins of the
 * given {@param window}. The lists represent the sum of all values in each bin of the histogram
 * normalised such that the sum of all values in all lists is 1.
 *
 * If {@param sumByCount} is false then the value of each data point is added to the total value of
 * the histogram bin it belongs in before normalisation. If sumByCount is true then the value of each
 * histogram bin before normalisation is the number of data points that fall in that bin.
 *
 * {@param sample} - The data points to generate a histogram for
 * {@param window} - The TimeHistogramWindow specifying the domain and number of bins of the histogram
 * {@param feature} - The Feature for which the histogram is being generated
 * {@param sumByCount} - Whether this histogram represents the number of data points tracked or
 * the sum of their values
 */
internal fun getHistogramBinsForSample(
    sample: DataSample,
    window: TimeHistogramWindow,
    feature: Feature,
    sumByCount: Boolean,
    aggPreferences: AggregationWindowPreferences? = null
): Map<Int, List<Double>>? {
    if (sample.dataPoints.isEmpty()) return null
    val endTime = getNextEndOfWindow(
        window,
        sample.dataPoints.maxBy { it.timestamp }!!
            .cutoffTimestampForAggregation(aggPreferences?.startTimeOfDay),
        aggPreferences?.firstDayOfWeek
    )
    val isDiscrete = feature.featureType == FeatureType.DISCRETE
    val keys =
        if (isDiscrete) feature.discreteValues.map { it.index }.toSet()
        else listOf(0).toSet()

    return when {
        isDiscrete && sumByCount ->
            getHistogramBinsForSample(sample.dataPoints, window, keys, endTime, aggPreferences?.startTimeOfDay,
            ::addOneDiscreteValueToBin
        )
        isDiscrete ->
            getHistogramBinsForSample(sample.dataPoints, window, keys, endTime, aggPreferences?.startTimeOfDay,
            ::addDiscreteValueToBin
        )
        sumByCount ->
            getHistogramBinsForSample(sample.dataPoints, window, keys, endTime, aggPreferences?.startTimeOfDay,
            ::addOneToBin
        )
        else ->
            getHistogramBinsForSample(sample.dataPoints, window, keys, endTime, aggPreferences?.startTimeOfDay,
            ::addValueToBin
        )
    }
}

private fun getHistogramBinsForSample(
    sample: List<DataPointInterface>,
    window: TimeHistogramWindow,
    keys: Set<Int>,
    endTime: OffsetDateTime,
    startTimeOfDay: Duration?,
    addFunction: (DataPointInterface, Map<Int, MutableList<Double>>, Int) -> Unit
): Map<Int, List<Double>> {
    val binTotalMaps =
        calculateBinTotals(sample, window, keys, endTime, startTimeOfDay, addFunction)
    val total = binTotalMaps.map { it.value.sum() }.sum()
    return binTotalMaps.map { kvp -> kvp.key to kvp.value.map { it / total } }.toMap()
}

/**
 * Create a map structure and place every data point in it using the provided addFunction
 */
private fun calculateBinTotals(
    sample: List<DataPointInterface>,
    window: TimeHistogramWindow,
    keys: Set<Int>,
    endTime: OffsetDateTime,
    startTimeOfDay: Duration?,
    addFunction: (DataPointInterface, Map<Int, MutableList<Double>>, Int) -> Unit
): Map<Int, List<Double>> {
    val binTotalMap = keys.map { it to MutableList(window.numBins) { 0.0 } }.toMap()
    var currEnd = endTime
    var currStart = currEnd - window.period
    var binned = 0
    val reversed = sample.asReversed()
    var nextPoint = reversed[0]
    while (binned < sample.size) {
        val periodDuration = Duration
            .between(currStart, currEnd)
            .seconds
            .toDouble()
        while (nextPoint.cutoffTimestampForAggregation(startTimeOfDay) > currStart) {
            val distance = Duration.between(
                currStart,
                nextPoint.cutoffTimestampForAggregation(startTimeOfDay)
            ).seconds.toDouble()
            var binIndex = (window.numBins * (distance / periodDuration)).toInt()
            if (binIndex == window.numBins) binIndex--
            addFunction(nextPoint, binTotalMap, binIndex)
            if (++binned == sample.size) break
            nextPoint = reversed[binned]
        }
        currEnd -= window.period
        currStart -= window.period
    }
    return binTotalMap
}

/**
 * Add the value of the given data point to the bin at the given binIndex
 */
private fun addValueToBin(
    dataPoint: DataPointInterface,
    bin: Map<Int, MutableList<Double>>,
    binIndex: Int
) {
    bin[0]?.set(binIndex, (bin[0]?.get(binIndex) ?: 0.0) + dataPoint.value)
}

/**
 * Add one to the bin at the given binIndex
 */
private fun addOneToBin(
    dataPoint: DataPointInterface,
    bin: Map<Int, MutableList<Double>>,
    binIndex: Int
) {
    bin[0]?.set(binIndex, (bin[0]?.get(binIndex) ?: 0.0) + 1.0)
}

/**
 * Add the value of the given data point to the bin at the given binIndex within the histogram
 * specific to its discrete value.
 */
private fun addDiscreteValueToBin(
    dataPoint: DataPointInterface,
    bin: Map<Int, MutableList<Double>>,
    binIndex: Int
) {
    val i = dataPoint.value.toInt()
    bin[i]?.set(binIndex, (bin[i]?.get(binIndex) ?: 0.0) + dataPoint.value)
}

/**
 * Add one to the bin at the given binIndex within the histogram specific to its discrete value.
 */
private fun addOneDiscreteValueToBin(
    dataPoint: DataPointInterface,
    bin: Map<Int, MutableList<Double>>,
    binIndex: Int
) {
    val i = dataPoint.value.toInt()
    bin[i]?.set(binIndex, (bin[i]?.get(binIndex) ?: 0.0) + 1.0)
}

/**
 * Given an input list of lists where each list represents the discrete value of a feature and each
 * sub-list has the same size and represents the values of each histogram bin for that discrete value.
 * Calculate the largest bin by summing the values of each discrete value for each bin and returning
 * the largest of those sums.
 */
internal fun getLargestBin(bins: List<List<Double>>?): Double? {
    return bins
        ?.getOrElse(0) { null }
        ?.size
        ?.downTo(1)
        ?.map { index -> bins.sumByDouble { it[index - 1] } }
        ?.max()
}


