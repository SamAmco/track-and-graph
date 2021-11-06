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

import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class DataClippingFunction(
    private val endTime: OffsetDateTime?,
    private val sampleDuration: Duration?
) : DataSampleFunction {
    /**
     * Return all the data points in the sample that lie within the sampleDuration leading up to the endTime.
     * If the sampleDuration is null then all data points leading up to the end time will be returned.
     * If the endTime is null then all data points within the sampleDuration leading up to the last data point
     * will be returned. If both the sampleDuration and endTime are null then the whole list will be returned.
     */
    override suspend fun execute(dataSample: DataSample): DataSample {
        if (dataSample.dataPoints.isEmpty()) return dataSample

        var newDataPoints = dataSample.dataPoints
        if (endTime != null) {
            val lastIndex = newDataPoints.indexOfLast { dp -> dp.timestamp <= endTime }
            newDataPoints = newDataPoints.take(lastIndex + 1)
        }
        if (sampleDuration != null) {
            val endOfDuration = endTime ?: dataSample.dataPoints.last().timestamp
            val startTime = endOfDuration.minus(sampleDuration)
            val firstIndex = newDataPoints.indexOfFirst { dp -> dp.timestamp >= startTime }
            newDataPoints = if (firstIndex < 0) emptyList() else newDataPoints.drop(firstIndex)
        }
        return DataSample(newDataPoints, dataSample.featureType, dataSample.featureId)
    }
}