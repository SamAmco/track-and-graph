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
import com.samco.trackandgraph.data.interactor.DataInteractor
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
internal class AlarmInteractorImplTest {

    // Test dependencies
    private val dataInteractor: DataInteractor = mock()
    private val reminderPref = FakeReminderPrefWrapper()
    private val alarmManager = FakeAlarmManagerWrapper()
    private val alarmScheduler = FakeAlarmScheduler()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }
    
    private val uut = AlarmInteractorImpl(
        reminderPref = reminderPref,
        alarmManager = alarmManager,
        dataInteractor = dataInteractor,
        alarmScheduler = alarmScheduler,
        json = json,
        io = testDispatcher
    )

    @Test
    fun `sync alarms schedules alarms for all reminders`() = runTest(testDispatcher) {
        // PREPARE
        val reminder1 = reminderFixture.copy(
            id = 1L,
            alarmName = "Morning Reminder",
            time = LocalTime.of(8, 0),
            checkedDays = CheckedDays.none().copy(monday = true, wednesday = true)
        )
        val reminder2 = reminderFixture.copy(
            id = 2L,
            displayIndex = 1,
            alarmName = "Evening Reminder",
            time = LocalTime.of(20, 0),
            checkedDays = CheckedDays.none().copy(tuesday = true, thursday = true)
        )
        
        whenever(dataInteractor.getAllRemindersSync()).thenReturn(listOf(reminder1, reminder2))
        
        val nextAlarmTime1 = Instant.ofEpochMilli(1000000L)
        val nextAlarmTime2 = Instant.ofEpochMilli(2000000L)
        alarmScheduler.setNextAlarmTime(1L, nextAlarmTime1)
        alarmScheduler.setNextAlarmTime(2L, nextAlarmTime2)
        
        // EXECUTE
        uut.syncAlarms()
        
        // VERIFY
        assertEquals(2, alarmManager.setAlarms.size)
        
        val (triggerTime1, alarmInfo1) = alarmManager.setAlarms[0]
        assertEquals(1000000L, triggerTime1)
        assertEquals(1L, alarmInfo1.reminderId)
        assertEquals("Morning Reminder", alarmInfo1.reminderName)
        
        val (triggerTime2, alarmInfo2) = alarmManager.setAlarms[1]
        assertEquals(2000000L, triggerTime2)
        assertEquals(2L, alarmInfo2.reminderId)
        assertEquals("Evening Reminder", alarmInfo2.reminderName)
    }

    @Test
    fun `sync alarms with legacy reminders clears legacy and schedules new`() = runTest(testDispatcher) {
        // PREPARE - Realistic scenario: same reminders exist in database as were stored in legacy JSON
        val legacyAlarms = """
            [
                {"reminderId":123,"reminderName":"Morning Workout","pendingIntentId":999},
                {"reminderId":456,"reminderName":"Evening Meditation","pendingIntentId":888}
            ]
        """.trimIndent()
        reminderPref.encodedIntents = legacyAlarms
        
        val reminders = listOf(
            reminderFixture.copy(
                id = 123L,
                alarmName = "Morning Workout",
                time = LocalTime.of(7, 0),
                checkedDays = CheckedDays.none().copy(monday = true, wednesday = true, friday = true)
            ),
            reminderFixture.copy(
                id = 456L,
                displayIndex = 1,
                alarmName = "Evening Meditation",
                time = LocalTime.of(19, 30),
                checkedDays = CheckedDays.none().copy(tuesday = true, thursday = true)
            )
        )
        
        whenever(dataInteractor.getAllRemindersSync()).thenReturn(reminders)
        alarmScheduler.setNextAlarmTime(123L, Instant.ofEpochMilli(1000000L))
        alarmScheduler.setNextAlarmTime(456L, Instant.ofEpochMilli(2000000L))
        
        // EXECUTE
        uut.syncAlarms()
        
        // VERIFY
        // Legacy alarms should be cancelled
        assertEquals(2, alarmManager.cancelledStoredAlarms.size)
        assertTrue(alarmManager.cancelledStoredAlarms.any { it.pendingIntentId == 999 && it.reminderId == 123L })
        assertTrue(alarmManager.cancelledStoredAlarms.any { it.pendingIntentId == 888 && it.reminderId == 456L })
        
        // New alarms should be scheduled for the same reminders
        assertEquals(2, alarmManager.setAlarms.size)
        
        val (triggerTime1, alarmInfo1) = alarmManager.setAlarms[0]
        assertEquals(1000000L, triggerTime1)
        assertEquals(123L, alarmInfo1.reminderId)
        assertEquals("Morning Workout", alarmInfo1.reminderName)
        
        val (triggerTime2, alarmInfo2) = alarmManager.setAlarms[1]
        assertEquals(2000000L, triggerTime2)
        assertEquals(456L, alarmInfo2.reminderId)
        assertEquals("Evening Meditation", alarmInfo2.reminderName)
        
        // Verify alarm IDs are different from both pending intent IDs and reminder IDs
        assertTrue("Alarm ID should not equal pending intent ID", alarmInfo1.alarmId != 999)
        assertTrue("Alarm ID should not equal reminder ID", alarmInfo1.alarmId != 123)
        assertTrue("Alarm ID should not equal pending intent ID", alarmInfo2.alarmId != 888)
        assertTrue("Alarm ID should not equal reminder ID", alarmInfo2.alarmId != 456)
        assertTrue("Alarm IDs should be unique", alarmInfo1.alarmId != alarmInfo2.alarmId)
        
        // Legacy storage should be cleared
        assertEquals(null, reminderPref.getStoredIntents())
    }

    @Test
    fun `schedule next schedules alarm for reminder`() = runTest(testDispatcher) {
        // PREPARE
        val reminder = reminderFixture.copy(
            id = 5L,
            alarmName = "Next Reminder",
            time = LocalTime.of(15, 30),
            checkedDays = CheckedDays.none().copy(friday = true)
        )
        
        val nextAlarmTime = Instant.ofEpochMilli(7000000L)
        alarmScheduler.setNextAlarmTime(5L, nextAlarmTime)
        
        // EXECUTE
        uut.scheduleNext(reminder)
        
        // VERIFY
        assertEquals(1, alarmManager.setAlarms.size)
        val (triggerTime, alarmInfo) = alarmManager.setAlarms[0]
        assertEquals(7000000L, triggerTime)
        assertEquals(5L, alarmInfo.reminderId)
        assertEquals("Next Reminder", alarmInfo.reminderName)
    }
    
    @Test
    fun `schedule next with no next time does not schedule alarm`() = runTest(testDispatcher) {
        // PREPARE
        val reminder = reminderFixture.copy(
            id = 6L,
            alarmName = "No Schedule Reminder",
            time = LocalTime.of(12, 0),
            checkedDays = CheckedDays.none() // No days checked
        )
        
        alarmScheduler.setNextAlarmTime(6L, null) // No next time
        
        // EXECUTE
        uut.scheduleNext(reminder)
        
        // VERIFY
        assertEquals(0, alarmManager.setAlarms.size)
    }
    
    @Test
    fun `delete alarms cancels alarm for reminder`() = runTest(testDispatcher) {
        // PREPARE
        val reminder = reminderFixture.copy(
            id = 10L,
            alarmName = "Delete Me",
            time = LocalTime.of(11, 0),
            checkedDays = CheckedDays.none().copy(saturday = true)
        )
        
        // EXECUTE
        uut.deleteAlarms(reminder)
        
        // VERIFY
        assertEquals(1, alarmManager.cancelledAlarms.size)
        val cancelledAlarm = alarmManager.cancelledAlarms[0]
        assertEquals(10L, cancelledAlarm.reminderId)
        assertEquals("Delete Me", cancelledAlarm.reminderName)
    }
    
    @Test
    fun `clear alarms cancels all alarms`() = runTest(testDispatcher) {
        // PREPARE
        val reminders = listOf(
            reminderFixture.copy(
                id = 20L,
                alarmName = "Clear Me 1",
                time = LocalTime.of(7, 0),
                checkedDays = CheckedDays.none().copy(monday = true)
            ),
            reminderFixture.copy(
                id = 21L,
                displayIndex = 1,
                alarmName = "Clear Me 2",
                time = LocalTime.of(19, 0),
                checkedDays = CheckedDays.none().copy(tuesday = true)
            )
        )
        
        whenever(dataInteractor.getAllRemindersSync()).thenReturn(reminders)
        
        // EXECUTE
        uut.clearAlarms()
        
        // VERIFY
        assertEquals(2, alarmManager.cancelledAlarms.size)
        
        val cancelledIds = alarmManager.cancelledAlarms.map { it.reminderId }
        assertTrue(cancelledIds.contains(20L))
        assertTrue(cancelledIds.contains(21L))
        
        val cancelledNames = alarmManager.cancelledAlarms.map { it.reminderName }
        assertTrue(cancelledNames.contains("Clear Me 1"))
        assertTrue(cancelledNames.contains("Clear Me 2"))
    }

}