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
import com.samco.trackandgraph.functions.functions.DataSampleFunction
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

/**
 * Calculate the moving aggregation of all of the data points given over the moving duration given.
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

        val iterator = dataSample.iterator()

        var currentStart: OffsetDateTime? = null
        val currentList = mutableListOf<IDataPoint>()

        while (iterator.hasNext()) {
            val next = iterator.next()

            //If we're right at the start of the list, set the current start time
            if (currentStart == null) currentStart = next.timestamp

            //If the next data point is outside of the window, yield the current window of data points
            // and move the start time backwards to the next data point. Keep yielding until the next
            // data point is within the window or the list is empty.
            while (Duration.between(next.timestamp, currentStart) >= movingAggDuration) {
                yield(
                    object : IDataPoint() {
                        override val timestamp: OffsetDateTime = currentStart!!
                        override val value: Double = calculateValue(currentList)
                        override val label: String = calculateLabel(currentList)
                    }
                )

                currentList.removeFirst()

                if (currentList.isEmpty()) {
                    currentStart = next.timestamp
                    //Explicitly breaking here saves us from comparing a duration of 0 and getting
                    // true causing un-expected behaviour if you passed 0 for movingAggDuration
                    break
                } else currentStart = currentList.first().timestamp
            }

            //Move the current data point into the window
            currentList.add(next)
        }

        //Yield the remaining data points. Each data point will be the average of all remaining
        // data points within the window
        while (currentList.isNotEmpty()) {
            yield(
                object : IDataPoint() {
                    override val timestamp: OffsetDateTime = currentList[0].timestamp
                    override val value: Double = calculateValue(currentList)
                    override val label: String = calculateLabel(currentList)
                }
            )
            currentList.removeFirst()
        }
    }
}