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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.HeroCardButton
import com.samco.trackandgraph.ui.compose.ui.cardElevation

@Composable
fun ReminderTypeSelectionScreen(
    onWeekDayReminderSelected: () -> Unit,
    onPeriodicReminderSelected: () -> Unit,
    onMonthDayReminderSelected: () -> Unit,
    onTimeSinceLastReminderSelected: () -> Unit,
    onDismiss: () -> Unit,
    hasAnyFeatures: Boolean = true
) {
    Column(
        modifier = Modifier.padding(horizontal = cardElevation),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.select_reminder_type),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.tngColors.onSurface
        )

        DialogInputSpacing()

        HeroCardButton(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.week_day_reminder),
            description = stringResource(R.string.week_day_reminder_description),
            onClick = onWeekDayReminderSelected
        )

        DialogInputSpacing()

        HeroCardButton(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.periodic_reminder),
            description = stringResource(R.string.periodic_reminder_description),
            onClick = onPeriodicReminderSelected
        )

        DialogInputSpacing()

        HeroCardButton(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.month_day_reminder),
            description = stringResource(R.string.month_day_reminder_description),
            onClick = onMonthDayReminderSelected
        )

        if (hasAnyFeatures) {
            DialogInputSpacing()

            HeroCardButton(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.time_since_last_reminder),
                description = stringResource(R.string.time_since_last_reminder_description),
                onClick = onTimeSinceLastReminderSelected
            )
        }

        DialogInputSpacing()

        ContinueCancelButtons(
            cancelVisible = true,
            continueVisible = false,
            cancelText = R.string.cancel,
            onCancel = onDismiss
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReminderTypeSelectionScreenPreview() {
    TnGComposeTheme {
        ReminderTypeSelectionScreen(
            onWeekDayReminderSelected = {},
            onPeriodicReminderSelected = {},
            onMonthDayReminderSelected = {},
            onTimeSinceLastReminderSelected = {},
            onDismiss = {}
        )
    }
}
