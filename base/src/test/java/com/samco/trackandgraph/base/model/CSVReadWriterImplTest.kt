package com.samco.trackandgraph.base.model

import com.nhaarman.mockitokotlin2.*
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.database.entity.DataPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class CSVReadWriterImplTest {
    //TODO try a continuous with a note with several colons
    //TODO try a duration with a note with several colons
    private val testCSV = """
FeatureName,Timestamp,Value,Note
A,2022-09-14T21:30:41.432+01:00,1,
A,2022-09-14T12:55:37.508+01:00,-10.1,
"B",2022-09-17T21:14:56.864+01:00,0.0,
"B",2022-09-03T19:36:45.924+01:00,2.859,
"B",2022-08-28T19:30:39.711+01:00,1.0,So:me n:ot:e
Tracker C,2022-09-16T00:13:27.182+01:00,0.0:Label 1,
Tracker C,2022-09-15T00:20:40.986+01:00,1.0:Label 2,
Tracker C,2022-09-13T22:55:46.533+01:00,3:Label 1,
Tracker C,2022-09-12T23:00:55.750+01:00,-2:Label 3,
D,2021-02-09T11:07:28Z,1:10:00,
D,2021-02-09T11:07:28Z,1:10:00:,
D,2021-02-08T11:17:39.165Z,-10:-30:20:Some: :la:bel,
D,2021-02-05T11:10:01.808Z,12345:18:20,Some note ending with colon:,
D,2021-02-05T11:10:01.808Z,12345:18:20:Label,Some note ending with colon:,
    """.trimIndent()

    private val trackerHelper = mock<TrackerHelper>()
    private val dao = mock<TrackAndGraphDatabaseDao>()
    private val dispatcher = UnconfinedTestDispatcher()

    private val groupId = 2L
    private var allTrackers = mutableListOf(
        Tracker(
            id = 0L,
            name = "B",
            groupId = groupId,
            featureId = 0L,
            displayIndex = 0,
            description = "",
            dataType = DataType.CONTINUOUS,
            hasDefaultValue = false,
            defaultValue = 1.0,
            defaultLabel = ""
        )
    )

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `can import the Test CSV`() = runTest {
        //PREPARE
        val csvReadWriterImpl = CSVReadWriterImpl(dao, trackerHelper, dispatcher)
        val allInsertedDataPoints = mutableListOf<DataPoint>()
        whenever(trackerHelper.getTrackersForGroupSync(groupId)).thenAnswer { allTrackers }
        whenever(trackerHelper.insertTracker(any())).thenAnswer {
            val newId = allTrackers.size.toLong()
            val newTracker = (it.arguments[0] as Tracker).copy(
                id = newId,
                featureId = newId
            )
            allTrackers.add(newTracker)
            return@thenAnswer newId
        }
        whenever(trackerHelper.getTrackerById(any())).thenAnswer { inv ->
            val id = inv.arguments[0] as Long
            allTrackers.first { it.id == id }
        }
        whenever(dao.insertDataPoints(any())).thenAnswer {
            allInsertedDataPoints.addAll(it.arguments[0] as List<DataPoint>)
        }

        //EXECUTE
        csvReadWriterImpl.readFeaturesFromCSV(
            ByteArrayInputStream(testCSV.toByteArray()),
            groupId
        )

        //VERIFY
        verifyInsertedTrackers()
        verifyInsertedDataPoints(allInsertedDataPoints)
    }

    private fun verifyInsertedDataPoints(allInsertedDataPoints: List<DataPoint>) {
        assertEquals(14, allInsertedDataPoints.size)
        assertEquals(
            DataPoint(
                timestamp = OffsetDateTime.of(2022, 9, 14, 21, 30, 41, 432_000_000, ZoneOffset.ofHours(1)),
                featureId = 1L,
                value = 1.0,
                label = "",
                note = ""
            ),
            allInsertedDataPoints[0]
        )
        assertEquals(
            -10.1,
            allInsertedDataPoints[1].value,
            0.00001
        )
        assertEquals(
            DataPoint(
                timestamp = OffsetDateTime.of(2022, 9, 17, 21, 14, 56, 864_000_000, ZoneOffset.ofHours(1)),
                featureId = 0L,
                value = 0.0,
                label = "",
                note = ""
            ),
            allInsertedDataPoints[2]
        )
        assertEquals(
            2.859,
            allInsertedDataPoints[3].value,
            0.00001
        )
        assertEquals(
            1.0,
            allInsertedDataPoints[4].value,
            0.00001
        )
        assertEquals(
            "So:me n:ot:e",
            allInsertedDataPoints[4].note
        )
        assertEquals(
            DataPoint(
                timestamp = OffsetDateTime.of(2022, 9, 16, 0, 13, 27, 182_000_000, ZoneOffset.ofHours(1)),
                featureId = 2L,
                value = 0.0,
                label = "Label 1",
                note = ""
            ),
            allInsertedDataPoints[5]
        )
        assertEquals(
            "Label 2",
            allInsertedDataPoints[6].label
        )
        assertEquals(
            1.0,
            allInsertedDataPoints[6].value,
            0.00001
        )
        assertEquals(
            3.0,
            allInsertedDataPoints[7].value,
            0.00001
        )
        assertEquals(
            -2.0,
            allInsertedDataPoints[8].value,
            0.00001
        )
        assertEquals(
            DataPoint(
                timestamp = OffsetDateTime.of(2021, 2, 9, 11, 7, 28, 0, ZoneOffset.ofHours(0)),
                featureId = 3L,
                value = 4200.0,
                label = "",
                note = ""
            ),
            allInsertedDataPoints[9]
        )
        assertEquals(
            DataPoint(
                timestamp = OffsetDateTime.of(2021, 2, 9, 11, 7, 28, 0, ZoneOffset.ofHours(0)),
                featureId = 3L,
                value = 4200.0,
                label = "",
                note = ""
            ),
            allInsertedDataPoints[10]
        )
        assertEquals(
            "Some: :la:bel",
            allInsertedDataPoints[11].label
        )
        assertEquals(
            -37780.0,
            allInsertedDataPoints[11].value,
            0.00001
        )
        assertEquals(
            "Some: :la:bel",
            allInsertedDataPoints[11].label
        )
        assertEquals(
            -37780.0,
            allInsertedDataPoints[11].value,
            0.00001
        )
        assertEquals(
            "Some note ending with colon:",
            allInsertedDataPoints[12].note
        )
        assertEquals(
            "",
            allInsertedDataPoints[12].label
        )
        assertEquals(
            (60 * 60 * 12345) + (60 * 18) + 20.0,
            allInsertedDataPoints[12].value,
            0.00001
        )
        assertEquals(
            "Some note ending with colon:",
            allInsertedDataPoints[13].note
        )
        assertEquals(
            "Label",
            allInsertedDataPoints[13].label
        )
        assertEquals(
            (60 * 60 * 12345) + (60 * 18) + 20.0,
            allInsertedDataPoints[13].value,
            0.00001
        )
    }

    private suspend fun verifyInsertedTrackers() {
        verify(trackerHelper, times(1)).insertTracker(
            eq(
                Tracker(
                    id = 0L,
                    name = "A",
                    groupId = groupId,
                    featureId = 0L,
                    displayIndex = 0,
                    description = "",
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = false,
                    defaultValue = 1.0,
                    defaultLabel = ""
                )
            )
        )
        verify(trackerHelper, never()).insertTracker(
            eq(
                Tracker(
                    id = 0L,
                    name = "B",
                    groupId = groupId,
                    featureId = 0L,
                    displayIndex = 0,
                    description = "",
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = false,
                    defaultValue = 1.0,
                    defaultLabel = ""
                )
            )
        )
        verify(trackerHelper, times(1)).insertTracker(
            eq(
                Tracker(
                    id = 0L,
                    name = "Tracker C",
                    groupId = groupId,
                    featureId = 0L,
                    displayIndex = 0,
                    description = "",
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = false,
                    defaultValue = 1.0,
                    defaultLabel = ""
                )
            )
        )
        verify(trackerHelper, times(1)).insertTracker(
            eq(
                Tracker(
                    id = 0L,
                    name = "D",
                    groupId = groupId,
                    featureId = 0L,
                    displayIndex = 0,
                    description = "",
                    dataType = DataType.DURATION,
                    hasDefaultValue = false,
                    defaultValue = 1.0,
                    defaultLabel = ""
                )
            )
        )
    }
}