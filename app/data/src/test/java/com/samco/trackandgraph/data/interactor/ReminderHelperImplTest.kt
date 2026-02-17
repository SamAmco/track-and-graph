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
        reminderSerializer = ReminderSerializer(Json { ignoreUnknownKeys = true })

        uut = ReminderHelperImpl(
            reminderDao = fakeReminderDao,
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
        assertEquals(null, reminder.groupId)
        assertEquals(null, reminder.featureId)
        assertEquals(0, reminder.displayIndex)
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
        assertEquals(5L, reminder!!.groupId)
        assertEquals(10L, reminder.featureId)
    }

    @Test
    fun `createReminder sets displayIndex to 0`() = runTest(dispatcher) {
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
        assertEquals(0, reminder!!.displayIndex)
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
    fun `updateReminder preserves groupId`() = runTest(dispatcher) {
        // PREPARE
        val id = uut.createReminder(
            ReminderCreateRequest(
                reminderName = "Test",
                groupId = 5L,
                featureId = null,
                params = defaultParams
            )
        )

        // EXECUTE - update something else
        uut.updateReminder(
            ReminderUpdateRequest(
                id = id,
                reminderName = "New Name",
                featureId = null,
                params = null
            )
        )

        // VERIFY - groupId should be unchanged
        val reminder = uut.getReminderById(id)
        assertEquals(5L, reminder!!.groupId)
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
    // Display order tests
    // =========================================================================

    @Test
    fun `updateReminderDisplayOrder updates indices for matching group`() = runTest(dispatcher) {
        // PREPARE - create 3 reminders in same group (null)
        val id1 = uut.createReminder(
            ReminderCreateRequest("Reminder 1", null, null, defaultParams)
        )
        val id2 = uut.createReminder(
            ReminderCreateRequest("Reminder 2", null, null, defaultParams)
        )
        val id3 = uut.createReminder(
            ReminderCreateRequest("Reminder 3", null, null, defaultParams)
        )

        // EXECUTE - reorder: 3, 1, 2
        uut.updateReminderDisplayOrder(
            groupId = null,
            orders = listOf(
                ReminderDisplayOrderData(id3, 0),
                ReminderDisplayOrderData(id1, 1),
                ReminderDisplayOrderData(id2, 2)
            )
        )

        // VERIFY
        assertEquals(1, uut.getReminderById(id1)!!.displayIndex)
        assertEquals(2, uut.getReminderById(id2)!!.displayIndex)
        assertEquals(0, uut.getReminderById(id3)!!.displayIndex)
    }

    @Test
    fun `updateReminderDisplayOrder ignores reminders from different group`() = runTest(dispatcher) {
        // PREPARE - create reminders in different groups
        val idGroup1 = uut.createReminder(
            ReminderCreateRequest("Group 1 Reminder", 1L, null, defaultParams)
        )
        val idGroupNull = uut.createReminder(
            ReminderCreateRequest("Null Group Reminder", null, null, defaultParams)
        )

        // EXECUTE - try to update null group, including id from group 1
        uut.updateReminderDisplayOrder(
            groupId = null,
            orders = listOf(
                ReminderDisplayOrderData(idGroupNull, 5),
                ReminderDisplayOrderData(idGroup1, 10) // Should be ignored
            )
        )

        // VERIFY
        assertEquals(5, uut.getReminderById(idGroupNull)!!.displayIndex)
        assertEquals(0, uut.getReminderById(idGroup1)!!.displayIndex) // Unchanged
    }

    @Test
    fun `updateReminderDisplayOrder preserves indices for reminders not in orders list`() =
        runTest(dispatcher) {
            // PREPARE
            val id1 = uut.createReminder(
                ReminderCreateRequest("Reminder 1", null, null, defaultParams)
            )
            val id2 = uut.createReminder(
                ReminderCreateRequest("Reminder 2", null, null, defaultParams)
            )

            // Manually set display indices
            uut.updateReminderDisplayOrder(
                groupId = null,
                orders = listOf(
                    ReminderDisplayOrderData(id1, 10),
                    ReminderDisplayOrderData(id2, 20)
                )
            )

            // EXECUTE - only update id1
            uut.updateReminderDisplayOrder(
                groupId = null,
                orders = listOf(ReminderDisplayOrderData(id1, 5))
            )

            // VERIFY
            assertEquals(5, uut.getReminderById(id1)!!.displayIndex)
            assertEquals(20, uut.getReminderById(id2)!!.displayIndex) // Unchanged
        }

    @Test
    fun `updateReminderDisplayOrder handles empty orders list`() = runTest(dispatcher) {
        // PREPARE
        val id = uut.createReminder(
            ReminderCreateRequest("Test", null, null, defaultParams)
        )

        // EXECUTE
        uut.updateReminderDisplayOrder(groupId = null, orders = emptyList())

        // VERIFY - no crash, index unchanged
        assertEquals(0, uut.getReminderById(id)!!.displayIndex)
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
        assertEquals(original.groupId, duplicate.groupId)
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
    fun `getAllRemindersSync returns all reminders sorted by displayIndex`() =
        runTest(dispatcher) {
            // PREPARE
            val id1 = uut.createReminder(
                ReminderCreateRequest("First", null, null, defaultParams)
            )
            val id2 = uut.createReminder(
                ReminderCreateRequest("Second", null, null, defaultParams)
            )
            val id3 = uut.createReminder(
                ReminderCreateRequest("Third", null, null, defaultParams)
            )

            // Set display indices: 3, 1, 2
            uut.updateReminderDisplayOrder(
                groupId = null,
                orders = listOf(
                    ReminderDisplayOrderData(id1, 1),
                    ReminderDisplayOrderData(id2, 2),
                    ReminderDisplayOrderData(id3, 0)
                )
            )

            // EXECUTE
            val reminders = uut.getAllRemindersSync()

            // VERIFY - sorted by displayIndex
            assertEquals(3, reminders.size)
            assertEquals("Third", reminders[0].reminderName)
            assertEquals("First", reminders[1].reminderName)
            assertEquals("Second", reminders[2].reminderName)
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
