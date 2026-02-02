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
    val id: Long = 0L,
    override val featureId: Long = 0L,
    override val name: String,
    override val groupIds: Set<Long>,
    override val displayIndex: Int = 0,
    override val description: String,
    val functionGraph: FunctionGraph,
    val inputFeatureIds: List<Long>
) : Feature {
    internal fun toEntity(serializedFunctionGraph: String) =
        com.samco.trackandgraph.data.database.entity.Function(
            id = id,
            featureId = featureId,
            functionGraph = serializedFunctionGraph
        )
}

/**
 * Request object for creating a new Function.
 *
 * Note: id, featureId, and displayIndex are handled by the data layer and should not be provided.
 */
data class FunctionCreateRequest(
    val name: String,
    val groupId: Long,
    val description: String = "",
    val functionGraph: FunctionGraph,
    val inputFeatureIds: List<Long> = emptyList(),
)

/**
 * Request object for updating an existing Function.
 *
 * All fields except [id] are optional. A null value means "don't change this field".
 *
 * Note: To move a function between groups, use [MoveFeatureRequest] instead.
 */
data class FunctionUpdateRequest(
    val id: Long,
    val name: String? = null,
    val description: String? = null,
    val functionGraph: FunctionGraph? = null,
    val inputFeatureIds: List<Long>? = null,
)

/**
 * Request object for deleting a Function.
 *
 * @param functionId The ID of the function to delete.
 * @param groupId If specified, the function will only be removed from this group.
 *                If null, the function will be deleted entirely from all groups.
 */
data class FunctionDeleteRequest(
    val functionId: Long,
    val groupId: Long? = null,
)
