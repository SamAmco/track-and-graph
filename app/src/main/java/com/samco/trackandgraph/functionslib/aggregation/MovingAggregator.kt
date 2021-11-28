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

package com.samco.trackandgraph.functionslib.aggregation

import com.samco.trackandgraph.database.entity.AggregatedDataPoint
import com.samco.trackandgraph.database.entity.IDataPoint
import com.samco.trackandgraph.functionslib.DataSample
import kotlinx.coroutines.yield
import org.threeten.bp.Duration

/**
 * Calculate the moving aggregation-parents of all of the data points given over the moving duration given.
 * RawAggregatedDatapoints will be returned containing one data point for every data point in the input set.
 *
 * The data points in the input sample are expected to be in date order with the oldest data points
 * earliest in the list
 */
internal class MovingAggregator(private val movingAggDuration: Duration) : DataAggregator {
    override suspend fun aggregate(dataSample: DataSample): AggregatedDataSample {
        return AggregatedDataSample.fromSequence(
            getSequence(dataSample),
            dataSample.dataSampleProperties
        )
    }

    override suspend fun aggregate(dataSample: AggregatedDataSample): AggregatedDataSample {
        return AggregatedDataSample.fromSequence(
            getSequence(dataSample),
            dataSample.dataSampleProperties
        )
    }

    private fun getSequence(dataSample: Sequence<IDataPoint>) = sequence {
        for ((index, current) in dataSample.mapIndexed { idx, point -> Pair(idx, point) }) {
            val parents = dataSample.drop(index)
                .takeWhile { dp ->
                    Duration.between(dp.timestamp, current.timestamp) < movingAggDuration
                }

            yield(
                AggregatedDataPoint(
                    current.timestamp,
                    current.featureId,
                    value = Double.NaN,
                    label = current.label,
                    note = current.note,
                    parents = parents.toList()
                )
            )
        }
    }
}