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

@file:OptIn(ExperimentalFoundationApi::class)

package com.samco.trackandgraph.reminders.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.MonthDayOccurrence
import com.samco.trackandgraph.data.database.dto.MonthDayType
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import org.threeten.bp.LocalDateTime

@Composable
fun Reminder(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    reminderViewData: ReminderViewData,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
) = Surface(
    modifier = modifier
        .fillMaxWidth()
        .padding(halfDialogInputSpacing),
    shadowElevation = if (isElevated) cardElevation * 3f else cardElevation,
    shape = MaterialTheme.shapes.medium,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (onEditClick != null) {
                    it.clickable(onClick = onEditClick)
                } else {
                    it
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = reminderViewData.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = formatNextScheduled(reminderViewData.nextScheduled),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (reminderViewData) {
                    is ReminderViewData.WeekDayReminderViewData -> {
                        DialogInputSpacing()
                        WeekDayReminderDetails(reminderViewData)
                    }
                    is ReminderViewData.PeriodicReminderViewData -> {
                        PeriodicReminderDetails(reminderViewData)
                    }
                    is ReminderViewData.MonthDayReminderViewData -> {
                        DialogInputSpacing()
                        MonthDayReminderDetails(reminderViewData)
                    }
                }
            }
        }

        if (onEditClick != null || onDeleteClick != null) {
            ReminderMenuButton(
                modifier = Modifier.align(Alignment.TopEnd),
                onEditClick = onEditClick ?: {},
                onDeleteClick = onDeleteClick ?: {},
            )
        }
    }
}

@Composable
private fun ReminderMenuButton(
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.size(buttonSize)
    ) {
        IconButton(
            modifier = Modifier.size(buttonSize),
            onClick = { showContextMenu = true },
        ) {
            Icon(
                painterResource(R.drawable.list_menu_icon),
                contentDescription = stringResource(R.string.tracked_data_menu_button_content_description),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                onClick = {
                    showContextMenu = false
                    onEditClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    showContextMenu = false
                    onDeleteClick()
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderPreview() = TnGComposeTheme {
    Column(
        modifier = Modifier
//            .width(200.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Regular reminder
        Reminder(
            reminderViewData = ReminderViewData.WeekDayReminderViewData(
                id = 1L,
                displayIndex = 0,
                name = "Morning Workout",
                nextScheduled = LocalDateTime.of(2025, 12, 16, 7, 30),
                checkedDays = CheckedDays(
                    monday = true,
                    tuesday = true,
                    wednesday = true,
                    thursday = true,
                    friday = true,
                    saturday = false,
                    sunday = false
                ),
                reminderDto = null,
            ),
        )

        // Elevated reminder
        Reminder(
            reminderViewData = ReminderViewData.WeekDayReminderViewData(
                id = 2L,
                displayIndex = 1,
                name = "Evening Meditation",
                nextScheduled = LocalDateTime.of(2025, 12, 16, 21, 0),
                checkedDays = CheckedDays.all(),
                reminderDto = null,
            ),
        )

        // Non-scheduled reminder
        Reminder(
            reminderViewData = ReminderViewData.WeekDayReminderViewData(
                id = 2L,
                displayIndex = 1,
                name = "Evening Meditation",
                nextScheduled = null,
                checkedDays = CheckedDays.none(),
                reminderDto = null,
            ),
        )

        // Active periodic reminder
        Reminder(
            reminderViewData = ReminderViewData.PeriodicReminderViewData(
                id = 3L,
                displayIndex = 2,
                name = "Daily Exercise",
                nextScheduled = LocalDateTime.of(2025, 12, 17, 14, 30),
                starts = LocalDateTime.of(2025, 12, 1, 14, 30),
                ends = LocalDateTime.of(2026, 3, 1, 14, 30),
                interval = 1,
                period = Period.DAYS,
                reminderDto = null,
                progressToNextReminder = 0.6f,
                isBeforeStartTime = false,
            ),
        )

        // Starting periodic reminder
        Reminder(
            reminderViewData = ReminderViewData.PeriodicReminderViewData(
                id = 4L,
                displayIndex = 3,
                name = "Weekly Review",
                nextScheduled = LocalDateTime.of(2025, 12, 30, 9, 0),
                starts = LocalDateTime.of(2025, 12, 30, 9, 0),
                ends = null,
                interval = 1,
                period = Period.WEEKS,
                reminderDto = null,
                progressToNextReminder = 0f,
                isBeforeStartTime = true,
            ),
        )

        // Ended periodic reminder
        Reminder(
            reminderViewData = ReminderViewData.PeriodicReminderViewData(
                id = 5L,
                displayIndex = 4,
                name = "Monthly Goals",
                nextScheduled = null,
                starts = LocalDateTime.of(2025, 6, 1, 10, 0),
                ends = LocalDateTime.of(2025, 11, 30, 10, 0),
                interval = 1,
                period = Period.MONTHS,
                reminderDto = null,
                progressToNextReminder = 0f,
                isBeforeStartTime = false,
            ),
        )

        Reminder(
            reminderViewData = ReminderViewData.MonthDayReminderViewData(
                id = 4L,
                displayIndex = 5,
                name = "Monthly Reminder",
                nextScheduled = LocalDateTime.of(2025, 6, 1, 10, 0),
                occurrence = MonthDayOccurrence.LAST,
                dayType = MonthDayType.DAY,
                ends = null,
                reminderDto = null,
            )
        )
    }
}
