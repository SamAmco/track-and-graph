package com.samco.trackandgraph.base.model

import com.nhaarman.mockitokotlin2.*
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.entity.DataPoint
import com.samco.trackandgraph.base.database.entity.TrackerSuggestionOrder
import com.samco.trackandgraph.base.database.entity.TrackerSuggestionType
import com.samco.trackandgraph.base.database.entity.queryresponse.TrackerWithFeature
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.threeten.bp.OffsetDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerHelperImpl_UpdateDataPoints_Test {

    private val dao: TrackAndGraphDatabaseDao = mock()
    private val dataPointUpdateHelper: DataPointUpdateHelper = DataPointUpdateHelperImpl()
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private lateinit var uut: TrackerHelperImpl

    @Before
    fun before() = runBlocking {
        val transactionHelper = object : DatabaseTransactionHelper {
            override suspend fun <R> withTransaction(block: suspend () -> R): R {
                return block()
            }
        }

        uut = TrackerHelperImpl(
            transactionHelper = transactionHelper,
            dao = dao,
            dataPointUpdateHelper = dataPointUpdateHelper,
            io = dispatcher
        )
    }

    @Test
    fun `test no interactions given invalid input`() = runTest(dispatcher) {
        val featureId = 0L
        uut.updateDataPoints(
            trackerId = featureId,
            whereValue = null,
            whereLabel = null,
            toValue = null,
            toLabel = null
        )
        uut.updateDataPoints(
            trackerId = featureId,
            whereValue = 0.0,
            whereLabel = null,
            toValue = null,
            toLabel = null
        )
        uut.updateDataPoints(
            trackerId = featureId,
            whereValue = null,
            whereLabel = "hello",
            toValue = null,
            toLabel = null
        )
        uut.updateDataPoints(
            trackerId = featureId,
            whereValue = null,
            whereLabel = null,
            toValue = 0.0,
            toLabel = null
        )
        uut.updateDataPoints(
            trackerId = featureId,
            whereValue = null,
            whereLabel = null,
            toValue = null,
            toLabel = "hello"
        )
        uut.updateDataPoints(
            trackerId = featureId,
            whereValue = 0.0,
            whereLabel = "hello",
            toValue = null,
            toLabel = null
        )
        uut.updateDataPoints(
            trackerId = featureId,
            whereValue = null,
            whereLabel = null,
            toValue = 0.0,
            toLabel = "hello"
        )

        verifyZeroInteractions(dao)
    }

    @Test
    fun `where value, to value`() = runTest(dispatcher) {
        testCorrectUpdate(
            db = listOf(Pair(-8.234, "label 1"), Pair(2.0, "label 2")),
            expectUpdate = listOf(Pair(3.0, "label 1")),
            updateParams = UpdateParams(
                whereValue = -8.234,
                toValue = 3.0
            ),
            isDuration = false
        )
    }

    @Test
    fun `where label, to label`() = runTest(dispatcher) {
        testCorrectUpdate(
            db = listOf(Pair(1.0, "label 1"), Pair(2.0, "label 2")),
            expectUpdate = listOf(Pair(2.0, "label 3")),
            updateParams = UpdateParams(
                whereLabel = "label 2",
                toLabel = "label 3"
            ),
            isDuration = false
        )
    }

    @Test
    fun `where value, to label`() = runTest(dispatcher) {
        testCorrectUpdate(
            db = listOf(Pair(1.0, "label 1"), Pair(1.1, "label 2")),
            expectUpdate = listOf(Pair(1.0, "label 3")),
            updateParams = UpdateParams(
                whereValue = 1.0,
                toLabel = "label 3"
            ),
            isDuration = false
        )
    }

    @Test
    fun `where label, to value`() = runTest(dispatcher) {
        testCorrectUpdate(
            db = listOf(Pair(1.0, "label 1"), Pair(2.0, "label 2")),
            expectUpdate = listOf(Pair(4.0, "label 2")),
            updateParams = UpdateParams(
                whereLabel = "label 2",
                toValue = 4.0
            ),
            isDuration = false
        )
    }

    @Test
    fun `where value, to value, duration`() = runTest(dispatcher) {
        testCorrectUpdate(
            db = listOf(Pair(1.1234, "label 1"), Pair(2.7890, "label 2")),
            expectUpdate = listOf(Pair(4.0, "label 2")),
            updateParams = UpdateParams(
                whereValue = 2.0,
                toValue = 4.0
            ),
            isDuration = true
        )
    }

    @Test
    fun `where value, to value, duration negatives`() = runTest(dispatcher) {
        testCorrectUpdate(
            db = listOf(Pair(-1.1234, "label 1"), Pair(2.7890, "label 2")),
            expectUpdate = listOf(Pair(-4.0, "label 1")),
            updateParams = UpdateParams(
                whereValue = -1.0,
                toValue = -4.0
            ),
            isDuration = true
        )
    }

    @Test
    fun `where value and label, to value, duration`() = runTest(dispatcher) {
        testCorrectUpdate(
            db = listOf(
                Pair(-1.1234, "label 2"),
                Pair(-1.1234, "label 1"),
                Pair(1.0, "label 1"),
                Pair(2.7890, "label 2")
            ),
            expectUpdate = listOf(Pair(-4.0, "label 1")),
            updateParams = UpdateParams(
                whereValue = -1.0,
                whereLabel = "label 1",
                toValue = -4.0
            ),
            isDuration = true
        )
    }

    @Test
    fun `where label, to label and value`() = runTest(dispatcher) {
        testCorrectUpdate(
            db = listOf(
                Pair(1.0, "label 2"),
                Pair(2.0, "label 1"),
                Pair(1.0, "label 123"),
                Pair(1.0, "howdy"),
            ),
            expectUpdate = listOf(Pair(-4.0, "howdy")),
            updateParams = UpdateParams(
                whereLabel = "label 123",
                toLabel = "howdy",
                toValue = -4.0
            ),
            isDuration = false
        )
    }

    @Test
    fun `where value and label, to value and label`() = runTest(dispatcher) {
        testCorrectUpdate(
            db = listOf(
                Pair(-1.0, "label 2"),
                Pair(-1.1234, "label 1"),
                Pair(-1.0, "label 1"),
                Pair(2.7890, "label 2")
            ),
            expectUpdate = listOf(Pair(-4.89, "howdy")),
            updateParams = UpdateParams(
                whereValue = -1.0,
                whereLabel = "label 1",
                toValue = -4.89,
                toLabel = "howdy"
            ),
            isDuration = false
        )
    }

    @Test
    fun `large batch update`() = runTest(dispatcher) {
        val testValue = 8.779
        val db = (0..8500).map {
            Pair(it.toDouble(), "label ${it % 2 == 0}")
        }
        val expected = (0..8500).filter { it % 2 == 0 }.map {
            Pair(testValue, "label true")
        }

        testCorrectUpdate(
            db = db,
            expectUpdate = expected,
            updateParams = UpdateParams(
                whereLabel = "label true",
                toValue = testValue
            ),
            isDuration = false
        )
    }

    private fun List<Pair<Double, String>>.dataPoints() = this.map {
        DataPoint(
            epochMilli = Long.MAX_VALUE,
            utcOffsetSec = 0,
            featureId = 0L,
            value = it.first,
            label = it.second,
            note = ""
        )
    }

    data class UpdateParams(
        val whereValue: Double? = null,
        val whereLabel: String? = null,
        val toValue: Double? = null,
        val toLabel: String? = null
    )

    private suspend fun testCorrectUpdate(
        db: List<Pair<Double, String>>,
        expectUpdate: List<Pair<Double, String>>,
        updateParams: UpdateParams,
        isDuration: Boolean
    ) {
        val receivedUpdate = mutableListOf<DataPoint>()

        whenever(dao.getTrackerById(any())).thenReturn(
            TrackerWithFeature(
                id = 0L,
                name = "",
                groupId = 0L,
                featureId = 0L,
                displayIndex = 0,
                description = "",
                dataType = if (isDuration) DataType.DURATION else DataType.CONTINUOUS,
                hasDefaultValue = false,
                defaultValue = 1.0,
                defaultLabel = "",
                suggestionType = TrackerSuggestionType.NONE,
                suggestionOrder = TrackerSuggestionOrder.LATEST
            )
        )
        whenever(dao.getDataPointCount(any())).thenReturn(db.size)
        whenever(dao.getDataPoints(any(), any(), any())).thenAnswer {
            val limit = it.arguments[1] as Int
            val offset = it.arguments[2] as Int
            return@thenAnswer db.drop(offset).take(limit).dataPoints()
        }
        whenever(dao.updateDataPoints(any())).thenAnswer {
            receivedUpdate.addAll(it.arguments[0] as List<DataPoint>)
        }

        uut.updateDataPoints(
            trackerId = 0L,
            whereValue = updateParams.whereValue,
            whereLabel = updateParams.whereLabel,
            toValue = updateParams.toValue,
            toLabel = updateParams.toLabel
        )

        assertEquals(expectUpdate.dataPoints(), receivedUpdate)
    }
}