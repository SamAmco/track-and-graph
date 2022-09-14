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

package com.samco.trackandgraph.base.model

import com.nhaarman.mockitokotlin2.*
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.database.sampling.DataSampler
import com.samco.trackandgraph.base.service.ServiceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.threeten.bp.OffsetDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DataInteractorImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var uut: DataInteractorImpl

    private val database: TrackAndGraphDatabase = mock()
    private val dao: TrackAndGraphDatabaseDao = mock()
    private val trackerHelper: TrackerHelper = mock()
    private val csvReadWriter: CSVReadWriter = mock()
    private val alarmInteractor: AlarmInteractor = mock()
    private val serviceManager: ServiceManager = mock()
    private val dataSampler: DataSampler = mock()

    @Before
    fun before() {
        Dispatchers.setMain(testDispatcher)

        uut = DataInteractorImpl(
            database = database,
            dao = dao,
            io = testDispatcher,
            trackerHelper = trackerHelper,
            csvReadWriter = csvReadWriter,
            alarmInteractor = alarmInteractor,
            serviceManager = serviceManager,
            dataSampler = dataSampler
        )
    }

    @Test
    fun `Modifying the data tracked should cause a data update event`() = runTest {

        //PREPARE
        val testTracker = Tracker(
            id = 0L,
            featureId = 0L,
            name = "none",
            groupId = 0L,
            dataType = DataType.CONTINUOUS,
            discreteValues = emptyList(),
            displayIndex = 0,
            hasDefaultValue = false,
            defaultValue = 0.0,
            description = "none",
        )

        val testDataPoint = DataPoint(
            OffsetDateTime.MAX,
            featureId = 0L,
            value = 0.0,
            label = "",
            note = ""
        )

        val testGlobalNote = GlobalNote(
            timestamp = OffsetDateTime.MAX,
            note = "hi hi"
        )

        val testFunction = FunctionDto(
            id = 0L,
            name = "name",
            featureId = 0L,
            dataSources = emptyList(),
            script = "",
            groupId = 0L,
            displayIndex = 0,
            description = "",
        )

        var count = 0
        val collectJob = launch(testDispatcher) {
            uut.getDataUpdateEvents().collect { count++ }
        }

        whenever(trackerHelper.insertTracker(any())).thenReturn(0L)
        whenever(dao.getTrackerById(any())).thenReturn(
            com.samco.trackandgraph.base.database.entity.Tracker(
                id = 0L,
                featureId = 0L,
                dataType = DataType.CONTINUOUS,
                discreteValues = emptyList(),
                hasDefaultValue = false,
                defaultValue = 1.0,
            )
        )

        //EXECUTE

        //let the async start collecting
        yield()

        uut.deleteGroup(0L)
        uut.insertTracker(testTracker)
        uut.updateTracker(testTracker)
        uut.deleteFeature(0L)
        uut.deleteDataPoint(testDataPoint)
        uut.insertDataPoint(testDataPoint)
        uut.insertDataPoints(listOf(testDataPoint))
        uut.updateDataPoints(listOf(testDataPoint))
        uut.removeNote2(OffsetDateTime.MAX, 0L)
        uut.deleteGlobalNote(testGlobalNote)
        uut.insertGlobalNote(testGlobalNote)
        uut.insertFunction(testFunction)
        uut.updateFunction(testFunction)

        //VERIFY
        assertEquals(13, count)
        collectJob.cancel()
        verify(trackerHelper, times(1)).insertTracker(eq(testTracker))
        verify(trackerHelper, times(1)).updateTracker(eq(testTracker))
    }
}