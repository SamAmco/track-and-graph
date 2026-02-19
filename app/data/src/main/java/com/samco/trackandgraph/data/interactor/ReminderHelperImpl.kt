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
import com.samco.trackandgraph.data.database.ReminderDao
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderCreateRequest
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
    @IODispatcher private val io: CoroutineDispatcher,
) : ReminderHelper {

    override suspend fun getAllRemindersSync(): List<Reminder> = withContext(io) {
        reminderDao.getAllRemindersSync()
            .mapNotNull(::fromEntity)
            .sortedWith(compareBy({ it.displayIndex }, { -it.id }))
    }

    override suspend fun getReminderById(id: Long): Reminder? = withContext(io) {
        reminderDao.getReminderById(id)?.let(::fromEntity)
    }

    override suspend fun createReminder(request: ReminderCreateRequest): Long = withContext(io) {
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
        groupItemDao.insertGroupItem(groupItem)

        reminderId
    }

    override suspend fun updateReminder(request: ReminderUpdateRequest) = withContext(io) {
        val existing = reminderDao.getReminderById(request.id)
            ?: throw IllegalArgumentException("Reminder with id ${request.id} not found")

        val newParams = request.params ?: reminderSerializer.deserializeParams(existing.encodedReminderParams)
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

    override suspend fun updateReminderDisplayOrder(
        groupId: Long?,
        orders: List<ReminderDisplayOrderData>
    ) = withContext(io) {
        val orderMap = orders.associate { it.id to it.displayIndex }

        val groupItems = if (groupId != null) {
            groupItemDao.getGroupItemsForGroup(groupId)
        } else {
            groupItemDao.getGroupItemsWithNoGroup()
        }

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

    override suspend fun deleteReminder(id: Long) = withContext(io) {
        reminderDao.deleteReminder(id)
    }

    override suspend fun duplicateReminder(id: Long): Long = withContext(io) {
        val existing = reminderDao.getReminderById(id)
            ?: throw IllegalArgumentException("Reminder with id $id not found")

        // Get the existing GroupItem to find the groupId
        val existingGroupItem = groupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
            .firstOrNull()
        val groupId = existingGroupItem?.groupId

        val newEntity = existing.copy(id = 0L)
        val newReminderId = reminderDao.insertReminder(newEntity)

        // Create GroupItem for the new reminder
        val newGroupItem = GroupItem(
            groupId = groupId,
            displayIndex = 0,
            childId = newReminderId,
            type = GroupItemType.REMINDER,
            createdAt = System.currentTimeMillis()
        )
        groupItemDao.insertGroupItem(newGroupItem)

        newReminderId
    }

    override suspend fun hasAnyReminders(): Boolean = withContext(io) {
        reminderDao.hasAnyReminders()
    }

    /** Converts a ReminderEntity to a Reminder DTO. */
    private fun fromEntity(
        entity: ReminderEntity,
    ): Reminder? {
        val params = reminderSerializer.deserializeParams(entity.encodedReminderParams)
            ?: return null

        val groupItem = groupItemDao.getGroupItemsForChild(entity.id, GroupItemType.REMINDER)
            .firstOrNull()

        return Reminder(
            id = entity.id,
            displayIndex = groupItem?.displayIndex ?: 0,
            reminderName = entity.alarmName,
            groupId = groupItem?.groupId,
            featureId = entity.featureId,
            params = params
        )
    }
}
