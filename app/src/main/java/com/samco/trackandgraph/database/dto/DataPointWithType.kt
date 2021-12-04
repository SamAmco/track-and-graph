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

import androidx.room.ColumnInfo
import com.samco.trackandgraph.database.entity.DataType
import org.threeten.bp.OffsetDateTime

data class DataPointWithType (
    @ColumnInfo(name = "timestamp")
    override val timestamp: OffsetDateTime = OffsetDateTime.now(),

    @ColumnInfo(name = "feature_id")
    val featureId: Long,

    @ColumnInfo(name = "data_type")
    override val dataType: DataType,

    @ColumnInfo(name = "value")
    override val value: Double,

    @ColumnInfo(name = "label")
    override val label: String,

    @ColumnInfo(name = "note")
    override val note: String
) : IDataPoint()