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
package com.samco.trackandgraph.adddatapoint

import com.nhaarman.mockitokotlin2.*
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.base.model.DataInteractor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.threeten.bp.OffsetDateTime

@ExperimentalCoroutinesApi
class SuggestedValueHelperImplTest {

    private lateinit var uut: SuggestedValueHelperImpl

    private val dataInteractor: DataInteractor = mock()

    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val testTracker = Tracker(
        id = 1,
        name = "test",
        groupId = 1,
        featureId = 1,
        displayIndex = 1,
        description = "test",
        dataType = DataType.CONTINUOUS,
        hasDefaultValue = true,
        defaultValue = 1.0,
        defaultLabel = "test",
        suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
        suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
    )

    @Before
    fun setUp() {

        uut = SuggestedValueHelperImpl(dataInteractor, dispatcher)
    }

    //A function to create a data point from a value and a label
    private fun createDataPoint(value: Double?, label: String?) = object : IDataPoint() {
        override val timestamp: OffsetDateTime = OffsetDateTime.MIN
        override val value: Double = value ?: 1.0
        override val label: String = label ?: ""
    }

    //A function to create a sequence of data points from a list of values
    private fun dpFromValues(values: List<Double>) = values
        .map { createDataPoint(it, null) }
        .asSequence()

    //A function to create a sequence of data points from a list of values and labels
    private fun dpFromValuesAndLabels(values: List<Double>, labels: List<String>) = values
        .zip(labels)
        .map { createDataPoint(it.first, it.second) }
        .asSequence()

    //A function for a list of data points that returns true if the values are sorted by value ascending
    private fun isSortedByValueAscending(dataPoints: List<IDataPoint>) = dataPoints
        .zipWithNext()
        .all { it.first.value <= it.second.value }

    @Test
    fun `test getSuggestedValues values only`() = runTest {
        //Create a list of 1000 random double values
        val values = List(1000) { Math.random() }

        //Create a list of data points from the values
        val dataPoints = dpFromValues(values)

        //Assert that the data points are not sorted
        assert(!isSortedByValueAscending(dataPoints.toList()))

        whenever(dataInteractor.getDataSampleForFeatureId(any()))
            .thenReturn(DataSample.fromSequence(dataPoints))

        //suggested values is a flow of lists of SuggestedValue
        val suggestedValues = uut.getSuggestedValues(
            testTracker.copy(
                suggestionType = TrackerSuggestionType.VALUE_ONLY,
                suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
            )
        )

        //Collect the whole suggested values flow
        val suggestedValuesList = suggestedValues.toList()

        //Assert that each of the lists contains more values than the last
        suggestedValuesList
            .zipWithNext()
            .forEach { (first, second) -> assert(first.size < second.size) }

        //Assert that each of the lists is sorted
        suggestedValuesList.forEach { list -> assert(list.sortedBy { it.value } == list) }

        //Verify that dataInteractor.getDataSampleForFeatureId was called only once
        verify(dataInteractor, times(1)).getDataSampleForFeatureId(eq(1))
    }

    @Test
    fun `test suggested value with value and label ordered by latest different suggestion types`() =
        runTest {
            //Create a sequence of data points with values and labels
            val dataPoints = dpFromValuesAndLabels(
                listOf(5.0, 3.0, 5.0, 4.0, 4.0),
                listOf("e", "b", "e", "c", "e")
            )

            whenever(dataInteractor.getDataSampleForFeatureId(any()))
                .thenReturn(DataSample.fromSequence(dataPoints))

            //suggested values is a flow of lists of SuggestedValue
            val suggestedValueAndLabels = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
                    suggestionOrder = TrackerSuggestionOrder.LATEST
                )
            )

            val suggestedValues = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.LATEST
                )
            )

            val suggestedLabels = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.LABEL_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.LATEST
                )
            )

            //Collect the whole suggested values flow
            val suggestedValuesAndLabelsList = suggestedValueAndLabels.toList()
            val suggestedValuesList = suggestedValues.toList()
            val suggestedLabelsList = suggestedLabels.toList()

            assertEquals(
                listOf(
                    SuggestedValue(5.0, "e"),
                    SuggestedValue(3.0, "b"),
                    SuggestedValue(4.0, "c"),
                    SuggestedValue(4.0, "e")
                ),
                suggestedValuesAndLabelsList.last()
            )

            assertEquals(
                listOf(
                    SuggestedValue(5.0, null),
                    SuggestedValue(3.0, null),
                    SuggestedValue(4.0, null)
                ),
                suggestedValuesList.last()
            )

            assertEquals(
                listOf(
                    SuggestedValue(null, "e"),
                    SuggestedValue(null, "b"),
                    SuggestedValue(null, "c")
                ),
                suggestedLabelsList.last()
            )
        }

    @Test
    fun `test suggested value with value only different suggestion orders`() =
        runTest {
            //Create a sequence of data points with values and labels
            val dataPoints = dpFromValuesAndLabels(
                listOf(5.0, 3.0, 5.0, 4.0, 4.0, 1.0),
                listOf("e", "b", "e", "c", "e", "")
            )

            whenever(dataInteractor.getDataSampleForFeatureId(any()))
                .thenReturn(DataSample.fromSequence(dataPoints))

            val suggestedValuesValueAscending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
                )
            )

            val suggestedValuesValueDescending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.VALUE_DESCENDING
                )
            )

            val suggestedValuesLabelAscending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
                )
            )

            val suggestedValuesLabelDescending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_DESCENDING
                )
            )

            val suggestedValuesLatest = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.LATEST
                )
            )

            val suggestedValuesOldest = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.OLDEST
                )
            )

            //Collect the whole suggested values flows
            val suggestedValuesValueAscendingList = suggestedValuesValueAscending.toList()
            val suggestedValuesValueDescendingList = suggestedValuesValueDescending.toList()
            val suggestedValuesLabelAscendingList = suggestedValuesLabelAscending.toList()
            val suggestedValuesLabelDescendingList = suggestedValuesLabelDescending.toList()
            val suggestedValuesLatestList = suggestedValuesLatest.toList()
            val suggestedValuesOldestList = suggestedValuesOldest.toList()

            //Assert value ascending
            assertEquals(
                listOf(
                    SuggestedValue(1.0, null),
                    SuggestedValue(3.0, null),
                    SuggestedValue(4.0, null),
                    SuggestedValue(5.0, null)
                ),
                suggestedValuesValueAscendingList.last()
            )

            //Assert value descending
            assertEquals(
                listOf(
                    SuggestedValue(5.0, null),
                    SuggestedValue(4.0, null),
                    SuggestedValue(3.0, null),
                    SuggestedValue(1.0, null)
                ),
                suggestedValuesValueDescendingList.last()
            )

            //Note that empty labels always come last whether sorting by label ascending or descending
            //Assert label ascending
            assertEquals(
                listOf(
                    SuggestedValue(3.0, null),
                    SuggestedValue(4.0, null),
                    SuggestedValue(5.0, null),
                    SuggestedValue(1.0, null)
                ),
                suggestedValuesLabelAscendingList.last()
            )

            //Note that empty labels always come last whether sorting by label ascending or descending
            //Assert label descending
            assertEquals(
                listOf(
                    SuggestedValue(5.0, null),
                    SuggestedValue(4.0, null),
                    SuggestedValue(3.0, null),
                    SuggestedValue(1.0, null)
                ),
                suggestedValuesLabelDescendingList.last()
            )

            //Assert latest
            assertEquals(
                listOf(
                    SuggestedValue(5.0, null),
                    SuggestedValue(3.0, null),
                    SuggestedValue(4.0, null),
                    SuggestedValue(1.0, null)
                ),
                suggestedValuesLatestList.last()
            )

            //Assert oldest
            assertEquals(
                listOf(
                    SuggestedValue(1.0, null),
                    SuggestedValue(4.0, null),
                    SuggestedValue(5.0, null),
                    SuggestedValue(3.0, null)
                ),
                suggestedValuesOldestList.last()
            )
        }

    @Test
    fun `test suggested value with label only different suggestion orders`() =
        runTest {
            //Create a sequence of data points with values and labels
            val dataPoints = dpFromValuesAndLabels(
                listOf(5.0, 3.0, 5.0, 4.0, 4.0, 1.0),
                listOf("e", "b", "e", "c", "e", "")
            )

            whenever(dataInteractor.getDataSampleForFeatureId(any()))
                .thenReturn(DataSample.fromSequence(dataPoints))

            val suggestedValuesValueAscending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.LABEL_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
                )
            )

            val suggestedValuesValueDescending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.LABEL_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.VALUE_DESCENDING
                )
            )

            val suggestedValuesLabelAscending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.LABEL_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
                )
            )

            val suggestedValuesLabelDescending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.LABEL_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_DESCENDING
                )
            )

            val suggestedValuesLatest = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.LABEL_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.LATEST
                )
            )

            val suggestedValuesOldest = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.LABEL_ONLY,
                    suggestionOrder = TrackerSuggestionOrder.OLDEST
                )
            )

            //Collect the whole suggested values flows
            val suggestedValuesValueAscendingList = suggestedValuesValueAscending.toList()
            val suggestedValuesValueDescendingList = suggestedValuesValueDescending.toList()
            val suggestedValuesLabelAscendingList = suggestedValuesLabelAscending.toList()
            val suggestedValuesLabelDescendingList = suggestedValuesLabelDescending.toList()
            val suggestedValuesLatestList = suggestedValuesLatest.toList()
            val suggestedValuesOldestList = suggestedValuesOldest.toList()

            //Assert value ascending
            assertEquals(
                listOf(
                    SuggestedValue(null, "b"),
                    SuggestedValue(null, "c"),
                    SuggestedValue(null, "e")
                ),
                suggestedValuesValueAscendingList.last()
            )

            //Assert value descending
            assertEquals(
                listOf(
                    SuggestedValue(null, "e"),
                    SuggestedValue(null, "c"),
                    SuggestedValue(null, "b")
                ),
                suggestedValuesValueDescendingList.last()
            )

            //Assert label ascending
            assertEquals(
                listOf(
                    SuggestedValue(null, "b"),
                    SuggestedValue(null, "c"),
                    SuggestedValue(null, "e")
                ),
                suggestedValuesLabelAscendingList.last()
            )

            //Assert label descending
            assertEquals(
                listOf(
                    SuggestedValue(null, "e"),
                    SuggestedValue(null, "c"),
                    SuggestedValue(null, "b")
                ),
                suggestedValuesLabelDescendingList.last()
            )

            //Assert latest
            assertEquals(
                listOf(
                    SuggestedValue(null, "e"),
                    SuggestedValue(null, "b"),
                    SuggestedValue(null, "c")
                ),
                suggestedValuesLatestList.last()
            )

            //Assert oldest
            assertEquals(
                listOf(
                    SuggestedValue(null, "e"),
                    SuggestedValue(null, "c"),
                    SuggestedValue(null, "b")
                ),
                suggestedValuesOldestList.last()
            )
        }

    @Test
    fun `test suggested value with value and label different suggestion orders`() =
        runTest {
            //Create a sequence of data points with values and labels
            val dataPoints = dpFromValuesAndLabels(
                listOf(5.0, 3.0, 5.0, 4.0, 4.0, 1.0),
                listOf("e", "b", "e", "c", "e", "")
            )

            whenever(dataInteractor.getDataSampleForFeatureId(any()))
                .thenReturn(DataSample.fromSequence(dataPoints))

            val suggestedValuesValueAscending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
                    suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
                )
            )

            val suggestedValuesValueDescending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
                    suggestionOrder = TrackerSuggestionOrder.VALUE_DESCENDING
                )
            )

            val suggestedValuesLabelAscending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
                )
            )

            val suggestedValuesLabelDescending = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
                    suggestionOrder = TrackerSuggestionOrder.LABEL_DESCENDING
                )
            )

            val suggestedValuesLatest = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
                    suggestionOrder = TrackerSuggestionOrder.LATEST
                )
            )

            val suggestedValuesOldest = uut.getSuggestedValues(
                testTracker.copy(
                    suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
                    suggestionOrder = TrackerSuggestionOrder.OLDEST
                )
            )

            //Collect the whole suggested values flows
            val suggestedValuesValueAscendingList = suggestedValuesValueAscending.toList()
            val suggestedValuesValueDescendingList = suggestedValuesValueDescending.toList()
            val suggestedValuesLabelAscendingList = suggestedValuesLabelAscending.toList()
            val suggestedValuesLabelDescendingList = suggestedValuesLabelDescending.toList()
            val suggestedValuesLatestList = suggestedValuesLatest.toList()
            val suggestedValuesOldestList = suggestedValuesOldest.toList()

            //Assert value ascending
            assertEquals(
                listOf(
                    SuggestedValue(1.0, null),
                    SuggestedValue(3.0, "b"),
                    SuggestedValue(4.0, "c"),
                    SuggestedValue(4.0, "e"),
                    SuggestedValue(5.0, "e"),
                ),
                suggestedValuesValueAscendingList.last()
            )

            //Assert value descending
            assertEquals(
                listOf(
                    SuggestedValue(5.0, "e"),
                    SuggestedValue(4.0, "c"),
                    SuggestedValue(4.0, "e"),
                    SuggestedValue(3.0, "b"),
                    SuggestedValue(1.0, null),
                ),
                suggestedValuesValueDescendingList.last()
            )

            //Assert label ascending
            assertEquals(
                listOf(
                    SuggestedValue(3.0, "b"),
                    SuggestedValue(4.0, "c"),
                    SuggestedValue(5.0, "e"),
                    SuggestedValue(4.0, "e"),
                    SuggestedValue(1.0, null),
                ),
                suggestedValuesLabelAscendingList.last()
            )

            //Assert label descending
            assertEquals(
                listOf(
                    SuggestedValue(5.0, "e"),
                    SuggestedValue(4.0, "e"),
                    SuggestedValue(4.0, "c"),
                    SuggestedValue(3.0, "b"),
                    SuggestedValue(1.0, null),
                ),
                suggestedValuesLabelDescendingList.last()
            )

            //Assert latest
            assertEquals(
                listOf(
                    SuggestedValue(5.0, "e"),
                    SuggestedValue(3.0, "b"),
                    SuggestedValue(4.0, "c"),
                    SuggestedValue(4.0, "e"),
                    SuggestedValue(1.0, null),
                ),
                suggestedValuesLatestList.last()
            )

            //Assert oldest
            assertEquals(
                listOf(
                    SuggestedValue(1.0, null),
                    SuggestedValue(4.0, "e"),
                    SuggestedValue(4.0, "c"),
                    SuggestedValue(5.0, "e"),
                    SuggestedValue(3.0, "b"),
                ),
                suggestedValuesOldestList.last()
            )
        }

    @Test
    fun `test suggested value with none and different suggestion orders`() = runTest {
        //Create a sequence of data points with values and labels
        val dataPoints = dpFromValuesAndLabels(
            listOf(5.0, 3.0, 5.0, 4.0, 4.0, 1.0),
            listOf("e", "b", "e", "c", "e", "")
        )

        whenever(dataInteractor.getDataSampleForFeatureId(any()))
            .thenReturn(DataSample.fromSequence(dataPoints))

        val suggestedValuesValueAscending = uut.getSuggestedValues(
            testTracker.copy(
                suggestionType = TrackerSuggestionType.NONE,
                suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
            )
        )

        val suggestedValuesValueDescending = uut.getSuggestedValues(
            testTracker.copy(
                suggestionType = TrackerSuggestionType.NONE,
                suggestionOrder = TrackerSuggestionOrder.VALUE_DESCENDING
            )
        )

        val suggestedValuesLabelAscending = uut.getSuggestedValues(
            testTracker.copy(
                suggestionType = TrackerSuggestionType.NONE,
                suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
            )
        )

        val suggestedValuesLabelDescending = uut.getSuggestedValues(
            testTracker.copy(
                suggestionType = TrackerSuggestionType.NONE,
                suggestionOrder = TrackerSuggestionOrder.LABEL_DESCENDING
            )
        )

        val suggestedValuesLatest = uut.getSuggestedValues(
            testTracker.copy(
                suggestionType = TrackerSuggestionType.NONE,
                suggestionOrder = TrackerSuggestionOrder.LATEST
            )
        )

        val suggestedValuesOldest = uut.getSuggestedValues(
            testTracker.copy(
                suggestionType = TrackerSuggestionType.NONE,
                suggestionOrder = TrackerSuggestionOrder.OLDEST
            )
        )

        //Collect the whole suggested values flows
        val suggestedValuesValueAscendingList = suggestedValuesValueAscending.toList()
        val suggestedValuesValueDescendingList = suggestedValuesValueDescending.toList()
        val suggestedValuesLabelAscendingList = suggestedValuesLabelAscending.toList()
        val suggestedValuesLabelDescendingList = suggestedValuesLabelDescending.toList()
        val suggestedValuesLatestList = suggestedValuesLatest.toList()
        val suggestedValuesOldestList = suggestedValuesOldest.toList()

        assertTrue(
            listOf(
                suggestedValuesLabelAscendingList,
                suggestedValuesLabelDescendingList,
                suggestedValuesLatestList,
                suggestedValuesOldestList,
                suggestedValuesValueAscendingList,
                suggestedValuesValueDescendingList
            ).all { it.last().isEmpty() }
        )
    }

    @Test
    fun `We recieve at most max values`() = runTest {
        //Create a list of > MAX_VALUES size
        val values = List(SuggestedValueHelperImpl.MAX_VALUES + 100) { it.toDouble() }
        val dataPoints = dpFromValues(values)

        whenever(dataInteractor.getDataSampleForFeatureId(any()))
            .thenReturn(DataSample.fromSequence(dataPoints))

        val sizes = mutableListOf<Int>()

        uut.getSuggestedValues(testTracker).collect { sizes.add(it.size) }

        assertTrue(sizes.last() == SuggestedValueHelperImpl.MAX_VALUES)
    }

    @Test
    fun `Disposes upstream once read when more than max values`() = runTest {
        val values = List(SuggestedValueHelperImpl.MAX_VALUES + 100) { it.toDouble() }
        val dataPoints = dpFromValues(values)

        var disposeCalled = false

        whenever(dataInteractor.getDataSampleForFeatureId(any())).thenReturn(
            DataSample.fromSequence(
                data = dataPoints,
                onDispose = { disposeCalled = true }
            )
        )

        uut.getSuggestedValues(testTracker).collect {}

        assertTrue(disposeCalled)
    }

    @Test
    fun `Disposes upstream once read when less than max values`() = runTest {
        val values = List(SuggestedValueHelperImpl.MAX_VALUES / 2) { it.toDouble() }
        val dataPoints = dpFromValues(values)

        var disposeCalled = false

        whenever(dataInteractor.getDataSampleForFeatureId(any())).thenReturn(
            DataSample.fromSequence(
                data = dataPoints,
                onDispose = { disposeCalled = true }
            )
        )

        uut.getSuggestedValues(testTracker).collect {}

        assertTrue(disposeCalled)
    }
}