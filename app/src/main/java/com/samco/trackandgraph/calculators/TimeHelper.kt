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

package com.samco.trackandgraph.calculators

import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.TemporalAmount
import kotlin.math.ceil

class TimeHelper(
    val aggregationPreferences: AggregationPreferences
) {

    /**
     * Finds the first ending of temporalAmount before dateTime. For example if temporalAmount is a
     * week then it will find the very end of the sunday before dateTime.
     *
     * temporalAmount supports the use of duration instead of Period.
     * In this case the function will try to approximate the behaviour as closely as possible as though
     * temporalAmount was a period. For example if you pass in a duration of 7 days it will try to find
     * the last day of the week before the week containing dateTime. It is always the largest recognised
     * duration that is used when deciding what the start of the period should be. The recognised durations
     * are:
     *
     *  Duration.ofHours(1)
     *  Duration.ofHours(24)
     *  Duration.ofDays(7)
     *  Duration.ofDays(30)
     *  Duration.ofDays(365 / 4) or 3 months
     *  Duration.ofDays(365 / 2) or 6 months
     *  Duration.ofDays(365) or a year
     *
     */
    fun findBeginningOfTemporal(
        dateTime: OffsetDateTime,
        temporalAmount: TemporalAmount,
    ): OffsetDateTime {
        return when (temporalAmount) {
            is Duration -> findBeginningOfDuration(dateTime, temporalAmount)
            is Period -> findBeginningOfPeriod(dateTime, temporalAmount)
            else -> dateTime
        }
    }

    private fun findBeginningOfPeriod(
        dateTime: OffsetDateTime,
        period: Period,
    ): OffsetDateTime {
        val dt = dateTime.withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        return when {
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
    }

    private fun isPeriodNegativeOrZero(period: Period) = period.years < 0
            || (period.years == 0 && period.months < 0)
            || (period.years == 0 && period.months == 0 && period.days <= 0)

    private fun findBeginningOfDuration(
        dateTime: OffsetDateTime,
        duration: Duration,
    ): OffsetDateTime {
        val dtHour = dateTime
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        if (duration <= Duration.ofMinutes(60)) return dtHour

        val dt = dtHour.withHour(0)

        return when {
            duration <= Duration.ofDays(1) -> dt
            duration <= Duration.ofDays(7) -> {
                dt.with(
                    TemporalAdjusters.previousOrSame(
                        aggregationPreferences.firstDayOfWeek
                    )
                )
            }
            duration <= Duration.ofDays(31) -> dt.withDayOfMonth(1)
            duration <= Duration.ofDays(ceil(365.0 / 4).toLong()) -> {
                val month = getQuaterForMonthValue(dt.monthValue)
                dt.withMonth(month).withDayOfMonth(1)
            }
            duration <= Duration.ofDays(ceil(365.0 / 2).toLong()) -> {
                val month = getBiYearForMonthValue(dt.monthValue)
                dt.withMonth(month).withDayOfMonth(1).withHour(0)
            }
            else -> dt.withDayOfYear(1)
        }
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