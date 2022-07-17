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

package com.samco.trackandgraph.base.database.sampling

import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.dto.DataPoint
import org.threeten.bp.temporal.TemporalAmount

data class DataSampleProperties(
    val regularity: TemporalAmount? = null,
    val isDuration: Boolean = false
)

/**
 * A sequence of data points in order from newest to oldest
 */
abstract class DataSample(
    val dataSampleProperties: DataSampleProperties = DataSampleProperties()
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
    }

    /**
     * Get a list of all the raw data points that have been used so far to generate this
     * data sample. This will not contain any data points that have not yet been iterated in the
     * sequence.
     */
    abstract fun getRawDataPoints(): List<DataPoint>

    /**
     * Get a list of all the raw data points for this data sample. This will iterate the entire
     * sequence and then retrieve all accessed raw data points.
     */
    open fun getAllRawDataPoints(): List<DataPoint> {
        iterator().let { while (it.hasNext()) it.next() }
        return getRawDataPoints()
    }
}
