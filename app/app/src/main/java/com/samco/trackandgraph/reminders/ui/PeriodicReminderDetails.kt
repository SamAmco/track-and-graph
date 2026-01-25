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
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import org.threeten.bp.LocalDateTime

@Composable
fun PeriodicReminderDetails(
    reminderViewData: ReminderViewData.PeriodicReminderViewData,
    modifier: Modifier = Modifier
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.Start,
) {
    // Handle three cases: before start, active with next reminder, or ended
    when {
        // Case 1: Before start time - show "Starting at" with no progress bar
        reminderViewData.isBeforeStartTime -> {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = formatStartingAt(reminderViewData.starts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Case 2: Active reminder with next scheduled time - show configured period and progress
        reminderViewData.nextScheduled != null -> {
            // Show configured period using "every" plurals
            val periodText = when (reminderViewData.period) {
                Period.MINUTES -> pluralStringResource(R.plurals.every_x_minutes, reminderViewData.interval, reminderViewData.interval)
                Period.HOURS -> pluralStringResource(R.plurals.every_x_hours, reminderViewData.interval, reminderViewData.interval)
                Period.DAYS -> pluralStringResource(R.plurals.every_x_days, reminderViewData.interval, reminderViewData.interval)
                Period.WEEKS -> pluralStringResource(R.plurals.every_x_weeks, reminderViewData.interval, reminderViewData.interval)
                Period.MONTHS -> pluralStringResource(R.plurals.every_x_months, reminderViewData.interval, reminderViewData.interval)
                Period.YEARS -> pluralStringResource(R.plurals.every_x_years, reminderViewData.interval, reminderViewData.interval)
            }
            
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = periodText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Progress bar
            LinearProgressIndicator(
                progress = { reminderViewData.progressToNextReminder },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(halfDialogInputSpacing),
            )
        }
        
        // Case 3: Ended - show "Ended at" if end date exists
        else -> {
            reminderViewData.ends?.let { endDate ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = formatEndedAt(endDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
}

@Preview(showBackground = true)
@Composable
private fun PeriodicReminderDetailsActivePreview() {
    TnGComposeTheme {
        PeriodicReminderDetails(
            reminderViewData = ReminderViewData.PeriodicReminderViewData(
                id = 1L,
                displayIndex = 0,
                name = "Daily Exercise",
                nextScheduled = LocalDateTime.of(2024, 1, 15, 14, 30),
                starts = LocalDateTime.of(2024, 1, 1, 9, 0),
                ends = LocalDateTime.of(2024, 2, 15, 9, 0),
                interval = 1,
                period = Period.DAYS,
                reminderDto = null,
                progressToNextReminder = 0.7f,
                isBeforeStartTime = false
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PeriodicReminderDetailsStartingPreview() {
    TnGComposeTheme {
        PeriodicReminderDetails(
            reminderViewData = ReminderViewData.PeriodicReminderViewData(
                id = 2L,
                displayIndex = 0,
                name = "Weekly Workout",
                nextScheduled = LocalDateTime.of(2024, 1, 20, 10, 0),
                starts = LocalDateTime.of(2024, 1, 20, 10, 0),
                ends = null,
                interval = 1,
                period = Period.WEEKS,
                reminderDto = null,
                progressToNextReminder = 0f,
                isBeforeStartTime = true
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PeriodicReminderDetailsEndedPreview() {
    TnGComposeTheme {
        PeriodicReminderDetails(
            reminderViewData = ReminderViewData.PeriodicReminderViewData(
                id = 3L,
                displayIndex = 0,
                name = "Monthly Check-in",
                nextScheduled = null,
                starts = LocalDateTime.of(2023, 6, 1, 9, 0),
                ends = LocalDateTime.of(2023, 12, 31, 23, 59),
                interval = 1,
                period = Period.MONTHS,
                reminderDto = null,
                progressToNextReminder = 0f,
                isBeforeStartTime = false
            )
        )
    }
}
