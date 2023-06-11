/*
* This file is part of Track & Graph
* 
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.base.database.dto

import com.samco.trackandgraph.base.database.entity.DataPoint
import org.threeten.bp.OffsetDateTime

/**
 * Represents a data point for a feature specifically. You can insert data points into the database.
 * For a more generic form of data iteration (that supports data from functions as well) see
 * DataSample.
 */
data class DataPoint(
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val featureId: Long,
    val value: Double,
    val label: String,
    val note: String
) {
    internal fun toEntity() = DataPoint(
        epochMilli = timestamp.toInstant().toEpochMilli(),
        utcOffsetSec = timestamp.offset.totalSeconds,
        featureId = featureId,
        value = value,
        label = label,
        note = note
    )
}