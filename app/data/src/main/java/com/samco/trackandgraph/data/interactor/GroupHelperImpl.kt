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

import com.samco.trackandgraph.data.database.GroupItemDao
import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.DeletedGroupInfo
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupCreateRequest
import com.samco.trackandgraph.data.database.dto.GroupDeleteRequest
import com.samco.trackandgraph.data.database.dto.GroupUpdateRequest
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
import com.samco.trackandgraph.data.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class GroupHelperImpl @Inject constructor(
    private val dao: TrackAndGraphDatabaseDao,
    private val groupItemDao: GroupItemDao,
    @IODispatcher private val io: CoroutineDispatcher
) : GroupHelper {

    override suspend fun insertGroup(request: GroupCreateRequest): Long = withContext(io) {
        val group = com.samco.trackandgraph.data.database.entity.Group(
            id = 0L,
            name = request.name,
            colorIndex = request.colorIndex
        )
        val groupId = dao.insertGroup(group)

        // Create GroupItem entry for this group in its parent
        if (request.parentGroupId != null) {
            groupItemDao.shiftDisplayIndexesDown(request.parentGroupId)
        } else {
            groupItemDao.shiftDisplayIndexesDownForNullGroup()
        }

        val groupItem = GroupItem(
            groupId = request.parentGroupId,
            displayIndex = 0,
            childId = groupId,
            type = GroupItemType.GROUP,
            createdAt = System.currentTimeMillis()
        )
        groupItemDao.insertGroupItem(groupItem)

        groupId
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
        val entity = dao.getGroupById(id)
        val groupItems = groupItemDao.getGroupItemsForChild(id, GroupItemType.GROUP)
        val parentGroupIds = groupItems.mapNotNull { it.groupId }.toSet()
        val displayIndex = groupItems.firstOrNull()?.displayIndex ?: 0
        entity.toDto(parentGroupIds, displayIndex)
    }

    override suspend fun getAllGroupsSync(): List<Group> = withContext(io) {
        dao.getAllGroupsSync().map { entity ->
            val groupItems = groupItemDao.getGroupItemsForChild(entity.id, GroupItemType.GROUP)
            val parentGroupIds = groupItems.mapNotNull { it.groupId }.toSet()
            val displayIndex = groupItems.firstOrNull()?.displayIndex ?: 0
            entity.toDto(parentGroupIds, displayIndex)
        }
    }

    override suspend fun getGroupsForGroupSync(parentGroupId: Long): List<Group> = withContext(io) {
        dao.getGroupsForGroupSync(parentGroupId).map { entity ->
            val groupItems = groupItemDao.getGroupItemsForChild(entity.id, GroupItemType.GROUP)
            val parentGroupIds = groupItems.mapNotNull { it.groupId }.toSet()
            val displayIndex = groupItems.firstOrNull()?.displayIndex ?: 0
            entity.toDto(parentGroupIds, displayIndex)
        }
    }

    override suspend fun hasAnyGroups(): Boolean = withContext(io) {
        dao.hasAnyGroups()
    }
}
