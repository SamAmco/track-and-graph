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
import com.samco.trackandgraph.FakeTrackerDao
import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.TrackerCreateRequest
import com.samco.trackandgraph.data.database.dto.TrackerDeleteRequest
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType
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
import com.samco.trackandgraph.FakeDataPointUpdateHelper

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerHelperImplTest {

    private lateinit var fakeTrackerDao: FakeTrackerDao
    private lateinit var fakeGroupItemDao: FakeGroupItemDao
    private val dataPointUpdateHelper: DataPointUpdateHelper = FakeDataPointUpdateHelper()
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private lateinit var uut: TrackerHelperImpl

    @Before
    fun before() {
        fakeTrackerDao = FakeTrackerDao()
        fakeGroupItemDao = FakeGroupItemDao()

        val fakeTransactionHelper = object : DatabaseTransactionHelper {
            override suspend fun <R> withTransaction(block: suspend () -> R): R = block()
        }

        val fakeTimeProvider = object : TimeProvider {
            override fun now(): org.threeten.bp.ZonedDateTime = org.threeten.bp.ZonedDateTime.now()
            override fun epochMilli(): Long = 1000L
            override fun defaultZone(): org.threeten.bp.ZoneId =
                org.threeten.bp.ZoneId.systemDefault()
        }

        uut = TrackerHelperImpl(
            transactionHelper = fakeTransactionHelper,
            dao = fakeTrackerDao,
            groupItemDao = fakeGroupItemDao,
            dataPointUpdateHelper = dataPointUpdateHelper,
            timeProvider = fakeTimeProvider,
            io = dispatcher
        )
    }

    // =========================================================================
    // Create tests
    // =========================================================================

    @Test
    fun `createTracker inserts feature, tracker, and GroupItem`() = runTest(dispatcher) {
        // PREPARE
        val groupId = 1L
        val request = TrackerCreateRequest(
            name = "Test Tracker",
            groupId = groupId,
            dataType = DataType.CONTINUOUS,
            hasDefaultValue = false,
            defaultValue = 0.0,
            defaultLabel = "",
            description = "Test description",
            suggestionType = TrackerSuggestionType.NONE,
            suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
        )

        // EXECUTE
        val trackerId = uut.createTracker(request)

        // VERIFY
        assertTrue(trackerId > 0)

        // Verify GroupItem was created
        val groupItems = fakeGroupItemDao.getGroupItemsForChild(trackerId, GroupItemType.TRACKER)
        assertEquals(1, groupItems.size)
        assertEquals(groupId, groupItems[0].groupId)
        assertEquals(0, groupItems[0].displayIndex)
    }

    // =========================================================================
    // Delete tests - Simple cases
    // =========================================================================

    @Test
    fun `deleteTracker removes tracker when only in one group`() = runTest(dispatcher) {
        // PREPARE
        val groupId = 1L
        val trackerId = uut.createTracker(
            TrackerCreateRequest(
                name = "To Delete",
                groupId = groupId,
                dataType = DataType.CONTINUOUS,
                hasDefaultValue = false,
                defaultValue = 0.0,
                defaultLabel = "",
                description = "",
                suggestionType = TrackerSuggestionType.NONE,
                suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
            )
        )

        // Verify it exists
        assertNotNull(uut.getTrackerById(trackerId))
        assertEquals(1, fakeGroupItemDao.getGroupItemsForChild(trackerId, GroupItemType.TRACKER).size)

        // EXECUTE
        uut.deleteTracker(TrackerDeleteRequest(trackerId = trackerId))

        // VERIFY
        assertNull(uut.getTrackerById(trackerId))
        assertTrue(fakeGroupItemDao.getGroupItemsForChild(trackerId, GroupItemType.TRACKER).isEmpty())
    }

    @Test
    fun `deleteTracker does nothing for non-existent tracker`() = runTest(dispatcher) {
        // EXECUTE
        uut.deleteTracker(TrackerDeleteRequest(trackerId = 999L))

        // VERIFY - no exception thrown, no features deleted
        assertEquals(0, fakeTrackerDao.numTrackers())
    }

    // =========================================================================
    // Delete tests - Multiple groups (symlink behavior)
    // =========================================================================

    @Test
    fun `deleteTracker with groupId removes only symlink when in multiple groups`() =
        runTest(dispatcher) {
            // PREPARE - Create tracker in group1, then add symlink to group2
            val group1 = 1L
            val group2 = 2L
            val trackerId = uut.createTracker(
                TrackerCreateRequest(
                    name = "Multi-group Tracker",
                    groupId = group1,
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = false,
                    defaultValue = 0.0,
                    defaultLabel = "",
                    description = "",
                    suggestionType = TrackerSuggestionType.NONE,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
                )
            )
            // Add symlink to group2
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = trackerId,
                    type = GroupItemType.TRACKER,
                    createdAt = 1000L
                )
            )
            assertEquals(2, fakeGroupItemDao.getGroupItemsForChild(trackerId, GroupItemType.TRACKER).size)

            // EXECUTE - Delete from group1 only
            uut.deleteTracker(TrackerDeleteRequest(trackerId = trackerId, groupId = group1))

            // VERIFY - Tracker still exists, only removed from group1
            assertNotNull(uut.getTrackerById(trackerId))
            val remainingItems = fakeGroupItemDao.getGroupItemsForChild(trackerId, GroupItemType.TRACKER)
            assertEquals(1, remainingItems.size)
            assertEquals(group2, remainingItems[0].groupId)
            assertEquals(1, fakeTrackerDao.numTrackers())
        }

    @Test
    fun `deleteTracker without groupId deletes tracker and all symlinks`() =
        runTest(dispatcher) {
            // PREPARE - Create tracker in multiple groups
            val group1 = 1L
            val group2 = 2L
            val group3 = 3L
            val trackerId = uut.createTracker(
                TrackerCreateRequest(
                    name = "Multi-group Tracker",
                    groupId = group1,
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = false,
                    defaultValue = 0.0,
                    defaultLabel = "",
                    description = "",
                    suggestionType = TrackerSuggestionType.NONE,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
                )
            )
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = trackerId,
                    type = GroupItemType.TRACKER,
                    createdAt = 1000L
                )
            )
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group3,
                    displayIndex = 0,
                    childId = trackerId,
                    type = GroupItemType.TRACKER,
                    createdAt = 1000L
                )
            )
            assertEquals(3, fakeGroupItemDao.getGroupItemsForChild(trackerId, GroupItemType.TRACKER).size)

            // EXECUTE - Delete without specifying group
            uut.deleteTracker(TrackerDeleteRequest(trackerId = trackerId))

            // VERIFY - Tracker and all GroupItems deleted
            assertNull(uut.getTrackerById(trackerId))
            assertTrue(fakeGroupItemDao.getGroupItemsForChild(trackerId, GroupItemType.TRACKER).isEmpty())
        }

    @Test
    fun `deleteTracker with groupId deletes tracker when only in that group`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = 5L
            val trackerId = uut.createTracker(
                TrackerCreateRequest(
                    name = "Single Group Tracker",
                    groupId = groupId,
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = false,
                    defaultValue = 0.0,
                    defaultLabel = "",
                    description = "",
                    suggestionType = TrackerSuggestionType.NONE,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
                )
            )
            assertEquals(1, fakeGroupItemDao.getGroupItemsForChild(trackerId, GroupItemType.TRACKER).size)

            // EXECUTE - Delete from group (but it's the only location)
            uut.deleteTracker(TrackerDeleteRequest(trackerId = trackerId, groupId = groupId))

            // VERIFY - Tracker fully deleted since it was only in one group
            assertNull(uut.getTrackerById(trackerId))
            assertTrue(fakeGroupItemDao.getGroupItemsForChild(trackerId, GroupItemType.TRACKER).isEmpty())
        }

    // =========================================================================
    // Display tracker uniqueness tests
    // =========================================================================

    @Test
    fun `getDisplayTrackersForGroupSync returns unique=true when tracker in only one group`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = 1L
            val trackerId = uut.createTracker(
                TrackerCreateRequest(
                    name = "Single Group Tracker",
                    groupId = groupId,
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = false,
                    defaultValue = 0.0,
                    defaultLabel = "",
                    description = "",
                    suggestionType = TrackerSuggestionType.NONE,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
                )
            )

            // EXECUTE
            val result = uut.getDisplayTrackersForGroupSync(groupId)

            // VERIFY
            assertEquals(1, result.size)
            assertEquals(trackerId, result[0].id)
            assertEquals(true, result[0].unique)
        }

    @Test
    fun `getDisplayTrackersForGroupSync returns unique=false when tracker in multiple groups`() =
        runTest(dispatcher) {
            // PREPARE
            val group1 = 1L
            val group2 = 2L
            val trackerId = uut.createTracker(
                TrackerCreateRequest(
                    name = "Multi Group Tracker",
                    groupId = group1,
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = false,
                    defaultValue = 0.0,
                    defaultLabel = "",
                    description = "",
                    suggestionType = TrackerSuggestionType.NONE,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
                )
            )
            // Add symlink to group2
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = trackerId,
                    type = GroupItemType.TRACKER,
                    createdAt = 1000L
                )
            )

            // EXECUTE - check from group1's perspective
            val result = uut.getDisplayTrackersForGroupSync(group1)

            // VERIFY
            assertEquals(1, result.size)
            assertEquals(trackerId, result[0].id)
            assertEquals(false, result[0].unique)
        }

    @Test
    fun `getDisplayTrackersForGroupSync sets unique independently per tracker`() =
        runTest(dispatcher) {
            // PREPARE - one unique tracker, one non-unique tracker in the same group
            val group1 = 1L
            val group2 = 2L

            val uniqueTrackerId = uut.createTracker(
                TrackerCreateRequest(
                    name = "Unique Tracker",
                    groupId = group1,
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = false,
                    defaultValue = 0.0,
                    defaultLabel = "",
                    description = "",
                    suggestionType = TrackerSuggestionType.NONE,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
                )
            )
            val sharedTrackerId = uut.createTracker(
                TrackerCreateRequest(
                    name = "Shared Tracker",
                    groupId = group1,
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = false,
                    defaultValue = 0.0,
                    defaultLabel = "",
                    description = "",
                    suggestionType = TrackerSuggestionType.NONE,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
                )
            )
            // Add symlink for sharedTracker to group2
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = sharedTrackerId,
                    type = GroupItemType.TRACKER,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            val result = uut.getDisplayTrackersForGroupSync(group1)

            // VERIFY
            val uniqueResult = result.first { it.id == uniqueTrackerId }
            val sharedResult = result.first { it.id == sharedTrackerId }
            assertEquals(true, uniqueResult.unique)
            assertEquals(false, sharedResult.unique)
        }

    // =========================================================================
    // Get tests
    // =========================================================================

    @Test
    fun `getTrackerById returns tracker when exists`() = runTest(dispatcher) {
        // PREPARE
        val trackerId = uut.createTracker(
            TrackerCreateRequest(
                name = "Test Tracker",
                groupId = 1L,
                dataType = DataType.DURATION,
                hasDefaultValue = true,
                defaultValue = 60.0,
                defaultLabel = "default",
                description = "Test description",
                suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
                suggestionOrder = TrackerSuggestionOrder.LATEST
            )
        )

        // EXECUTE
        val result = uut.getTrackerById(trackerId)

        // VERIFY
        assertNotNull(result)
        assertEquals(trackerId, result!!.id)
        assertEquals("Test Tracker", result.name)
        assertEquals(DataType.DURATION, result.dataType)
        assertEquals(true, result.hasDefaultValue)
        assertEquals(60.0, result.defaultValue, 0.001)
        assertEquals("default", result.defaultLabel)
    }

    @Test
    fun `getTrackerById returns null when not found`() = runTest(dispatcher) {
        // EXECUTE & VERIFY
        assertNull(uut.getTrackerById(999L))
    }

    @Test
    fun `hasAtLeastOneTracker returns false when empty`() = runTest(dispatcher) {
        // EXECUTE & VERIFY
        assertEquals(false, uut.hasAtLeastOneTracker())
    }

    @Test
    fun `hasAtLeastOneTracker returns true when trackers exist`() = runTest(dispatcher) {
        // PREPARE
        uut.createTracker(
            TrackerCreateRequest(
                name = "Test",
                groupId = 1L,
                dataType = DataType.CONTINUOUS,
                hasDefaultValue = false,
                defaultValue = 0.0,
                defaultLabel = "",
                description = "",
                suggestionType = TrackerSuggestionType.NONE,
                suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
            )
        )

        // EXECUTE & VERIFY
        assertEquals(true, uut.hasAtLeastOneTracker())
    }
}
