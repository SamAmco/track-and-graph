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
package com.samco.trackandgraph.database

import androidx.room.ColumnInfo
import org.threeten.bp.OffsetDateTime

data class DisplayFeature(
    @ColumnInfo(name = "id")
    var id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "track_group_id")
    val trackGroupId: Long,

    @ColumnInfo(name = "type")
    val featureType: FeatureType = FeatureType.CONTINUOUS,

    @ColumnInfo(name = "discrete_values")
    val discreteValues: List<DiscreteValue>,

    @ColumnInfo(name = "has_default_value")
    val hasDefaultValue: Boolean,

    @ColumnInfo(name = "default_value")
    val defaultValue: Double,

    @ColumnInfo(name = "last_timestamp")
    val timestamp: OffsetDateTime?,

    @ColumnInfo(name = "num_data_points")
    val numDataPoints: Long?,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "feature_description")
    val description: String
) {
    fun getDisplayTimestamp() = timestamp?.let { displayFeatureDateFormat.format(timestamp) }
}