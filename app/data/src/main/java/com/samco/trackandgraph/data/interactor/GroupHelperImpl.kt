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

package com.samco.trackandgraph.data.interactor

import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.DeletedGroupInfo
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupCreateRequest
import com.samco.trackandgraph.data.database.dto.GroupDeleteRequest
import com.samco.trackandgraph.data.database.dto.GroupUpdateRequest
import com.samco.trackandgraph.data.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class GroupHelperImpl @Inject constructor(
    private val dao: TrackAndGraphDatabaseDao,
    @IODispatcher private val io: CoroutineDispatcher
) : GroupHelper {

    override suspend fun insertGroup(request: GroupCreateRequest): Long = withContext(io) {
        val group = com.samco.trackandgraph.data.database.entity.Group(
            id = 0L,
            name = request.name,
            displayIndex = 0,
            parentGroupId = request.parentGroupId,
            colorIndex = request.colorIndex
        )
        dao.insertGroup(group)
    }

    override suspend fun updateGroup(request: GroupUpdateRequest) = withContext(io) {
        val existingGroup = dao.getGroupById(request.id)

        val updatedGroup = existingGroup.copy(
            name = request.name ?: existingGroup.name,
            colorIndex = request.colorIndex ?: existingGroup.colorIndex
        )

        dao.updateGroup(updatedGroup)
    }

    override suspend fun deleteGroup(request: GroupDeleteRequest): DeletedGroupInfo =
        withContext(io) {
            // Get all feature ids before we delete the group
            val allFeatureIdsBeforeDelete = dao.getAllFeaturesSync().map { it.id }.toSet()

            // TODO: When multi-parent support is added, check request.parentGroupId
            // to determine if we should remove from one parent or delete entirely
            dao.deleteGroup(request.groupId)

            // Get all feature ids after deleting the group
            val allFeatureIdsAfterDelete = dao.getAllFeaturesSync().map { it.id }.toSet()
            val deletedFeatureIds = allFeatureIdsBeforeDelete.minus(allFeatureIdsAfterDelete)

            DeletedGroupInfo(deletedFeatureIds = deletedFeatureIds)
        }

    override suspend fun getGroupById(id: Long): Group = withContext(io) {
        dao.getGroupById(id).toDto()
    }

    override suspend fun getAllGroupsSync(): List<Group> = withContext(io) {
        dao.getAllGroupsSync().map { it.toDto() }
    }

    override suspend fun getGroupsForGroupSync(parentGroupId: Long): List<Group> = withContext(io) {
        dao.getGroupsForGroupSync(parentGroupId).map { it.toDto() }
    }

    override suspend fun hasAnyGroups(): Boolean = withContext(io) {
        dao.hasAnyGroups()
    }
}
