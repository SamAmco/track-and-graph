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

import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.time.TimeProvider
import org.threeten.bp.Instant
import javax.inject.Inject

internal open class PeriodicReminderScheduler @Inject constructor(
    private val timeProvider: TimeProvider
) {
    open fun scheduleNext(
        params: ReminderParams.PeriodicParams,
        afterTime: Instant
    ): Instant? {
        val currentZone = timeProvider.defaultZone()
        val afterTimeWithBuffer = afterTime.plusSeconds(2)
        val afterDateTime = afterTimeWithBuffer.atZone(currentZone)
        val ends = params.ends?.atZone(currentZone)

        // Check if the reminder has ended
        if (ends != null && afterDateTime.isAfter(ends)) {
            return null
        }

        // Start from the initial start time
        var candidate = params.starts.atZone(currentZone)

        // If we haven't reached the start time yet, return the start time
        if (candidate.isAfter(afterDateTime)) {
            return candidate.toInstant()
        }

        // Calculate the next occurrence based on the interval and period
        while (candidate.isBefore(afterDateTime) || candidate.isEqual(afterDateTime)) {
            candidate = when (params.period) {
                Period.MINUTES -> candidate.plusMinutes(params.interval.toLong())
                Period.HOURS -> candidate.plusHours(params.interval.toLong())
                Period.DAYS -> candidate.plusDays(params.interval.toLong())
                Period.WEEKS -> candidate.plusWeeks(params.interval.toLong())
                Period.MONTHS -> candidate.plusMonths(params.interval.toLong())
                Period.YEARS -> candidate.plusYears(params.interval.toLong())
            }
        }

        // Check if this occurrence is after the end time
        if (ends != null && candidate.isAfter(ends)) {
            return null
        }

        return candidate.toInstant()
    }
}
