package com.samco.trackandgraph.reminders.scheduling

import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.reminders.reminderFixture
import com.samco.trackandgraph.time.FakeTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

internal class WeekDayReminderSchedulerTest {

    private val timeProvider = FakeTimeProvider()
    private val uut = ReminderSchedulerImpl(timeProvider)

    @Test
    fun `schedule next with no days checked returns null`() {
        // PREPARE
        val reminder = reminderFixture.copy(
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(14, 0),
                checkedDays = CheckedDays.none() // No days enabled
            ),
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY
        assertNull("Should return null when no days are checked", result)
    }

    @Test
    fun `schedule next when today is enabled and reminder time is later today returns today`() {
        // PREPARE - It's Tuesday 10:00 AM, reminder is for 2:00 PM, Tuesday is enabled
        timeProvider.currentTime =
            ZonedDateTime.of(2024, 1, 2, 10, 0, 0, 0, ZoneId.of("UTC")) // Tuesday

        val reminder = reminderFixture.copy(
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(14, 0), // 2:00 PM
                checkedDays = CheckedDays.none().copy(tuesday = true)
            ),
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY
        val expected = ZonedDateTime.of(2024, 1, 2, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        assertEquals("Should schedule for today when time is later today", expected, result)
    }

    @Test
    fun `schedule next when today is enabled but reminder time already passed returns next occurrence`() {
        // PREPARE - It's Tuesday 4:00 PM, reminder was for 2:00 PM, Tuesday and Thursday are enabled
        timeProvider.currentTime =
            ZonedDateTime.of(2024, 1, 2, 16, 0, 0, 0, ZoneId.of("UTC")) // Tuesday 4:00 PM

        val reminder = reminderFixture.copy(
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(14, 0), // 2:00 PM (already passed)
                checkedDays = CheckedDays.none().copy(tuesday = true, thursday = true)
            ),
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should schedule for Thursday at 2:00 PM
        val expected =
            ZonedDateTime.of(2024, 1, 4, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant() // Thursday
        assertEquals(
            "Should schedule for next occurrence when today's time has passed",
            expected,
            result
        )
    }

    @Test
    fun `schedule next with today not enabled returns next enabled day`() {
        // PREPARE - It's Tuesday 10:00 AM, reminder is for 2:00 PM, but only Thursday is enabled
        timeProvider.currentTime =
            ZonedDateTime.of(2024, 1, 2, 10, 0, 0, 0, ZoneId.of("UTC")) // Tuesday

        val reminder = reminderFixture.copy(
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(14, 0), // 2:00 PM
                checkedDays = CheckedDays.none().copy(thursday = true) // Only Thursday enabled
            ),
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should schedule for Thursday at 2:00 PM
        val expected =
            ZonedDateTime.of(2024, 1, 4, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant() // Thursday
        assertEquals(
            "Should schedule for next enabled day when today is not enabled",
            expected,
            result
        )
    }

    @Test
    fun `schedule next with multiple days enabled returns earliest next occurrence`() {
        // PREPARE - It's Tuesday 10:00 AM, reminder is for 2:00 PM, Wednesday and Friday are enabled
        timeProvider.currentTime =
            ZonedDateTime.of(2024, 1, 2, 10, 0, 0, 0, ZoneId.of("UTC")) // Tuesday

        val reminder = reminderFixture.copy(
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(14, 0), // 2:00 PM
                checkedDays = CheckedDays.none()
                    .copy(wednesday = true, friday = true) // Wed & Fri enabled
            ),
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should schedule for Wednesday (earliest), not Friday
        val expected =
            ZonedDateTime.of(2024, 1, 3, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant() // Wednesday
        assertEquals(
            "Should schedule for earliest next occurrence when multiple days are enabled",
            expected,
            result
        )
    }

    @Test
    fun `schedule next wraps to next week when needed`() {
        // PREPARE - It's Friday 10:00 AM, reminder is for 2:00 PM, but only Monday is enabled
        timeProvider.currentTime =
            ZonedDateTime.of(2024, 1, 5, 10, 0, 0, 0, ZoneId.of("UTC")) // Friday

        val reminder = reminderFixture.copy(
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(14, 0), // 2:00 PM
                checkedDays = CheckedDays.none().copy(monday = true) // Only Monday enabled
            ),
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should schedule for next Monday (wraps to next week)
        val expected =
            ZonedDateTime.of(2024, 1, 8, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant() // Next Monday
        assertEquals(
            "Should wrap to next week when no enabled days remain this week",
            expected,
            result
        )
    }

    @Test
    fun `schedule next handles timezone correctly`() {
        // PREPARE - Set a different timezone (EST) and test scheduling
        val estZone = ZoneId.of("America/New_York")
        timeProvider.timeZone = estZone
        timeProvider.currentTime =
            ZonedDateTime.of(2024, 1, 2, 10, 0, 0, 0, estZone) // Tuesday 10:00 AM EST

        val reminder = reminderFixture.copy(
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(14, 0), // 2:00 PM
                checkedDays = CheckedDays.none().copy(tuesday = true)
            ),
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should schedule for today at 2:00 PM EST
        val expected = ZonedDateTime.of(2024, 1, 2, 14, 0, 0, 0, estZone).toInstant()
        assertEquals("Should handle timezone correctly", expected, result)
    }

    @Test
    fun `schedule next adds buffer to avoid race conditions`() {
        // PREPARE - It's Tuesday exactly at 2:00 PM, reminder is for 2:00 PM, Tuesday is enabled
        // The buffer should prevent scheduling for the exact same time
        timeProvider.currentTime =
            ZonedDateTime.of(2024, 1, 2, 14, 0, 0, 0, ZoneId.of("UTC")) // Tuesday 2:00 PM exactly

        val reminder = reminderFixture.copy(
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(14, 0), // 2:00 PM (same as current time)
                checkedDays = CheckedDays.none()
                    .copy(tuesday = true, thursday = true) // Tue & Thu enabled
            ),
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should schedule for Thursday (next occurrence) due to 2-second buffer
        val expected =
            ZonedDateTime.of(2024, 1, 4, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant() // Thursday
        assertEquals("Should add buffer to avoid race conditions", expected, result)
    }

    @Test
    fun `schedule next with all days enabled returns next occurrence`() {
        // PREPARE - It's Tuesday 4:00 PM, reminder is for 2:00 PM, all days are enabled (daily reminder)
        timeProvider.currentTime =
            ZonedDateTime.of(2024, 1, 2, 16, 0, 0, 0, ZoneId.of("UTC")) // Tuesday 4:00 PM

        val reminder = reminderFixture.copy(
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(14, 0), // 2:00 PM (already passed today)
                checkedDays = CheckedDays.all() // All days enabled
            ),
        )

        // EXECUTE
        val result = uut.scheduleNext(reminder)

        // VERIFY - Should schedule for tomorrow (Wednesday) at 2:00 PM
        val expected =
            ZonedDateTime.of(2024, 1, 3, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant() // Wednesday
        assertEquals("Should schedule for next day when all days are enabled", expected, result)
    }
}
