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

package com.samco.trackandgraph.data.database.entity.queryresponse

import androidx.room.ColumnInfo

internal data class FunctionWithFeature(
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "feature_id")
    val featureId: Long,

    @ColumnInfo(name = "function_graph")
    val functionGraph: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "feature_description")
    val description: String
) {
    fun toDto(
        functionGraphDto: com.samco.trackandgraph.data.database.dto.FunctionGraph,
        inputFeatures: List<Long>,
    ) = com.samco.trackandgraph.data.database.dto.Function(
        id = id,
        featureId = featureId,
        name = name,
        groupId = groupId,
        displayIndex = displayIndex,
        description = description,
        functionGraph = functionGraphDto,
        inputFeatureIds = inputFeatures
    )
}
