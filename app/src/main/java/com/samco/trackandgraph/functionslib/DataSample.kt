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

import com.samco.trackandgraph.database.dto.IDataPoint
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.DataType
import org.threeten.bp.temporal.TemporalAmount

data class DataSampleProperties(
    val regularity: TemporalAmount? = null
)

/**
 * A sequence of data points in order from newest to oldest
 */
abstract class DataSample(
    val dataSampleProperties: DataSampleProperties
) : Sequence<IDataPoint> {
    companion object {
        /**
         * Useful for testing purposes, but this form of data sample will not return any raw data
         * when queried. You should use a sequence that supports [DataSample.getRawDataPoints]
         * in production.
         */
        fun fromSequence(
            data: Sequence<IDataPoint>,
            dataSampleProperties: DataSampleProperties = DataSampleProperties()
        ): DataSample {
            return object : DataSample(dataSampleProperties) {
                override fun getRawDataPoints() = emptyList<DataPoint>()
                override fun iterator(): Iterator<IDataPoint> = data.iterator()
            }
        }

        /**
         * Return a DataSample from a sequence with the given properties and the given function
         * for returning the raw data used.
         */
        fun fromSequence(
            data: Sequence<IDataPoint>,
            dataSampleProperties: DataSampleProperties = DataSampleProperties(),
            getRawDataPoints: () -> List<DataPoint>
        ): DataSample {
            return object : DataSample(dataSampleProperties) {
                override fun getRawDataPoints() = getRawDataPoints()
                override fun iterator(): Iterator<IDataPoint> = data.iterator()
            }
        }

        /**
         * Useful in the short term but this slightly defeats the point of using sequences since
         * it is backed by a pre loaded list. Ideally we should implement a database cursor
         * implementation of DataSample that only loads data as it is needed.
         *
         * //TODO write a database cursor version of DataSample
         */
        fun fromList(
            data: List<DataPoint>,
            dataType: DataType,
            dataSampleProperties: DataSampleProperties = DataSampleProperties()
        ): DataSample {
            return object : DataSample(dataSampleProperties) {
                override fun getRawDataPoints() = data
                override fun iterator(): Iterator<IDataPoint> = data.asSequence()
                    .map { toIDataPoint(it, dataType) }
                    .iterator()
            }
        }

        private fun toIDataPoint(dataPoint: DataPoint, dataType: DataType): IDataPoint {
            return object : IDataPoint() {
                override val timestamp = dataPoint.timestamp
                override val dataType = dataType
                override val value = dataPoint.value
                override val label = dataPoint.label
                override val note = dataPoint.note
            }
        }
    }

    /**
     * Get a list of all the raw data points that have been used so far to generate this
     * data sample.
     */
    abstract fun getRawDataPoints(): List<DataPoint>
}
