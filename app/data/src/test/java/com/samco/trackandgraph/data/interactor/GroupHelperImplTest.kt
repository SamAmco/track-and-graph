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

import com.samco.trackandgraph.FakeGroupDao
import com.samco.trackandgraph.FakeGroupItemDao
import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.dto.GroupCreateRequest
import com.samco.trackandgraph.data.database.dto.GroupDeleteRequest
import com.samco.trackandgraph.data.database.dto.GroupUpdateRequest
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
import com.samco.trackandgraph.data.time.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupHelperImplTest {

    private lateinit var fakeGroupDao: FakeGroupDao
    private lateinit var fakeGroupItemDao: FakeGroupItemDao
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private lateinit var uut: GroupHelperImpl

    @Before
    fun before() {
        fakeGroupDao = FakeGroupDao()
        fakeGroupItemDao = FakeGroupItemDao()

        val fakeTimeProvider = object : TimeProvider {
            override fun now(): org.threeten.bp.ZonedDateTime = org.threeten.bp.ZonedDateTime.now()
            override fun epochMilli(): Long = 1000L
            override fun defaultZone(): org.threeten.bp.ZoneId =
                org.threeten.bp.ZoneId.systemDefault()
        }

        val fakeTransactionHelper = object : DatabaseTransactionHelper {
            override suspend fun <R> withTransaction(block: suspend () -> R): R = block()
        }

        uut = GroupHelperImpl(
            groupDao = fakeGroupDao,
            groupItemDao = fakeGroupItemDao,
            timeProvider = fakeTimeProvider,
            transactionHelper = fakeTransactionHelper,
            io = dispatcher
        )
    }

    // =========================================================================
    // Insert tests
    // =========================================================================

    @Test
    fun `insertGroup creates group and group item`() = runTest(dispatcher) {
        // PREPARE
        val request = GroupCreateRequest(
            name = "Test Group",
            colorIndex = 2,
            parentGroupId = 0L
        )

        // EXECUTE
        val groupId = uut.insertGroup(request)

        // VERIFY
        assertTrue(groupId > 0)
        val group = fakeGroupDao.getGroupById(groupId)
        assertNotNull(group)
        assertEquals("Test Group", group!!.name)
        assertEquals(2, group.colorIndex)

        val groupItems = fakeGroupItemDao.getGroupItemsForChild(groupId, GroupItemType.GROUP)
        assertEquals(1, groupItems.size)
        assertEquals(0L, groupItems[0].groupId)
        assertEquals(0, groupItems[0].displayIndex)
    }

    // =========================================================================
    // Update tests
    // =========================================================================

    @Test
    fun `updateGroup updates name and colorIndex`() = runTest(dispatcher) {
        // PREPARE
        val groupId = uut.insertGroup(
            GroupCreateRequest(name = "Original", colorIndex = 1, parentGroupId = 0L)
        )

        // EXECUTE
        uut.updateGroup(GroupUpdateRequest(id = groupId, name = "Updated", colorIndex = 5))

        // VERIFY
        val group = fakeGroupDao.getGroupById(groupId)
        assertEquals("Updated", group!!.name)
        assertEquals(5, group.colorIndex)
    }

    @Test
    fun `updateGroup keeps original values when not provided`() = runTest(dispatcher) {
        // PREPARE
        val groupId = uut.insertGroup(
            GroupCreateRequest(name = "Original", colorIndex = 3, parentGroupId = 0L)
        )

        // EXECUTE
        uut.updateGroup(GroupUpdateRequest(id = groupId, name = null, colorIndex = null))

        // VERIFY
        val group = fakeGroupDao.getGroupById(groupId)
        assertEquals("Original", group!!.name)
        assertEquals(3, group.colorIndex)
    }

    // =========================================================================
    // Delete tests - Simple cases
    // =========================================================================

    @Test
    fun `deleteGroup removes empty group`() = runTest(dispatcher) {
        // PREPARE
        val groupId = uut.insertGroup(
            GroupCreateRequest(name = "Empty Group", colorIndex = 0, parentGroupId = 0L)
        )

        // EXECUTE
        val result = uut.deleteGroup(GroupDeleteRequest(groupId = groupId, parentGroupId = null))

        // VERIFY
        assertNull(fakeGroupDao.getGroupById(groupId))
        assertTrue(result.deletedFeatureIds.isEmpty())
        assertTrue(fakeGroupItemDao.getGroupItemsForChild(groupId, GroupItemType.GROUP).isEmpty())
    }

    @Test
    fun `deleteGroup with parentGroupId removes only symlink when group exists in multiple places`() =
        runTest(dispatcher) {
            // PREPARE - Create a group that exists in two parent groups
            val parentGroup1 = uut.insertGroup(
                GroupCreateRequest(name = "Parent 1", colorIndex = 0, parentGroupId = 0L)
            )
            val parentGroup2 = uut.insertGroup(
                GroupCreateRequest(name = "Parent 2", colorIndex = 0, parentGroupId = 0L)
            )
            val childGroup = uut.insertGroup(
                GroupCreateRequest(name = "Child", colorIndex = 0, parentGroupId = parentGroup1)
            )
            // Add the child to parent2 as well (symlink)
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = parentGroup2,
                    displayIndex = 0,
                    childId = childGroup,
                    type = GroupItemType.GROUP,
                    createdAt = 1000L
                )
            )

            // EXECUTE - Delete from parent1 only
            val result = uut.deleteGroup(
                GroupDeleteRequest(groupId = childGroup, parentGroupId = parentGroup1)
            )

            // VERIFY - Group still exists, only removed from parent1
            assertNotNull(fakeGroupDao.getGroupById(childGroup))
            val remainingItems =
                fakeGroupItemDao.getGroupItemsForChild(childGroup, GroupItemType.GROUP)
            assertEquals(1, remainingItems.size)
            assertEquals(parentGroup2, remainingItems[0].groupId)
            assertTrue(result.deletedFeatureIds.isEmpty())
        }

    @Test
    fun `deleteGroup returns non-existent group gracefully`() = runTest(dispatcher) {
        // EXECUTE
        val result = uut.deleteGroup(GroupDeleteRequest(groupId = 999L, parentGroupId = null))

        // VERIFY
        assertTrue(result.deletedFeatureIds.isEmpty())
    }

    // =========================================================================
    // Delete tests - With trackers
    // =========================================================================

    @Test
    fun `deleteGroup with tracker deletes tracker when only in deleted group`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = uut.insertGroup(
                GroupCreateRequest(name = "Group", colorIndex = 0, parentGroupId = 0L)
            )
            val trackerId = 100L
            val featureId = 200L
            fakeGroupDao.addTracker(trackerId, featureId)
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = groupId,
                    displayIndex = 0,
                    childId = trackerId,
                    type = GroupItemType.TRACKER,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            val result =
                uut.deleteGroup(GroupDeleteRequest(groupId = groupId, parentGroupId = null))

            // VERIFY
            assertNull(fakeGroupDao.getGroupById(groupId))
            assertTrue(featureId in result.deletedFeatureIds)
            assertTrue(fakeGroupDao.deletedFeatureIds.contains(featureId))
        }

    @Test
    fun `deleteGroup with tracker preserves tracker when it exists elsewhere`() =
        runTest(dispatcher) {
            // PREPARE
            val groupToDelete = uut.insertGroup(
                GroupCreateRequest(name = "Delete Me", colorIndex = 0, parentGroupId = 0L)
            )
            val otherGroup = uut.insertGroup(
                GroupCreateRequest(name = "Keep Me", colorIndex = 0, parentGroupId = 0L)
            )
            val trackerId = 100L
            val featureId = 200L
            fakeGroupDao.addTracker(trackerId, featureId)

            // Tracker exists in both groups
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = groupToDelete,
                    displayIndex = 0,
                    childId = trackerId,
                    type = GroupItemType.TRACKER,
                    createdAt = 1000L
                )
            )
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = otherGroup,
                    displayIndex = 0,
                    childId = trackerId,
                    type = GroupItemType.TRACKER,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            val result = uut.deleteGroup(
                GroupDeleteRequest(groupId = groupToDelete, parentGroupId = null)
            )

            // VERIFY - Group deleted but tracker and feature preserved
            assertNull(fakeGroupDao.getGroupById(groupToDelete))
            assertTrue(result.deletedFeatureIds.isEmpty())
            assertTrue(fakeGroupDao.deletedFeatureIds.isEmpty())

            // Tracker still exists in otherGroup
            val remainingItems =
                fakeGroupItemDao.getGroupItemsForChild(trackerId, GroupItemType.TRACKER)
            assertEquals(1, remainingItems.size)
            assertEquals(otherGroup, remainingItems[0].groupId)
        }

    // =========================================================================
    // Delete tests - With functions
    // =========================================================================

    @Test
    fun `deleteGroup with function deletes function when only in deleted group`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = uut.insertGroup(
                GroupCreateRequest(name = "Group", colorIndex = 0, parentGroupId = 0L)
            )
            val functionId = 100L
            val featureId = 200L
            fakeGroupDao.addFunction(functionId, featureId)
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = groupId,
                    displayIndex = 0,
                    childId = functionId,
                    type = GroupItemType.FUNCTION,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            val result =
                uut.deleteGroup(GroupDeleteRequest(groupId = groupId, parentGroupId = null))

            // VERIFY
            assertTrue(featureId in result.deletedFeatureIds)
            assertTrue(fakeGroupDao.deletedFeatureIds.contains(featureId))
        }

    // =========================================================================
    // Delete tests - With graphs
    // =========================================================================

    @Test
    fun `deleteGroup with graph deletes graph when only in deleted group`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = uut.insertGroup(
                GroupCreateRequest(name = "Group", colorIndex = 0, parentGroupId = 0L)
            )
            val graphId = 100L
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = groupId,
                    displayIndex = 0,
                    childId = graphId,
                    type = GroupItemType.GRAPH,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            uut.deleteGroup(GroupDeleteRequest(groupId = groupId, parentGroupId = null))

            // VERIFY
            assertTrue(fakeGroupDao.deletedGraphIds.contains(graphId))
        }

    @Test
    fun `deleteGroup with graph preserves graph when it exists elsewhere`() =
        runTest(dispatcher) {
            // PREPARE
            val groupToDelete = uut.insertGroup(
                GroupCreateRequest(name = "Delete Me", colorIndex = 0, parentGroupId = 0L)
            )
            val otherGroup = uut.insertGroup(
                GroupCreateRequest(name = "Keep Me", colorIndex = 0, parentGroupId = 0L)
            )
            val graphId = 100L

            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = groupToDelete,
                    displayIndex = 0,
                    childId = graphId,
                    type = GroupItemType.GRAPH,
                    createdAt = 1000L
                )
            )
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = otherGroup,
                    displayIndex = 0,
                    childId = graphId,
                    type = GroupItemType.GRAPH,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            uut.deleteGroup(GroupDeleteRequest(groupId = groupToDelete, parentGroupId = null))

            // VERIFY
            assertTrue(fakeGroupDao.deletedGraphIds.isEmpty())
            val remainingItems =
                fakeGroupItemDao.getGroupItemsForChild(graphId, GroupItemType.GRAPH)
            assertEquals(1, remainingItems.size)
        }

    // =========================================================================
    // Delete tests - With reminders
    // =========================================================================

    @Test
    fun `deleteGroup with reminder deletes reminder when only in deleted group`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = uut.insertGroup(
                GroupCreateRequest(name = "Group", colorIndex = 0, parentGroupId = 0L)
            )
            val reminderId = 100L
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = groupId,
                    displayIndex = 0,
                    childId = reminderId,
                    type = GroupItemType.REMINDER,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            uut.deleteGroup(GroupDeleteRequest(groupId = groupId, parentGroupId = null))

            // VERIFY
            assertTrue(fakeGroupDao.deletedReminderIds.contains(reminderId))
        }

    @Test
    fun `deleteGroup does not affect reminder with null groupId`() =
        runTest(dispatcher) {
            // PREPARE - Reminder exists only in reminders screen (null groupId)
            val groupId = uut.insertGroup(
                GroupCreateRequest(name = "Group", colorIndex = 0, parentGroupId = 0L)
            )
            val reminderId = 100L
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = null,
                    displayIndex = 0,
                    childId = reminderId,
                    type = GroupItemType.REMINDER,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            uut.deleteGroup(GroupDeleteRequest(groupId = groupId, parentGroupId = null))

            // VERIFY - Reminder is not deleted
            assertTrue(fakeGroupDao.deletedReminderIds.isEmpty())
            val remainingItems =
                fakeGroupItemDao.getGroupItemsForChild(reminderId, GroupItemType.REMINDER)
            assertEquals(1, remainingItems.size)
            assertNull(remainingItems[0].groupId)
        }

    @Test
    fun `deleteGroup preserves reminder when it also has null groupId entry`() =
        runTest(dispatcher) {
            // PREPARE - Reminder exists in both a group and the reminders screen (null groupId)
            val groupId = uut.insertGroup(
                GroupCreateRequest(name = "Group", colorIndex = 0, parentGroupId = 0L)
            )
            val reminderId = 100L

            // Reminder in the group being deleted
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = groupId,
                    displayIndex = 0,
                    childId = reminderId,
                    type = GroupItemType.REMINDER,
                    createdAt = 1000L
                )
            )
            // Reminder also in reminders screen (null groupId)
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = null,
                    displayIndex = 0,
                    childId = reminderId,
                    type = GroupItemType.REMINDER,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            uut.deleteGroup(GroupDeleteRequest(groupId = groupId, parentGroupId = null))

            // VERIFY - Reminder preserved, but GroupItem linking to deleted group is removed
            assertTrue(fakeGroupDao.deletedReminderIds.isEmpty())
            val remainingItems =
                fakeGroupItemDao.getGroupItemsForChild(reminderId, GroupItemType.REMINDER)
            assertEquals(1, remainingItems.size)
            assertNull(remainingItems[0].groupId)
        }

    // =========================================================================
    // Delete tests - Recursive group deletion
    // =========================================================================

    @Test
    fun `deleteGroup recursively deletes child groups`() = runTest(dispatcher) {
        // PREPARE
        val parentGroup = uut.insertGroup(
            GroupCreateRequest(name = "Parent", colorIndex = 0, parentGroupId = 0L)
        )
        val childGroup = uut.insertGroup(
            GroupCreateRequest(name = "Child", colorIndex = 0, parentGroupId = parentGroup)
        )
        val grandchildGroup = uut.insertGroup(
            GroupCreateRequest(name = "Grandchild", colorIndex = 0, parentGroupId = childGroup)
        )

        // EXECUTE
        uut.deleteGroup(GroupDeleteRequest(groupId = parentGroup, parentGroupId = null))

        // VERIFY - All groups deleted
        assertNull(fakeGroupDao.getGroupById(parentGroup))
        assertNull(fakeGroupDao.getGroupById(childGroup))
        assertNull(fakeGroupDao.getGroupById(grandchildGroup))
    }

    @Test
    fun `deleteGroup recursively deletes trackers in child groups`() = runTest(dispatcher) {
        // PREPARE
        val parentGroup = uut.insertGroup(
            GroupCreateRequest(name = "Parent", colorIndex = 0, parentGroupId = 0L)
        )
        val childGroup = uut.insertGroup(
            GroupCreateRequest(name = "Child", colorIndex = 0, parentGroupId = parentGroup)
        )

        val trackerId = 100L
        val featureId = 200L
        fakeGroupDao.addTracker(trackerId, featureId)
        fakeGroupItemDao.insertGroupItem(
            GroupItem(
                groupId = childGroup,
                displayIndex = 0,
                childId = trackerId,
                type = GroupItemType.TRACKER,
                createdAt = 1000L
            )
        )

        // EXECUTE
        val result =
            uut.deleteGroup(GroupDeleteRequest(groupId = parentGroup, parentGroupId = null))

        // VERIFY
        assertTrue(featureId in result.deletedFeatureIds)
        assertTrue(fakeGroupDao.deletedFeatureIds.contains(featureId))
    }

    @Test
    fun `deleteGroup preserves child group when it exists in undeleted group`() =
        runTest(dispatcher) {
            // PREPARE
            val parentToDelete = uut.insertGroup(
                GroupCreateRequest(name = "Delete Me", colorIndex = 0, parentGroupId = 0L)
            )
            val parentToKeep = uut.insertGroup(
                GroupCreateRequest(name = "Keep Me", colorIndex = 0, parentGroupId = 0L)
            )
            val childGroup = uut.insertGroup(
                GroupCreateRequest(name = "Child", colorIndex = 0, parentGroupId = parentToDelete)
            )
            // Also add child to the other parent
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = parentToKeep,
                    displayIndex = 0,
                    childId = childGroup,
                    type = GroupItemType.GROUP,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            uut.deleteGroup(GroupDeleteRequest(groupId = parentToDelete, parentGroupId = null))

            // VERIFY - parentToDelete gone, but child and parentToKeep preserved
            assertNull(fakeGroupDao.getGroupById(parentToDelete))
            assertNotNull(fakeGroupDao.getGroupById(parentToKeep))
            assertNotNull(fakeGroupDao.getGroupById(childGroup))

            val childItems = fakeGroupItemDao.getGroupItemsForChild(childGroup, GroupItemType.GROUP)
            assertEquals(1, childItems.size)
            assertEquals(parentToKeep, childItems[0].groupId)
        }

    @Test
    fun `deleteGroup preserves tracker in child group when tracker also in undeleted group`() =
        runTest(dispatcher) {
            // PREPARE
            val parentToDelete = uut.insertGroup(
                GroupCreateRequest(name = "Delete Me", colorIndex = 0, parentGroupId = 0L)
            )
            val parentToKeep = uut.insertGroup(
                GroupCreateRequest(name = "Keep Me", colorIndex = 0, parentGroupId = 0L)
            )
            val childGroup = uut.insertGroup(
                GroupCreateRequest(name = "Child", colorIndex = 0, parentGroupId = parentToDelete)
            )

            val trackerId = 100L
            val featureId = 200L
            fakeGroupDao.addTracker(trackerId, featureId)

            // Tracker in childGroup (will be deleted) AND in parentToKeep
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = childGroup,
                    displayIndex = 0,
                    childId = trackerId,
                    type = GroupItemType.TRACKER,
                    createdAt = 1000L
                )
            )
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = parentToKeep,
                    displayIndex = 0,
                    childId = trackerId,
                    type = GroupItemType.TRACKER,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            val result = uut.deleteGroup(
                GroupDeleteRequest(groupId = parentToDelete, parentGroupId = null)
            )

            // VERIFY - Tracker preserved because it exists in parentToKeep
            assertTrue(result.deletedFeatureIds.isEmpty())
            assertTrue(fakeGroupDao.deletedFeatureIds.isEmpty())

            val trackerItems =
                fakeGroupItemDao.getGroupItemsForChild(trackerId, GroupItemType.TRACKER)
            assertEquals(1, trackerItems.size)
            assertEquals(parentToKeep, trackerItems[0].groupId)
        }

    // =========================================================================
    // Delete tests - Complex scenarios
    // =========================================================================

    @Test
    fun `deleteGroup handles deeply nested structure with mixed items`() = runTest(dispatcher) {
        // PREPARE - Create a complex structure:
        // Root
        // ├── Group A (to delete)
        // │   ├── Tracker 1 (only here)
        // │   ├── Graph 1 (also in Group B)
        // │   └── Group C
        // │       ├── Tracker 2 (only here)
        // │       └── Function 1 (only here)
        // └── Group B (to keep)
        //     └── Graph 1 (shared)

        val root = 0L
        val groupA = uut.insertGroup(
            GroupCreateRequest(name = "Group A", colorIndex = 0, parentGroupId = root)
        )
        val groupB = uut.insertGroup(
            GroupCreateRequest(name = "Group B", colorIndex = 0, parentGroupId = root)
        )
        val groupC = uut.insertGroup(
            GroupCreateRequest(name = "Group C", colorIndex = 0, parentGroupId = groupA)
        )

        // Tracker 1 - only in Group A
        val tracker1Id = 101L
        val feature1Id = 201L
        fakeGroupDao.addTracker(tracker1Id, feature1Id)
        fakeGroupItemDao.insertGroupItem(
            GroupItem(
                groupId = groupA,
                displayIndex = 0,
                childId = tracker1Id,
                type = GroupItemType.TRACKER,
                createdAt = 1000L
            )
        )

        // Graph 1 - in both Group A and Group B
        val graph1Id = 301L
        fakeGroupItemDao.insertGroupItem(
            GroupItem(
                groupId = groupA,
                displayIndex = 1,
                childId = graph1Id,
                type = GroupItemType.GRAPH,
                createdAt = 1000L
            )
        )
        fakeGroupItemDao.insertGroupItem(
            GroupItem(
                groupId = groupB,
                displayIndex = 0,
                childId = graph1Id,
                type = GroupItemType.GRAPH,
                createdAt = 1000L
            )
        )

        // Tracker 2 - only in Group C
        val tracker2Id = 102L
        val feature2Id = 202L
        fakeGroupDao.addTracker(tracker2Id, feature2Id)
        fakeGroupItemDao.insertGroupItem(
            GroupItem(
                groupId = groupC,
                displayIndex = 0,
                childId = tracker2Id,
                type = GroupItemType.TRACKER,
                createdAt = 1000L
            )
        )

        // Function 1 - only in Group C
        val function1Id = 401L
        val feature3Id = 203L
        fakeGroupDao.addFunction(function1Id, feature3Id)
        fakeGroupItemDao.insertGroupItem(
            GroupItem(
                groupId = groupC,
                displayIndex = 1,
                childId = function1Id,
                type = GroupItemType.FUNCTION,
                createdAt = 1000L
            )
        )

        // EXECUTE
        val result = uut.deleteGroup(GroupDeleteRequest(groupId = groupA, parentGroupId = null))

        // VERIFY
        // Groups A and C deleted, B preserved
        assertNull(fakeGroupDao.getGroupById(groupA))
        assertNull(fakeGroupDao.getGroupById(groupC))
        assertNotNull(fakeGroupDao.getGroupById(groupB))

        // Features 1, 2, and 3 deleted (trackers and function only in deleted groups)
        assertEquals(setOf(feature1Id, feature2Id, feature3Id), result.deletedFeatureIds)

        // Graph 1 preserved (also in Group B)
        assertTrue(graph1Id !in fakeGroupDao.deletedGraphIds)
        val graphItems = fakeGroupItemDao.getGroupItemsForChild(graph1Id, GroupItemType.GRAPH)
        assertEquals(1, graphItems.size)
        assertEquals(groupB, graphItems[0].groupId)
    }

    @Test
    fun `deleteGroup handles item existing in multiple deleted groups`() = runTest(dispatcher) {
        // PREPARE - Tracker exists in both parent and child groups (both being deleted)
        val parentGroup = uut.insertGroup(
            GroupCreateRequest(name = "Parent", colorIndex = 0, parentGroupId = 0L)
        )
        val childGroup = uut.insertGroup(
            GroupCreateRequest(name = "Child", colorIndex = 0, parentGroupId = parentGroup)
        )

        val trackerId = 100L
        val featureId = 200L
        fakeGroupDao.addTracker(trackerId, featureId)

        // Tracker in both parent and child
        fakeGroupItemDao.insertGroupItem(
            GroupItem(
                groupId = parentGroup,
                displayIndex = 0,
                childId = trackerId,
                type = GroupItemType.TRACKER,
                createdAt = 1000L
            )
        )
        fakeGroupItemDao.insertGroupItem(
            GroupItem(
                groupId = childGroup,
                displayIndex = 0,
                childId = trackerId,
                type = GroupItemType.TRACKER,
                createdAt = 1000L
            )
        )

        // EXECUTE
        val result =
            uut.deleteGroup(GroupDeleteRequest(groupId = parentGroup, parentGroupId = null))

        // VERIFY - Feature should be deleted (exists only in deleted groups)
        assertEquals(setOf(featureId), result.deletedFeatureIds)
        assertTrue(fakeGroupDao.deletedFeatureIds.contains(featureId))
    }

    // =========================================================================
    // Get tests
    // =========================================================================

    @Test
    fun `getGroupById returns group when exists`() = runTest(dispatcher) {
        // PREPARE
        val groupId = uut.insertGroup(
            GroupCreateRequest(name = "Test", colorIndex = 3, parentGroupId = 0L)
        )

        // EXECUTE
        val group = uut.getGroupById(groupId)

        // VERIFY
        assertNotNull(group)
        assertEquals(groupId, group!!.id)
        assertEquals("Test", group.name)
        assertEquals(3, group.colorIndex)
    }

    @Test
    fun `getGroupById returns null when not exists`() = runTest(dispatcher) {
        // EXECUTE & VERIFY
        assertNull(uut.getGroupById(999L))
    }

    @Test
    fun `getAllGroupsSync returns all groups`() = runTest(dispatcher) {
        // PREPARE
        uut.insertGroup(GroupCreateRequest(name = "Group 1", colorIndex = 0, parentGroupId = 0L))
        uut.insertGroup(GroupCreateRequest(name = "Group 2", colorIndex = 0, parentGroupId = 0L))
        uut.insertGroup(GroupCreateRequest(name = "Group 3", colorIndex = 0, parentGroupId = 0L))

        // EXECUTE
        val groups = uut.getAllGroupsSync()

        // VERIFY
        assertEquals(3, groups.size)
    }

    @Test
    fun `hasAnyGroups returns false when no groups`() = runTest(dispatcher) {
        // EXECUTE & VERIFY
        assertEquals(false, uut.hasAnyGroups())
    }

    @Test
    fun `hasAnyGroups returns true when groups exist`() = runTest(dispatcher) {
        // PREPARE
        uut.insertGroup(GroupCreateRequest(name = "Test", colorIndex = 0, parentGroupId = 0L))

        // EXECUTE & VERIFY
        assertEquals(true, uut.hasAnyGroups())
    }
}
