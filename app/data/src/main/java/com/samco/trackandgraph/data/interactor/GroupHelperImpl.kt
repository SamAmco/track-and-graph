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

import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.GroupDao
import com.samco.trackandgraph.data.database.GroupItemDao
import com.samco.trackandgraph.data.database.dto.DeletedGroupInfo
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupChildDisplayIndex
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.data.database.dto.GroupCreateRequest
import com.samco.trackandgraph.data.database.dto.GroupDeleteRequest
import com.samco.trackandgraph.data.database.dto.GroupUpdateRequest
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.time.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class GroupHelperImpl @Inject constructor(
    private val groupDao: GroupDao,
    private val groupItemDao: GroupItemDao,
    private val timeProvider: TimeProvider,
    private val transactionHelper: DatabaseTransactionHelper,
    @IODispatcher private val io: CoroutineDispatcher
) : GroupHelper {

    override suspend fun insertGroup(request: GroupCreateRequest): Long = withContext(io) {
        transactionHelper.withTransaction {
            val group = com.samco.trackandgraph.data.database.entity.Group(
                id = 0L,
                name = request.name,
                colorIndex = request.colorIndex
            )
            val groupId = groupDao.insertGroup(group)

            groupItemDao.shiftDisplayIndexesDown(request.parentGroupId)

            val groupItem = GroupItem(
                groupId = request.parentGroupId,
                displayIndex = 0,
                childId = groupId,
                type = GroupItemType.GROUP,
                createdAt = timeProvider.epochMilli(),
            )
            groupItemDao.insertGroupItem(groupItem)

            groupId
        }
    }

    override suspend fun updateGroup(request: GroupUpdateRequest) = withContext(io) {
        val existingGroup = groupDao.getGroupById(request.id) ?: return@withContext

        val updatedGroup = existingGroup.copy(
            name = request.name ?: existingGroup.name,
            colorIndex = request.colorIndex ?: existingGroup.colorIndex
        )

        groupDao.updateGroup(updatedGroup)
    }

    override suspend fun deleteGroup(request: GroupDeleteRequest): DeletedGroupInfo =
        withContext(io) {
            return@withContext transactionHelper.withTransaction {
                val group = groupDao.getGroupById(request.groupId)
                    ?: return@withTransaction DeletedGroupInfo(emptySet())

                val groupItems = groupItemDao.getGroupItemsForChild(
                    group.id,
                    GroupItemType.GROUP
                )

                if (request.parentGroupId != null && groupItems.size > 1) {
                    groupItems
                        .filter { it.groupId == request.parentGroupId }
                        .forEach { groupItemDao.deleteGroupItem(it.id) }
                    return@withTransaction DeletedGroupInfo(emptySet())
                }

                // Collect all items to delete recursively
                val itemsToDelete = CollectedDeletions()
                collectDeletionsRecursively(request.groupId, itemsToDelete)

                // Perform deletions - delete group item entries first, then entities
                itemsToDelete.groupItemIds.forEach { groupItemDao.deleteGroupItem(it) }
                itemsToDelete.graphIds.forEach { groupDao.deleteGraphOrStat(it) }
                itemsToDelete.reminderIds.forEach { groupDao.deleteReminder(it) }
                itemsToDelete.featureIds.forEach { groupDao.deleteFeature(it) }
                itemsToDelete.groupIds.forEach { groupDao.deleteGroup(it) }

                return@withTransaction DeletedGroupInfo(itemsToDelete.featureIds)
            }
        }

    /**
     * In-memory representation of the group hierarchy for efficient traversal.
     */
    private class GroupHierarchy(allItems: List<GroupItem>) {
        // groupId -> items in that group
        val childrenOf: Map<Long, List<GroupItem>> = allItems.groupBy { it.groupId ?: -1 }

        // (childId, type) -> all group_items for that child
        val parentsOf: Map<Pair<Long, GroupItemType>, List<GroupItem>> =
            allItems.groupBy { it.childId to it.type }

        fun getChildren(groupId: Long): List<GroupItem> = childrenOf[groupId] ?: emptyList()

        fun getParents(childId: Long, type: GroupItemType): List<GroupItem> =
            parentsOf[childId to type] ?: emptyList()
    }

    private data class CollectedDeletions(
        val groupItemIds: MutableSet<Long> = mutableSetOf(),
        val featureIds: MutableSet<Long> = mutableSetOf(),
        val graphIds: MutableSet<Long> = mutableSetOf(),
        val reminderIds: MutableSet<Long> = mutableSetOf(),
        val groupIds: MutableSet<Long> = mutableSetOf()
    )

    private fun collectDeletionsRecursively(rootGroupId: Long, collected: CollectedDeletions) {
        // Load entire hierarchy into memory (small dataset - max hundreds of items)
        val hierarchy = GroupHierarchy(groupItemDao.getAllGroupItems())

        // Step 1: Find all groups to delete using DFS
        collectGroupsToDelete(rootGroupId, hierarchy, collected.groupIds)

        // Step 2: Collect all items in deleted groups
        for (groupId in collected.groupIds) {
            for (item in hierarchy.getChildren(groupId)) {
                collected.groupItemIds.add(item.id)

                when (item.type) {
                    GroupItemType.TRACKER -> {
                        if (shouldDeleteChild(item.childId, item.type, hierarchy, collected.groupIds)) {
                            val tracker = groupDao.getTrackerById(item.childId)
                            if (tracker != null) {
                                collected.featureIds.add(tracker.featureId)
                            }
                            collectAllGroupItemsForChild(item.childId, item.type, hierarchy, collected.groupItemIds)
                        }
                    }

                    GroupItemType.FUNCTION -> {
                        if (shouldDeleteChild(item.childId, item.type, hierarchy, collected.groupIds)) {
                            val function = groupDao.getFunctionById(item.childId)
                            if (function != null) {
                                collected.featureIds.add(function.featureId)
                            }
                            collectAllGroupItemsForChild(item.childId, item.type, hierarchy, collected.groupItemIds)
                        }
                    }

                    GroupItemType.GRAPH -> {
                        if (shouldDeleteChild(item.childId, item.type, hierarchy, collected.groupIds)) {
                            collected.graphIds.add(item.childId)
                            collectAllGroupItemsForChild(item.childId, item.type, hierarchy, collected.groupItemIds)
                        }
                    }

                    GroupItemType.REMINDER -> {
                        if (shouldDeleteChild(item.childId, item.type, hierarchy, collected.groupIds)) {
                            collected.reminderIds.add(item.childId)
                            collectAllGroupItemsForChild(item.childId, item.type, hierarchy, collected.groupItemIds)
                        }
                    }

                    GroupItemType.GROUP -> {
                        // Groups already handled in step 1
                    }
                }
            }
        }

        // Step 3: Collect group_item entries for the deleted groups themselves
        for (groupId in collected.groupIds) {
            for (item in hierarchy.getParents(groupId, GroupItemType.GROUP)) {
                collected.groupItemIds.add(item.id)
            }
        }
    }

    /**
     * DFS to collect all groups that should be deleted.
     * A group is deleted if ALL its parent occurrences are in groups being deleted.
     */
    private fun collectGroupsToDelete(
        groupId: Long,
        hierarchy: GroupHierarchy,
        deletedGroups: MutableSet<Long>
    ) {
        deletedGroups.add(groupId)

        for (item in hierarchy.getChildren(groupId)) {
            if (item.type == GroupItemType.GROUP && item.childId !in deletedGroups) {
                val allParents = hierarchy.getParents(item.childId, GroupItemType.GROUP)
                val allParentsDeleted = allParents.all { it.groupId in deletedGroups }

                if (allParentsDeleted) {
                    collectGroupsToDelete(item.childId, hierarchy, deletedGroups)
                }
            }
        }
    }

    /**
     * Returns true if a child item should be fully deleted (all its occurrences are in deleted groups).
     */
    private fun shouldDeleteChild(
        childId: Long,
        type: GroupItemType,
        hierarchy: GroupHierarchy,
        deletedGroups: Set<Long>
    ): Boolean {
        return hierarchy.getParents(childId, type).all { it.groupId in deletedGroups }
    }

    /**
     * Collects all group_item IDs for a given child.
     */
    private fun collectAllGroupItemsForChild(
        childId: Long,
        type: GroupItemType,
        hierarchy: GroupHierarchy,
        groupItemIds: MutableSet<Long>
    ) {
        hierarchy.getParents(childId, type).forEach { groupItemIds.add(it.id) }
    }

    override suspend fun getGroupById(id: Long): Group? = withContext(io) {
        groupDao.getGroupById(id)?.toDto()
    }

    override suspend fun getAllGroupsSync(): List<Group> = withContext(io) {
        groupDao.getAllGroupsSync().map { it.toDto() }
    }

    override suspend fun getGroupsForGroupSync(parentGroupId: Long): List<Group> = withContext(io) {
        groupDao.getGroupsForGroupSync(parentGroupId).map { it.toDto() }
    }

    override suspend fun hasAnyGroups(): Boolean = withContext(io) {
        groupDao.hasAnyGroups()
    }

    override suspend fun getDisplayIndicesForGroup(groupId: Long): List<GroupChildDisplayIndex> =
        withContext(io) {
            groupItemDao.getGroupItemsForGroup(groupId).map { item ->
                val type = when (item.type) {
                    GroupItemType.GROUP -> GroupChildType.GROUP
                    GroupItemType.GRAPH -> GroupChildType.GRAPH
                    GroupItemType.TRACKER -> GroupChildType.TRACKER
                    GroupItemType.FUNCTION -> GroupChildType.FUNCTION
                    GroupItemType.REMINDER -> GroupChildType.REMINDER
                }
                GroupChildDisplayIndex(
                    id = item.childId,
                    type = type,
                    displayIndex = item.displayIndex
                )
            }
        }

    override suspend fun updateGroupChildOrder(groupId: Long, children: List<GroupChildDisplayIndex>) =
        transactionHelper.withTransaction {
            val groupItems = groupItemDao.getGroupItemsForGroup(groupId)
            val newIndices = children.associateBy { Pair(it.type, it.id) }

            val updates = groupItems.mapNotNull { groupItem ->
                val childType = when (groupItem.type) {
                    GroupItemType.GROUP -> GroupChildType.GROUP
                    GroupItemType.GRAPH -> GroupChildType.GRAPH
                    GroupItemType.TRACKER -> GroupChildType.TRACKER
                    GroupItemType.FUNCTION -> GroupChildType.FUNCTION
                    GroupItemType.REMINDER -> GroupChildType.REMINDER
                }
                val newDisplayIndex = newIndices[Pair(childType, groupItem.childId)]
                    ?.displayIndex
                    ?: return@mapNotNull null
                if (newDisplayIndex >= 0 && newDisplayIndex != groupItem.displayIndex) {
                    groupItem.copy(displayIndex = newDisplayIndex)
                } else {
                    null
                }
            }

            updates.forEach { groupItemDao.updateGroupItem(it) }
        }
}
