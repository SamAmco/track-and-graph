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

package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.ReminderDao
import com.samco.trackandgraph.data.database.entity.Reminder

/**
 * A fake in-memory implementation of [ReminderDao] for testing purposes.
 * This allows tests to run without a real database and without mocking frameworks.
 */
internal class FakeReminderDao : ReminderDao {

    private var nextId = 1L
    private val reminders = mutableMapOf<Long, Reminder>()

    override fun getAllRemindersSync(): List<Reminder> {
        return reminders.values.toList()
    }

    override fun getReminderById(id: Long): Reminder? {
        return reminders[id]
    }

    override fun insertReminder(reminder: Reminder): Long {
        val id = if (reminder.id == 0L) nextId++ else reminder.id
        reminders[id] = reminder.copy(id = id)
        return id
    }

    override fun updateReminder(reminder: Reminder) {
        reminders[reminder.id] = reminder
    }

    override fun deleteReminder(id: Long) {
        reminders.remove(id)
    }

    override fun hasAnyReminders(): Boolean = reminders.isNotEmpty()

    // =========================================================================
    // Test helper methods
    // =========================================================================

    fun clear() {
        reminders.clear()
        nextId = 1L
    }

    fun getAll(): List<Reminder> = reminders.values.toList()
}
