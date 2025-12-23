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

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.Locale

@Composable
fun formatNextScheduled(nextScheduled: LocalDateTime?): String {
    return if (nextScheduled != null) {
        val formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
        val dateTime = nextScheduled.format(formatter)
        stringResource(R.string.next_reminder_format, dateTime)
    } else {
        stringResource(R.string.no_upcoming_reminders)
    }
}

// TODO extract these strings
@Composable
fun formatEndedAt(endDateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
    val dateTime = endDateTime.format(formatter)
    return "Ended at: $dateTime"
}

@Composable
fun formatStartingAt(startDateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
    val dateTime = startDateTime.format(formatter)
    return "Starting at: $dateTime"
}
