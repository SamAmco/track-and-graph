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

package com.samco.trackandgraph.base.database.dto

import com.samco.trackandgraph.base.database.entity.DataPoint
import com.squareup.moshi.JsonClass
import java.lang.Exception

@JsonClass(generateAdapter = true)
data class DiscreteValue(
    val index: Int,
    val label: String
) {

    //Ideally we wouldn't need fromString and toString here but they are still used by CSVReadWriter.
    override fun toString() = "$index:$label"

    companion object {
        fun fromString(value: String): DiscreteValue {
            if (!value.contains(':')) throw Exception("value did not contain a colon")
            val label = value.substring(value.indexOf(':') + 1).trim()
            val index = value.substring(0, value.indexOf(':')).trim().toInt()
            return DiscreteValue(index, label)
        }

        fun fromDataPoint(dataPoint: DataPoint) =
            DiscreteValue(
                dataPoint.value.toInt(),
                dataPoint.label
            )

        fun fromIDataPoint(dataPoint: IDataPoint) =
            DiscreteValue(
                dataPoint.value.toInt(),
                dataPoint.label
            )
    }
}
