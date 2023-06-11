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
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import java.util.*

internal class DataPointCursorSequence(
    private val cursor: Cursor
) : Sequence<IDataPoint> {
    private val visited =
        Collections.synchronizedList(mutableListOf<com.samco.trackandgraph.base.database.dto.DataPoint>())

    private val count = cursor.count

    private val lock = Any()

    fun getRawDataPoints(): List<com.samco.trackandgraph.base.database.dto.DataPoint> = visited

    fun dispose() = synchronized(lock) { cursor.close() }

    override fun iterator(): Iterator<IDataPoint> = object : Iterator<IDataPoint> {
        private var position = 0

        override fun hasNext(): Boolean = synchronized(lock) { position < count }

        override fun next(): IDataPoint = synchronized(lock) {
            val dataPoint =
                if (position in visited.indices) visited[position]
                else {
                    cursor.moveToNext()
                    val rawDataPoint = getCursorDataPoint()
                    visited.add(rawDataPoint)
                    rawDataPoint
                }
            position++
            return object : IDataPoint() {
                override val timestamp = dataPoint.timestamp
                override val value = dataPoint.value
                override val label = dataPoint.label
            }
        }

        private fun getCursorDataPoint(): com.samco.trackandgraph.base.database.dto.DataPoint {
            val epochMilli = cursor.getLong(0)
            val featureId = cursor.getLong(1)
            val utcOffsetSec = cursor.getInt(2)
            val value = cursor.getDouble(3)
            val label = cursor.getString(4)
            val note = cursor.getString(5)
            return com.samco.trackandgraph.base.database.dto.DataPoint(
                timestamp = OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(epochMilli),
                    ZoneOffset.ofTotalSeconds(utcOffsetSec)
                ),
                featureId = featureId,
                value = value,
                label = label,
                note = note
            )
        }
    }
}