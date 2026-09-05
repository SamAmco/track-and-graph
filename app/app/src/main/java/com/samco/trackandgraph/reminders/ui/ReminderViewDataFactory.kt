/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.reminders.ui

import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.time.TimeProvider
import com.samco.trackandgraph.reminders.NextScheduled
import com.samco.trackandgraph.reminders.ReminderInteractor
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import javax.inject.Inject

class ReminderViewDataFactory @Inject constructor(
    private val reminderInteractor: ReminderInteractor,
    private val timeProvider: TimeProvider,
    private val dataSampler: DataSampler,
) {
    suspend fun create(reminder: Reminder, groupItemId: Long): ReminderViewData {
        val nextScheduled = when (val scheduled = reminderInteractor.getNextScheduled(reminder)) {
            is NextScheduled.AtInstant -> LocalDateTime.ofInstant(
                scheduled.instant,
                timeProvider.defaultZone(),
            )
            is NextScheduled.Never -> null
        }

        return ReminderViewData.fromReminder(
            reminder = reminder,
            groupItemId = groupItemId,
            nextScheduled = nextScheduled,
            lastTrackedInstant = getLastTrackedInstant(reminder),
        )
    }

    private suspend fun getLastTrackedInstant(reminder: Reminder): Instant? {
        if (reminder.params !is ReminderParams.TimeSinceLastParams) return null

        val featureId = reminder.featureId ?: return null
        val dataSample = dataSampler.getRawDataSampleForFeatureId(featureId) ?: return null

        return try {
            dataSample.iterator().asSequence().firstOrNull()?.timestamp?.toInstant()
        } finally {
            dataSample.dispose()
        }
    }
}
