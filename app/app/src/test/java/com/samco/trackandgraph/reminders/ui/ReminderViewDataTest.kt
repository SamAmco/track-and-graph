/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.reminders.ui

import com.samco.trackandgraph.data.database.dto.IntervalPeriodPair
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.LocalDateTime

class ReminderViewDataTest {
    @Test
    fun `one time reminder progress fills toward first notification`() {
        val now = LocalDateTime.now()
        val viewData = createOneTimeViewData(
            createdAt = now.minusHours(1),
            starts = now.plusHours(1),
            nextScheduled = now.plusHours(1),
        )

        assertTrue(viewData.progressToNextReminder in 0.49f..0.51f)
        assertEquals(1, viewData.currentInterval)
        assertEquals(Period.HOURS, viewData.currentPeriod)
    }

    @Test
    fun `one time reminder progress remains full during repeating interval`() {
        val now = LocalDateTime.now()
        val viewData = createOneTimeViewData(
            createdAt = now.minusHours(3),
            starts = now.minusHours(2),
            nextScheduled = now.plusHours(1),
        )

        assertEquals(1f, viewData.progressToNextReminder)
        assertEquals(2, viewData.currentInterval)
        assertEquals(Period.HOURS, viewData.currentPeriod)
    }

    private fun createOneTimeViewData(
        createdAt: LocalDateTime,
        starts: LocalDateTime,
        nextScheduled: LocalDateTime?,
    ): ReminderViewData.OneTimeReminderViewData {
        val reminder = Reminder(
            id = 1L,
            reminderName = "Standalone reminder",
            featureId = null,
            params = ReminderParams.OneTimeParams(
                createdAt = createdAt,
                starts = starts,
                initialDelay = IntervalPeriodPair(1, Period.HOURS),
                repeatInterval = IntervalPeriodPair(2, Period.HOURS),
            ),
            unique = true,
        )
        return ReminderViewData.fromReminder(
            reminder = reminder,
            groupItemId = 2L,
            nextScheduled = nextScheduled,
        ) as ReminderViewData.OneTimeReminderViewData
    }
}
