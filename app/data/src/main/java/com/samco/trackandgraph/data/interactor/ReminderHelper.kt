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

import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderCreateRequest
import com.samco.trackandgraph.data.database.dto.ReminderDisplayOrderData
import com.samco.trackandgraph.data.database.dto.ReminderUpdateRequest

/**
 * An interface for managing reminders. Do not use this interface directly, it is implemented by
 * the DataInteractor interface.
 *
 * The implementation of ReminderHelper will manage the complete lifecycle of reminders including
 * their serialization and persistence. It will perform all changes inside a transaction and
 * throw an exception if anything goes wrong.
 */
interface ReminderHelper {
    suspend fun getAllRemindersSync(): List<Reminder>

    suspend fun getReminderById(id: Long): Reminder?

    suspend fun createReminder(request: ReminderCreateRequest): Long

    suspend fun updateReminder(request: ReminderUpdateRequest)

    /**
     * Updates the display order of reminders within a specific group.
     *
     * This method uses a forgiving approach:
     * - Only reminders belonging to [groupId] are affected
     * - Reminders not included in [orders] retain their current display index
     * - IDs in [orders] that don't belong to [groupId] are ignored
     *
     * @param groupId The group to update, or null for ungrouped reminders
     * @param orders The new display order data for reminders in this group
     */
    suspend fun updateReminderDisplayOrder(groupId: Long?, orders: List<ReminderDisplayOrderData>)

    suspend fun deleteReminder(id: Long)

    suspend fun duplicateReminder(id: Long): Long

    suspend fun hasAnyReminders(): Boolean
}
