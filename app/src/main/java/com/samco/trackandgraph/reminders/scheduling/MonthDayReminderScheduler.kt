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

import com.samco.trackandgraph.data.database.dto.MonthDayOccurrence
import com.samco.trackandgraph.data.database.dto.MonthDayType
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.time.TimeProvider
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalTime
import org.threeten.bp.YearMonth
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.TemporalAdjusters
import javax.inject.Inject

internal open class MonthDayReminderScheduler @Inject constructor(
    private val timeProvider: TimeProvider
) {
    open fun scheduleNext(
        params: ReminderParams.MonthDayParams,
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

        val currentMonth = YearMonth.from(afterDateTime.toLocalDate())

        // Try current month first, then next month
        return findValidMonthDayCandidate(currentMonth, params, currentZone, afterDateTime, ends)
            ?: findValidMonthDayCandidate(currentMonth.plusMonths(1), params, currentZone, afterDateTime, ends)
    }

    private fun findValidMonthDayCandidate(
        month: YearMonth,
        params: ReminderParams.MonthDayParams,
        zone: ZoneId,
        afterDateTime: ZonedDateTime,
        ends: ZonedDateTime?
    ): Instant? {
        val candidate = findMonthDayOccurrence(month, params.occurrence, params.dayType, params.time, zone)

        if (candidate != null && candidate.isAfter(afterDateTime)) {
            // Check if this occurrence is after the end time
            if (ends != null && candidate.isAfter(ends)) {
                return null
            }
            return candidate.toInstant()
        }

        return null
    }

    private fun findMonthDayOccurrence(
        month: YearMonth,
        occurrence: MonthDayOccurrence,
        dayType: MonthDayType,
        time: LocalTime,
        zone: ZoneId
    ): ZonedDateTime? {
        return when (dayType) {
            MonthDayType.DAY -> {
                val targetDay = when (occurrence) {
                    MonthDayOccurrence.FIRST -> month.atDay(1)
                    MonthDayOccurrence.SECOND -> month.atDay(2)
                    MonthDayOccurrence.THIRD -> month.atDay(3)
                    MonthDayOccurrence.FOURTH -> month.atDay(4)
                    MonthDayOccurrence.LAST -> month.atEndOfMonth()
                }
                targetDay.atTime(time).atZone(zone)
            }
            else -> {
                val dayOfWeek = when (dayType) {
                    MonthDayType.MONDAY -> DayOfWeek.MONDAY
                    MonthDayType.TUESDAY -> DayOfWeek.TUESDAY
                    MonthDayType.WEDNESDAY -> DayOfWeek.WEDNESDAY
                    MonthDayType.THURSDAY -> DayOfWeek.THURSDAY
                    MonthDayType.FRIDAY -> DayOfWeek.FRIDAY
                    MonthDayType.SATURDAY -> DayOfWeek.SATURDAY
                    MonthDayType.SUNDAY -> DayOfWeek.SUNDAY
                    MonthDayType.DAY -> return null // Already handled above
                }

                val monthStart = month.atDay(1)
                when (occurrence) {
                    MonthDayOccurrence.FIRST -> {
                        val firstOccurrence = monthStart.with(TemporalAdjusters.firstInMonth(dayOfWeek))
                        firstOccurrence.atTime(time).atZone(zone)
                    }
                    MonthDayOccurrence.SECOND -> {
                        val firstOccurrence = monthStart.with(TemporalAdjusters.firstInMonth(dayOfWeek))
                        val secondOccurrence = firstOccurrence.plusWeeks(1)
                        if (secondOccurrence.month == month.month) {
                            secondOccurrence.atTime(time).atZone(zone)
                        } else null
                    }
                    MonthDayOccurrence.THIRD -> {
                        val firstOccurrence = monthStart.with(TemporalAdjusters.firstInMonth(dayOfWeek))
                        val thirdOccurrence = firstOccurrence.plusWeeks(2)
                        if (thirdOccurrence.month == month.month) {
                            thirdOccurrence.atTime(time).atZone(zone)
                        } else null
                    }
                    MonthDayOccurrence.FOURTH -> {
                        val firstOccurrence = monthStart.with(TemporalAdjusters.firstInMonth(dayOfWeek))
                        val fourthOccurrence = firstOccurrence.plusWeeks(3)
                        if (fourthOccurrence.month == month.month) {
                            fourthOccurrence.atTime(time).atZone(zone)
                        } else null
                    }
                    MonthDayOccurrence.LAST -> {
                        val lastOccurrence = monthStart.with(TemporalAdjusters.lastInMonth(dayOfWeek))
                        lastOccurrence.atTime(time).atZone(zone)
                    }
                }
            }
        }
    }
}
