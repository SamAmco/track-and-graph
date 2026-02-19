/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.data.interactor

import com.samco.trackandgraph.FakeGroupItemDao
import com.samco.trackandgraph.FakeReminderDao
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.ReminderCreateRequest
import com.samco.trackandgraph.data.database.dto.ReminderDisplayOrderData
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.database.dto.ReminderUpdateRequest
import com.samco.trackandgraph.data.serialization.ReminderSerializer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderHelperImplTest {

    private lateinit var fakeReminderDao: FakeReminderDao
    private lateinit var fakeGroupItemDao: FakeGroupItemDao
    private lateinit var reminderSerializer: ReminderSerializer
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private lateinit var uut: ReminderHelperImpl

    private val defaultParams = ReminderParams.WeekDayParams(
        time = LocalTime.of(9, 0),
        checkedDays = CheckedDays(
            monday = true,
            tuesday = true,
            wednesday = true,
            thursday = true,
            friday = true,
            saturday = false,
            sunday = false
        )
    )

    @Before
    fun before() {
        fakeReminderDao = FakeReminderDao()
        fakeGroupItemDao = FakeGroupItemDao()
        reminderSerializer = ReminderSerializer(Json { ignoreUnknownKeys = true })

        uut = ReminderHelperImpl(
            reminderDao = fakeReminderDao,
            groupItemDao = fakeGroupItemDao,
            reminderSerializer = reminderSerializer,
            io = dispatcher
        )
    }

    // =========================================================================
    // Create tests
    // =========================================================================

    @Test
    fun `createReminder inserts reminder and returns id`() = runTest(dispatcher) {
        // PREPARE
        val request = ReminderCreateRequest(
            reminderName = "Morning Reminder",
            groupId = null,
            featureId = null,
            params = defaultParams
        )

        // EXECUTE
        val id = uut.createReminder(request)

        // VERIFY
        assertTrue(id > 0)
        val reminder = uut.getReminderById(id)
        assertNotNull(reminder)
        assertEquals("Morning Reminder", reminder!!.reminderName)
        assertEquals(null, reminder.featureId)
        assertEquals(defaultParams, reminder.params)
    }

    @Test
    fun `createReminder with groupId and featureId stores them correctly`() = runTest(dispatcher) {
        // PREPARE
        val request = ReminderCreateRequest(
            reminderName = "Grouped Reminder",
            groupId = 5L,
            featureId = 10L,
            params = defaultParams
        )

        // EXECUTE
        val id = uut.createReminder(request)

        // VERIFY
        val reminder = uut.getReminderById(id)
        assertNotNull(reminder)
        assertEquals(10L, reminder!!.featureId)
    }

    @Test
    fun `createReminder returns valid id`() = runTest(dispatcher) {
        // PREPARE
        val request = ReminderCreateRequest(
            reminderName = "Test",
            groupId = null,
            featureId = null,
            params = defaultParams
        )

        // EXECUTE
        val id = uut.createReminder(request)

        // VERIFY
        val reminder = uut.getReminderById(id)
        assertNotNull(reminder)
        assertEquals("Test", reminder!!.reminderName)
    }

    // =========================================================================
    // Update tests
    // =========================================================================

    @Test
    fun `updateReminder updates name when provided`() = runTest(dispatcher) {
        // PREPARE
        val id = uut.createReminder(
            ReminderCreateRequest(
                reminderName = "Original Name",
                groupId = null,
                featureId = null,
                params = defaultParams
            )
        )

        // EXECUTE
        uut.updateReminder(
            ReminderUpdateRequest(
                id = id,
                reminderName = "Updated Name",
                featureId = null,
                params = null
            )
        )

        // VERIFY
        val reminder = uut.getReminderById(id)
        assertEquals("Updated Name", reminder!!.reminderName)
        assertEquals(defaultParams, reminder.params) // Params unchanged
    }

    @Test
    fun `updateReminder updates params when provided`() = runTest(dispatcher) {
        // PREPARE
        val id = uut.createReminder(
            ReminderCreateRequest(
                reminderName = "Test",
                groupId = null,
                featureId = null,
                params = defaultParams
            )
        )

        val newParams = ReminderParams.WeekDayParams(
            time = LocalTime.of(18, 30),
            checkedDays = CheckedDays(
                monday = false,
                tuesday = false,
                wednesday = false,
                thursday = false,
                friday = false,
                saturday = true,
                sunday = true
            )
        )

        // EXECUTE
        uut.updateReminder(
            ReminderUpdateRequest(
                id = id,
                reminderName = null,
                featureId = null,
                params = newParams
            )
        )

        // VERIFY
        val reminder = uut.getReminderById(id)
        assertEquals("Test", reminder!!.reminderName) // Name unchanged
        assertEquals(newParams, reminder.params)
    }

    @Test
    fun `updateReminder updates featureId when provided`() = runTest(dispatcher) {
        // PREPARE
        val id = uut.createReminder(
            ReminderCreateRequest(
                reminderName = "Test",
                groupId = null,
                featureId = null,
                params = defaultParams
            )
        )

        // EXECUTE
        uut.updateReminder(
            ReminderUpdateRequest(
                id = id,
                reminderName = null,
                featureId = 42L,
                params = null
            )
        )

        // VERIFY
        val reminder = uut.getReminderById(id)
        assertEquals(42L, reminder!!.featureId)
    }

    @Test
    fun `updateReminder updates only provided fields`() = runTest(dispatcher) {
        // PREPARE
        val id = uut.createReminder(
            ReminderCreateRequest(
                reminderName = "Test",
                groupId = 5L,
                featureId = null,
                params = defaultParams
            )
        )

        // EXECUTE - update name only
        uut.updateReminder(
            ReminderUpdateRequest(
                id = id,
                reminderName = "New Name",
                featureId = null,
                params = null
            )
        )

        // VERIFY - name should be updated
        val reminder = uut.getReminderById(id)
        assertEquals("New Name", reminder!!.reminderName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateReminder throws when reminder not found`() = runTest(dispatcher) {
        // EXECUTE
        uut.updateReminder(
            ReminderUpdateRequest(
                id = 999L,
                reminderName = "Test",
                featureId = null,
                params = null
            )
        )
    }

    // =========================================================================
    // Delete tests
    // =========================================================================

    @Test
    fun `deleteReminder removes reminder from dao`() = runTest(dispatcher) {
        // PREPARE
        val id = uut.createReminder(
            ReminderCreateRequest("To Delete", null, null, defaultParams)
        )
        assertNotNull(uut.getReminderById(id))

        // EXECUTE
        uut.deleteReminder(id)

        // VERIFY
        assertNull(uut.getReminderById(id))
    }

    // =========================================================================
    // Duplicate tests
    // =========================================================================

    @Test
    fun `duplicateReminder creates copy with new id`() = runTest(dispatcher) {
        // PREPARE
        val originalId = uut.createReminder(
            ReminderCreateRequest(
                reminderName = "Original",
                groupId = 5L,
                featureId = 10L,
                params = defaultParams
            )
        )

        // EXECUTE
        val duplicateId = uut.duplicateReminder(originalId)

        // VERIFY
        assertNotEquals(originalId, duplicateId)

        val original = uut.getReminderById(originalId)
        val duplicate = uut.getReminderById(duplicateId)

        assertNotNull(original)
        assertNotNull(duplicate)
        assertNotEquals(original!!.id, duplicate!!.id)
        assertEquals(original.reminderName, duplicate.reminderName)
        assertEquals(original.featureId, duplicate.featureId)
        assertEquals(original.params, duplicate.params)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicateReminder throws when reminder not found`() = runTest(dispatcher) {
        // EXECUTE
        uut.duplicateReminder(999L)
    }

    // =========================================================================
    // Get tests
    // =========================================================================

    @Test
    fun `getReminderById returns null when not found`() = runTest(dispatcher) {
        // EXECUTE & VERIFY
        assertNull(uut.getReminderById(999L))
    }

    @Test
    fun `hasAnyReminders returns false when empty`() = runTest(dispatcher) {
        // EXECUTE & VERIFY
        assertEquals(false, uut.hasAnyReminders())
    }

    @Test
    fun `hasAnyReminders returns true when reminders exist`() = runTest(dispatcher) {
        // PREPARE
        uut.createReminder(
            ReminderCreateRequest("Test", null, null, defaultParams)
        )

        // EXECUTE & VERIFY
        assertEquals(true, uut.hasAnyReminders())
    }
}
