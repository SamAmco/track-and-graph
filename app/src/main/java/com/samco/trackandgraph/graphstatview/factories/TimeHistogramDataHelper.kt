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

import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.entity.TimeHistogramWindow
import com.samco.trackandgraph.functions.sampling.DataSample
import com.samco.trackandgraph.functions.helpers.TimeHelper
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
        sample: DataSample,
        window: TimeHistogramWindow,
        sumByCount: Boolean,
    ): Map<String, List<Double>>? {
        val sampleList = sample.toList()
        if (sampleList.isEmpty()) return null
        val endTime = getNextEndOfWindow(window, sample.first().timestamp)

        return when {
            sumByCount ->
                getHistogramBinsForSample(sampleList, window, endTime, ::addOneToBin)
            else ->
                getHistogramBinsForSample(sampleList, window, endTime, ::addValueToBin)
        }
    }

    private fun getNextEndOfWindow(
        window: TimeHistogramWindow,
        endDate: OffsetDateTime?,
    ): ZonedDateTime {
        val end = endDate ?: OffsetDateTime.now()
        return timeHelper.findEndOfTemporal(end, window.period)
    }


    private fun getHistogramBinsForSample(
        sample: List<IDataPoint>,
        window: TimeHistogramWindow,
        endTime: ZonedDateTime,
        addFunction: (IDataPoint, MutableMap<String, MutableList<Double>>, Int) -> Unit
    ): Map<String, List<Double>> {
        val binTotalMaps = calculateBinTotals(sample, window, endTime, addFunction)
        val total = binTotalMaps.map { it.value.sum() }.sum()
        return binTotalMaps.map { kvp -> kvp.key to kvp.value.map { it / total } }.toMap()
    }

    /**
     * Create a map structure and place every data point in it using the provided addFunction
     */
    private fun calculateBinTotals(
        sample: List<IDataPoint>,
        window: TimeHistogramWindow,
        endTime: ZonedDateTime,
        addFunction: (IDataPoint, MutableMap<String, MutableList<Double>>, Int) -> Unit
    ): Map<String, List<Double>> {
        val binTotalMap = mutableMapOf<String, MutableList<Double>>()
        var currEnd = endTime
        var currStart = currEnd - window.period
        var binned = 0
        var nextPoint = sample[0]
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
                if (!binTotalMap.containsKey(nextPoint.label)) {
                    binTotalMap[nextPoint.label] = MutableList(window.numBins) { 0.0 }
                }
                addFunction(nextPoint, binTotalMap, binIndex)
                if (++binned == sample.size) break
                nextPoint = sample[binned]
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
        bin: MutableMap<String, MutableList<Double>>,
        binIndex: Int
    ) {
        val list = bin[dataPoint.label]
        list?.set(binIndex, list[binIndex] + dataPoint.value)
    }

    /**
     * Add one to the bin at the given binIndex
     */
    private fun addOneToBin(
        dataPoint: IDataPoint,
        bin: MutableMap<String, MutableList<Double>>,
        binIndex: Int
    ) {
        val list = bin[dataPoint.label]
        list?.set(binIndex, list[binIndex] + 1.0)
    }
}