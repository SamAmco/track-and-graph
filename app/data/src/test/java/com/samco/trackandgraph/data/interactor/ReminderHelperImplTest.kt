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
import com.samco.trackandgraph.data.database.dto.ComponentDeleteRequest
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
        val id = uut.createReminder(request).componentId

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
        val id = uut.createReminder(request).componentId

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
        val id = uut.createReminder(request).componentId

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
        ).componentId

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
        ).componentId

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
        ).componentId

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
        ).componentId

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
            ).componentId
            assertNotNull(uut.getReminderById(id))
            val groupItems = fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
            assertEquals(1, groupItems.size)
            val groupItemId = groupItems[0].id

            // EXECUTE
            uut.deleteReminder(ComponentDeleteRequest(groupItemId = groupItemId))

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
        ).componentId
        assertNotNull(uut.getReminderById(id))
        val groupItems = fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
        assertEquals(1, groupItems.size)
        assertEquals(groupId, groupItems[0].groupId)
        val groupItemId = groupItems[0].id

        // EXECUTE
        uut.deleteReminder(ComponentDeleteRequest(groupItemId = groupItemId))

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
            ).componentId
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
            val allItems = fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
            assertEquals(2, allItems.size)
            // Find the groupItemId for the group1 placement
            val group1ItemId = allItems.first { it.groupId == group1 }.id

            // EXECUTE - Delete from group1 only (deleteEverywhere=false, multiple placements)
            uut.deleteReminder(
                ComponentDeleteRequest(
                    groupItemId = group1ItemId,
                    deleteEverywhere = false
                )
            )

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
            ).componentId
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
            val allItems = fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
            assertEquals(2, allItems.size)
            // Deleting from the reminders screen uses the null-group placement's id
            val remindersScreenItemId = allItems.first { it.groupId == null }.id

            // EXECUTE - Delete from reminders screen (deleteEverywhere=true deletes all placements)
            uut.deleteReminder(
                ComponentDeleteRequest(
                    groupItemId = remindersScreenItemId,
                    deleteEverywhere = true
                )
            )

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
            ).componentId
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
            val allItems = fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
            assertEquals(2, allItems.size)
            // Find the groupItemId for the group placement
            val groupItemId = allItems.first { it.groupId == groupId }.id

            // EXECUTE - Delete from group only (deleteEverywhere=false, multiple placements)
            uut.deleteReminder(
                ComponentDeleteRequest(
                    groupItemId = groupItemId,
                    deleteEverywhere = false
                )
            )

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
            ).componentId
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
            val allItems = fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
            assertEquals(3, allItems.size)
            // Use any groupItemId; deleteEverywhere=true removes all placements
            val anyGroupItemId = allItems.first().id

            // EXECUTE - Delete everywhere regardless of location
            uut.deleteReminder(
                ComponentDeleteRequest(
                    groupItemId = anyGroupItemId,
                    deleteEverywhere = true
                )
            )

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
            ).componentId
            val groupItems = fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
            assertEquals(1, groupItems.size)
            val groupItemId = groupItems[0].id

            // EXECUTE - Delete from group (it's the only location; deleteEverywhere=false still
            // deletes the reminder since only one placement exists)
            uut.deleteReminder(
                ComponentDeleteRequest(
                    groupItemId = groupItemId,
                    deleteEverywhere = false
                )
            )

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
            ).componentId
            val groupItems = fakeGroupItemDao.getGroupItemsForChild(id, GroupItemType.REMINDER)
            assertEquals(1, groupItems.size)
            val groupItemId = groupItems[0].id

            // EXECUTE - Delete from reminders screen (single placement, deleteEverywhere=false
            // still deletes the reminder since only one placement exists)
            uut.deleteReminder(
                ComponentDeleteRequest(groupItemId = groupItemId)
            )

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
        val (originalId, groupItemId) = uut.createReminder(
            ReminderCreateRequest(
                reminderName = "Original",
                groupId = 5L,
                featureId = 10L,
                params = defaultParams
            )
        )

        // EXECUTE
        val duplicateId = uut.duplicateReminder(groupItemId).componentId

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
    fun `duplicateReminder throws when group item not found`() = runTest(dispatcher) {
        // EXECUTE
        uut.duplicateReminder(999L)
    }

    @Test
    fun `duplicateReminder inserts duplicate immediately after original in null group`() =
        runTest(dispatcher) {
            // PREPARE - create three reminders so we can check indices around the duplicated one
            val idA = uut.createReminder(ReminderCreateRequest("A", null, null, defaultParams)).componentId
            val (idB, groupItemIdB) = uut.createReminder(ReminderCreateRequest("B", null, null, defaultParams))
            val idC = uut.createReminder(ReminderCreateRequest("C", null, null, defaultParams)).componentId

            // After 3 creates (each shifts others down then inserts at 0):
            // C=0, B=1, A=2
            fun indexOf(id: Long) = fakeGroupItemDao
                .getGroupItemsForChild(id, GroupItemType.REMINDER)
                .first().displayIndex

            assertEquals(0, indexOf(idC))
            assertEquals(1, indexOf(idB))
            assertEquals(2, indexOf(idA))

            // EXECUTE - duplicate B (currently at index 1)
            val idBCopy = uut.duplicateReminder(groupItemIdB).componentId

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
            val (idA, groupItemIdA) = uut.createReminder(ReminderCreateRequest("A", groupId, null, defaultParams))
            val idB = uut.createReminder(ReminderCreateRequest("B", groupId, null, defaultParams)).componentId

            fun indexOf(id: Long) = fakeGroupItemDao
                .getGroupItemsForChild(id, GroupItemType.REMINDER)
                .first { it.groupId == groupId }.displayIndex

            // B=0, A=1
            assertEquals(0, indexOf(idB))
            assertEquals(1, indexOf(idA))

            // EXECUTE - duplicate A (at index 1, the last item)
            val idACopy = uut.duplicateReminder(groupItemIdA).componentId

            // VERIFY - A-copy at index 2, nothing else shifts
            assertEquals(0, indexOf(idB))
            assertEquals(1, indexOf(idA))
            assertEquals(2, indexOf(idACopy))
        }

    @Test
    fun `duplicateReminder does not shift items before the original`() = runTest(dispatcher) {
        // PREPARE
        val idA = uut.createReminder(ReminderCreateRequest("A", null, null, defaultParams)).componentId
        val idB = uut.createReminder(ReminderCreateRequest("B", null, null, defaultParams)).componentId
        val (idC, groupItemIdC) = uut.createReminder(ReminderCreateRequest("C", null, null, defaultParams))

        // C=0, B=1, A=2
        fun indexOf(id: Long) = fakeGroupItemDao
            .getGroupItemsForChild(id, GroupItemType.REMINDER)
            .first().displayIndex

        // EXECUTE - duplicate C (at index 0)
        val idCCopy = uut.duplicateReminder(groupItemIdC).componentId

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

    // =========================================================================
    // Uniqueness tests
    // =========================================================================

    @Test
    fun `getAllRemindersSync returns unique=true when reminder has only one group item`() =
        runTest(dispatcher) {
            // PREPARE
            uut.createReminder(ReminderCreateRequest("Single Reminder", null, null, defaultParams))

            // EXECUTE
            val result = uut.getAllRemindersSync()

            // VERIFY
            assertEquals(1, result.size)
            assertEquals(true, result[0].unique)
        }

    @Test
    fun `getAllRemindersSync returns unique=false when reminder has multiple group items`() =
        runTest(dispatcher) {
            // PREPARE
            val reminderId = uut.createReminder(
                ReminderCreateRequest("Shared Reminder", null, null, defaultParams)
            ).componentId
            // Add a second group item (symlink to a group)
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = 1L,
                    displayIndex = 0,
                    childId = reminderId,
                    type = GroupItemType.REMINDER,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            val result = uut.getAllRemindersSync()

            // VERIFY
            assertEquals(1, result.size)
            assertEquals(reminderId, result[0].id)
            assertEquals(false, result[0].unique)
        }

    @Test
    fun `getAllRemindersSync sets unique independently per reminder`() =
        runTest(dispatcher) {
            // PREPARE - one unique reminder, one with extra group item
            val uniqueId = uut.createReminder(
                ReminderCreateRequest("Unique Reminder", null, null, defaultParams)
            ).componentId
            val sharedId = uut.createReminder(
                ReminderCreateRequest("Shared Reminder", null, null, defaultParams)
            ).componentId
            // Add extra group item for sharedId only
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = 1L,
                    displayIndex = 0,
                    childId = sharedId,
                    type = GroupItemType.REMINDER,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            val result = uut.getAllRemindersSync()

            // VERIFY
            assertEquals(2, result.size)
            val uniqueResult = result.first { it.id == uniqueId }
            val sharedResult = result.first { it.id == sharedId }
            assertEquals(true, uniqueResult.unique)
            assertEquals(false, sharedResult.unique)
        }

    @Test
    fun `getReminderById returns unique=true when reminder in only one group`() = runTest(dispatcher) {
        val id = uut.createReminder(ReminderCreateRequest("Test", null, null, defaultParams)).componentId
        val result = uut.getReminderById(id)
        assertNotNull(result)
        assertEquals(true, result!!.unique)
    }

    @Test
    fun `getReminderById returns unique=false when reminder in multiple groups`() = runTest(dispatcher) {
        val id = uut.createReminder(ReminderCreateRequest("Test", null, null, defaultParams)).componentId
        fakeGroupItemDao.insertGroupItem(GroupItem(groupId = 1L, displayIndex = 0, childId = id, type = GroupItemType.REMINDER, createdAt = 1000L))
        val result = uut.getReminderById(id)
        assertNotNull(result)
        assertEquals(false, result!!.unique)
    }
}
