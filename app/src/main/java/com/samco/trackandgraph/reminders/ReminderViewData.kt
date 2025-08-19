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

package com.samco.trackandgraph.reminders

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.threeten.bp.LocalTime

/**
 * UI-specific wrapper for Reminder DTO that contains mutable state objects
 * for direct UI updates in Compose components.
 */
data class ReminderViewData(
    val id: Long,
    val displayIndex: Int,
    val name: MutableState<TextFieldValue>,
    val time: MutableState<LocalTime>,
    val checkedDays: MutableState<CheckedDays>
) {
    /**
     * Flow that emits whenever any of the mutable states change
     */
    val stateChanges: Flow<Unit> = merge(
        name.stateEvents(),
        time.stateEvents(),
        checkedDays.stateEvents()
    )

    private fun <T> MutableState<T>.stateEvents(): Flow<Unit> = snapshotFlow { value }.map { }

    companion object {
        /**
         * Creates a ReminderViewData from a Reminder DTO
         */
        fun fromReminder(reminder: Reminder): ReminderViewData {
            return ReminderViewData(
                id = reminder.id,
                displayIndex = reminder.displayIndex,
                name = mutableStateOf(TextFieldValue(reminder.alarmName)),
                time = mutableStateOf(reminder.time),
                checkedDays = mutableStateOf(reminder.checkedDays)
            )
        }
    }

    /**
     * Converts this ReminderViewData back to a Reminder DTO
     */
    fun toReminder(): Reminder {
        return Reminder(
            id = id,
            displayIndex = displayIndex,
            alarmName = name.value.text,
            time = time.value,
            checkedDays = checkedDays.value
        )
    }
}
