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

package com.samco.trackandgraph.calculators

import com.samco.trackandgraph.database.entity.AggregatedDataPoint
import kotlinx.coroutines.yield
import org.threeten.bp.Duration

class MovingAverageCalculator(
    private val movingAvgDuration: Duration
) : DataCalculator {
    /**
     * Calculate the moving averages of all of the data points given over the moving average duration given.
     * A new DataSample will be returned with one data point for every data point in the input set whose
     * timestamp shall be the same but value will be equal to the average of it and all previous data points
     * within the movingAvDuration.
     *
     * The data points in the input sample are expected to be in date order with the oldest data points
     * earliest in the list
     */
    override suspend fun execute(dataSample: DataSample): DataSample {
        val movingAggregationPointsRaw = mutableListOf<AggregatedDataPoint>()
        val dataPointsReversed = dataSample.dataPoints.reversed()

        for ((index, current) in dataPointsReversed.mapIndexed { idx, point -> Pair(idx, point) }) {
            yield()
            val parents = dataPointsReversed.drop(index)
                .takeWhile { dp ->
                    Duration.between(dp.timestamp, current.timestamp) < movingAvgDuration
                }

            movingAggregationPointsRaw.add(
                0,
                AggregatedDataPoint(
                    current.timestamp,
                    current.featureId,
                    value = Double.NaN,
                    label = current.label,
                    note = current.note,
                    parents = parents
                )
            )
        }
        return RawAggregatedDatapoints(movingAggregationPointsRaw).average()
    }
}