/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.reminders.scheduling

import com.samco.trackandgraph.data.database.dto.IntervalPeriodPair
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.time.TimeProvider
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

internal open class OneTimeReminderScheduler @Inject constructor(
    private val timeProvider: TimeProvider,
) {
    open fun scheduleNext(
        params: ReminderParams.OneTimeParams,
        afterTime: Instant,
    ): Instant? {
        val afterDateTime = afterTime.plusSeconds(2).atZone(timeProvider.defaultZone())
        var candidate = params.starts.atZone(timeProvider.defaultZone())

        if (candidate.isAfter(afterDateTime)) return candidate.toInstant()

        val repeatInterval = params.repeatInterval ?: return null
        while (!candidate.isAfter(afterDateTime)) {
            candidate = candidate.plus(repeatInterval)
        }
        return candidate.toInstant()
    }

    private fun ZonedDateTime.plus(interval: IntervalPeriodPair): ZonedDateTime =
        when (interval.period) {
            Period.MINUTES -> plusMinutes(interval.interval.toLong())
            Period.HOURS -> plusHours(interval.interval.toLong())
            Period.DAYS -> plusDays(interval.interval.toLong())
            Period.WEEKS -> plusWeeks(interval.interval.toLong())
            Period.MONTHS -> plusMonths(interval.interval.toLong())
            Period.YEARS -> plusYears(interval.interval.toLong())
        }
}
