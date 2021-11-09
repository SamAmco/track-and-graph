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

import com.samco.trackandgraph.antlr.evaluation.toDuration
import com.samco.trackandgraph.database.entity.AggregatedDataPoint
import com.samco.trackandgraph.functionslib.DataSample
import kotlinx.coroutines.yield
import org.threeten.bp.Duration
import org.threeten.bp.temporal.TemporalAmount

/**
 * Calculate the moving aggregation-parents of all of the data points given over the moving duration given.
 * RawAggregatedDatapoints will be returned containing one data point for every data point in the input set.
 *
 * The data points in the input sample are expected to be in date order with the oldest data points
 * earliest in the list
 */
internal class MovingAggregator(private val movingAggDuration: TemporalAmount) : DataAggregator {
    override suspend fun aggregate(dataSample: DataSample): RawAggregatedDatapoints {
        val movingAggregationPointsRaw = mutableListOf<AggregatedDataPoint>()
        val dataPointsReversed = dataSample.dataPoints.reversed()

        for ((index, current) in dataPointsReversed.mapIndexed { idx, point -> Pair(idx, point) }) {
            yield()
            val parents = dataPointsReversed.drop(index)
                .takeWhile { dp ->
                    Duration.between(dp.timestamp, current.timestamp) < movingAggDuration.toDuration()
                }

            movingAggregationPointsRaw.add(
                0,
                AggregatedDataPoint(
                    current.timestamp,
                    value = Double.NaN,
                    parents = parents,
                    label = current.label,
                    note = current.note
                )
            )
        }

        return RawAggregatedDatapoints(movingAggregationPointsRaw, dataSample.featureType)
    }
}