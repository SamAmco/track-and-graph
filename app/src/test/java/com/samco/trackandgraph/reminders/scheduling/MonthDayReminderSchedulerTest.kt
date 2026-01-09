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

import com.samco.trackandgraph.data.database.dto.MonthDayOccurrence
import com.samco.trackandgraph.data.database.dto.MonthDayType
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.time.FakeTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

internal class MonthDayReminderSchedulerTest {

    private val timeProvider = FakeTimeProvider()
    private val uut = MonthDayReminderScheduler(timeProvider)

    @Test
    fun `schedule next returns current month occurrence when time has not passed`() {
        // PREPARE - It's January 10th 10:00 AM, reminder is for 1st of month at 2:00 PM
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 10, 10, 0, 0, 0, ZoneId.of("UTC"))

        val params = ReminderParams.MonthDayParams(
            occurrence = MonthDayOccurrence.FIRST,
            dayType = MonthDayType.DAY,
            time = LocalTime.of(14, 0), // 2:00 PM
            ends = null
        )

        // EXECUTE
        val result = uut.scheduleNext(params, timeProvider.now().toInstant())

        // VERIFY - Should schedule for January 1st at 2:00 PM (current month, but time already passed)
        // Since Jan 1st has passed, should schedule for February 1st
        val expected = ZonedDateTime.of(2024, 2, 1, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should schedule for next month when current month occurrence has passed", expected, result)
    }

    @Test
    fun `schedule next returns current month occurrence when time has not passed today`() {
        // PREPARE - It's January 15th 10:00 AM, reminder is for 20th of month at 2:00 PM
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 4, 10, 0, 0, 0, ZoneId.of("UTC"))

        val params = ReminderParams.MonthDayParams(
            occurrence = MonthDayOccurrence.FOURTH, // 4th day = 4th of month
            dayType = MonthDayType.DAY,
            time = LocalTime.of(14, 0), // 2:00 PM
            ends = null
        )

        // EXECUTE
        val result = uut.scheduleNext(params, timeProvider.now().toInstant())

        // VERIFY - Should schedule for January 4th
        val expected = ZonedDateTime.of(2024, 1, 4, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should schedule for next month when current month occurrence has passed", expected, result)
    }

    @Test
    fun `schedule next handles last day of month correctly`() {
        // PREPARE - It's January 15th, reminder is for last day of month
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 15, 10, 0, 0, 0, ZoneId.of("UTC"))

        val params = ReminderParams.MonthDayParams(
            occurrence = MonthDayOccurrence.LAST,
            dayType = MonthDayType.DAY,
            time = LocalTime.of(14, 0),
            ends = null
        )

        // EXECUTE
        val result = uut.scheduleNext(params, timeProvider.now().toInstant())

        // VERIFY - Should schedule for January 31st (last day of January)
        val expected = ZonedDateTime.of(2024, 1, 31, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should schedule for last day of current month", expected, result)
    }

    @Test
    fun `schedule next handles first Monday of month`() {
        // PREPARE - It's January 5th 2024 (Friday), reminder is for first Monday of month
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 5, 10, 0, 0, 0, ZoneId.of("UTC"))

        val params = ReminderParams.MonthDayParams(
            occurrence = MonthDayOccurrence.FIRST,
            dayType = MonthDayType.MONDAY,
            time = LocalTime.of(14, 0),
            ends = null
        )

        // EXECUTE
        val result = uut.scheduleNext(params, timeProvider.now().toInstant())

        // VERIFY - First Monday of January 2024 is January 1st, which has passed, so February 5th
        val expected = ZonedDateTime.of(2024, 2, 5, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should schedule for first Monday of next month", expected, result)
    }

    @Test
    fun `schedule next handles second Tuesday of month`() {
        // PREPARE - It's January 5th 2024, reminder is for second Tuesday of month
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 5, 10, 0, 0, 0, ZoneId.of("UTC"))

        val params = ReminderParams.MonthDayParams(
            occurrence = MonthDayOccurrence.SECOND,
            dayType = MonthDayType.TUESDAY,
            time = LocalTime.of(14, 0),
            ends = null
        )

        // EXECUTE
        val result = uut.scheduleNext(params, timeProvider.now().toInstant())

        // VERIFY - Second Tuesday of January 2024 is January 9th
        val expected = ZonedDateTime.of(2024, 1, 9, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should schedule for second Tuesday of current month", expected, result)
    }

    @Test
    fun `schedule next handles last Friday of month`() {
        // PREPARE - It's January 15th 2024, reminder is for last Friday of month
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 15, 10, 0, 0, 0, ZoneId.of("UTC"))

        val params = ReminderParams.MonthDayParams(
            occurrence = MonthDayOccurrence.LAST,
            dayType = MonthDayType.FRIDAY,
            time = LocalTime.of(14, 0),
            ends = null
        )

        // EXECUTE
        val result = uut.scheduleNext(params, timeProvider.now().toInstant())

        // VERIFY - Last Friday of January 2024 is January 26th
        val expected = ZonedDateTime.of(2024, 1, 26, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should schedule for last Friday of current month", expected, result)
    }

    @Test
    fun `schedule next returns null when past end time`() {
        // PREPARE - Current time is after end time
        timeProvider.currentTime = ZonedDateTime.of(2024, 3, 15, 10, 0, 0, 0, ZoneId.of("UTC"))

        val params = ReminderParams.MonthDayParams(
            occurrence = MonthDayOccurrence.FIRST,
            dayType = MonthDayType.DAY,
            time = LocalTime.of(14, 0),
            ends = LocalDateTime.of(2024, 2, 28, 23, 59) // End in February
        )

        // EXECUTE
        val result = uut.scheduleNext(params, timeProvider.now().toInstant())

        // VERIFY
        assertNull("Should return null when past end time", result)
    }

    @Test
    fun `schedule next returns null when next occurrence would be past end time`() {
        // PREPARE - Current time is before end, but next occurrence would be after end
        timeProvider.currentTime = ZonedDateTime.of(2024, 2, 25, 10, 0, 0, 0, ZoneId.of("UTC"))

        val params = ReminderParams.MonthDayParams(
            occurrence = MonthDayOccurrence.FIRST,
            dayType = MonthDayType.DAY,
            time = LocalTime.of(14, 0),
            ends = LocalDateTime.of(2024, 2, 28, 23, 59) // End before March 1st
        )

        // EXECUTE
        val result = uut.scheduleNext(params, timeProvider.now().toInstant())

        // VERIFY
        assertNull("Should return null when next occurrence would be past end time", result)
    }

    @Test
    fun `schedule next handles timezone correctly`() {
        // PREPARE - Set a different timezone (EST) and test scheduling
        val estZone = ZoneId.of("America/New_York")
        timeProvider.timeZone = estZone
        timeProvider.currentTime = ZonedDateTime.of(2024, 1, 15, 10, 0, 0, 0, estZone)

        val params = ReminderParams.MonthDayParams(
            occurrence = MonthDayOccurrence.LAST,
            dayType = MonthDayType.DAY,
            time = LocalTime.of(14, 0),
            ends = null
        )

        // EXECUTE
        val result = uut.scheduleNext(params, timeProvider.now().toInstant())

        // VERIFY - Should schedule for January 31st at 2:00 PM EST
        val expected = ZonedDateTime.of(2024, 1, 31, 14, 0, 0, 0, estZone).toInstant()
        assertEquals("Should handle timezone correctly", expected, result)
    }

    @Test
    fun `schedule next adds buffer to avoid race conditions`() {
        // PREPARE - It's exactly 2:00 PM on the second of February, reminder is for 2:00 PM on the second
        timeProvider.currentTime = ZonedDateTime.of(2024, 2, 2, 14, 0, 0, 0, ZoneId.of("UTC"))

        val params = ReminderParams.MonthDayParams(
            occurrence = MonthDayOccurrence.SECOND, // 2nd day = 2nd of month
            dayType = MonthDayType.DAY,
            time = LocalTime.of(14, 0), // Same time as current
            ends = null
        )

        // EXECUTE
        val result = uut.scheduleNext(params, timeProvider.now().toInstant())

        // VERIFY - Should schedule for next month due to 2-second buffer
        val expected = ZonedDateTime.of(2024, 3, 2, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should add buffer to avoid race conditions", expected, result)
    }

    @Test
    fun `schedule next handles leap year February correctly`() {
        // PREPARE - It's February 15th 2024 (leap year), reminder is for last day of month
        timeProvider.currentTime = ZonedDateTime.of(2024, 2, 15, 10, 0, 0, 0, ZoneId.of("UTC"))

        val params = ReminderParams.MonthDayParams(
            occurrence = MonthDayOccurrence.LAST,
            dayType = MonthDayType.DAY,
            time = LocalTime.of(14, 0),
            ends = null
        )

        // EXECUTE
        val result = uut.scheduleNext(params, timeProvider.now().toInstant())

        // VERIFY - Should schedule for February 29th (leap year)
        val expected = ZonedDateTime.of(2024, 2, 29, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should handle leap year February correctly", expected, result)
    }
}
