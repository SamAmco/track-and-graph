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
import com.samco.trackandgraph.data.database.GroupItemDao
import com.samco.trackandgraph.data.database.ReminderDao
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderCreateRequest
import com.samco.trackandgraph.data.database.dto.ComponentDeleteRequest
import com.samco.trackandgraph.data.database.dto.CreatedComponent
import com.samco.trackandgraph.data.database.dto.GroupChildDisplayIndex
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.data.database.dto.ReminderDisplayOrderData
import com.samco.trackandgraph.data.database.dto.ReminderUpdateRequest
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
import com.samco.trackandgraph.data.database.entity.Reminder as ReminderEntity
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.serialization.ReminderSerializer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class ReminderHelperImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val groupItemDao: GroupItemDao,
    private val reminderSerializer: ReminderSerializer,
    private val transactionHelper: DatabaseTransactionHelper,
    @IODispatcher private val io: CoroutineDispatcher,
) : ReminderHelper {

    override suspend fun getAllRemindersSync(): List<Reminder> = withContext(io) {
        reminderDao.getAllRemindersSync().mapNotNull { entity ->
            fromEntity(entity)
        }
    }

    override suspend fun getRemindersForGroupSync(groupId: Long): List<Reminder> =
        withContext(io) {
            val reminderIds = groupItemDao
                .getGroupItemsByType(groupId, GroupItemType.REMINDER)
                .map(GroupItem::childId)
            if (reminderIds.isEmpty()) return@withContext emptyList()

            reminderDao.getRemindersByIdsSync(reminderIds).mapNotNull { entity ->
                fromEntity(entity)
            }
        }

    override suspend fun getReminderById(id: Long): Reminder? = withContext(io) {
        reminderDao.getReminderById(id)?.let(::fromEntity)
    }

    override suspend fun createReminder(request: ReminderCreateRequest): CreatedComponent = withContext(io) {
        transactionHelper.withTransaction {
            val encodedParams = reminderSerializer.serializeParams(request.params)
                ?: throw IllegalArgumentException("Failed to serialize reminder params")

            groupItemDao.shiftDisplayIndexesDownForNullGroup()
            request.groupId?.let(groupItemDao::shiftDisplayIndexesDown)

            val entity = ReminderEntity(
                id = 0L,
                alarmName = request.reminderName,
                featureId = request.featureId,
                encodedReminderParams = encodedParams
            )
            val reminderId = reminderDao.insertReminder(entity)

            val globalGroupItemId = groupItemDao.insertGroupItem(
                GroupItem(
                    groupId = null,
                    displayIndex = 0,
                    childId = reminderId,
                    type = GroupItemType.REMINDER,
                    createdAt = System.currentTimeMillis()
                )
            )
            val createdGroupItemId = request.groupId?.let { groupId ->
                groupItemDao.insertGroupItem(
                    GroupItem(
                        groupId = groupId,
                        displayIndex = 0,
                        childId = reminderId,
                        type = GroupItemType.REMINDER,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            CreatedComponent(
                componentId = reminderId,
                groupItemId = createdGroupItemId ?: globalGroupItemId,
            )
        }
    }

    override suspend fun updateReminder(request: ReminderUpdateRequest) = withContext(io) {
        val existing = reminderDao.getReminderById(request.id)
            ?: throw IllegalArgumentException("Reminder with id ${request.id} not found")

        val newParams =
            request.params ?: reminderSerializer.deserializeParams(existing.encodedReminderParams)
            ?: throw IllegalArgumentException("Failed to deserialize existing reminder params")

        val encodedParams = reminderSerializer.serializeParams(newParams)
            ?: throw IllegalArgumentException("Failed to serialize reminder params")

        val updatedEntity = existing.copy(
            alarmName = request.reminderName ?: existing.alarmName,
            featureId = request.featureId ?: existing.featureId,
            encodedReminderParams = encodedParams
        )
        reminderDao.updateReminder(updatedEntity)
    }

    override suspend fun updateReminderScreenDisplayOrder(orders: List<ReminderDisplayOrderData>) =
        withContext(io) {
            transactionHelper.withTransaction {
                val orderMap = orders.associate { it.id to it.displayIndex }

                val groupItems = groupItemDao.getGroupItemsWithNoGroup()
                    .filter { it.type == GroupItemType.REMINDER }

                val groupItemsToUpdate = groupItems.mapNotNull { groupItem ->
                    val newIndex = orderMap[groupItem.childId]
                    if (newIndex != null && newIndex != groupItem.displayIndex) {
                        groupItem.copy(displayIndex = newIndex)
                    } else {
                        null
                    }
                }

                groupItemsToUpdate.forEach { groupItemDao.updateGroupItem(it) }
            }
        }

    override suspend fun deleteReminder(request: ComponentDeleteRequest) = withContext(io) {
        transactionHelper.withTransaction {
            val groupItem = groupItemDao.getGroupItemById(request.groupItemId)
                ?: return@withTransaction
            val reminderId = groupItem.childId

            val groupItems = groupItemDao.getGroupItemsForChild(
                reminderId,
                GroupItemType.REMINDER
            )

            groupItems.forEach { groupItemDao.deleteGroupItem(it.id) }
            reminderDao.deleteReminder(reminderId)
        }
    }

    override suspend fun duplicateReminder(groupItemId: Long): CreatedComponent = withContext(io) {
        transactionHelper.withTransaction {
            val existingGroupItem = groupItemDao.getGroupItemById(groupItemId)
                ?: throw IllegalArgumentException("GroupItem with id $groupItemId not found")

            val reminderId = existingGroupItem.childId
            val existing = reminderDao.getReminderById(reminderId)
                ?: throw IllegalArgumentException("Reminder with id $reminderId not found")

            val existingPlacements = groupItemDao
                .getGroupItemsForChild(reminderId, GroupItemType.REMINDER)
            require(existingPlacements.count { it.groupId == null } == 1) {
                "Reminder $reminderId must have exactly one reminders-screen placement"
            }
            require(existingPlacements.count { it.groupId != null } <= 1) {
                "Reminder $reminderId cannot belong to more than one group"
            }

            existingPlacements.forEach { placement ->
                if (placement.groupId == null) {
                    groupItemDao.shiftDisplayIndexesDownAfterForNullGroup(placement.displayIndex)
                } else {
                    groupItemDao.shiftDisplayIndexesDownAfter(placement.groupId, placement.displayIndex)
                }
            }

            val newEntity = existing.copy(id = 0L)
            val newReminderId = reminderDao.insertReminder(newEntity)

            val newPlacementIds = existingPlacements.associate { placement ->
                placement.groupId to groupItemDao.insertGroupItem(
                    GroupItem(
                        groupId = placement.groupId,
                        displayIndex = placement.displayIndex + 1,
                        childId = newReminderId,
                        type = GroupItemType.REMINDER,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            val newGroupItemId = checkNotNull(newPlacementIds[existingGroupItem.groupId])

            CreatedComponent(componentId = newReminderId, groupItemId = newGroupItemId)
        }
    }

    override suspend fun hasAnyReminders(): Boolean = withContext(io) {
        reminderDao.hasAnyReminders()
    }

    override suspend fun getDisplayIndicesForRemindersScreen(): List<GroupChildDisplayIndex> =
        withContext(io) {
            groupItemDao.getGroupItemsWithNoGroup()
                .filter { it.type == GroupItemType.REMINDER }
                .map {
                    GroupChildDisplayIndex(
                        groupItemId = it.id,
                        type = GroupChildType.REMINDER,
                        id = it.childId,
                        displayIndex = it.displayIndex,
                    )
                }
        }

    /** Converts a ReminderEntity to a Reminder DTO. */
    private fun fromEntity(entity: ReminderEntity): Reminder? {
        val params = reminderSerializer.deserializeParams(entity.encodedReminderParams)
            ?: return null

        return Reminder(
            id = entity.id,
            reminderName = entity.alarmName,
            featureId = entity.featureId,
            params = params,
            unique = true,
        )
    }
}
