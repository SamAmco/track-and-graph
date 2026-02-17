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

package com.samco.trackandgraph.data.database

import com.samco.trackandgraph.data.database.entity.Reminder

/**
 * Data access interface for reminder-related operations.
 * This interface abstracts the database operations needed by ReminderHelper,
 * allowing for different implementations (Room, fake for testing, etc.)
 */
internal interface ReminderDao {

    fun getAllRemindersSync(): List<Reminder>

    fun getReminderById(id: Long): Reminder?

    fun insertReminder(reminder: Reminder): Long

    fun updateReminder(reminder: Reminder)

    fun deleteReminder(id: Long)

    fun hasAnyReminders(): Boolean
}
