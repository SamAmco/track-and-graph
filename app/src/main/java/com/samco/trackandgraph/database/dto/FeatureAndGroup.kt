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
import com.samco.trackandgraph.database.entity.FeatureType

data class FeatureAndGroup(
    @ColumnInfo(name = "id")
    var id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "type")
    val featureType: FeatureType = FeatureType.CONTINUOUS,

    @ColumnInfo(name = "discrete_values")
    val discreteValues: List<DiscreteValue>,

    @ColumnInfo(name = "group_name")
    val trackGroupName: String
)
