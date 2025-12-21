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

import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.time.TimeProvider
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import javax.inject.Inject

/**
 * Returns the next time a reminder notification should be scheduled for a
 * given reminder or null if no reminder should be scheduled.
 */
interface ReminderScheduler {
    /**
     * Returns the next time (from now) a reminder notification should be
     * scheduled for a given reminder or null if no reminder should be
     * scheduled.
     */
    fun scheduleNext(reminder: Reminder): Instant?

    /**
     * Returns the next time a reminder notification should be scheduled for a
     * given reminder after the specified time, or null if no reminder should
     * be scheduled.
     */
    fun scheduleNext(reminder: Reminder, afterTime: Instant): Instant?
}

internal class ReminderSchedulerImpl @Inject constructor(
    private val timeProvider: TimeProvider
) : ReminderScheduler {

    override fun scheduleNext(reminder: Reminder): Instant? {
        return scheduleNext(reminder, timeProvider.now().toInstant())
    }

    override fun scheduleNext(reminder: Reminder, afterTime: Instant): Instant? {
        return when (val params = reminder.params) {
            is ReminderParams.WeekDayParams -> scheduleNextWeekDayReminder(params, afterTime)
            is ReminderParams.PeriodicParams -> scheduleNextPeriodicReminder(params, afterTime)
        }
    }

    private fun scheduleNextWeekDayReminder(
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

    private fun scheduleNextPeriodicReminder(
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
