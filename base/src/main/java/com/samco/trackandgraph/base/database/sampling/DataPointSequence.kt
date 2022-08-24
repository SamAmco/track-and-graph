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

import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.entity.DataPoint

internal class DataPointSequence(
    private val dao: TrackAndGraphDatabaseDao,
    private val featureId: Long
) : Sequence<IDataPoint> {

    companion object {
        //The number of data points to read from the database at once
        private const val BUFFER_AMOUNT = 80
    }

    private var visited: Int = 0
    private val cached = mutableListOf<DataPoint>()
    private val dataSize by lazy { dao.getNumberOfDataPointsForFeature(featureId)
    }

    private fun bufferNext() {
        cached.addAll(dao.getDataPointsForFeatureSync(featureId, cached.size, BUFFER_AMOUNT))
    }

    fun getRawDataPoints(): List<com.samco.trackandgraph.base.database.dto.DataPoint> = cached
        .take(visited)
        .map { it.toDto() }

    override fun iterator(): Iterator<IDataPoint> = object : Iterator<IDataPoint> {
        private var position = 0

        override fun hasNext(): Boolean = position < dataSize - 1

        override fun next(): IDataPoint {
            if (position + 1 > cached.size) bufferNext()
            val next = cached[position]
            position += 1
            if (position > visited) visited = position
            return next
        }
    }
}