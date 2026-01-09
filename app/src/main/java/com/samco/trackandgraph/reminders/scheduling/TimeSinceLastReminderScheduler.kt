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

import com.samco.trackandgraph.data.database.dto.IntervalPeriodPair
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.time.TimeProvider
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import javax.inject.Inject

internal open class TimeSinceLastReminderScheduler @Inject constructor(
    private val timeProvider: TimeProvider,
    private val dataSampler: DataSampler
) {
    open suspend fun scheduleNext(
        featureId: Long?,
        params: ReminderParams.TimeSinceLastParams,
        afterTime: Instant
    ): Instant? {
        // If no feature is associated, can't schedule
        val resolvedFeatureId = featureId ?: return null

        // Get the latest data point for the feature
        val dataSample = dataSampler.getRawDataSampleForFeatureId(resolvedFeatureId) ?: return null
        val latestDataPoint = try {
            dataSample.iterator().asSequence().firstOrNull()
        } finally {
            dataSample.dispose()
        } ?: return null

        val lastTrackedInstant = latestDataPoint.timestamp.toInstant()
        val currentZone = timeProvider.defaultZone()
        val afterTimeWithBuffer = afterTime.plusSeconds(2)

        // Calculate the first reminder time (lastTracked + firstInterval)
        val firstReminderTime = addIntervalToInstant(lastTrackedInstant, params.firstInterval, currentZone)

        // If first reminder time is after now, schedule it
        if (firstReminderTime.isAfter(afterTimeWithBuffer)) {
            return firstReminderTime
        }

        // We're past the first interval. If there's no second interval,
        // there's no future reminder time to schedule
        val secondInterval = params.secondInterval ?: return null

        // Iterate forward by secondInterval until we find a time after afterTime
        var candidate = firstReminderTime
        while (!candidate.isAfter(afterTimeWithBuffer)) {
            candidate = addIntervalToInstant(candidate, secondInterval, currentZone)
        }

        return candidate
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
