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

    suspend fun updateReminders(reminders: List<Reminder>)

    suspend fun hasAnyReminders(): Boolean
}
