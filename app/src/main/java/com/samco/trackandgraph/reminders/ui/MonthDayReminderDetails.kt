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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.data.database.dto.MonthDayOccurrence
import com.samco.trackandgraph.data.database.dto.MonthDayType
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import org.threeten.bp.LocalDateTime

@Composable
fun MonthDayReminderDetails(
    reminderViewData: ReminderViewData.MonthDayReminderViewData,
    modifier: Modifier = Modifier,
    chipScale: Float = 0.65f
) = Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    val occurrenceText = when (reminderViewData.occurrence) {
        MonthDayOccurrence.FIRST -> "First"
        MonthDayOccurrence.SECOND -> "Second"
        MonthDayOccurrence.THIRD -> "Third"
        MonthDayOccurrence.FOURTH -> "Fourth"
        MonthDayOccurrence.LAST -> "Last"
    }

    val dayTypeText = when (reminderViewData.dayType) {
        MonthDayType.DAY -> "Day"
        MonthDayType.MONDAY -> "Monday"
        MonthDayType.TUESDAY -> "Tuesday"
        MonthDayType.WEDNESDAY -> "Wednesday"
        MonthDayType.THURSDAY -> "Thursday"
        MonthDayType.FRIDAY -> "Friday"
        MonthDayType.SATURDAY -> "Saturday"
        MonthDayType.SUNDAY -> "Sunday"
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(
            (cardPadding * chipScale),
            Alignment.CenterHorizontally
        ),
        verticalArrangement = Arrangement.spacedBy(
            (cardPadding * chipScale),
            Alignment.CenterVertically
        ),
    ) {
        Text(
            text = "Every:",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = occurrenceText,
            style = MaterialTheme.typography.titleSmall,
            textDecoration = TextDecoration.Underline,
        )
        Text(
            text = dayTypeText,
            style = MaterialTheme.typography.titleSmall,
            textDecoration = TextDecoration.Underline,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MonthDayReminderDetailsPreview() {
    TnGComposeTheme {
        MonthDayReminderDetails(
            reminderViewData = ReminderViewData.MonthDayReminderViewData(
                id = 1L,
                displayIndex = 0,
                name = "Monthly Report",
                nextScheduled = LocalDateTime.of(2025, 1, 6, 9, 0),
                occurrence = MonthDayOccurrence.FIRST,
                dayType = MonthDayType.MONDAY,
                ends = null,
                reminderDto = null,
            )
        )
    }
}
