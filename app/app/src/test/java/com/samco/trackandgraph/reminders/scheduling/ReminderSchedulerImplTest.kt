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

import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.IntervalPeriodPair
import com.samco.trackandgraph.data.database.dto.MonthDayOccurrence
import com.samco.trackandgraph.data.database.dto.MonthDayType
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.reminders.reminderFixture
import com.samco.trackandgraph.time.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

internal class ReminderSchedulerImplTest {

    private val timeProvider = FakeTimeProvider()

    private val weekDayResult = Instant.ofEpochSecond(1000)
    private val periodicResult = Instant.ofEpochSecond(2000)
    private val monthDayResult = Instant.ofEpochSecond(3000)
    private val timeSinceLastResult = Instant.ofEpochSecond(4000)
    private val oneTimeResult = Instant.ofEpochSecond(5000)

    private var weekDaySchedulerCalled = false
    private var periodicSchedulerCalled = false
    private var monthDaySchedulerCalled = false
    private var timeSinceLastSchedulerCalled = false
    private var oneTimeSchedulerCalled = false

    private val weekDayScheduler = object : WeekDayReminderScheduler(timeProvider) {
        override fun scheduleNext(params: ReminderParams.WeekDayParams, afterTime: Instant): Instant? {
            weekDaySchedulerCalled = true
            return weekDayResult
        }
    }

    private val periodicScheduler = object : PeriodicReminderScheduler(timeProvider) {
        override fun scheduleNext(params: ReminderParams.PeriodicParams, afterTime: Instant): Instant? {
            periodicSchedulerCalled = true
            return periodicResult
        }
    }

    private val monthDayScheduler = object : MonthDayReminderScheduler(timeProvider) {
        override fun scheduleNext(params: ReminderParams.MonthDayParams, afterTime: Instant): Instant? {
            monthDaySchedulerCalled = true
            return monthDayResult
        }
    }

    private val timeSinceLastScheduler = object : TimeSinceLastReminderScheduler(timeProvider, com.samco.trackandgraph.NoOpDataSampler()) {
        override suspend fun scheduleNext(featureId: Long?, params: ReminderParams.TimeSinceLastParams, afterTime: Instant): Instant? {
            timeSinceLastSchedulerCalled = true
            return timeSinceLastResult
        }
    }

    private val oneTimeScheduler = object : OneTimeReminderScheduler(timeProvider) {
        override fun scheduleNext(params: ReminderParams.OneTimeParams, afterTime: Instant): Instant? {
            oneTimeSchedulerCalled = true
            return oneTimeResult
        }
    }

    private val uut = ReminderSchedulerImpl(
        timeProvider,
        weekDayScheduler,
        periodicScheduler,
        monthDayScheduler,
        timeSinceLastScheduler,
        oneTimeScheduler,
    )

    @Test
    fun `delegates WeekDayParams to weekDayScheduler`() = runTest {
        // PREPARE
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneId.of("UTC"))
        val reminder = reminderFixture.copy(
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(14, 0),
                checkedDays = CheckedDays.none().copy(monday = true)
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY
        assertEquals(weekDayResult, result)
        assertEquals(true, weekDaySchedulerCalled)
        assertEquals(false, periodicSchedulerCalled)
        assertEquals(false, monthDaySchedulerCalled)
        assertEquals(false, timeSinceLastSchedulerCalled)
    }

    @Test
    fun `delegates PeriodicParams to periodicScheduler`() = runTest {
        // PREPARE
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneId.of("UTC"))
        val reminder = reminderFixture.copy(
            params = ReminderParams.PeriodicParams(
                starts = LocalDateTime.of(2024, 1, 1, 10, 0),
                ends = null,
                interval = 1,
                period = Period.DAYS
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY
        assertEquals(periodicResult, result)
        assertEquals(false, weekDaySchedulerCalled)
        assertEquals(true, periodicSchedulerCalled)
        assertEquals(false, monthDaySchedulerCalled)
        assertEquals(false, timeSinceLastSchedulerCalled)
    }

    @Test
    fun `delegates MonthDayParams to monthDayScheduler`() = runTest {
        // PREPARE
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneId.of("UTC"))
        val reminder = reminderFixture.copy(
            params = ReminderParams.MonthDayParams(
                occurrence = MonthDayOccurrence.FIRST,
                dayType = MonthDayType.DAY,
                time = LocalTime.of(14, 0),
                ends = null
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY
        assertEquals(monthDayResult, result)
        assertEquals(false, weekDaySchedulerCalled)
        assertEquals(false, periodicSchedulerCalled)
        assertEquals(true, monthDaySchedulerCalled)
        assertEquals(false, timeSinceLastSchedulerCalled)
    }

    @Test
    fun `delegates TimeSinceLastParams to timeSinceLastScheduler`() = runTest {
        // PREPARE
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneId.of("UTC"))
        val reminder = reminderFixture.copy(
            featureId = 123L,
            params = ReminderParams.TimeSinceLastParams(
                firstInterval = IntervalPeriodPair(interval = 3, period = Period.DAYS),
                secondInterval = null
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY
        assertEquals(timeSinceLastResult, result)
        assertEquals(false, weekDaySchedulerCalled)
        assertEquals(false, periodicSchedulerCalled)
        assertEquals(false, monthDaySchedulerCalled)
        assertEquals(true, timeSinceLastSchedulerCalled)
        assertEquals(false, oneTimeSchedulerCalled)
    }

    @Test
    fun `delegates OneTimeParams to oneTimeScheduler`() = runTest {
        val reminder = reminderFixture.copy(
            params = ReminderParams.OneTimeParams(
                createdAt = LocalDateTime.of(2024, 1, 10, 10, 0),
                starts = LocalDateTime.of(2024, 1, 10, 12, 0),
                initialDelay = IntervalPeriodPair(2, Period.HOURS),
                repeatInterval = null,
            )
        )

        val result = uut.scheduleNext(reminder)

        assertEquals(oneTimeResult, result)
        assertEquals(false, weekDaySchedulerCalled)
        assertEquals(false, periodicSchedulerCalled)
        assertEquals(false, monthDaySchedulerCalled)
        assertEquals(false, timeSinceLastSchedulerCalled)
        assertEquals(true, oneTimeSchedulerCalled)
    }

    @Test
    fun `disabled reminders do not delegate to type scheduler`() = runTest {
        // PREPARE
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneId.of("UTC"))
        val reminder = reminderFixture.copy(
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(14, 0),
                checkedDays = CheckedDays.all(),
                enabled = false
            )
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY
        assertEquals(null, result)
        assertEquals(false, weekDaySchedulerCalled)
        assertEquals(false, periodicSchedulerCalled)
        assertEquals(false, monthDaySchedulerCalled)
        assertEquals(false, timeSinceLastSchedulerCalled)
        assertEquals(false, oneTimeSchedulerCalled)
    }
}
