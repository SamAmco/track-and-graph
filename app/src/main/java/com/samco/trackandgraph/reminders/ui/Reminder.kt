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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

        IconButton(
            onClick = onDeleteClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.delete_icon),
                contentDescription = stringResource(id = R.string.delete_reminder_content_description),
                tint = MaterialTheme.colorScheme.onSurface
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
            onDeleteClick = {}
        )
    }
}
