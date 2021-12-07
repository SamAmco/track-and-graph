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

package com.samco.trackandgraph.database.dto

import com.samco.trackandgraph.database.doubleFormatter
import com.samco.trackandgraph.database.entity.DataType
import com.samco.trackandgraph.ui.formatTimeDuration
import org.threeten.bp.OffsetDateTime

abstract class IDataPoint {
    abstract val timestamp: OffsetDateTime
    abstract val dataType: DataType
    abstract val value: Double
    abstract val label: String

    fun getDisplayValue(): String {
        return when (dataType) {
            DataType.DISCRETE -> doubleFormatter.format(value) + " : $label"
            DataType.CONTINUOUS -> doubleFormatter.format(value)
            DataType.DURATION -> formatTimeDuration(value.toLong())
        }
    }

    override fun equals(other: Any?): Boolean {
        return other != null
                && other is IDataPoint
                && this.timestamp == other.timestamp
                && this.dataType == other.dataType
                && this.value == other.value
                && this.label == other.label
    }
}
