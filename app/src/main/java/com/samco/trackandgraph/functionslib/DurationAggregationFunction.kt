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
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.TemporalAmount

/**
 * Add up all data points per plotTotalTime. For example if the plot total time is 1 day and the
 * sample data contains 3 data points {1, 3, 7} all tracked on the same day then the function will
 * return a data sample containing 1 point with the value 11.
 *
 * The currently supported plotTotalTime values are: Duration.ofHours(1), Period.ofDays(1),
 * Period.ofWeeks(1), Period.ofMonths(1), Period.ofYears(1)
 *
 * If sampleDuration is provided then totals will be generated at least as far back as now minus the
 * sampleDuration. However if there is more data before this period then it that data will also be
 * totalled. For clipping see clipDataSample.
 *
 * If end time is provided then totals will be generated at least up to this time. However if there
 * is more data after the end time in the input sample then that data will also be totalled. For
 * clipping see clipDataSample.
 *
 * sampleData.dataPoints should be sorted from oldest timestamp to newest timestamp
 */
class DurationAggregationFunction(
    private val timeHelper: TimeHelper,
    private val featureId: Long,
    private val sampleDuration: Duration?,
    private val endTime: OffsetDateTime?,
    private val binSize: TemporalAmount,
) : DataSampleFunction {
    override suspend fun execute(dataSample: DataSample): DataSample {
        return FixedBinAggregator(timeHelper, featureId, sampleDuration, endTime, binSize)
            .aggregate(dataSample)
            .sum()
    }
}