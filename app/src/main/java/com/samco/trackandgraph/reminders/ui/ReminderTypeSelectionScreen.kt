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
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = cardElevation),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Select a Reminder Type",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.tngColors.onSurface
        )

        DialogInputSpacing()

        HeroCardButton(
            modifier = Modifier.fillMaxWidth(),
            title = "Week Day Reminder",
            description = "Set reminders for specific days of the week, e.g. every Monday, Wednesday, and Friday.",
            onClick = onWeekDayReminderSelected
        )

        DialogInputSpacing()

        HeroCardButton(
            modifier = Modifier.fillMaxWidth(),
            title = "Periodic Reminder",
            description = "Repeat at regular intervals, e.g. every 3 days or every 2 weeks.",
            onClick = onPeriodicReminderSelected
        )

        DialogInputSpacing()

        HeroCardButton(
            modifier = Modifier.fillMaxWidth(),
            title = "Month Day Reminder",
            description = "Set reminders for specific days of the month, e.g. the first Monday or the last day.",
            onClick = onMonthDayReminderSelected
        )

        DialogInputSpacing()

        HeroCardButton(
            modifier = Modifier.fillMaxWidth(),
            title = "Time Since Last Reminder",
            description = "Get reminded when you haven't tracked for a period of time.",
            onClick = onTimeSinceLastReminderSelected
        )

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
