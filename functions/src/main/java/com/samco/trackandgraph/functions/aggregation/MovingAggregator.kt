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
import com.samco.trackandgraph.base.sequencehelpers.cache
import com.samco.trackandgraph.functions.functions.DataSampleFunction
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

/**
 * Calculate the moving aggregation-parents of all of the data points given over the moving duration given.
 * RawAggregatedDatapoints will be returned containing one data point for every data point in the input set.
 *
 * The data points in the input sample are expected to be in date order with the oldest data points
 * earliest in the list
 */
internal class MovingAggregator(
    private val movingAggDuration: Duration,
    private val calculateValue: (List<IDataPoint>) -> Double,
    private val calculateLabel: (List<IDataPoint>) -> String,
) : DataSampleFunction {


    override suspend fun mapSample(dataSample: DataSample): DataSample {
        return DataSample.fromSequence(
            data = getSequence(dataSample),
            dataSampleProperties = dataSample.dataSampleProperties,
            getRawDataPoints = dataSample::getRawDataPoints,
            onDispose = dataSample::dispose
        )
    }

    private fun getSequence(dataSample: Sequence<IDataPoint>) = sequence {
        val cachedSample = dataSample.cache()
        for ((index, current) in cachedSample.mapIndexed { idx, point -> Pair(idx, point) }) {
            val parents = cachedSample
                .drop(index)
                .takeWhile { dp ->
                    //We expect the durations to be negative but < just compares the absolute values
                    Duration.between(dp.timestamp, current.timestamp) < movingAggDuration
                }
                .toList()

            yield(
                object : IDataPoint() {
                    override val timestamp: OffsetDateTime = current.timestamp
                    override val value: Double = calculateValue(parents)
                    override val label: String = calculateLabel(parents)
                }
            )
        }
    }
}