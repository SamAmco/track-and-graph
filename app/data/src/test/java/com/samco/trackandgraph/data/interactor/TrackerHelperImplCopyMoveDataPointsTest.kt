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
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.TrackerCreateRequest
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType
import com.samco.trackandgraph.data.time.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.samco.trackandgraph.FakeDataPointUpdateHelper
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerHelperImplCopyMoveDataPointsTest {

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

    private suspend fun createTracker(name: String, groupId: Long = 1L): Long {
        return uut.createTracker(
            TrackerCreateRequest(
                name = name,
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
    }

    private fun makeDataPoint(
        featureId: Long,
        timestamp: OffsetDateTime,
        value: Double = 1.0,
        label: String = "",
        note: String = ""
    ) = DataPoint(
        timestamp = timestamp,
        featureId = featureId,
        value = value,
        label = label,
        note = note
    )

    private val time1 = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    private val time2 = OffsetDateTime.of(2024, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC)
    private val time3 = OffsetDateTime.of(2024, 1, 3, 10, 0, 0, 0, ZoneOffset.UTC)

    // =========================================================================
    // Copy tests
    // =========================================================================

    @Test
    fun `copyDataPointsToTracker copies data points to target tracker`() = runTest(dispatcher) {
        // PREPARE
        val sourceTrackerId = createTracker("Source")
        val targetTrackerId = createTracker("Target")

        val sourceTracker = uut.getTrackerById(sourceTrackerId)!!
        val targetTracker = uut.getTrackerById(targetTrackerId)!!

        val dp1 = makeDataPoint(sourceTracker.featureId, time1, value = 10.0, label = "a")
        val dp2 = makeDataPoint(sourceTracker.featureId, time2, value = 20.0, label = "b", note = "note")

        fakeTrackerDao.insertDataPoints(listOf(dp1.toEntity(), dp2.toEntity()))

        // EXECUTE
        uut.copyDataPointsToTracker(listOf(dp1, dp2), targetTrackerId)

        // VERIFY - originals still exist
        val sourceDataPoints = fakeTrackerDao.getDataPointsForFeatureSync(sourceTracker.featureId)
        assertEquals(2, sourceDataPoints.size)

        // VERIFY - copies exist on target
        val targetDataPoints = fakeTrackerDao.getDataPointsForFeatureSync(targetTracker.featureId)
        assertEquals(2, targetDataPoints.size)

        val copiedDp1 = targetDataPoints.first { it.epochMilli == time1.toInstant().toEpochMilli() }
        assertEquals(targetTracker.featureId, copiedDp1.featureId)
        assertEquals(10.0, copiedDp1.value, 0.001)
        assertEquals("a", copiedDp1.label)

        val copiedDp2 = targetDataPoints.first { it.epochMilli == time2.toInstant().toEpochMilli() }
        assertEquals(targetTracker.featureId, copiedDp2.featureId)
        assertEquals(20.0, copiedDp2.value, 0.001)
        assertEquals("b", copiedDp2.label)
        assertEquals("note", copiedDp2.note)
    }

    @Test
    fun `copyDataPointsToTracker with empty list does nothing`() = runTest(dispatcher) {
        // PREPARE
        val targetTrackerId = createTracker("Target")

        // EXECUTE
        uut.copyDataPointsToTracker(emptyList(), targetTrackerId)

        // VERIFY - no crash, no data points
        val targetTracker = uut.getTrackerById(targetTrackerId)!!
        val targetDataPoints = fakeTrackerDao.getDataPointsForFeatureSync(targetTracker.featureId)
        assertTrue(targetDataPoints.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `copyDataPointsToTracker throws when target tracker does not exist`() =
        runTest(dispatcher) {
            val sourceTrackerId = createTracker("Source")
            val sourceTracker = uut.getTrackerById(sourceTrackerId)!!
            val dp = makeDataPoint(sourceTracker.featureId, time1)
            fakeTrackerDao.insertDataPoints(listOf(dp.toEntity()))

            uut.copyDataPointsToTracker(listOf(dp), 999L)
        }

    // =========================================================================
    // Move tests
    // =========================================================================

    @Test
    fun `moveDataPointsToTracker moves data points to target tracker`() = runTest(dispatcher) {
        // PREPARE
        val sourceTrackerId = createTracker("Source")
        val targetTrackerId = createTracker("Target")

        val sourceTracker = uut.getTrackerById(sourceTrackerId)!!
        val targetTracker = uut.getTrackerById(targetTrackerId)!!

        val dp1 = makeDataPoint(sourceTracker.featureId, time1, value = 10.0, label = "a")
        val dp2 = makeDataPoint(sourceTracker.featureId, time2, value = 20.0, label = "b", note = "note")
        val dp3 = makeDataPoint(sourceTracker.featureId, time3, value = 30.0, label = "c")

        fakeTrackerDao.insertDataPoints(listOf(dp1.toEntity(), dp2.toEntity(), dp3.toEntity()))

        // EXECUTE - move only dp1 and dp2
        uut.moveDataPointsToTracker(listOf(dp1, dp2), targetTrackerId)

        // VERIFY - dp3 still on source, dp1/dp2 removed
        val sourceDataPoints = fakeTrackerDao.getDataPointsForFeatureSync(sourceTracker.featureId)
        assertEquals(1, sourceDataPoints.size)
        assertEquals(time3.toInstant().toEpochMilli(), sourceDataPoints[0].epochMilli)

        // VERIFY - dp1/dp2 now on target
        val targetDataPoints = fakeTrackerDao.getDataPointsForFeatureSync(targetTracker.featureId)
        assertEquals(2, targetDataPoints.size)

        val movedDp1 = targetDataPoints.first { it.epochMilli == time1.toInstant().toEpochMilli() }
        assertEquals(targetTracker.featureId, movedDp1.featureId)
        assertEquals(10.0, movedDp1.value, 0.001)
        assertEquals("a", movedDp1.label)

        val movedDp2 = targetDataPoints.first { it.epochMilli == time2.toInstant().toEpochMilli() }
        assertEquals(targetTracker.featureId, movedDp2.featureId)
        assertEquals(20.0, movedDp2.value, 0.001)
        assertEquals("note", movedDp2.note)
    }

    @Test
    fun `moveDataPointsToTracker with empty list does nothing`() = runTest(dispatcher) {
        // PREPARE
        val targetTrackerId = createTracker("Target")

        // EXECUTE
        uut.moveDataPointsToTracker(emptyList(), targetTrackerId)

        // VERIFY - no crash
        val targetTracker = uut.getTrackerById(targetTrackerId)!!
        val targetDataPoints = fakeTrackerDao.getDataPointsForFeatureSync(targetTracker.featureId)
        assertTrue(targetDataPoints.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `moveDataPointsToTracker throws when target tracker does not exist`() =
        runTest(dispatcher) {
            val sourceTrackerId = createTracker("Source")
            val sourceTracker = uut.getTrackerById(sourceTrackerId)!!
            val dp = makeDataPoint(sourceTracker.featureId, time1)
            fakeTrackerDao.insertDataPoints(listOf(dp.toEntity()))

            uut.moveDataPointsToTracker(listOf(dp), 999L)
        }

    @Test
    fun `moveDataPointsToTracker preserves timestamp and offset`() = runTest(dispatcher) {
        // PREPARE
        val sourceTrackerId = createTracker("Source")
        val targetTrackerId = createTracker("Target")

        val sourceTracker = uut.getTrackerById(sourceTrackerId)!!
        val targetTracker = uut.getTrackerById(targetTrackerId)!!

        val offset = ZoneOffset.ofHours(5)
        val timestamp = OffsetDateTime.of(2024, 6, 15, 14, 30, 0, 0, offset)
        val dp = makeDataPoint(sourceTracker.featureId, timestamp, value = 42.0)
        fakeTrackerDao.insertDataPoints(listOf(dp.toEntity()))

        // EXECUTE
        uut.moveDataPointsToTracker(listOf(dp), targetTrackerId)

        // VERIFY
        val targetDataPoints = fakeTrackerDao.getDataPointsForFeatureSync(targetTracker.featureId)
        assertEquals(1, targetDataPoints.size)
        assertEquals(timestamp.toInstant().toEpochMilli(), targetDataPoints[0].epochMilli)
        assertEquals(offset.totalSeconds, targetDataPoints[0].utcOffsetSec)
        assertEquals(42.0, targetDataPoints[0].value, 0.001)
    }
}