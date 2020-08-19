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
package com.samco.trackandgraph.database.dto

import androidx.room.ColumnInfo
import com.samco.trackandgraph.database.entity.DiscreteValue
import com.samco.trackandgraph.database.entity.FeatureShowCountMethod
import com.samco.trackandgraph.database.entity.FeatureShowCountPeriod
import com.samco.trackandgraph.database.entity.FeatureType
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

    // TODO confirm if we should not just
    //  change the meaning of num_data_points instead
    //  but at this point maybe it is best
    //  if num_data_points represent total COUNT(*)
    //  regardless of showCountPeriod and showCountMethod
    @ColumnInfo(name = "shown_count")
    val shownCount: Long?,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "feature_description")
    val description: String,

    @ColumnInfo(name = "show_count_for_period")
    val showCountPeriod: FeatureShowCountPeriod = FeatureShowCountPeriod.ALL,

    @ColumnInfo(name = "show_count_using_method")
    val showCountMethod: FeatureShowCountMethod = FeatureShowCountMethod.COUNT_ENTRIES
)