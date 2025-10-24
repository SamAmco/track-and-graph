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

package com.samco.trackandgraph.data.database.dto

import com.samco.trackandgraph.data.database.entity.Function

data class Function(
    val id: Long = -1L,
    val featureId: Long = -1L,
    val name: String,
    val groupId: Long,
    val displayIndex: Int = -1,
    val description: String,
    val functionGraph: FunctionGraph,
    val inputFeatureIds: List<Long>
) {
    internal fun toEntity(serializedFunctionGraph: String) = Function(
        id = id,
        featureId = featureId,
        functionGraph = serializedFunctionGraph
    )
}
