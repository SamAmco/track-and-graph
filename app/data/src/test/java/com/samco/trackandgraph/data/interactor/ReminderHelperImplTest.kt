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
        val created = uut.createReminder(request)

        // VERIFY
        val reminder = uut.getReminderById(created.componentId)
        assertNotNull(reminder)
        assertEquals(10L, reminder!!.featureId)
        val placements = fakeGroupItemDao.getGroupItemsForChild(
            created.componentId,
            GroupItemType.REMINDER,
        )
        assertEquals(2, placements.size)
        assertTrue(placements.any { it.groupId == null })
        assertTrue(placements.any { it.groupId == 5L })
        assertEquals(5L, fakeGroupItemDao.getGroupItemById(created.groupItemId)?.groupId)
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
    // Delete tests
    // =========================================================================

    @Test
    fun `deleteReminder removes global reminder and its placement`() =
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
    fun `deleteReminder from group deletes reminder and both placements`() = runTest(dispatcher) {
        val groupId = 5L
        val created = uut.createReminder(
            ReminderCreateRequest("Grouped Reminder", groupId, null, defaultParams)
        )
        val groupItems = fakeGroupItemDao.getGroupItemsForChild(
            created.componentId,
            GroupItemType.REMINDER,
        )
        assertEquals(2, groupItems.size)
        val groupItemId = groupItems.single { it.groupId == groupId }.id

        uut.deleteReminder(
            ComponentDeleteRequest(groupItemId = groupItemId, deleteEverywhere = false)
        )

        assertNull(uut.getReminderById(created.componentId))
        assertTrue(
            fakeGroupItemDao.getGroupItemsForChild(
                created.componentId,
                GroupItemType.REMINDER,
            ).isEmpty()
        )
    }

    @Test
    fun `deleteReminder from reminders screen deletes grouped reminder and both placements`() =
        runTest(dispatcher) {
            val created = uut.createReminder(
                ReminderCreateRequest("Grouped Reminder", 5L, null, defaultParams)
            )
            val placements = fakeGroupItemDao.getGroupItemsForChild(
                created.componentId,
                GroupItemType.REMINDER,
            )
            val remindersScreenItemId = placements.single { it.groupId == null }.id

            uut.deleteReminder(
                ComponentDeleteRequest(
                    groupItemId = remindersScreenItemId,
                    deleteEverywhere = false,
                )
            )

            assertNull(uut.getReminderById(created.componentId))
            assertTrue(
                fakeGroupItemDao.getGroupItemsForChild(
                    created.componentId,
                    GroupItemType.REMINDER,
                ).isEmpty()
            )
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
            val globalPlacementA = fakeGroupItemDao
                .getGroupItemsForChild(idA, GroupItemType.REMINDER)
                .single { it.groupId == null }
            fakeGroupItemDao.updateGroupItem(globalPlacementA.copy(displayIndex = 4))

            // EXECUTE - duplicate A (at index 1, the last item)
            val idACopy = uut.duplicateReminder(groupItemIdA).componentId

            // VERIFY - A-copy is inserted after A in both independent lists
            assertEquals(0, indexOf(idB))
            assertEquals(1, indexOf(idA))
            assertEquals(2, indexOf(idACopy))
            val copiedPlacements = fakeGroupItemDao.getGroupItemsForChild(
                idACopy,
                GroupItemType.REMINDER,
            )
            assertEquals(2, copiedPlacements.size)
            assertEquals(5, copiedPlacements.single { it.groupId == null }.displayIndex)
            assertEquals(2, copiedPlacements.single { it.groupId == groupId }.displayIndex)
        }

    @Test
    fun `duplicateReminder from reminders screen retains group placement`() = runTest(dispatcher) {
        val groupId = 7L
        val original = uut.createReminder(
            ReminderCreateRequest("Grouped", groupId, null, defaultParams)
        )
        val originalPlacements = fakeGroupItemDao.getGroupItemsForChild(
            original.componentId,
            GroupItemType.REMINDER,
        )
        val globalPlacementId = originalPlacements.single { it.groupId == null }.id

        val duplicate = uut.duplicateReminder(globalPlacementId)

        val copiedPlacements = fakeGroupItemDao.getGroupItemsForChild(
            duplicate.componentId,
            GroupItemType.REMINDER,
        )
        assertEquals(2, copiedPlacements.size)
        assertTrue(copiedPlacements.any { it.groupId == null })
        assertTrue(copiedPlacements.any { it.groupId == groupId })
        assertNull(fakeGroupItemDao.getGroupItemById(duplicate.groupItemId)?.groupId)
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

    @Test
    fun `getDisplayIndicesForRemindersScreen includes the null placement for every reminder`() =
        runTest(dispatcher) {
            val global = uut.createReminder(
                ReminderCreateRequest("Global", null, null, defaultParams)
            )
            val grouped = uut.createReminder(
                ReminderCreateRequest("Grouped", 42L, null, defaultParams)
            )

            val result = uut.getDisplayIndicesForRemindersScreen()

            assertEquals(2, result.size)
            assertEquals(global.groupItemId, result.first { it.id == global.componentId }.groupItemId)
            assertNotEquals(
                grouped.groupItemId,
                result.first { it.id == grouped.componentId }.groupItemId,
            )
        }

    @Test
    fun `getRemindersForGroupSync returns only reminders in requested group`() =
        runTest(dispatcher) {
            uut.createReminder(ReminderCreateRequest("Global", null, null, defaultParams))
            val requestedGroupReminder = uut.createReminder(
                ReminderCreateRequest("Requested group", 42L, null, defaultParams)
            )
            uut.createReminder(ReminderCreateRequest("Other group", 43L, null, defaultParams))

            val result = uut.getRemindersForGroupSync(42L)

            assertEquals(listOf(requestedGroupReminder.componentId), result.map { it.id })
        }

    @Test
    fun `getRemindersForGroupSync returns empty list when group has no reminders`() =
        runTest(dispatcher) {
            uut.createReminder(ReminderCreateRequest("Global", null, null, defaultParams))
            uut.createReminder(ReminderCreateRequest("Other group", 43L, null, defaultParams))

            assertTrue(uut.getRemindersForGroupSync(42L).isEmpty())
        }

    // =========================================================================
    // Reminder DTO tests
    // =========================================================================

    @Test
    fun `getAllRemindersSync marks global reminder unique`() =
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
    fun `getAllRemindersSync marks grouped reminder unique despite its two placements`() =
        runTest(dispatcher) {
            val reminderId = uut.createReminder(
                ReminderCreateRequest("Grouped Reminder", 1L, null, defaultParams)
            ).componentId

            val result = uut.getAllRemindersSync()

            assertEquals(1, result.size)
            assertEquals(reminderId, result[0].id)
            assertEquals(true, result[0].unique)
        }

    @Test
    fun `getReminderById returns unique true for grouped reminder`() = runTest(dispatcher) {
        val id = uut.createReminder(ReminderCreateRequest("Test", 1L, null, defaultParams)).componentId
        val result = uut.getReminderById(id)
        assertNotNull(result)
        assertEquals(true, result!!.unique)
    }
}
