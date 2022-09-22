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

import android.database.Cursor
import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.odtFromString
import java.lang.Exception

internal class DataPointCursorSequence(
    private val cursor: Cursor
) : Sequence<IDataPoint> {
    private val visited = mutableListOf<com.samco.trackandgraph.base.database.dto.DataPoint>()
    fun getRawDataPoints(): List<com.samco.trackandgraph.base.database.dto.DataPoint> = visited

    override fun iterator(): Iterator<IDataPoint> = object : Iterator<IDataPoint> {
        private var position = 0

        override fun hasNext(): Boolean = position < cursor.count

        override fun next(): IDataPoint {
            val dataPoint = if (position >= cursor.position) {
                cursor.moveToNext()
                val rawDataPoint = getCursorDataPoint()
                visited.add(rawDataPoint)
                rawDataPoint
            } else visited[position]
            position++
            return object : IDataPoint() {
                override val timestamp = dataPoint.timestamp
                override val value = dataPoint.value
                override val label = dataPoint.label
            }
        }

        private fun getCursorDataPoint(): com.samco.trackandgraph.base.database.dto.DataPoint {
            return com.samco.trackandgraph.base.database.dto.DataPoint(
                odtFromString(cursor.getString(0))
                    ?: throw Exception("Could not read timestamp for data point row"),
                cursor.getLong(1),
                cursor.getDouble(2),
                cursor.getString(3),
                cursor.getString(4),
            )
        }
    }
}