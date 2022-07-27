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

import com.nhaarman.mockitokotlin2.mock
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.*
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
    private val featureUpdater: FeatureUpdater = mock()
    private val csvReadWriter: CSVReadWriter = mock()

    @Before
    fun before() {
        Dispatchers.setMain(testDispatcher)

        uut = DataInteractorImpl(
            database,
            dao,
            testDispatcher,
            featureUpdater,
            csvReadWriter
        )
    }

    @Test
    fun `Modifying the data tracked should cause a data update event`() = runTest {

        //PREPARE
        val testFeature = Feature(
            id = 0L,
            name = "none",
            groupId = 0L,
            featureType = DataType.CONTINUOUS,
            discreteValues = emptyList(),
            displayIndex = 0,
            hasDefaultValue = false,
            defaultValue = 0.0,
            description = "none"
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

        var count = 0
        val collectJob = launch(testDispatcher) {
            uut.getDataUpdateEvents()
                .take(12)
                .collect { count++ }
        }

        //EXECUTE

        //let the async start collecting
        yield()

        uut.deleteGroup(0L)
        uut.insertFeature(testFeature)
        uut.updateFeature(testFeature)
        uut.deleteFeature(0L)
        uut.deleteDataPoint(testDataPoint)
        uut.deleteAllDataPointsForDiscreteValue(0L, 0.0)
        uut.insertDataPoint(testDataPoint)
        uut.insertDataPoints(listOf(testDataPoint))
        uut.updateDataPoints(listOf(testDataPoint))
        uut.removeNote(OffsetDateTime.MAX, 0L)
        uut.deleteGlobalNote(testGlobalNote)
        uut.insertGlobalNote(testGlobalNote)

        //VERIFY
        assertEquals(12, count)
        collectJob.cancel()
    }
}