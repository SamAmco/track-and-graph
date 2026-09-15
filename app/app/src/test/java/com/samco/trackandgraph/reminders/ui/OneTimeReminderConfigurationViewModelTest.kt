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
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.time.FakeTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class OneTimeReminderConfigurationViewModelTest {
    private val timeProvider = FakeTimeProvider().apply {
        timeZone = ZoneId.of("UTC")
        currentTime = ZonedDateTime.parse("2026-01-31T10:00:00Z")
    }
    private val viewModel = OneTimeReminderConfigurationViewModelImpl(timeProvider)

    @Test
    fun `relative delay is resolved from save time using calendar arithmetic`() {
        viewModel.updateStartType(OneTimeStartType.AFTER_DELAY)
        viewModel.updateDelayInterval("1")
        viewModel.updateDelayPeriod(Period.MONTHS)

        val params = viewModel.getReminderInput().params as ReminderParams.OneTimeParams

        assertEquals(LocalDateTime.of(2026, 1, 31, 10, 0), params.createdAt)
        assertEquals(LocalDateTime.of(2026, 2, 28, 10, 0), params.starts)
        assertEquals(IntervalPeriodPair(1, Period.MONTHS), params.initialDelay)
    }

    @Test
    fun `absolute date and optional repeat interval are stored independently`() {
        viewModel.updateStartType(OneTimeStartType.DATE_TIME)
        viewModel.updateStarts(OffsetDateTime.parse("2026-03-04T14:30:00Z"))
        viewModel.updateHasRepeatInterval(true)
        viewModel.updateRepeatInterval("3")
        viewModel.updateRepeatPeriod(Period.DAYS)

        val params = viewModel.getReminderInput().params as ReminderParams.OneTimeParams

        assertEquals(LocalDateTime.of(2026, 3, 4, 14, 30), params.starts)
        assertNull(params.initialDelay)
        assertEquals(IntervalPeriodPair(3, Period.DAYS), params.repeatInterval)
    }
}
