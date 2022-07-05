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

package com.samco.trackandgraph.functions.aggregation

import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.functions.helpers.TimeHelper
import org.threeten.bp.temporal.TemporalAmount

/**
 * Aggregate all data points into binSize length bins. For example if the bin size is 1 day and the
 * sample data contains 3 data points {1, 3, 7} all tracked on the same day then the function will
 * return an aggregated data sample with one point with all three data-points as its parent.
 *
 * The supported binSize values are those supported by [TimeHelper.findBeginningOfTemporal]
 */

internal class FixedBinAggregator(
    private val timeHelper: TimeHelper,
    private val binSize: TemporalAmount,
) : DataAggregator {

    override suspend fun aggregate(dataSample: DataSample): AggregatedDataSample {
        return AggregatedDataSample.fromSequence(
            getSequence(dataSample),
            dataSample.dataSampleProperties.copy(regularity = binSize),
            dataSample::getRawDataPoints
        )
    }

    private fun getSequence(dataSample: Sequence<IDataPoint>) = sequence {
        val latest = dataSample.firstOrNull()?.timestamp ?: return@sequence
        var nextBinTimeStamp = timeHelper.findEndOfTemporal(latest, binSize)
        var nextCutOff = timeHelper.findBeginningOfTemporal(latest, binSize)

        val iterator = dataSample.iterator()
        var nextPoints = mutableListOf<IDataPoint>()
        while (iterator.hasNext()) {
            val next = iterator.next()
            while (timeHelper.toZonedDateTime(next.timestamp) < nextCutOff) {
                yield(
                    AggregatedDataPoint(
                        timestamp = nextBinTimeStamp.toOffsetDateTime(),
                        parents = nextPoints
                    )
                )
                nextBinTimeStamp = nextBinTimeStamp.minus(binSize)
                nextCutOff = nextCutOff.minus(binSize)
                nextPoints = mutableListOf()
            }
            nextPoints.add(next)
        }
        yield(
            AggregatedDataPoint(
                timestamp = nextBinTimeStamp.toOffsetDateTime(),
                parents = nextPoints
            )
        )
    }
}