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

    private fun isReminderUnique(reminderId: Long) =
        groupItemDao.getGroupItemsForChild(reminderId, GroupItemType.REMINDER).size == 1

    override suspend fun getAllRemindersSync(): List<Reminder> = withContext(io) {
        reminderDao.getAllRemindersSync().mapNotNull { entity ->
            fromEntity(entity, unique = isReminderUnique(entity.id))
        }
    }

    override suspend fun getReminderById(id: Long): Reminder? = withContext(io) {
        reminderDao.getReminderById(id)?.let { fromEntity(it, unique = isReminderUnique(it.id)) }
    }

    override suspend fun createReminder(request: ReminderCreateRequest): CreatedComponent = withContext(io) {
        transactionHelper.withTransaction {
            val encodedParams = reminderSerializer.serializeParams(request.params)
                ?: throw IllegalArgumentException("Failed to serialize reminder params")

            // Shift existing reminders via GroupItem
            if (request.groupId != null) {
                groupItemDao.shiftDisplayIndexesDown(request.groupId)
            } else {
                groupItemDao.shiftDisplayIndexesDownForNullGroup()
            }

            val entity = ReminderEntity(
                id = 0L,
                alarmName = request.reminderName,
                featureId = request.featureId,
                encodedReminderParams = encodedParams
            )
            val reminderId = reminderDao.insertReminder(entity)

            val groupItem = GroupItem(
                groupId = request.groupId,
                displayIndex = 0,
                childId = reminderId,
                type = GroupItemType.REMINDER,
                createdAt = System.currentTimeMillis()
            )
            val groupItemId = groupItemDao.insertGroupItem(groupItem)

            CreatedComponent(componentId = reminderId, groupItemId = groupItemId)
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

            if (!request.deleteEverywhere && groupItems.size > 1) {
                groupItemDao.deleteGroupItem(request.groupItemId)
                return@withTransaction
            }

            // Delete all GroupItems and the reminder itself
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

            val groupId = existingGroupItem.groupId
            val insertAtIndex = existingGroupItem.displayIndex + 1

            // Shift items after the original down to make room
            if (groupId != null) {
                groupItemDao.shiftDisplayIndexesDownAfter(groupId, insertAtIndex - 1)
            } else {
                groupItemDao.shiftDisplayIndexesDownAfterForNullGroup(insertAtIndex - 1)
            }

            val newEntity = existing.copy(id = 0L)
            val newReminderId = reminderDao.insertReminder(newEntity)

            // Create GroupItem for the new reminder, right after the original
            val newGroupItem = GroupItem(
                groupId = groupId,
                displayIndex = insertAtIndex,
                childId = newReminderId,
                type = GroupItemType.REMINDER,
                createdAt = System.currentTimeMillis()
            )
            val newGroupItemId = groupItemDao.insertGroupItem(newGroupItem)

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
    private fun fromEntity(
        entity: ReminderEntity,
        unique: Boolean,
    ): Reminder? {
        val params = reminderSerializer.deserializeParams(entity.encodedReminderParams)
            ?: return null

        return Reminder(
            id = entity.id,
            reminderName = entity.alarmName,
            featureId = entity.featureId,
            params = params,
            unique = unique,
        )
    }
}
