/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.functionslib

import org.threeten.bp.*
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.TemporalAmount

class TimeHelper(
    val aggregationPreferences: AggregationPreferences
) {

    /**
     * Finds the first ending of temporalAmount before dateTime. For example if temporalAmount is a
     * week and the aggregationPreferences specify that the week should start on Monday at 00:00
     * then it will find the very end of the sunday before dateTime.
     *
     * temporalAmount supports the use of duration instead of Period for the following values:
     *
     *  Duration.ofHours(1)
     *  Duration.ofHours(24)
     *  Duration.ofDays(7)
     *
     * For example if you pass in a duration of 7 days it will try to find
     * the last day of the week before the week containing dateTime. It is always the largest recognised
     * duration that is used when deciding what the start of the period should be.
     *
     * temporalAmount supports the following Period values:
     * Period.ofDays(1)
     * Period.ofWeeks(1)
     * Period.ofMonths(1)
     * Period.ofMonths(3)
     * Period.ofMonths(6)
     * Period.ofYears(1)
     *
     * A ZonedDateTime is returned so as to make sure that the time returned is the beginning of
     * the period relative to the users time zone. For example the beginning of the day for a time
     * 00:30 in a time zone that is one hour ahead of UTC should still return 00:00 for the same day
     * in the given time zone.
     *
     */
    fun findBeginningOfTemporal(
        dateTime: OffsetDateTime,
        temporalAmount: TemporalAmount,
        zoneId: ZoneId
    ): ZonedDateTime {
        val zonedDateTime = dateTime.atZoneSameInstant(zoneId)
        return when (temporalAmount) {
            is Duration -> findBeginningOfDuration(zonedDateTime, temporalAmount)
            is Period -> findBeginningOfPeriod(zonedDateTime, temporalAmount)
            else -> zonedDateTime
        }
    }

    fun toZonedDateTime(dateTime: OffsetDateTime): ZonedDateTime {
        return toZonedDateTime(dateTime, ZoneId.systemDefault())
    }

    fun toZonedDateTime(dateTime: OffsetDateTime, zoneId: ZoneId): ZonedDateTime {
        return dateTime.atZoneSameInstant(zoneId)
    }

    /**
     * @see findBeginningOfTemporal
     */
    fun findBeginningOfTemporal(
        dateTime: OffsetDateTime,
        temporalAmount: TemporalAmount,
    ): ZonedDateTime {
        return findBeginningOfTemporal(dateTime, temporalAmount, ZoneId.systemDefault())
    }

    private fun findBeginningOfPeriod(
        dateTime: ZonedDateTime,
        period: Period,
    ): ZonedDateTime {
        val dt = dateTime.withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .plus(aggregationPreferences.startTimeOfDay)

        val startOfPeriod = when {
            isPeriodNegativeOrZero(period.minus(Period.ofDays(1))) -> dt
            isPeriodNegativeOrZero(period.minus(Period.ofWeeks(1))) -> {
                dt.with(TemporalAdjusters.previousOrSame(aggregationPreferences.firstDayOfWeek))
            }
            isPeriodNegativeOrZero(period.minus(Period.ofMonths(1))) -> {
                dt.withDayOfMonth(1)
            }
            isPeriodNegativeOrZero(period.minus(Period.ofMonths(3))) -> {
                val month = getQuaterForMonthValue(dateTime.monthValue)
                dt.withMonth(month).withDayOfMonth(1)
            }
            isPeriodNegativeOrZero(period.minus(Period.ofMonths(6))) -> {
                val month = getBiYearForMonthValue(dateTime.monthValue)
                dt.withMonth(month).withDayOfMonth(1)
            }
            else -> dt.withDayOfYear(1)
        }

        return if (startOfPeriod > dateTime) startOfPeriod.minus(period) else startOfPeriod
    }

    private fun isPeriodNegativeOrZero(period: Period) = period.years < 0
            || (period.years == 0 && period.months < 0)
            || (period.years == 0 && period.months == 0 && period.days <= 0)

    private fun findBeginningOfDuration(
        dateTime: ZonedDateTime,
        duration: Duration,
    ): ZonedDateTime {
        val dtHour = dateTime
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        if (duration <= Duration.ofMinutes(60)) return dtHour

        val dt = dtHour.withHour(0).plus(aggregationPreferences.startTimeOfDay)

        val startOfDuration = when {
            duration <= Duration.ofDays(1) -> dt
            else -> dt.with(
                TemporalAdjusters.previousOrSame(
                    aggregationPreferences.firstDayOfWeek
                )
            )
        }

        return if (startOfDuration > dateTime) startOfDuration.minus(duration) else startOfDuration
    }

    /**
     * Given a number representing a month in the range 1 to 12, this function will return you the
     * integer value of the month starting the quater of the year containing that month.
     */
    fun getQuaterForMonthValue(monthValue: Int) = (3 * ((monthValue - 1) / 3)) + 1

    /**
     * Given a number representing a month in the range 1 to 12, this function will return you the
     * integer value of the month starting the bi year containing that month.
     */
    fun getBiYearForMonthValue(monthValue: Int) = if (monthValue < 7) 1 else 7
}