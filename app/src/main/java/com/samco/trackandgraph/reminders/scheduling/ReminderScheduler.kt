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
import com.samco.trackandgraph.data.time.TimeProvider
import org.threeten.bp.Instant
import javax.inject.Inject

/**
 * Returns the next time a reminder notification should be scheduled for a
 * given reminder or null if no reminder should be scheduled.
 *
 * Implementations must never return an instant in the past. If there is no
 * valid future time for the reminder, null should be returned.
 */
interface ReminderScheduler {
    /**
     * Returns the next time (from now) a reminder notification should be
     * scheduled for a given reminder or null if no reminder should be
     * scheduled. The returned instant is always in the future.
     */
    suspend fun scheduleNext(reminder: Reminder): Instant?

    /**
     * Returns the next time a reminder notification should be scheduled for a
     * given reminder after the specified time, or null if no reminder should
     * be scheduled. The returned instant is always after [afterTime].
     */
    suspend fun scheduleNext(reminder: Reminder, afterTime: Instant): Instant?
}

internal class ReminderSchedulerImpl @Inject constructor(
    private val timeProvider: TimeProvider,
    private val weekDayScheduler: WeekDayReminderScheduler,
    private val periodicScheduler: PeriodicReminderScheduler,
    private val monthDayScheduler: MonthDayReminderScheduler,
    private val timeSinceLastScheduler: TimeSinceLastReminderScheduler
) : ReminderScheduler {

    override suspend fun scheduleNext(reminder: Reminder): Instant? {
        return scheduleNext(reminder, timeProvider.now().toInstant())
    }

    override suspend fun scheduleNext(reminder: Reminder, afterTime: Instant): Instant? {
        return when (val params = reminder.params) {
            is ReminderParams.WeekDayParams -> weekDayScheduler.scheduleNext(params, afterTime)
            is ReminderParams.PeriodicParams -> periodicScheduler.scheduleNext(params, afterTime)
            is ReminderParams.MonthDayParams -> monthDayScheduler.scheduleNext(params, afterTime)
            is ReminderParams.TimeSinceLastParams -> timeSinceLastScheduler.scheduleNext(
                reminder.featureId,
                params,
                afterTime
            )
        }
    }
}
