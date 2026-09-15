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
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.ui.halfDialogInputSpacing
import org.threeten.bp.LocalDateTime

@Composable
fun TimeSinceLastReminderDetails(
    reminderViewData: ReminderViewData.TimeSinceLastReminderViewData,
    modifier: Modifier = Modifier
) = IntervalReminderDetails(
    nextScheduled = reminderViewData.nextScheduled,
    progressToNextReminder = reminderViewData.progressToNextReminder,
    currentInterval = reminderViewData.currentInterval,
    currentPeriod = reminderViewData.currentPeriod,
    modifier = modifier,
)

@Composable
fun OneTimeReminderDetails(
    reminderViewData: ReminderViewData.OneTimeReminderViewData,
    modifier: Modifier = Modifier,
) = IntervalReminderDetails(
    nextScheduled = reminderViewData.nextScheduled,
    progressToNextReminder = reminderViewData.progressToNextReminder,
    currentInterval = reminderViewData.currentInterval,
    currentPeriod = reminderViewData.currentPeriod,
    modifier = modifier,
)

@Composable
private fun IntervalReminderDetails(
    nextScheduled: LocalDateTime?,
    progressToNextReminder: Float,
    currentInterval: Int?,
    currentPeriod: Period?,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.Start,
) {
    // Show current period text if we have a next scheduled reminder
    if (nextScheduled != null &&
        currentInterval != null &&
        currentPeriod != null
    ) {
        val periodText = when (currentPeriod) {
            Period.MINUTES -> pluralStringResource(
                R.plurals.after_x_minutes,
                currentInterval,
                currentInterval
            )
            Period.HOURS -> pluralStringResource(
                R.plurals.after_x_hours,
                currentInterval,
                currentInterval
            )
            Period.DAYS -> pluralStringResource(
                R.plurals.after_x_days,
                currentInterval,
                currentInterval
            )
            Period.WEEKS -> pluralStringResource(
                R.plurals.after_x_weeks,
                currentInterval,
                currentInterval
            )
            Period.MONTHS -> pluralStringResource(
                R.plurals.after_x_months,
                currentInterval,
                currentInterval
            )
            Period.YEARS -> pluralStringResource(
                R.plurals.after_x_years,
                currentInterval,
                currentInterval
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
        progress = { progressToNextReminder },
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
                groupItemId = 0L,
                name = "Exercise Reminder",
                enabled = true,
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
                groupItemId = 0L,
                name = "No Upcoming",
                enabled = true,
                nextScheduled = null,
                reminderDto = null,
                progressToNextReminder = 0f,
                currentInterval = null,
                currentPeriod = null,
            )
        )
    }
}
