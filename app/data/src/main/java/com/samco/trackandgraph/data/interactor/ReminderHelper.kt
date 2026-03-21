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
import com.samco.trackandgraph.data.database.dto.ComponentDeleteRequest
import com.samco.trackandgraph.data.database.dto.CreatedComponent
import com.samco.trackandgraph.data.database.dto.GroupChildDisplayIndex
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

    suspend fun createReminder(request: ReminderCreateRequest): CreatedComponent

    suspend fun updateReminder(request: ReminderUpdateRequest)

    /**
     * Updates the display order of reminders within a specific group.
     *
     * This method uses a forgiving approach:
     * - Only the order of reminders in the reminders screen is affected
     * - Reminders not included in [orders] retain their current display index
     *
     * @param orders The new display order data for reminders
     */
    suspend fun updateReminderScreenDisplayOrder(orders: List<ReminderDisplayOrderData>)

    /**
     * Deletes a reminder or removes a single placement.
     * @see [ComponentDeleteRequest] for the semantics of deleteEverywhere.
     */
    suspend fun deleteReminder(request: ComponentDeleteRequest)

    suspend fun duplicateReminder(groupItemId: Long): CreatedComponent

    suspend fun hasAnyReminders(): Boolean

    /**
     * Returns display indices for all reminders shown on the reminders screen
     * (i.e. reminders with a null groupId), as a list of [GroupChildDisplayIndex] entries
     * that include the groupItemId needed for delete/duplicate/reorder operations.
     */
    suspend fun getDisplayIndicesForRemindersScreen(): List<GroupChildDisplayIndex>
}
