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

data class Group(
    val id: Long,
    val name: String,
    val displayIndex: Int,
    val parentGroupIds: Set<Long>,
    val colorIndex: Int,
) {
    internal fun toEntity() = com.samco.trackandgraph.data.database.entity.Group(
        id,
        name,
        colorIndex,
    )
}

/**
 * Request object for creating a new Group.
 *
 * Note: id and displayIndex are handled by the data layer and should not be provided.
 */
data class GroupCreateRequest(
    val name: String,
    val parentGroupId: Long? = null,
    val colorIndex: Int = 0,
)

/**
 * Request object for updating an existing Group.
 *
 * All fields except [id] are optional. A null value means "don't change this field".
 *
 * Note: To move a group to a different parent, use [MoveGroupRequest] instead (when implemented).
 */
data class GroupUpdateRequest(
    val id: Long,
    val name: String? = null,
    val colorIndex: Int? = null,
)

/**
 * Request object for deleting a Group.
 *
 * @param groupId The ID of the group to delete.
 * @param parentGroupId If specified in the future when groups can have multiple parents,
 *                      the group will only be removed from this parent.
 *                      If null, the group will be deleted entirely.
 */
data class GroupDeleteRequest(
    val groupId: Long,
    val parentGroupId: Long? = null,
)