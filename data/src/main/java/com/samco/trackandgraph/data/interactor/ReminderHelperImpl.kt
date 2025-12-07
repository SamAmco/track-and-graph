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
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.entity.Reminder as ReminderEntity
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.serialization.ReminderSerializer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class ReminderHelperImpl @Inject constructor(
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
        val entity = toEntity(reminder) ?: throw IllegalArgumentException("Failed to serialize reminder")
        dao.insertReminder(entity)
    }

    override suspend fun updateReminder(reminder: Reminder) = withContext(io) {
        val entity = toEntity(reminder) ?: throw IllegalArgumentException("Failed to serialize reminder")
        dao.updateReminder(entity)
    }

    override suspend fun deleteReminder(id: Long) = withContext(io) {
        dao.deleteReminder(id)
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
            displayIndex = entity.displayIndex,
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
            displayIndex = reminder.displayIndex,
            alarmName = reminder.reminderName,
            groupId = reminder.groupId,
            featureId = reminder.featureId,
            encodedReminderParams = encodedParams
        )
    }
}
