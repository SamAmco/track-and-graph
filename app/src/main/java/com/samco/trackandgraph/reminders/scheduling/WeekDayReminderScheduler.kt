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

package com.samco.trackandgraph.reminders.scheduling

import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.time.TimeProvider
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import javax.inject.Inject

internal open class WeekDayReminderScheduler @Inject constructor(
    private val timeProvider: TimeProvider
) {
    open fun scheduleNext(
        params: ReminderParams.WeekDayParams,
        afterTime: Instant
    ): Instant? {
        val currentZone = timeProvider.defaultZone()
        val checkedDays = params.checkedDays.toList()

        // If no days are checked, return null
        if (!checkedDays.any { it }) return null

        // Get the days of the week that are checked (Monday = 0, Sunday = 6)
        val enabledDays = checkedDays.mapIndexedNotNull { index, isChecked ->
            if (isChecked) DayOfWeek.of(index + 1) else null
        }

        // Add a small buffer to afterTime to avoid race condition where an alarm
        // that just fired could be rescheduled for the same time
        val afterTimeWithBuffer = afterTime.plusSeconds(2)

        // Start checking from the day of afterTime
        var candidate = afterTimeWithBuffer.atZone(currentZone).toLocalDate().atTime(params.time)
            .atZone(currentZone)

        // If the time today hasn't passed yet and today is enabled, use today
        if (enabledDays.contains(candidate.dayOfWeek) && candidate.toInstant()
                .isAfter(afterTimeWithBuffer)
        ) {
            return candidate.toInstant()
        }

        // Otherwise, find the next enabled day
        (1..7).forEach { _ ->
            candidate = candidate.plusDays(1)
            if (enabledDays.contains(candidate.dayOfWeek)) {
                return candidate.toInstant()
            }
        }

        error("User has at least one checked day, but no next alarm time was found")
    }
}
