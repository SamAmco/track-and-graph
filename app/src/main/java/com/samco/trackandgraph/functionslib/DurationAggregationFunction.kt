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

package com.samco.trackandgraph.functionslib

import com.samco.trackandgraph.functionslib.aggregation.FixedBinAggregator
import org.threeten.bp.temporal.TemporalAmount

/**
 * Add up all data points per plotTotalTime. For example if the plot total time is 1 day and the
 * sample data contains 3 data points {1, 3, 7} all tracked on the same day then the function will
 * return a data sample containing 1 point with the value 11.
 *
 * The timestamp of each generated data point will be one nanosecond before the end of that [binSize]
 * period. For example if the [binSize] is one week and the [timeHelper] specifies [TimeHelper.findEndOfTemporal]
 * for one week to be at 00:00 on a Monday then each generated data point will have a timestamp
 * of the last nanosecond of the preceding Sunday.
 *
 * The currently supported [binSize] values are: Duration.ofHours(1), Period.ofDays(1),
 * Period.ofWeeks(1), Period.ofMonths(1), Period.ofYears(1)
 *
 * sampleData.dataPoints should be sorted from oldest timestamp to newest timestamp
 */
class DurationAggregationFunction(
    private val timeHelper: TimeHelper,
    private val binSize: TemporalAmount,
) : DataSampleFunction {
    override suspend fun mapSample(dataSample: DataSample): DataSample {
        return FixedBinAggregator(timeHelper, binSize)
            .aggregate(dataSample)
            .sum()
    }
}