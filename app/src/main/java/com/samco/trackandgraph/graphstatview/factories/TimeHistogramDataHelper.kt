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

import com.samco.trackandgraph.database.dto.IDataPoint
import com.samco.trackandgraph.functionslib.*
import com.samco.trackandgraph.database.entity.Feature
import com.samco.trackandgraph.database.entity.DataType
import com.samco.trackandgraph.database.entity.TimeHistogramWindow
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class TimeHistogramDataHelper(
    private val timeHelper: TimeHelper
) {
    /**
     * Given an input list of lists where each list represents the discrete value of a feature and each
     * sub-list has the same size and represents the values of each histogram bin for that discrete value.
     * Calculate the largest bin by summing the values of each discrete value for each bin and returning
     * the largest of those sums.
     */
    fun getLargestBin(bins: List<List<Double>>?): Double? {
        return bins
            ?.getOrElse(0) { null }
            ?.size
            ?.downTo(1)
            ?.map { index -> bins.sumByDouble { it[index - 1] } }
            ?.maxOrNull()
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
        sample: List<IDataPoint>,
        window: TimeHistogramWindow,
        feature: Feature,
        sumByCount: Boolean,
    ): Map<Int, List<Double>>? {
        if (sample.isEmpty()) return null
        val endTime = getNextEndOfWindow(
            window,
            sample.maxBy { it.timestamp }!!.timestamp
        )
        val isDiscrete = feature.featureType == DataType.DISCRETE
        val keys =
            if (isDiscrete) feature.discreteValues.map { it.index }.toSet()
            else listOf(0).toSet()

        return when {
            isDiscrete && sumByCount ->
                getHistogramBinsForSample(
                    sample, window, keys, endTime,
                    ::addOneDiscreteValueToBin
                )
            isDiscrete ->
                getHistogramBinsForSample(
                    sample, window, keys, endTime,
                    ::addDiscreteValueToBin
                )
            sumByCount ->
                getHistogramBinsForSample(
                    sample, window, keys, endTime,
                    ::addOneToBin
                )
            else ->
                getHistogramBinsForSample(
                    sample, window, keys, endTime,
                    ::addValueToBin
                )
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
    ): ZonedDateTime {
        val end = endDate ?: OffsetDateTime.now()
        return timeHelper.findBeginningOfTemporal(
            end.plus(window.period),
            window.period,
        )
    }


    private fun getHistogramBinsForSample(
        sample: List<IDataPoint>,
        window: TimeHistogramWindow,
        keys: Set<Int>,
        endTime: ZonedDateTime,
        addFunction: (IDataPoint, Map<Int, MutableList<Double>>, Int) -> Unit
    ): Map<Int, List<Double>> {
        val binTotalMaps = calculateBinTotals(sample, window, keys, endTime, addFunction)
        val total = binTotalMaps.map { it.value.sum() }.sum()
        return binTotalMaps.map { kvp -> kvp.key to kvp.value.map { it / total } }.toMap()
    }

    /**
     * Create a map structure and place every data point in it using the provided addFunction
     */
    private fun calculateBinTotals(
        sample: List<IDataPoint>,
        window: TimeHistogramWindow,
        keys: Set<Int>,
        endTime: ZonedDateTime,
        addFunction: (IDataPoint, Map<Int, MutableList<Double>>, Int) -> Unit
    ): Map<Int, List<Double>> {
        val binTotalMap = keys.map { it to MutableList(window.numBins) { 0.0 } }.toMap()
        var currEnd = endTime
        var currStart = currEnd - window.period
        var binned = 0
        val reversed = sample.asReversed()
        var nextPoint = reversed[0]
        val timeOf = { dp: IDataPoint ->
            dp.timestamp.atZoneSameInstant(ZoneId.systemDefault())
        }
        while (binned < sample.size) {
            val periodDuration = Duration
                .between(currStart, currEnd)
                .seconds
                .toDouble()
            var nextPointTime = timeOf(nextPoint)
            while (nextPointTime > currStart) {
                val distance = Duration.between(
                    currStart,
                    nextPointTime
                ).seconds.toDouble()
                var binIndex = (window.numBins * (distance / periodDuration)).toInt()
                if (binIndex == window.numBins) binIndex--
                addFunction(nextPoint, binTotalMap, binIndex)
                if (++binned == sample.size) break
                nextPoint = reversed[binned]
                nextPointTime = timeOf(nextPoint)
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
        dataPoint: IDataPoint,
        bin: Map<Int, MutableList<Double>>,
        binIndex: Int
    ) {
        bin[0]?.set(binIndex, (bin[0]?.get(binIndex) ?: 0.0) + dataPoint.value)
    }

    /**
     * Add one to the bin at the given binIndex
     */
    private fun addOneToBin(
        dataPoint: IDataPoint,
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
        dataPoint: IDataPoint,
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
        dataPoint: IDataPoint,
        bin: Map<Int, MutableList<Double>>,
        binIndex: Int
    ) {
        val i = dataPoint.value.toInt()
        bin[i]?.set(binIndex, (bin[i]?.get(binIndex) ?: 0.0) + 1.0)
    }
}