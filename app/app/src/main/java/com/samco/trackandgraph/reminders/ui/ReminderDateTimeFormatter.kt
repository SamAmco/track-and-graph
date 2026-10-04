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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

@Composable
private fun formatLocalizedDateTime(dateTime: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0]
    return dateTime.format(
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
    )
}

@Composable
fun formatNextScheduled(nextScheduled: LocalDateTime?): String {
    return if (nextScheduled != null) {
        val dateTime = formatLocalizedDateTime(nextScheduled)
        stringResource(R.string.next_reminder_format, dateTime)
    } else {
        stringResource(R.string.no_upcoming_reminders)
    }
}

@Composable
fun formatEndedAt(endDateTime: LocalDateTime): String {
    val dateTime = formatLocalizedDateTime(endDateTime)
    return stringResource(R.string.ended_at_format, dateTime)
}

@Composable
fun formatStartingAt(startDateTime: LocalDateTime): String {
    val dateTime = formatLocalizedDateTime(startDateTime)
    return stringResource(R.string.starting_at_format, dateTime)
}
