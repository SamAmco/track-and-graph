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

import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.MonthDayOccurrence
import com.samco.trackandgraph.data.database.dto.MonthDayType
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime

/**
 * Sealed class for view data with 1-to-1 mapping to ReminderParams
 * types. Simplified view data for displaying reminders without mutable
 * state. Since we removed in-place editing, this is now just a read-only
 * representation.
 */
sealed class ReminderViewData {
    abstract val id: Long
    abstract val displayIndex: Int
    abstract val name: String
    abstract val reminderDto: Reminder?
    abstract val nextScheduled: LocalDateTime?

    /** View data for weekly reminders, mapping to ReminderParams.WeekDayParams */
    data class WeekDayReminderViewData(
        override val id: Long,
        override val displayIndex: Int,
        override val name: String,
        override val nextScheduled: LocalDateTime?,
        val checkedDays: CheckedDays,
        override val reminderDto: Reminder?,
    ) : ReminderViewData()

    /**
     * View data for periodic reminders, mapping to
     * ReminderParams.PeriodicParams
     */
    data class PeriodicReminderViewData(
        override val id: Long,
        override val displayIndex: Int,
        override val name: String,
        override val nextScheduled: LocalDateTime?,
        val starts: LocalDateTime,
        val ends: LocalDateTime?,
        val interval: Int,
        val period: Period,
        override val reminderDto: Reminder?,
        val progressToNextReminder: Float,
        val isBeforeStartTime: Boolean,
    ) : ReminderViewData()

    /** View data for month day reminders, mapping to ReminderParams.MonthDayParams */
    data class MonthDayReminderViewData(
        override val id: Long,
        override val displayIndex: Int,
        override val name: String,
        override val nextScheduled: LocalDateTime?,
        val occurrence: MonthDayOccurrence,
        val dayType: MonthDayType,
        val ends: LocalDateTime?,
        override val reminderDto: Reminder?,
    ) : ReminderViewData()

    companion object {
        /** Creates a ReminderViewData from a Reminder DTO */
        fun fromReminder(reminder: Reminder, nextScheduled: LocalDateTime?): ReminderViewData {
            return when (val params = reminder.params) {
                is ReminderParams.WeekDayParams -> {
                    WeekDayReminderViewData(
                        id = reminder.id,
                        displayIndex = reminder.displayIndex,
                        name = reminder.reminderName,
                        nextScheduled = nextScheduled,
                        checkedDays = params.checkedDays,
                        reminderDto = reminder,
                    )
                }

                is ReminderParams.PeriodicParams -> {
                    val now = LocalDateTime.now()
                    val isBeforeStart = now.isBefore(params.starts)
                    val progress = calculateProgressToNextReminder(
                        nextScheduled = nextScheduled,
                        interval = params.interval,
                        period = params.period,
                        now = now
                    )

                    PeriodicReminderViewData(
                        id = reminder.id,
                        displayIndex = reminder.displayIndex,
                        name = reminder.reminderName,
                        nextScheduled = nextScheduled,
                        starts = params.starts,
                        ends = params.ends,
                        interval = params.interval,
                        period = params.period,
                        reminderDto = reminder,
                        progressToNextReminder = progress,
                        isBeforeStartTime = isBeforeStart,
                    )
                }

                is ReminderParams.MonthDayParams -> {
                    MonthDayReminderViewData(
                        id = reminder.id,
                        displayIndex = reminder.displayIndex,
                        name = reminder.reminderName,
                        nextScheduled = nextScheduled,
                        occurrence = params.occurrence,
                        dayType = params.dayType,
                        ends = params.ends,
                        reminderDto = reminder,
                    )
                }
            }
        }

        /** Calculate progress (0.0 to 1.0) from last reminder to next reminder */
        private fun calculateProgressToNextReminder(
            nextScheduled: LocalDateTime?,
            interval: Int,
            period: Period,
            now: LocalDateTime
        ): Float {
            val next = nextScheduled ?: return 0f

            // Calculate the previous reminder time based on interval and period
            val previousReminder = when (period) {
                Period.MINUTES -> next.minusMinutes(interval.toLong())
                Period.HOURS -> next.minusHours(interval.toLong())
                Period.DAYS -> next.minusDays(interval.toLong())
                Period.WEEKS -> next.minusWeeks(interval.toLong())
                Period.MONTHS -> next.minusMonths(interval.toLong())
                Period.YEARS -> next.minusYears(interval.toLong())
            }

            val totalDuration = Duration.between(previousReminder, next).toMillis()
            val elapsedDuration = Duration.between(previousReminder, now).toMillis()

            return if (totalDuration <= 0) 0f
            else (elapsedDuration.toDouble() / totalDuration.toDouble())
                .coerceAtLeast(0.0)
                .coerceAtMost(1.0)
                .toFloat()
        }

    }
}