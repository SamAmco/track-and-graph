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
import com.samco.trackandgraph.time.FakeTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

class OneTimeReminderSchedulerTest {
    private val timeProvider = FakeTimeProvider().apply { timeZone = ZoneId.of("UTC") }
    private val scheduler = OneTimeReminderScheduler(timeProvider)

    @Test
    fun `returns initial date when it is still in the future`() {
        val params = params(starts = LocalDateTime.of(2026, 9, 16, 12, 0))

        val result = scheduler.scheduleNext(params, Instant.parse("2026-09-15T12:00:00Z"))

        assertEquals(Instant.parse("2026-09-16T12:00:00Z"), result)
    }

    @Test
    fun `returns null after initial date when reminder does not repeat`() {
        val params = params(starts = LocalDateTime.of(2026, 9, 15, 12, 0))

        val result = scheduler.scheduleNext(params, Instant.parse("2026-09-15T12:00:00Z"))

        assertNull(result)
    }

    @Test
    fun `returns next occurrence after initial date when reminder repeats`() {
        val params = params(
            starts = LocalDateTime.of(2026, 9, 15, 12, 0),
            repeatInterval = IntervalPeriodPair(2, Period.DAYS),
        )

        val result = scheduler.scheduleNext(params, Instant.parse("2026-09-18T12:00:00Z"))

        assertEquals(Instant.parse("2026-09-19T12:00:00Z"), result)
    }

    @Test
    fun `calendar repeat intervals advance from each previous occurrence`() {
        val params = params(
            starts = LocalDateTime.of(2026, 1, 31, 9, 0),
            repeatInterval = IntervalPeriodPair(1, Period.MONTHS),
        )

        val result = scheduler.scheduleNext(params, Instant.parse("2026-02-28T10:00:00Z"))

        assertEquals(Instant.parse("2026-03-28T09:00:00Z"), result)
    }

    private fun params(
        starts: LocalDateTime,
        repeatInterval: IntervalPeriodPair? = null,
    ) = ReminderParams.OneTimeParams(
        createdAt = LocalDateTime.of(2026, 9, 15, 10, 0),
        starts = starts,
        initialDelay = null,
        repeatInterval = repeatInterval,
    )
}
