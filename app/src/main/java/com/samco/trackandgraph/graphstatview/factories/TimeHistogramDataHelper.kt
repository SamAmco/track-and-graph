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

import com.samco.trackandgraph.TimeHistogramWindowData
import com.samco.trackandgraph.data.database.dto.IDataPoint
import com.samco.trackandgraph.data.database.dto.TimeHistogramWindow
import com.samco.trackandgraph.data.sampling.DataSample
import com.samco.trackandgraph.graphstatview.functions.helpers.TimeHelper
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
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
            ?.maxOfOrNull { index -> bins.sumOf { it[index - 1] } }
    }

    internal fun getHistogramBinsForSample(
        sample: DataSample,
        window: TimeHistogramWindow,
        sumByCount: Boolean,
    ) = getHistogramBinsForSample(sample, TimeHistogramWindowData.getWindowData(window), sumByCount)

    /**
     * This function essentially loops over the data sample and puts every input data point into a bin
     * depending on where its timestamp falls within the given input window. For example if the window
     * represents a week then each data point may be put into one of 7 bins depending on which day of the
     * week it was tracked.
     *
     * A map is generated with a data structure similar to a Matrix whereby each value in the map is a list of the
     * same length. The keys in the map are the labels of the [sample] or just empty string if the sample
     * does not have labels. The length of the lists is the number of bins of the given [window].
     * The lists represent the sum of all values in each bin of the histogram
     * normalised such that the sum of all values in all lists is 100.
     *
     * If [sumByCount] is false then the value of each data point is added to the total value of
     * the histogram bin it belongs in before normalisation. If sumByCount is true then the value of each
     * histogram bin before normalisation is the number of data points that fall in that bin.
     *
     * [sample] - The data points to generate a histogram for
     * [window] - The TimeHistogramWindowData specifying the domain and number of bins of the histogram
     * [sumByCount] - Whether this histogram represents the number of data points tracked or
     * the sum of their values
     */
    internal fun getHistogramBinsForSample(
        sample: DataSample,
        window: TimeHistogramWindowData,
        sumByCount: Boolean,
    ): Map<String, List<Double>>? {
        val sampleList = sample.toList()
        if (sampleList.isEmpty()) return null
        val endTime = getNextEndOfWindow(window, sample.first().timestamp)

        return when {
            sumByCount -> getHistogramBinsForSample(sampleList, window, endTime, ::addOneToBin)
            else -> getHistogramBinsForSample(sampleList, window, endTime, ::addValueToBin)
        }
    }

    private fun getNextEndOfWindow(
        window: TimeHistogramWindowData,
        endDate: OffsetDateTime?,
    ): ZonedDateTime {
        val end = endDate ?: OffsetDateTime.now()
        return timeHelper.findEndOfTemporal(end, window.period)
    }


    private fun getHistogramBinsForSample(
        sample: List<IDataPoint>,
        window: TimeHistogramWindowData,
        endTime: ZonedDateTime,
        addFunction: (IDataPoint, MutableMap<String, MutableList<Double>>, Int) -> Unit
    ): Map<String, List<Double>> {
        val binTotalMaps = calculateBinTotals(sample, window, endTime, addFunction)
        val total = binTotalMaps.map { it.value.sum() }.sum()
        return binTotalMaps.map { kvp -> kvp.key to kvp.value.map { (it / total) * 100.0 } }.toMap()
    }

    /**
     * Create a map structure and place every data point in it using the provided addFunction
     */
    private fun calculateBinTotals(
        sample: List<IDataPoint>,
        window: TimeHistogramWindowData,
        endTime: ZonedDateTime,
        addFunction: (IDataPoint, MutableMap<String, MutableList<Double>>, Int) -> Unit
    ): Map<String, List<Double>> {
        val binTotalMap = mutableMapOf<String, MutableList<Double>>()
        var currEnd = endTime
        var currStart = currEnd - window.period
        var binned = 0
        var nextPoint = sample[0]
        val timeOf = { dp: IDataPoint ->
            //Drop the offset and use the local time. A time of 8:00 should always go in the 8:00 bin
            // regardless of what time zone it was tracked in.
            dp.timestamp.atZoneSimilarLocal(timeHelper.zoneId)
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