/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.reminders.ui

import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import org.threeten.bp.LocalDateTime

/**
 * Sealed class for view data with 1-to-1 mapping to ReminderParams types.
 * Simplified view data for displaying reminders without mutable state.
 * Since we removed in-place editing, this is now just a read-only
 * representation.
 */
sealed class ReminderViewData {
    abstract val id: Long
    abstract val displayIndex: Int
    abstract val name: String
    abstract val reminderDto: Reminder?

    /**
     * View data for weekly reminders, mapping to ReminderParams.WeekDayParams
     */
    data class WeekDayReminderViewData(
        override val id: Long,
        override val displayIndex: Int,
        override val name: String,
        val nextScheduled: LocalDateTime?,
        val checkedDays: CheckedDays,
        override val reminderDto: Reminder?,
    ) : ReminderViewData()

    companion object {
        /** Creates a ReminderViewData from a Reminder DTO */
        fun fromReminder(reminder: Reminder, nextScheduled: LocalDateTime?): ReminderViewData {
            return when (val params = reminder.params) {
                is ReminderParams.WeekDayParams -> {
                    WeekDayReminderViewData(
                        id = reminder.id,
                        displayIndex = reminder.displayIndex,
                        name = reminder.reminderName,
                        nextScheduled = nextScheduled,
                        checkedDays = params.checkedDays,
                        reminderDto = reminder,
                    )
                }
            }
        }
    }
}