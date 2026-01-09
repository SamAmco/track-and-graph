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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import org.threeten.bp.LocalDateTime

@Composable
fun TimeSinceLastReminderDetails(
    reminderViewData: ReminderViewData.TimeSinceLastReminderViewData,
    modifier: Modifier = Modifier
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.Start,
) {
    LinearProgressIndicator(
        progress = { reminderViewData.progressToNextReminder },
        modifier = Modifier
            .fillMaxWidth()
            .padding(halfDialogInputSpacing),
    )
}

@Preview(showBackground = true)
@Composable
private fun TimeSinceLastReminderDetailsPreview() {
    TnGComposeTheme {
        TimeSinceLastReminderDetails(
            reminderViewData = ReminderViewData.TimeSinceLastReminderViewData(
                id = 1L,
                displayIndex = 0,
                name = "Exercise Reminder",
                nextScheduled = LocalDateTime.of(2025, 12, 22, 14, 0),
                reminderDto = null,
                progressToNextReminder = 0.7f,
            )
        )
    }
}
