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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter

@Composable
fun Reminder(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    reminderViewData: ReminderViewData,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) = Surface(
    modifier = modifier
        .fillMaxWidth()
        .padding(halfDialogInputSpacing),
    shadowElevation = if (isElevated) cardElevation * 3f else cardElevation,
    shape = MaterialTheme.shapes.small,
) {
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm")
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = reminderViewData.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = reminderViewData.time.format(timeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCheckedDays(reminderViewData.checkedDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ReminderMenuButton(
            modifier = Modifier.align(Alignment.TopEnd),
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick
        )
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

@Composable
private fun formatCheckedDays(checkedDays: CheckedDays): String {
    val dayLabels = listOf(
        stringResource(id = R.string.mon),
        stringResource(id = R.string.tue),
        stringResource(id = R.string.wed),
        stringResource(id = R.string.thu),
        stringResource(id = R.string.fri),
        stringResource(id = R.string.sat),
        stringResource(id = R.string.sun)
    )

    val activeDays = checkedDays.toList()
        .mapIndexedNotNull { index, isChecked ->
            if (isChecked) dayLabels.getOrNull(index) else null
        }

    return if (activeDays.isNotEmpty()) {
        activeDays.joinToString(", ")
    } else ""
}


@Preview(showBackground = true)
@Composable
private fun ReminderPreview() = TnGComposeTheme {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Regular reminder
        Reminder(
            reminderViewData = ReminderViewData(
                id = 1L,
                displayIndex = 0,
                name = "Morning Workout",
                time = LocalTime.of(7, 30),
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
            onEditClick = {},
            onDeleteClick = {}
        )

        // Elevated reminder
        Reminder(
            isElevated = true,
            reminderViewData = ReminderViewData(
                id = 2L,
                displayIndex = 1,
                name = "Evening Meditation",
                time = LocalTime.of(21, 0),
                checkedDays = CheckedDays.all(),
                reminderDto = null,
            ),
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}
