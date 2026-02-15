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

import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.DeletedGroupInfo
import com.samco.trackandgraph.data.database.dto.GlobalNote
import com.samco.trackandgraph.data.database.dto.GroupDeleteRequest
import com.samco.trackandgraph.data.database.dto.TrackerCreateRequest
import com.samco.trackandgraph.data.database.dto.TrackerDeleteRequest
import com.samco.trackandgraph.data.database.dto.TrackerUpdateRequest
import com.samco.trackandgraph.data.database.entity.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.entity.TrackerSuggestionType
import com.samco.trackandgraph.data.database.entity.queryresponse.TrackerWithFeature
import com.samco.trackandgraph.data.dependencyanalyser.DependencyAnalyser
import com.samco.trackandgraph.data.dependencyanalyser.DependencyAnalyserProvider
import com.samco.trackandgraph.data.dependencyanalyser.DependentFeatures
import com.samco.trackandgraph.data.dependencyanalyser.DependentGraphs
import com.samco.trackandgraph.data.dependencyanalyser.OrphanedGraphs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.threeten.bp.OffsetDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DataInteractorImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var uut: DataInteractorImpl

    private val transactionHelper: DatabaseTransactionHelper =
        object : DatabaseTransactionHelper {
            override suspend fun <R> withTransaction(block: suspend () -> R): R = block()
        }
    private val dao: TrackAndGraphDatabaseDao = mock()
    private val trackerHelper: TrackerHelper = mock()
    private val functionHelper: FunctionHelper = mock()
    private val reminderHelper: ReminderHelper = mock()
    private val groupHelper: GroupHelper = mock()
    private val graphHelper: GraphHelper = mock()
    private val dependencyAnalyserProvider: DependencyAnalyserProvider = mock()

    @Before
    fun before() {
        Dispatchers.setMain(testDispatcher)

        uut = DataInteractorImpl(
            transactionHelper = transactionHelper,
            dao = dao,
            io = testDispatcher,
            trackerHelper = trackerHelper,
            functionHelper = functionHelper,
            reminderHelper = reminderHelper,
            groupHelper = groupHelper,
            graphHelper = graphHelper,
            dependencyAnalyserProvider = dependencyAnalyserProvider,
        )
    }

    @Test
    fun `Modifying the data should cause a data update event`() = runTest {

        //PREPARE
        val testCreateRequest = TrackerCreateRequest(
            name = "none",
            groupId = 0L,
            dataType = DataType.CONTINUOUS,
            hasDefaultValue = false,
            defaultValue = 0.0,
            description = "none",
            defaultLabel = ""
        )

        val testUpdateRequest = TrackerUpdateRequest(
            id = 0L,
            name = "updated"
        )

        val dataPointTime = OffsetDateTime.parse("2020-01-02T00:00:00Z")
        val testDataPoint = DataPoint(
            dataPointTime,
            featureId = 0L,
            value = 0.0,
            label = "",
            note = ""
        )

        val noteTime = OffsetDateTime.parse("2020-01-01T00:00:00Z")
        val testGlobalNote = GlobalNote(
            timestamp = noteTime,
            note = "hi hi"
        )

        var count = 0
        val collectJob = launch(testDispatcher) {
            uut.getDataUpdateEvents().collect { count++ }
        }

        whenever(trackerHelper.createTracker(any())).thenReturn(0L)
        whenever(dao.getTrackerById(any())).thenReturn(
            TrackerWithFeature(
                id = 0L,
                name = "name",
                groupId = 0L,
                featureId = 0L,
                displayIndex = 0,
                description = "",
                dataType = DataType.CONTINUOUS,
                hasDefaultValue = false,
                defaultValue = 1.0,
                defaultLabel = "",
                suggestionType = TrackerSuggestionType.NONE,
                suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
            )
        )

        val dependencyAnalyser = mock<DependencyAnalyser>()
        whenever(dependencyAnalyserProvider.create()).thenReturn(dependencyAnalyser)
        whenever(dependencyAnalyser.getOrphanedGraphs()).thenReturn(OrphanedGraphs(emptySet()))
        whenever(dependencyAnalyser.getDependentGraphs(any())).thenReturn(DependentGraphs(emptySet()))
        whenever(dependencyAnalyser.getFeaturesDependingOn(any())).thenReturn(
            DependentFeatures(
                emptySet()
            )
        )

        whenever(groupHelper.deleteGroup(any())).thenReturn(DeletedGroupInfo(emptySet()))

        //EXECUTE

        //let the async start collecting
        yield()

        uut.deleteGroup(GroupDeleteRequest(groupId = 0L))
        uut.createTracker(testCreateRequest)
        uut.updateTracker(testUpdateRequest)
        uut.deleteTracker(TrackerDeleteRequest(trackerId = 0L))
        uut.deleteDataPoint(testDataPoint)
        uut.insertDataPoint(testDataPoint)
        uut.insertDataPoints(listOf(testDataPoint))
        uut.removeNote(
            noteTime, 0L
        )
        uut.deleteGlobalNote(testGlobalNote)
        uut.insertGlobalNote(testGlobalNote)

        //VERIFY
        assertEquals(10, count)
        collectJob.cancel()
        verify(trackerHelper, times(1)).createTracker(eq(testCreateRequest))
        verify(trackerHelper, times(1)).updateTracker(eq(testUpdateRequest))
    }
}