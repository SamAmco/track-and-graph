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

package com.samco.trackandgraph.reminders

import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.interactor.DataInteractor
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.threeten.bp.Instant
import org.threeten.bp.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
internal class ReminderInteractorImplTest {

    // Test dependencies
    private val dataInteractor: DataInteractor = mock()
    private val reminderPref = FakeReminderPrefWrapper()
    private val platformScheduler = FakePlatformScheduler()
    private val reminderScheduler = FakeReminderScheduler()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

    private val uut = ReminderInteractorImpl(
        reminderPref = reminderPref,
        platformScheduler = platformScheduler,
        dataInteractor = dataInteractor,
        reminderScheduler = reminderScheduler,
        json = json,
        io = testDispatcher
    )

    @Test
    fun `sync notifications schedules notifications for all reminders`() = runTest(testDispatcher) {
        // PREPARE
        val reminder1 = reminderFixture.copy(
            id = 1L,
            reminderName = "Morning Reminder",
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(8, 0),
                checkedDays = CheckedDays.none().copy(monday = true, wednesday = true)
            ),
        )
        val reminder2 = reminderFixture.copy(
            id = 2L,
            displayIndex = 1,
            reminderName = "Evening Reminder",
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(20, 0),
                checkedDays = CheckedDays.none().copy(tuesday = true, thursday = true)
            ),
        )

        whenever(dataInteractor.getAllRemindersSync()).thenReturn(listOf(reminder1, reminder2))

        val nextNotificationTime1 = Instant.ofEpochMilli(1000000L)
        val nextNotificationTime2 = Instant.ofEpochMilli(2000000L)
        reminderScheduler.setNextNotificationTime(1L, nextNotificationTime1)
        reminderScheduler.setNextNotificationTime(2L, nextNotificationTime2)

        // EXECUTE
        uut.ensureReminderNotifications()

        // VERIFY
        assertEquals(2, platformScheduler.setNotifications.size)

        val (triggerTime1, notificationParams1) = platformScheduler.setNotifications[0]
        assertEquals(1000000L, triggerTime1)
        assertEquals(1L, notificationParams1.reminderId)
        assertEquals("Morning Reminder", notificationParams1.reminderName)

        val (triggerTime2, notificationParams2) = platformScheduler.setNotifications[1]
        assertEquals(2000000L, triggerTime2)
        assertEquals(2L, notificationParams2.reminderId)
        assertEquals("Evening Reminder", notificationParams2.reminderName)
    }

    @Test
    fun `sync notifications with legacy reminders clears legacy and schedules new`() =
        runTest(testDispatcher) {
            // PREPARE - Realistic scenario: same reminders exist in database as were stored in legacy JSON
            val legacyNotifications = """
            [
                {"reminderId":123,"reminderName":"Morning Workout","pendingIntentId":999},
                {"reminderId":456,"reminderName":"Evening Meditation","pendingIntentId":888}
            ]
        """.trimIndent()
            reminderPref.encodedIntents = legacyNotifications

            val reminders = listOf(
                reminderFixture.copy(
                    id = 123L,
                    reminderName = "Morning Workout",
                    params = ReminderParams.WeekDayParams(
                        time = LocalTime.of(7, 0),
                        checkedDays = CheckedDays.none()
                            .copy(monday = true, wednesday = true, friday = true)
                    ),
                ),
                reminderFixture.copy(
                    id = 456L,
                    displayIndex = 1,
                    reminderName = "Evening Meditation",
                    params = ReminderParams.WeekDayParams(
                        time = LocalTime.of(19, 30),
                        checkedDays = CheckedDays.none().copy(tuesday = true, thursday = true)
                    ),
                )
            )

            whenever(dataInteractor.getAllRemindersSync()).thenReturn(reminders)
            reminderScheduler.setNextNotificationTime(123L, Instant.ofEpochMilli(1000000L))
            reminderScheduler.setNextNotificationTime(456L, Instant.ofEpochMilli(2000000L))

            // EXECUTE
            uut.ensureReminderNotifications()

            // VERIFY
            // Legacy notifications should be cancelled
            assertEquals(2, platformScheduler.cancelledStoredAlarms.size)
            assertTrue(platformScheduler.cancelledStoredAlarms.any { it.pendingIntentId == 999 && it.reminderId == 123L })
            assertTrue(platformScheduler.cancelledStoredAlarms.any { it.pendingIntentId == 888 && it.reminderId == 456L })

            // New notifications should be scheduled for the same reminders
            assertEquals(2, platformScheduler.setNotifications.size)

            val (triggerTime1, notificationParams1) = platformScheduler.setNotifications[0]
            assertEquals(1000000L, triggerTime1)
            assertEquals(123L, notificationParams1.reminderId)
            assertEquals("Morning Workout", notificationParams1.reminderName)

            val (triggerTime2, notificationParams2) = platformScheduler.setNotifications[1]
            assertEquals(2000000L, triggerTime2)
            assertEquals(456L, notificationParams2.reminderId)
            assertEquals("Evening Meditation", notificationParams2.reminderName)

            // Verify notification IDs are different from both pending intent IDs and reminder IDs
            assertTrue(
                "Notification ID should not equal pending intent ID",
                notificationParams1.alarmId != 999
            )
            assertTrue(
                "Notification ID should not equal reminder ID",
                notificationParams1.alarmId != 123
            )
            assertTrue(
                "Notification ID should not equal pending intent ID",
                notificationParams2.alarmId != 888
            )
            assertTrue(
                "Notification ID should not equal reminder ID",
                notificationParams2.alarmId != 456
            )
            assertTrue(
                "Notification IDs should be unique",
                notificationParams1.alarmId != notificationParams2.alarmId
            )

            // Legacy storage should be cleared
            assertEquals(null, reminderPref.getStoredIntents())
        }

    @Test
    fun `schedule next schedules notification for reminder`() = runTest(testDispatcher) {
        // PREPARE
        val reminder = reminderFixture.copy(
            id = 5L,
            reminderName = "Next Reminder",
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(15, 30),
                checkedDays = CheckedDays.none().copy(friday = true)
            ),
        )

        val nextNotificationTime = Instant.ofEpochMilli(7000000L)
        reminderScheduler.setNextNotificationTime(5L, nextNotificationTime)

        // EXECUTE
        uut.scheduleNext(reminder)

        // VERIFY
        assertEquals(1, platformScheduler.setNotifications.size)
        val (triggerTime, notificationParams) = platformScheduler.setNotifications[0]
        assertEquals(7000000L, triggerTime)
        assertEquals(5L, notificationParams.reminderId)
        assertEquals("Next Reminder", notificationParams.reminderName)
    }

    @Test
    fun `schedule next with no next time does not schedule notification`() =
        runTest(testDispatcher) {
            // PREPARE
            val reminder = reminderFixture.copy(
                id = 6L,
                reminderName = "No Schedule Reminder",
                params = ReminderParams.WeekDayParams(
                    time = LocalTime.of(12, 0),
                    checkedDays = CheckedDays.none() // No days checked
                ),
            )

            reminderScheduler.setNextNotificationTime(6L, null) // No next time

            // EXECUTE
            uut.scheduleNext(reminder)

            // VERIFY
            assertEquals(0, platformScheduler.setNotifications.size)
        }

    @Test
    fun `cancel notifications cancels notification for reminder`() = runTest(testDispatcher) {
        // PREPARE
        val reminder = reminderFixture.copy(
            id = 10L,
            reminderName = "Delete Me",
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(11, 0),
                checkedDays = CheckedDays.none().copy(saturday = true)
            ),
        )

        // EXECUTE
        uut.cancelReminderNotifications(reminder)

        // VERIFY
        assertEquals(1, platformScheduler.cancelledNotifications.size)
        val cancelledNotification = platformScheduler.cancelledNotifications[0]
        assertEquals(10L, cancelledNotification.reminderId)
        assertEquals("Delete Me", cancelledNotification.reminderName)
    }

    @Test
    fun `schedule next emits SCHEDULED event`() = runTest(testDispatcher) {
        // PREPARE
        val reminder = reminderFixture.copy(
            id = 5L,
            reminderName = "Event Test Reminder",
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(15, 30),
                checkedDays = CheckedDays.none().copy(friday = true)
            ),
        )

        val nextNotificationTime = Instant.ofEpochMilli(7000000L)
        reminderScheduler.setNextNotificationTime(5L, nextNotificationTime)

        // EXECUTE and VERIFY
        uut.schedulingEvents.test {
            uut.scheduleNext(reminder)
            
            val event = awaitItem()
            assertEquals(5L, event.reminderId)
            assertEquals(SchedulingEventType.SCHEDULED, event.eventType)
            assertEquals(7000000L, event.scheduledTimeMillis)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancel notifications emits CANCELLED event`() = runTest(testDispatcher) {
        // PREPARE
        val reminder = reminderFixture.copy(
            id = 10L,
            reminderName = "Cancel Test Reminder",
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(11, 0),
                checkedDays = CheckedDays.none().copy(saturday = true)
            ),
        )

        // EXECUTE and VERIFY
        uut.schedulingEvents.test {
            uut.cancelReminderNotifications(reminder)
            
            val event = awaitItem()
            assertEquals(10L, event.reminderId)
            assertEquals(SchedulingEventType.CANCELLED, event.eventType)
            assertEquals(null, event.scheduledTimeMillis)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clear notifications emits CANCELLED events for all reminders`() = runTest(testDispatcher) {
        // PREPARE
        val reminders = listOf(
            reminderFixture.copy(
                id = 20L,
                reminderName = "Clear Me 1",
                params = ReminderParams.WeekDayParams(
                    time = LocalTime.of(7, 0),
                    checkedDays = CheckedDays.none().copy(monday = true)
                ),
            ),
            reminderFixture.copy(
                id = 21L,
                displayIndex = 1,
                reminderName = "Clear Me 2",
                params = ReminderParams.WeekDayParams(
                    time = LocalTime.of(19, 0),
                    checkedDays = CheckedDays.none().copy(tuesday = true)
                ),
            )
        )

        whenever(dataInteractor.getAllRemindersSync()).thenReturn(reminders)

        // EXECUTE and VERIFY
        uut.schedulingEvents.test {
            uut.clearNotifications()
            
            val event1 = awaitItem()
            val event2 = awaitItem()
            
            val eventIds = listOf(event1.reminderId, event2.reminderId)
            assertTrue(eventIds.contains(20L))
            assertTrue(eventIds.contains(21L))
            
            assertEquals(SchedulingEventType.CANCELLED, event1.eventType)
            assertEquals(null, event1.scheduledTimeMillis)
            assertEquals(SchedulingEventType.CANCELLED, event2.eventType)
            assertEquals(null, event2.scheduledTimeMillis)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clear notifications cancels all notifications`() = runTest(testDispatcher) {
        // PREPARE
        val reminders = listOf(
            reminderFixture.copy(
                id = 20L,
                reminderName = "Clear Me 1",
                params = ReminderParams.WeekDayParams(
                    time = LocalTime.of(7, 0),
                    checkedDays = CheckedDays.none().copy(monday = true)
                ),
            ),
            reminderFixture.copy(
                id = 21L,
                displayIndex = 1,
                reminderName = "Clear Me 2",
                params = ReminderParams.WeekDayParams(
                    time = LocalTime.of(19, 0),
                    checkedDays = CheckedDays.none().copy(tuesday = true)
                ),
            )
        )

        whenever(dataInteractor.getAllRemindersSync()).thenReturn(reminders)

        // EXECUTE
        uut.clearNotifications()

        // VERIFY
        assertEquals(2, platformScheduler.cancelledNotifications.size)

        val cancelledIds = platformScheduler.cancelledNotifications.map { it.reminderId }
        assertTrue(cancelledIds.contains(20L))
        assertTrue(cancelledIds.contains(21L))

        val cancelledNames = platformScheduler.cancelledNotifications.map { it.reminderName }
        assertTrue(cancelledNames.contains("Clear Me 1"))
        assertTrue(cancelledNames.contains("Clear Me 2"))
    }

}