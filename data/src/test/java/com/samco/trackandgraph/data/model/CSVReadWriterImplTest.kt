package com.samco.trackandgraph.data.model

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.Feature
import com.samco.trackandgraph.data.database.dto.IDataPoint
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.database.entity.DataPoint
import com.samco.trackandgraph.data.database.sampling.DataSample
import com.samco.trackandgraph.data.database.sampling.DataSampleProperties
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class CSVReadWriterImplTest {
    private val legacyImportTestCSV = """
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

    private val importTestCSV = """
FeatureName,Timestamp,Value,Label,Note
A,2022-09-14T21:30:41.432+01:00,1,,
A,2022-09-14T12:55:37.508+01:00,-10.1,,
B,2022-09-17T21:14:56.864+01:00,0.0,,
B,2022-09-03T19:36:45.924+01:00,2.859,,
B,2022-08-28T19:30:39.711+01:00,1.0,,So:me n:ot:e
Tracker C,2022-09-16T00:13:27.182+01:00,0.0,Label 1,
Tracker C,2022-09-15T00:20:40.986+01:00,1.0,Label 2,
Tracker C,2022-09-13T22:55:46.533+01:00,3,Label 1,
Tracker C,2022-09-12T23:00:55.750+01:00,-2,Label 3,
D,2021-02-09T11:07:28Z,1:10:00,,
D,2021-02-09T11:07:28Z,1:10:00,,
D,2021-02-08T11:17:39.165Z,-10:-30:20,Some: :la:bel,
D,2021-02-05T11:10:01.807Z,12345:18:20,,Some note ending with colon:
D,2021-02-05T11:10:01.808Z,12345:18:20,Label,Some note ending with colon:
    """.trimIndent()

    private val testDataPoints = listOf(
        //A,2022-09-14T21:30:41.432+01:00,1,
        toDp(
            timestamp = OffsetDateTime.of(
                2022,
                9,
                14,
                21,
                30,
                41,
                432_000_000,
                ZoneOffset.ofHours(1)
            ),
            featureId = 1L,
            value = 1.0,
            label = "",
            note = ""
        ),
        //A,2022-09-14T12:55:37.508+01:00,-10.1,
        toDp(
            timestamp = OffsetDateTime.of(
                2022,
                9,
                14,
                12,
                55,
                37,
                508_000_000,
                ZoneOffset.ofHours(1)
            ),
            featureId = 1L,
            value = -10.1,
            label = "",
            note = ""
        ),
        //"B",2022-09-17T21:14:56.864+01:00,0.0,
        toDp(
            timestamp = OffsetDateTime.of(
                2022,
                9,
                17,
                21,
                14,
                56,
                864_000_000,
                ZoneOffset.ofHours(1)
            ),
            featureId = 0L,
            value = 0.0,
            label = "",
            note = ""
        ),
        //"B",2022-09-03T19:36:45.924+01:00,2.859,
        toDp(
            timestamp = OffsetDateTime.of(
                2022,
                9,
                3,
                19,
                36,
                45,
                924_000_000,
                ZoneOffset.ofHours(1)
            ),
            featureId = 0L,
            value = 2.859,
            label = "",
            note = ""
        ),
        //"B",2022-08-28T19:30:39.711+01:00,1.0,So:me n:ot:e
        toDp(
            timestamp = OffsetDateTime.of(
                2022,
                8,
                28,
                19,
                30,
                39,
                711_000_000,
                ZoneOffset.ofHours(1)
            ),
            featureId = 0L,
            value = 1.0,
            label = "",
            note = "So:me n:ot:e"
        ),
        //Tracker C,2022-09-16T00:13:27.182+01:00,0.0:Label 1,
        toDp(
            timestamp = OffsetDateTime.of(
                2022,
                9,
                16,
                0,
                13,
                27,
                182_000_000,
                ZoneOffset.ofHours(1)
            ),
            featureId = 2L,
            value = 0.0,
            label = "Label 1",
            note = ""
        ),
        //Tracker C,2022-09-15T00:20:40.986+01:00,1.0:Label 2,
        toDp(
            timestamp = OffsetDateTime.of(
                2022,
                9,
                15,
                0,
                20,
                40,
                986_000_000,
                ZoneOffset.ofHours(1)
            ),
            featureId = 2L,
            value = 1.0,
            label = "Label 2",
            note = ""
        ),
        //Tracker C,2022-09-13T22:55:46.533+01:00,3:Label 1,
        toDp(
            timestamp = OffsetDateTime.of(
                2022,
                9,
                13,
                22,
                55,
                46,
                533_000_000,
                ZoneOffset.ofHours(1)
            ),
            featureId = 2L,
            value = 3.0,
            label = "Label 1",
            note = ""
        ),
        //Tracker C,2022-09-12T23:00:55.750+01:00,-2:Label 3,
        toDp(
            timestamp = OffsetDateTime.of(
                2022,
                9,
                12,
                23,
                0,
                55,
                750_000_000,
                ZoneOffset.ofHours(1)
            ),
            featureId = 2L,
            value = -2.0,
            label = "Label 3",
            note = ""
        ),
        //D,2021-02-09T11:07:28Z,1:10:00,
        toDp(
            timestamp = OffsetDateTime.of(
                2021,
                2,
                9,
                11,
                7,
                28,
                0,
                ZoneOffset.ofHours(0)
            ),
            featureId = 3L,
            value = 4200.0,
            label = "",
            note = ""
        ),
        //D,2021-02-09T11:07:28Z,1:10:00:,
        toDp(
            timestamp = OffsetDateTime.of(
                2021,
                2,
                9,
                11,
                7,
                28,
                0,
                ZoneOffset.ofHours(0)
            ),
            featureId = 3L,
            value = 4200.0,
            label = "",
            note = ""
        ),
        //D,2021-02-08T11:17:39.165Z,-10:-30:20:Some: :la:bel,
        toDp(
            timestamp = OffsetDateTime.of(
                2021,
                2,
                8,
                11,
                17,
                39,
                165_000_000,
                ZoneOffset.ofHours(0)
            ),
            featureId = 3L,
            value = (-10 * 60 * 60) + (-30 * 60) + 20.0,
            label = "Some: :la:bel",
            note = ""
        ),
        //D,2021-02-05T11:10:01.807Z,12345:18:20,Some note ending with colon:,
        toDp(
            timestamp = OffsetDateTime.of(
                2021,
                2,
                5,
                11,
                10,
                1,
                807_000_000,
                ZoneOffset.ofHours(0)
            ),
            featureId = 3L,
            value = (12345 * 60 * 60) + (18 * 60) + 20.0,
            label = "",
            note = "Some note ending with colon:"
        ),
        //D,2021-02-05T11:10:01.808Z,12345:18:20:Label,Some note ending with colon:,
        toDp(
            timestamp = OffsetDateTime.of(
                2021,
                2,
                5,
                11,
                10,
                1,
                808_000_000,
                ZoneOffset.ofHours(0)
            ),
            featureId = 3L,
            value = (12345 * 60 * 60) + (18 * 60) + 20.0,
            label = "Label",
            note = "Some note ending with colon:"

        )
    )

    private val groupId = 2L

    private val testTrackers = listOf(
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
        ),
        Tracker(
            id = 1L,
            name = "A",
            groupId = groupId,
            featureId = 1L,
            displayIndex = 0,
            description = "",
            dataType = DataType.CONTINUOUS,
            hasDefaultValue = false,
            defaultValue = 1.0,
            defaultLabel = ""
        ),
        Tracker(
            id = 2L,
            name = "Tracker C",
            groupId = groupId,
            featureId = 2L,
            displayIndex = 0,
            description = "",
            dataType = DataType.CONTINUOUS,
            hasDefaultValue = false,
            defaultValue = 1.0,
            defaultLabel = ""
        ),
        Tracker(
            id = 3L,
            name = "D",
            groupId = groupId,
            featureId = 3L,
            displayIndex = 0,
            description = "",
            dataType = DataType.DURATION,
            hasDefaultValue = false,
            defaultValue = 1.0,
            defaultLabel = ""
        ),
    )

    private val trackerHelper = mock<TrackerHelper>()
    private val dao = mock<TrackAndGraphDatabaseDao>()
    private val dispatcher = UnconfinedTestDispatcher()


    private lateinit var csvReadWriterImpl: CSVReadWriterImpl

    @Before
    fun before() {
        csvReadWriterImpl = CSVReadWriterImpl(
            dao = dao,
            trackerHelper = trackerHelper,
            io = dispatcher
        )
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `can import the legacy test CSV correctly`() = runTest(dispatcher) {
        runImportTest(legacyImportTestCSV)
    }

    @Test
    fun `can import the test CSV correctly`() = runTest(dispatcher) {
        runImportTest(importTestCSV)
    }

    private suspend fun runImportTest(testCSV: String) {
        //PREPARE
        val allInsertedDataPoints = mutableListOf<DataPoint>()

        val storedTrackers = mutableListOf(
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

        whenever(trackerHelper.getTrackersForGroupSync(groupId)).thenAnswer { storedTrackers }
        whenever(trackerHelper.insertTracker(any())).thenAnswer {
            val newId = storedTrackers.size.toLong()
            val newTracker = (it.arguments[0] as Tracker).copy(
                id = newId,
                featureId = newId
            )
            storedTrackers.add(newTracker)
            return@thenAnswer newId
        }
        whenever(trackerHelper.getTrackerById(any())).thenAnswer { inv ->
            val id = inv.arguments[0] as Long
            storedTrackers.first { it.id == id }
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

    @Test
    fun `can export the test data points correctly`() = runTest(dispatcher) {
        //PREPARE
        val featureMap = testDataPoints
            .groupBy { it.featureId }
            .map { tuple ->
                val feature = testTrackers.first { it.featureId == tuple.key } as Feature
                val dataSampleProperties =
                    DataSampleProperties(isDuration = feature.featureId == 3L)
                feature to DataSample.fromSequence(
                    data = tuple.value.map { it.asIDataPoint() }.asSequence(),
                    dataSampleProperties = dataSampleProperties,
                    getRawDataPoints = { tuple.value.map { it.toDto() } },
                    onDispose = {}
                )
            }
            .toMap()

        val outputStream = ByteArrayOutputStream()

        //EXECUTE
        csvReadWriterImpl.writeFeaturesToCSV(outputStream, featureMap)

        //VERIFY

        val expectedExportCSV = """
FeatureName,Timestamp,Value,Label,Note
A,2022-09-14T21:30:41.432+01:00,1.0,,
A,2022-09-14T12:55:37.508+01:00,-10.1,,
B,2022-09-17T21:14:56.864+01:00,0.0,,
B,2022-09-03T19:36:45.924+01:00,2.859,,
B,2022-08-28T19:30:39.711+01:00,1.0,,So:me n:ot:e
Tracker C,2022-09-16T00:13:27.182+01:00,0.0,Label 1,
Tracker C,2022-09-15T00:20:40.986+01:00,1.0,Label 2,
Tracker C,2022-09-13T22:55:46.533+01:00,3.0,Label 1,
Tracker C,2022-09-12T23:00:55.750+01:00,-2.0,Label 3,
D,2021-02-09T11:07:28Z,1:10:00,,
D,2021-02-09T11:07:28Z,1:10:00,,
D,2021-02-08T11:17:39.165Z,-10:-29:-40,Some: :la:bel,
D,2021-02-05T11:10:01.807Z,12345:18:20,,Some note ending with colon:
D,2021-02-05T11:10:01.808Z,12345:18:20,Label,Some note ending with colon:

        """.trimIndent()
            .replace("\n", "\r\n")

        assertEquals(expectedExportCSV, outputStream.toString())
    }

    private fun verifyInsertedDataPoints(allInsertedDataPoints: List<DataPoint>) {
        for ((_, i) in allInsertedDataPoints) {
            assertEquals(
                "Inserted data point wrong at index: $i",
                testDataPoints[i.toInt()],
                allInsertedDataPoints[i.toInt()]
            )
        }
    }

    private suspend fun verifyInsertedTrackers() {
        verify(trackerHelper, never()).insertTracker(testTrackers[0])

        testTrackers.drop(1).forEach {
            verify(trackerHelper, times(1))
                .insertTracker(it.copy(id = 0, featureId = 0))
        }
    }

    private fun DataPoint.asIDataPoint(): IDataPoint {
        val dpTs = OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(epochMilli),
            ZoneOffset.ofTotalSeconds(utcOffsetSec)
        )
        val dpValue = this.value
        val dpLabel = this.label

        return object : IDataPoint() {
            override val timestamp: OffsetDateTime = dpTs
            override val value: Double = dpValue
            override val label: String = dpLabel
        }
    }

    private fun toDp(
        timestamp: OffsetDateTime,
        featureId: Long,
        value: Double,
        label: String,
        note: String
    ) = DataPoint(
        epochMilli = timestamp.toInstant().toEpochMilli(),
        utcOffsetSec = timestamp.offset.totalSeconds,
        featureId = featureId,
        value = value,
        label = label,
        note = note
    )
}