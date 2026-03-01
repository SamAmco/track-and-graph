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
import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.ReminderCreateRequest
import com.samco.trackandgraph.data.database.dto.ReminderDeleteRequest
import com.samco.trackandgraph.data.database.dto.ReminderDisplayOrderData
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.database.dto.ReminderUpdateRequest
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
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

        val transactionHelper = object : DatabaseTransactionHelper {
            override suspend fun <R> withTransaction(block: suspend () -> R): R = block()
        }

        uut = ReminderHelperImpl(
            reminderDao = fakeReminderDao,
            groupItemDao = fakeGroupItemDao,
            reminderSerializer = reminderSerializer,
            transactionHelper = transactionHelper,
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
    // Delete tests - Simple cases
    // =========================================================================

    @Test
    fun `deleteReminder removes reminder and GroupItem when only in one location`() =
        runTest(dispatcher) {
            // PREPARE
            val id = uut.createReminder(
                ReminderCreateRequest("To Delete", null, null, defaultParams)
            )
            assertNotNull(uut.getReminderById(id))
            assertEquals(1, fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).size)

            // EXECUTE
            uut.deleteReminder(ReminderDeleteRequest(reminderId = id))

            // VERIFY
            assertNull(uut.getReminderById(id))
            assertTrue(fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).isEmpty())
        }

    @Test
    fun `deleteReminder removes reminder in group and its GroupItem`() = runTest(dispatcher) {
        // PREPARE
        val groupId = 5L
        val id = uut.createReminder(
            ReminderCreateRequest("Grouped Reminder", groupId, null, defaultParams)
        )
        assertNotNull(uut.getReminderById(id))
        val groupItems = fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
        assertEquals(1, groupItems.size)
        assertEquals(groupId, groupItems[0].groupId)

        // EXECUTE
        uut.deleteReminder(ReminderDeleteRequest(reminderId = id))

        // VERIFY
        assertNull(uut.getReminderById(id))
        assertTrue(fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).isEmpty())
    }

    // =========================================================================
    // Delete tests - Multiple locations (symlink behavior)
    // =========================================================================

    @Test
    fun `deleteReminder with groupId removes only symlink when in multiple groups`() =
        runTest(dispatcher) {
            // PREPARE - Create reminder in group1, then add symlink to group2
            val group1 = 1L
            val group2 = 2L
            val id = uut.createReminder(
                ReminderCreateRequest("Multi-group Reminder", group1, null, defaultParams)
            )
            // Add symlink to group2
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = id,
                    type = GroupItemType.REMINDER,
                    createdAt = 1000L
                )
            )
            assertEquals(2, fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).size)

            // EXECUTE - Delete from group1 only
            uut.deleteReminder(ReminderDeleteRequest(reminderId = id, groupId = group1))

            // VERIFY - Reminder still exists, only removed from group1
            assertNotNull(uut.getReminderById(id))
            val remainingItems = fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
            assertEquals(1, remainingItems.size)
            assertEquals(group2, remainingItems[0].groupId)
        }

    @Test
    fun `deleteReminder from reminders screen deletes reminder and all symlinks`() =
        runTest(dispatcher) {
            // PREPARE - Create reminder in reminders screen (null groupId), then add to a group
            val groupId = 5L
            val id = uut.createReminder(
                ReminderCreateRequest("Multi-location Reminder", null, null, defaultParams)
            )
            // Add symlink to group
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = groupId,
                    displayIndex = 0,
                    childId = id,
                    type = GroupItemType.REMINDER,
                    createdAt = 1000L
                )
            )
            assertEquals(2, fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).size)

            // EXECUTE - Delete from reminders screen (no groupId = delete everything)
            uut.deleteReminder(ReminderDeleteRequest(reminderId = id))

            // VERIFY - Reminder and all GroupItems deleted
            assertNull(uut.getReminderById(id))
            assertTrue(fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).isEmpty())
        }

    @Test
    fun `deleteReminder with groupId removes only symlink from group when also in reminders screen`() =
        runTest(dispatcher) {
            // PREPARE - Create reminder in a group, then add to reminders screen
            val groupId = 5L
            val id = uut.createReminder(
                ReminderCreateRequest("Multi-location Reminder", groupId, null, defaultParams)
            )
            // Add symlink to reminders screen (null groupId)
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = null,
                    displayIndex = 0,
                    childId = id,
                    type = GroupItemType.REMINDER,
                    createdAt = 1000L
                )
            )
            assertEquals(2, fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).size)

            // EXECUTE - Delete from group only
            uut.deleteReminder(ReminderDeleteRequest(reminderId = id, groupId = groupId))

            // VERIFY - Reminder still exists, only removed from group
            assertNotNull(uut.getReminderById(id))
            val remainingItems = fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
            assertEquals(1, remainingItems.size)
            assertNull(remainingItems[0].groupId)
        }

    @Test
    fun `deleteReminder without location deletes reminder and all symlinks`() =
        runTest(dispatcher) {
            // PREPARE - Create reminder in multiple locations
            val group1 = 1L
            val group2 = 2L
            val id = uut.createReminder(
                ReminderCreateRequest("Multi-group Reminder", group1, null, defaultParams)
            )
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = id,
                    type = GroupItemType.REMINDER,
                    createdAt = 1000L
                )
            )
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = null,
                    displayIndex = 0,
                    childId = id,
                    type = GroupItemType.REMINDER,
                    createdAt = 1000L
                )
            )
            assertEquals(3, fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).size)

            // EXECUTE - Delete without specifying location
            uut.deleteReminder(ReminderDeleteRequest(reminderId = id))

            // VERIFY - Reminder and all GroupItems deleted
            assertNull(uut.getReminderById(id))
            assertTrue(fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).isEmpty())
        }

    @Test
    fun `deleteReminder with groupId deletes reminder when only in that group`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = 5L
            val id = uut.createReminder(
                ReminderCreateRequest("Single Group Reminder", groupId, null, defaultParams)
            )
            assertEquals(1, fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).size)

            // EXECUTE - Delete from group (but it's the only location)
            uut.deleteReminder(ReminderDeleteRequest(reminderId = id, groupId = groupId))

            // VERIFY - Reminder fully deleted since it was only in one location
            assertNull(uut.getReminderById(id))
            assertTrue(fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).isEmpty())
        }

    @Test
    fun `deleteReminder from reminders screen when only in reminders screen deletes reminder`() =
        runTest(dispatcher) {
            // PREPARE
            val id = uut.createReminder(
                ReminderCreateRequest("Ungrouped Reminder", null, null, defaultParams)
            )
            assertEquals(1, fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).size)

            // EXECUTE - Delete from reminders screen (no groupId)
            uut.deleteReminder(ReminderDeleteRequest(reminderId = id))

            // VERIFY
            assertNull(uut.getReminderById(id))
            assertTrue(fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER).isEmpty())
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

    @Test
    fun `duplicateReminder inserts duplicate immediately after original in null group`() =
        runTest(dispatcher) {
            // PREPARE - create three reminders so we can check indices around the duplicated one
            val idA = uut.createReminder(ReminderCreateRequest("A", null, null, defaultParams))
            val idB = uut.createReminder(ReminderCreateRequest("B", null, null, defaultParams))
            val idC = uut.createReminder(ReminderCreateRequest("C", null, null, defaultParams))

            // After 3 creates (each shifts others down then inserts at 0):
            // C=0, B=1, A=2
            fun indexOf(id: Long) = fakeGroupItemDao
                .getGroupItemsForChild(id, GroupItemType.REMINDER)
                .first().displayIndex

            assertEquals(0, indexOf(idC))
            assertEquals(1, indexOf(idB))
            assertEquals(2, indexOf(idA))

            // EXECUTE - duplicate B (currently at index 1)
            val idBCopy = uut.duplicateReminder(idB)

            // VERIFY - B-copy should be at index 2, A shifted to 3, C and B unchanged
            assertEquals(0, indexOf(idC))
            assertEquals(1, indexOf(idB))
            assertEquals(2, indexOf(idBCopy))
            assertEquals(3, indexOf(idA))
        }

    @Test
    fun `duplicateReminder inserts duplicate immediately after original in named group`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = 7L
            val idA = uut.createReminder(ReminderCreateRequest("A", groupId, null, defaultParams))
            val idB = uut.createReminder(ReminderCreateRequest("B", groupId, null, defaultParams))

            fun indexOf(id: Long) = fakeGroupItemDao
                .getGroupItemsForChild(id, GroupItemType.REMINDER)
                .first { it.groupId == groupId }.displayIndex

            // B=0, A=1
            assertEquals(0, indexOf(idB))
            assertEquals(1, indexOf(idA))

            // EXECUTE - duplicate A (at index 1, the last item)
            val idACopy = uut.duplicateReminder(idA)

            // VERIFY - A-copy at index 2, nothing else shifts
            assertEquals(0, indexOf(idB))
            assertEquals(1, indexOf(idA))
            assertEquals(2, indexOf(idACopy))
        }

    @Test
    fun `duplicateReminder does not shift items before the original`() = runTest(dispatcher) {
        // PREPARE
        val idA = uut.createReminder(ReminderCreateRequest("A", null, null, defaultParams))
        val idB = uut.createReminder(ReminderCreateRequest("B", null, null, defaultParams))
        val idC = uut.createReminder(ReminderCreateRequest("C", null, null, defaultParams))

        // C=0, B=1, A=2
        fun indexOf(id: Long) = fakeGroupItemDao
            .getGroupItemsForChild(id, GroupItemType.REMINDER)
            .first().displayIndex

        // EXECUTE - duplicate C (at index 0)
        val idCCopy = uut.duplicateReminder(idC)

        // VERIFY - C stays at 0, C-copy at 1, B and A each shift by 1
        assertEquals(0, indexOf(idC))
        assertEquals(1, indexOf(idCCopy))
        assertEquals(2, indexOf(idB))
        assertEquals(3, indexOf(idA))
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
