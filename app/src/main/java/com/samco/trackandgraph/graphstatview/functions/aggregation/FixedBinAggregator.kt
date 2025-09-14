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

package com.samco.trackandgraph.graphstatview.functions.aggregation

import com.samco.trackandgraph.data.database.dto.IDataPoint
import com.samco.trackandgraph.data.database.sampling.DataSample
import com.samco.trackandgraph.graphstatview.functions.data_sample_functions.DataSampleFunction
import com.samco.trackandgraph.graphstatview.functions.helpers.TimeHelper
import org.threeten.bp.OffsetDateTime
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
    private val calculateValue: (List<IDataPoint>) -> Double,
    private val calculateLabel: (List<IDataPoint>) -> String,
) : DataSampleFunction {

    override suspend fun mapSample(dataSample: DataSample): DataSample {
        return DataSample.fromSequence(
            data = getSequence(dataSample),
            getRawDataPoints = dataSample::getRawDataPoints,
            dataSampleProperties = dataSample.dataSampleProperties.copy(regularity = binSize),
            onDispose = dataSample::dispose
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
                    getDataPoint(
                        timestamp = nextBinTimeStamp.toOffsetDateTime(),
                        points = nextPoints
                    )
                )
                nextBinTimeStamp = nextBinTimeStamp.minus(binSize)
                nextCutOff = nextCutOff.minus(binSize)
                nextPoints = mutableListOf()
            }
            nextPoints.add(next)
        }
        yield(
            getDataPoint(
                timestamp = nextBinTimeStamp.toOffsetDateTime(),
                points = nextPoints
            )
        )
    }

    private fun getDataPoint(
        timestamp: OffsetDateTime,
        points: List<IDataPoint>,
    ) = object : IDataPoint() {
        override val timestamp: OffsetDateTime = timestamp
        override val value: Double = calculateValue(points)
        override val label: String = calculateLabel(points)
    }
}