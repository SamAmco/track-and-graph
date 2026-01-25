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
import com.samco.trackandgraph.data.database.dto.IntervalPeriodPair
import com.samco.trackandgraph.data.database.dto.MonthDayOccurrence
import com.samco.trackandgraph.data.database.dto.MonthDayType
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

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

    /** View data for time since last reminders, mapping to ReminderParams.TimeSinceLastParams */
    data class TimeSinceLastReminderViewData(
        override val id: Long,
        override val displayIndex: Int,
        override val name: String,
        override val nextScheduled: LocalDateTime?,
        override val reminderDto: Reminder?,
        val progressToNextReminder: Float,
        val currentInterval: Int?,
        val currentPeriod: Period?,
    ) : ReminderViewData()

    companion object {
        /**
         * Creates a ReminderViewData from a Reminder DTO.
         * @param reminder The reminder to convert
         * @param nextScheduled The next scheduled time for the reminder
         * @param lastTrackedInstant For TimeSinceLastParams reminders, the instant of the last
         *   tracked data point. Pass null if unknown or if the reminder type doesn't need it.
         */
        fun fromReminder(
            reminder: Reminder,
            nextScheduled: LocalDateTime?,
            lastTrackedInstant: Instant? = null
        ): ReminderViewData {
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

                is ReminderParams.TimeSinceLastParams -> {
                    val progress = calculateTimeSinceLastProgress(
                        lastTrackedInstant = lastTrackedInstant,
                        firstInterval = params.firstInterval
                    )

                    val currentIntervalPair = calculateCurrentInterval(
                        nextScheduled = nextScheduled,
                        lastTrackedInstant = lastTrackedInstant,
                        params = params
                    )

                    TimeSinceLastReminderViewData(
                        id = reminder.id,
                        displayIndex = reminder.displayIndex,
                        name = reminder.reminderName,
                        nextScheduled = nextScheduled,
                        reminderDto = reminder,
                        progressToNextReminder = progress,
                        currentInterval = currentIntervalPair?.interval,
                        currentPeriod = currentIntervalPair?.period,
                    )
                }
            }
        }

        /**
         * Calculate progress (0.0 to 1.0) for time-since-last reminders.
         * Progress goes from 0 at the last track to 1 at the first interval.
         * Returns 1.0 if past the first interval, 0.0 if data is unavailable.
         */
        private fun calculateTimeSinceLastProgress(
            lastTrackedInstant: Instant?,
            firstInterval: IntervalPeriodPair
        ): Float {
            val lastTracked = lastTrackedInstant ?: return 0f

            val now = Instant.now()
            val elapsedMillis = Duration.between(lastTracked, now).toMillis()

            // Calculate the total duration of the first interval in milliseconds
            val totalMillis = when (firstInterval.period) {
                Period.MINUTES -> Duration.ofMinutes(firstInterval.interval.toLong()).toMillis()
                Period.HOURS -> Duration.ofHours(firstInterval.interval.toLong()).toMillis()
                Period.DAYS -> Duration.ofDays(firstInterval.interval.toLong()).toMillis()
                Period.WEEKS -> Duration.ofDays(firstInterval.interval.toLong() * 7).toMillis()
                Period.MONTHS -> Duration.ofDays(firstInterval.interval.toLong() * 30).toMillis()
                Period.YEARS -> Duration.ofDays(firstInterval.interval.toLong() * 365).toMillis()
            }

            if (totalMillis <= 0) return 0f

            val progress = elapsedMillis.toDouble() / totalMillis.toDouble()
            return progress.coerceIn(0.0, 1.0).toFloat()
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

        /**
         * Determine which interval is currently active for a time-since-last reminder.
         * Returns the first interval if we're waiting for the initial reminder,
         * or the second interval if we've moved to recurring reminders.
         * Returns null if there's no next scheduled reminder or we can't determine the interval.
         */
        private fun calculateCurrentInterval(
            nextScheduled: LocalDateTime?,
            lastTrackedInstant: Instant?,
            params: ReminderParams.TimeSinceLastParams
        ): IntervalPeriodPair? {
            // Can't determine current interval without a next scheduled time or last tracked time
            if (nextScheduled == null) return null
            val lastTracked = lastTrackedInstant ?: return null

            // Calculate when the first reminder would fire (lastTracked + firstInterval)
            val firstReminderTime = addIntervalToInstant(
                instant = lastTracked,
                intervalPeriod = params.firstInterval,
                zone = ZoneId.systemDefault()
            )

            val nextScheduledInstant = nextScheduled.atZone(ZoneId.systemDefault()).toInstant()

            // If nextScheduled is at or before firstReminderTime, we're on the first interval.
            // Otherwise we're past the first reminder and using the second interval.
            return if (!nextScheduledInstant.isAfter(firstReminderTime)) {
                params.firstInterval
            } else {
                params.secondInterval
            }
        }

        private fun addIntervalToInstant(
            instant: Instant,
            intervalPeriod: IntervalPeriodPair,
            zone: ZoneId
        ): Instant {
            val zonedDateTime = instant.atZone(zone)
            val result = when (intervalPeriod.period) {
                Period.MINUTES -> zonedDateTime.plusMinutes(intervalPeriod.interval.toLong())
                Period.HOURS -> zonedDateTime.plusHours(intervalPeriod.interval.toLong())
                Period.DAYS -> zonedDateTime.plusDays(intervalPeriod.interval.toLong())
                Period.WEEKS -> zonedDateTime.plusWeeks(intervalPeriod.interval.toLong())
                Period.MONTHS -> zonedDateTime.plusMonths(intervalPeriod.interval.toLong())
                Period.YEARS -> zonedDateTime.plusYears(intervalPeriod.interval.toLong())
            }
            return result.toInstant()
        }

    }
}