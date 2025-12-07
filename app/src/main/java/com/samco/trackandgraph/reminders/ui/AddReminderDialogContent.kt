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

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelDialogContent
import org.threeten.bp.LocalTime

@Composable
fun AddReminderDialogContent(
    modifier: Modifier = Modifier,
    onAddReminder: (Reminder) -> Unit,
    onDismiss: () -> Unit,
) {
    ContinueCancelDialogContent(
        modifier = modifier,
        content = { Text("Add Reminder") },
        onConfirm = {
            onAddReminder(
                // Create new reminder with predefined configuration
                Reminder(
                    id = 0L, // Will be assigned by database
                    displayIndex = 0, // Insert at top
                    reminderName = "New Reminder",
                    groupId = null,
                    featureId = null,
                    params = ReminderParams.WeekDayParams(
                        time = LocalTime.of(9, 0), // Default to 9:00 AM
                        checkedDays = CheckedDays.all() // Default to all days
                    ),
                )
            )
        },
        onDismissRequest = onDismiss
    )
}