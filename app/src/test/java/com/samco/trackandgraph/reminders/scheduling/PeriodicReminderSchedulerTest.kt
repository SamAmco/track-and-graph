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

import com.samco.trackandgraph.NoOpDataSampler
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.reminders.reminderFixture
import com.samco.trackandgraph.time.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

internal class PeriodicReminderSchedulerTest {

    private val timeProvider = FakeTimeProvider()
    private val uut = ReminderSchedulerImpl(timeProvider, NoOpDataSampler())

    @Test
    fun `schedule next returns start time when current time is before start`() = runTest {
        // PREPARE - Current time is before the start time
        val startTime = LocalDateTime.of(2024, 1, 15, 10, 0)
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 10, 9, 0, 0, 0, ZoneId.of("UTC"))
        
        val reminder = reminderFixture.copy(
            params = ReminderParams.PeriodicParams(
                starts = startTime,
                ends = null,
                interval = 1,
                period = Period.DAYS
            )
        )
        
        // EXECUTE
        val result = uut.scheduleNext(reminder)
        
        // VERIFY
        val expected = startTime.atZone(ZoneId.of("UTC")).toInstant()
        assertEquals("Should return start time when current time is before start", expected, result)
    }

    @Test
    fun `schedule next returns next daily occurrence`() = runTest {
        // PREPARE - Current time is after 2nd daily occurrence
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 12, 11, 0, 0, 0, ZoneId.of("UTC"))
        
        val reminder = reminderFixture.copy(
            params = ReminderParams.PeriodicParams(
                starts = LocalDateTime.of(2024, 1, 10, 10, 0),
                ends = null,
                interval = 1,
                period = Period.DAYS
            )
        )
        
        // EXECUTE
        val result = uut.scheduleNext(reminder)
        
        // VERIFY - Should schedule for next day
        val expected = ZonedDateTime.of(2024, 1, 13, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should return next daily occurrence", expected, result)
    }

    @Test
    fun `schedule next returns next weekly occurrence`() = runTest {
        // PREPARE - Current time is after first weekly occurrence
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 18, 11, 0, 0, 0, ZoneId.of("UTC")) // Wednesday + 1 hour
        
        val reminder = reminderFixture.copy(
            params = ReminderParams.PeriodicParams(
                starts = LocalDateTime.of(2024, 1, 10, 10, 0), // Previous Wednesday
                ends = null,
                interval = 1,
                period = Period.WEEKS
            )
        )
        
        // EXECUTE
        val result = uut.scheduleNext(reminder)
        
        // VERIFY - Should schedule for next Wednesday
        val expected = ZonedDateTime.of(2024, 1, 24, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should return next weekly occurrence", expected, result)
    }

    @Test
    fun `schedule next returns next monthly occurrence`() = runTest {
        // PREPARE - Current time is after February occurrence
        timeProvider.currentTime = ZonedDateTime.of(2024, 2, 16, 11, 0, 0, 0, ZoneId.of("UTC"))
        
        val reminder = reminderFixture.copy(
            params = ReminderParams.PeriodicParams(
                starts = LocalDateTime.of(2024, 1, 15, 10, 0),
                ends = null,
                interval = 1,
                period = Period.MONTHS
            )
        )
        
        // EXECUTE
        val result = uut.scheduleNext(reminder)
        
        // VERIFY - Should schedule for March
        val expected = ZonedDateTime.of(2024, 3, 15, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should return next monthly occurrence", expected, result)
    }

    @Test
    fun `schedule next returns next yearly occurrence`() = runTest {
        // PREPARE - Current time is same year, after start
        timeProvider.currentTime = ZonedDateTime.of(2024, 2, 16, 11, 0, 0, 0, ZoneId.of("UTC"))
        
        val reminder = reminderFixture.copy(
            params = ReminderParams.PeriodicParams(
                starts = LocalDateTime.of(2024, 1, 15, 10, 0),
                ends = null,
                interval = 1,
                period = Period.YEARS
            )
        )
        
        // EXECUTE
        val result = uut.scheduleNext(reminder)
        
        // VERIFY - Should schedule for next year
        val expected = ZonedDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should return next yearly occurrence", expected, result)
    }

    @Test
    fun `schedule next handles multi-interval periods`() = runTest {
        // PREPARE - Current time is after first occurrence, every 3 days
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 13, 11, 0, 0, 0, ZoneId.of("UTC"))
        
        val reminder = reminderFixture.copy(
            params = ReminderParams.PeriodicParams(
                starts = LocalDateTime.of(2024, 1, 10, 10, 0),
                ends = null,
                interval = 3, // Every 3 days
                period = Period.DAYS
            )
        )
        
        // EXECUTE
        val result = uut.scheduleNext(reminder)
        
        // VERIFY - Should schedule 3 days after the 13th (next occurrence after start + 3 days)
        val expected = ZonedDateTime.of(2024, 1, 16, 10, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should handle multi-interval periods", expected, result)
    }

    @Test
    fun `schedule next returns null when past end time`() = runTest {
        // PREPARE - Current time is after end time
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 25, 11, 0, 0, 0, ZoneId.of("UTC"))
        
        val reminder = reminderFixture.copy(
            params = ReminderParams.PeriodicParams(
                starts = LocalDateTime.of(2024, 1, 10, 10, 0),
                ends = LocalDateTime.of(2024, 1, 20, 10, 0),
                interval = 1,
                period = Period.DAYS
            )
        )
        
        // EXECUTE
        val result = uut.scheduleNext(reminder)
        
        // VERIFY
        assertNull("Should return null when past end time", result)
    }

    @Test
    fun `schedule next returns null when next occurrence would be past end time`() = runTest {
        // PREPARE - Current time is before end, but next occurrence would be after end
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 14, 11, 0, 0, 0, ZoneId.of("UTC"))
        
        val reminder = reminderFixture.copy(
            params = ReminderParams.PeriodicParams(
                starts = LocalDateTime.of(2024, 1, 10, 10, 0),
                ends = LocalDateTime.of(2024, 1, 15, 10, 0),
                interval = 7, // Weekly - next would be Jan 17, past end time
                period = Period.DAYS
            )
        )
        
        // EXECUTE
        val result = uut.scheduleNext(reminder)
        
        // VERIFY
        assertNull("Should return null when next occurrence would be past end time", result)
    }
}
