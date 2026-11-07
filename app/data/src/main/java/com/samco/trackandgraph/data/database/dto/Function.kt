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

data class Function(
    val id: Long = 0L,
    override val featureId: Long = 0L,
    override val name: String,
    override val description: String,
    val functionGraph: FunctionGraph,
    val inputFeatureIds: List<Long>,
    val unique: Boolean,
    /** Non-null when this function is an additional output of another function's canvas. */
    val parentFunctionId: Long? = null,
    /** The output node of [functionGraph] this function evaluates; null means the primary one. */
    val outputNodeId: Int? = null,
    /** The other outputs on this function's canvas. Only populated on the owning function. */
    val additionalOutputs: List<FunctionOutput> = emptyList(),
) : Feature {
    internal fun toEntity(serializedFunctionGraph: String) =
        com.samco.trackandgraph.data.database.entity.Function(
            id = id,
            featureId = featureId,
            functionGraph = serializedFunctionGraph,
            parentFunctionId = parentFunctionId,
            outputNodeId = outputNodeId,
        )
}

/**
 * An additional output of a multi-output function. Each output is a feature in its own right so
 * that it can be graphed, searched and used as the input of other functions.
 */
data class FunctionOutput(
    val functionId: Long,
    val featureId: Long,
    val outputNodeId: Int,
    val name: String,
    val description: String,
)

/** Name and description for an additional output node when creating or updating a function. */
data class FunctionOutputRequest(
    val outputNodeId: Int,
    val name: String,
    val description: String = "",
)

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
    val additionalOutputs: List<FunctionOutputRequest> = emptyList(),
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
    /** Null leaves the existing additional outputs unchanged (apart from removing deleted nodes). */
    val additionalOutputs: List<FunctionOutputRequest>? = null,
)

