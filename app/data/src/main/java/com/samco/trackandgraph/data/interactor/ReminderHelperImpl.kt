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
import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.LayoutItemType
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.entity.LayoutItem
import com.samco.trackandgraph.data.database.entity.Reminder as ReminderEntity
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.serialization.ReminderSerializer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class ReminderHelperImpl @Inject constructor(
    private val transactionHelper: DatabaseTransactionHelper,
    private val dao: TrackAndGraphDatabaseDao,
    private val reminderSerializer: ReminderSerializer,
    @IODispatcher private val io: CoroutineDispatcher,
) : ReminderHelper {

    override suspend fun getAllRemindersSync(): List<Reminder> = withContext(io) {
        dao.getAllRemindersSync().mapNotNull(::fromEntity)
    }

    override suspend fun getReminderById(id: Long): Reminder? = withContext(io) {
        dao.getReminderById(id)?.let(::fromEntity)
    }

    override suspend fun insertReminder(reminder: Reminder): Long = withContext(io) {
        transactionHelper.withTransaction {
            val entity = toEntity(reminder) ?: throw IllegalArgumentException("Failed to serialize reminder")
            val reminderId = dao.insertReminder(entity)

            // Insert layout item at top of the reminders group (groupId = -1)
            shiftUpGroupChildIndexes(REMINDER_LAYOUT_GROUP_ID)
            dao.insertLayoutItem(
                LayoutItem(
                    id = 0,
                    groupId = REMINDER_LAYOUT_GROUP_ID,
                    type = LayoutItemType.REMINDER,
                    itemId = reminderId,
                    displayIndex = 0
                )
            )

            reminderId
        }
    }

    private fun shiftUpGroupChildIndexes(groupId: Long) {
        val layoutItems = dao.getLayoutItemsForGroup(groupId)
        val updates = layoutItems.map { it.copy(displayIndex = it.displayIndex + 1) }
        dao.updateLayoutItems(updates)
    }

    override suspend fun updateReminder(reminder: Reminder) = withContext(io) {
        val entity = toEntity(reminder) ?: throw IllegalArgumentException("Failed to serialize reminder")
        dao.updateReminder(entity)
    }

    override suspend fun deleteReminder(id: Long) = withContext(io) {
        transactionHelper.withTransaction {
            dao.deleteLayoutItemByItemIdAndType(id, LayoutItemType.REMINDER)
            dao.deleteReminder(id)
        }
    }

    override suspend fun duplicateReminder(reminder: Reminder): Long = withContext(io) {
        insertReminder(reminder.copy(id = 0L))
    }

    override suspend fun hasAnyReminders(): Boolean = withContext(io) {
        dao.hasAnyReminders()
    }

    /** Converts a ReminderEntity to a Reminder DTO. */
    private fun fromEntity(
        entity: ReminderEntity,
    ): Reminder? {
        val params = reminderSerializer.deserializeParams(entity.encodedReminderParams)
            ?: return null

        return Reminder(
            id = entity.id,
            reminderName = entity.alarmName,
            groupId = entity.groupId,
            featureId = entity.featureId,
            params = params
        )
    }

    /** Converts a Reminder DTO to a ReminderEntity. */
    private fun toEntity(
        reminder: Reminder,
    ): ReminderEntity? {
        val encodedParams = reminderSerializer.serializeParams(reminder.params)
            ?: return null

        return ReminderEntity(
            id = reminder.id,
            alarmName = reminder.reminderName,
            groupId = reminder.groupId,
            featureId = reminder.featureId,
            encodedReminderParams = encodedParams
        )
    }

    companion object {
        // Reminders use a special group ID of -1 in the layout table since they
        // are displayed in a separate section rather than within regular groups
        private const val REMINDER_LAYOUT_GROUP_ID = -1L
    }
}
