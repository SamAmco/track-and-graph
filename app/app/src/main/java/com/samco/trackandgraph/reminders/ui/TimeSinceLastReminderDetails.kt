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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.Period
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
    // Show current period text if we have a next scheduled reminder
    if (reminderViewData.nextScheduled != null &&
        reminderViewData.currentInterval != null &&
        reminderViewData.currentPeriod != null
    ) {
        val periodText = when (reminderViewData.currentPeriod) {
            Period.MINUTES -> pluralStringResource(
                R.plurals.after_x_minutes,
                reminderViewData.currentInterval,
                reminderViewData.currentInterval
            )
            Period.HOURS -> pluralStringResource(
                R.plurals.after_x_hours,
                reminderViewData.currentInterval,
                reminderViewData.currentInterval
            )
            Period.DAYS -> pluralStringResource(
                R.plurals.after_x_days,
                reminderViewData.currentInterval,
                reminderViewData.currentInterval
            )
            Period.WEEKS -> pluralStringResource(
                R.plurals.after_x_weeks,
                reminderViewData.currentInterval,
                reminderViewData.currentInterval
            )
            Period.MONTHS -> pluralStringResource(
                R.plurals.after_x_months,
                reminderViewData.currentInterval,
                reminderViewData.currentInterval
            )
            Period.YEARS -> pluralStringResource(
                R.plurals.after_x_years,
                reminderViewData.currentInterval,
                reminderViewData.currentInterval
            )
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = periodText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

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
                currentInterval = 2,
                currentPeriod = Period.DAYS,
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeSinceLastReminderDetailsNoScheduledPreview() {
    TnGComposeTheme {
        TimeSinceLastReminderDetails(
            reminderViewData = ReminderViewData.TimeSinceLastReminderViewData(
                id = 2L,
                displayIndex = 0,
                name = "No Upcoming",
                nextScheduled = null,
                reminderDto = null,
                progressToNextReminder = 0f,
                currentInterval = null,
                currentPeriod = null,
            )
        )
    }
}
