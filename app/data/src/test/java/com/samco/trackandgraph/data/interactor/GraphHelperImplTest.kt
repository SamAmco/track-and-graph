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

import com.samco.trackandgraph.FakeGraphDao
import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.dto.GraphEndDate
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.LineGraphConfig
import com.samco.trackandgraph.data.database.dto.LineGraphCreateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphFeatureConfig
import com.samco.trackandgraph.data.database.dto.LineGraphUpdateRequest
import com.samco.trackandgraph.data.database.dto.GraphDeleteRequest
import com.samco.trackandgraph.data.database.dto.PieChartConfig
import com.samco.trackandgraph.data.database.dto.PieChartCreateRequest
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.DurationPlottingMode
import com.samco.trackandgraph.data.database.dto.LineGraphAveragingModes
import com.samco.trackandgraph.data.database.dto.LineGraphPlottingModes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GraphHelperImplTest {

    private lateinit var fakeGraphDao: FakeGraphDao
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private lateinit var uut: GraphHelperImpl

    @Before
    fun before() {
        fakeGraphDao = FakeGraphDao()

        val transactionHelper = object : DatabaseTransactionHelper {
            override suspend fun <R> withTransaction(block: suspend () -> R): R = block()
        }

        uut = GraphHelperImpl(
            transactionHelper = transactionHelper,
            graphDao = fakeGraphDao,
            io = dispatcher
        )
    }

    // =========================================================================
    // Create tests
    // =========================================================================

    @Test
    fun `createLineGraph inserts GraphOrStat and LineGraph and returns graphStatId`() =
        runTest(dispatcher) {
            // PREPARE
            val request = LineGraphCreateRequest(
                name = "Test Line Graph",
                groupId = 1L,
                config = LineGraphConfig(
                    features = emptyList(),
                    sampleSize = null,
                    yRangeType = YRangeType.DYNAMIC,
                    yFrom = 0.0,
                    yTo = 100.0,
                    endDate = GraphEndDate.Latest
                )
            )

            // EXECUTE
            val graphStatId = uut.createLineGraph(request)

            // VERIFY
            val graphOrStat = fakeGraphDao.getGraphStatById(graphStatId)
            assertEquals("Test Line Graph", graphOrStat.name)
            assertEquals(1L, graphOrStat.groupId)
            assertEquals(GraphStatType.LINE_GRAPH, graphOrStat.type)

            val lineGraph = fakeGraphDao.getLineGraphByGraphStatId(graphStatId)
            assertNotNull(lineGraph)
            assertEquals(YRangeType.DYNAMIC, lineGraph!!.yRangeType)
            assertEquals(0.0, lineGraph.yFrom, 0.001)
            assertEquals(100.0, lineGraph.yTo, 0.001)
        }

    @Test
    fun `createLineGraph with features stores features correctly`() =
        runTest(dispatcher) {
            // PREPARE
            val request = LineGraphCreateRequest(
                name = "Graph With Features",
                groupId = 1L,
                config = LineGraphConfig(
                    features = listOf(
                        LineGraphFeatureConfig(
                            featureId = 10L,
                            name = "Feature 1",
                            colorIndex = 0,
                            averagingMode = LineGraphAveragingModes.NO_AVERAGING,
                            plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                            pointStyle = LineGraphPointStyle.CIRCLES_AND_NUMBERS,
                            offset = 0.0,
                            scale = 1.0,
                            durationPlottingMode = DurationPlottingMode.HOURS
                        ),
                        LineGraphFeatureConfig(
                            featureId = 20L,
                            name = "Feature 2",
                            colorIndex = 1,
                            averagingMode = LineGraphAveragingModes.DAILY_MOVING_AVERAGE,
                            plottingMode = LineGraphPlottingModes.GENERATE_HOURLY_TOTALS,
                            pointStyle = LineGraphPointStyle.NONE,
                            offset = 5.0,
                            scale = 2.0,
                            durationPlottingMode = DurationPlottingMode.MINUTES
                        )
                    ),
                    sampleSize = null,
                    yRangeType = YRangeType.FIXED,
                    yFrom = 0.0,
                    yTo = 50.0,
                    endDate = GraphEndDate.Latest
                )
            )

            // EXECUTE
            val graphStatId = uut.createLineGraph(request)

            // VERIFY
            val lineGraph = fakeGraphDao.getLineGraphByGraphStatId(graphStatId)
            assertNotNull(lineGraph)
            assertEquals(2, lineGraph!!.features.size)

            assertEquals("Feature 1", lineGraph.features[0].name)
            assertEquals(10L, lineGraph.features[0].featureId)
            assertEquals(0, lineGraph.features[0].colorIndex)
            assertEquals(LineGraphAveragingModes.NO_AVERAGING, lineGraph.features[0].averagingMode)
            assertEquals(LineGraphPlottingModes.WHEN_TRACKED, lineGraph.features[0].plottingMode)
            assertEquals(LineGraphPointStyle.CIRCLES_AND_NUMBERS, lineGraph.features[0].pointStyle)
            assertEquals(0.0, lineGraph.features[0].offset, 0.001)
            assertEquals(1.0, lineGraph.features[0].scale, 0.001)
            assertEquals(DurationPlottingMode.HOURS, lineGraph.features[0].durationPlottingMode)

            assertEquals("Feature 2", lineGraph.features[1].name)
            assertEquals(20L, lineGraph.features[1].featureId)
            assertEquals(
                LineGraphAveragingModes.DAILY_MOVING_AVERAGE,
                lineGraph.features[1].averagingMode
            )
            assertEquals(
                LineGraphPlottingModes.GENERATE_HOURLY_TOTALS,
                lineGraph.features[1].plottingMode
            )
            assertEquals(LineGraphPointStyle.NONE, lineGraph.features[1].pointStyle)
            assertEquals(5.0, lineGraph.features[1].offset, 0.001)
            assertEquals(2.0, lineGraph.features[1].scale, 0.001)
            assertEquals(DurationPlottingMode.MINUTES, lineGraph.features[1].durationPlottingMode)
        }

    @Test
    fun `createPieChart inserts GraphOrStat and PieChart and returns graphStatId`() =
        runTest(dispatcher) {
            // PREPARE
            val request = PieChartCreateRequest(
                name = "Test Pie Chart",
                groupId = 2L,
                config = PieChartConfig(
                    featureId = 10L,
                    sampleSize = null,
                    endDate = GraphEndDate.Latest,
                    sumByCount = true
                )
            )

            // EXECUTE
            val graphStatId = uut.createPieChart(request)

            // VERIFY
            val graphOrStat = fakeGraphDao.getGraphStatById(graphStatId)
            assertEquals("Test Pie Chart", graphOrStat.name)
            assertEquals(2L, graphOrStat.groupId)
            assertEquals(GraphStatType.PIE_CHART, graphOrStat.type)

            val pieChart = fakeGraphDao.getPieChartByGraphStatId(graphStatId)
            assertNotNull(pieChart)
            assertEquals(10L, pieChart!!.featureId)
            assertTrue(pieChart.sumByCount)
            assertEquals(GraphEndDate.Latest, pieChart.endDate)
        }

    // =========================================================================
    // Update tests
    // =========================================================================

    @Test
    fun `updateLineGraph updates name when provided`() = runTest(dispatcher) {
        // PREPARE
        val graphStatId = uut.createLineGraph(
            LineGraphCreateRequest(
                name = "Original Name",
                groupId = 1L,
                config = LineGraphConfig(
                    features = emptyList(),
                    sampleSize = null,
                    yRangeType = YRangeType.DYNAMIC,
                    yFrom = 0.0,
                    yTo = 100.0,
                    endDate = GraphEndDate.Latest
                )
            )
        )

        // EXECUTE
        uut.updateLineGraph(
            LineGraphUpdateRequest(
                graphStatId = graphStatId,
                name = "Updated Name",
                config = null
            )
        )

        // VERIFY
        val graphOrStat = fakeGraphDao.getGraphStatById(graphStatId)
        assertEquals("Updated Name", graphOrStat.name)

        // Config should remain unchanged
        val lineGraph = fakeGraphDao.getLineGraphByGraphStatId(graphStatId)
        assertEquals(YRangeType.DYNAMIC, lineGraph!!.yRangeType)
        assertEquals(0.0, lineGraph.yFrom, 0.001)
        assertEquals(100.0, lineGraph.yTo, 0.001)
        assertEquals(null, lineGraph.sampleSize)
        assertEquals(GraphEndDate.Latest, lineGraph.endDate)
    }

    @Test
    fun `updateLineGraph updates config when provided`() = runTest(dispatcher) {
        // PREPARE
        val graphStatId = uut.createLineGraph(
            LineGraphCreateRequest(
                name = "Test Graph",
                groupId = 1L,
                config = LineGraphConfig(
                    features = listOf(
                        LineGraphFeatureConfig(
                            featureId = 10L,
                            name = "Old Feature",
                            colorIndex = 0,
                            averagingMode = LineGraphAveragingModes.NO_AVERAGING,
                            plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                            pointStyle = LineGraphPointStyle.CIRCLES_AND_NUMBERS,
                            offset = 0.0,
                            scale = 1.0,
                            durationPlottingMode = DurationPlottingMode.HOURS
                        )
                    ),
                    sampleSize = null,
                    yRangeType = YRangeType.DYNAMIC,
                    yFrom = 0.0,
                    yTo = 100.0,
                    endDate = GraphEndDate.Latest
                )
            )
        )

        // EXECUTE
        uut.updateLineGraph(
            LineGraphUpdateRequest(
                graphStatId = graphStatId,
                name = null,
                config = LineGraphConfig(
                    features = listOf(
                        LineGraphFeatureConfig(
                            featureId = 20L,
                            name = "New Feature",
                            colorIndex = 1,
                            averagingMode = LineGraphAveragingModes.DAILY_MOVING_AVERAGE,
                            plottingMode = LineGraphPlottingModes.GENERATE_HOURLY_TOTALS,
                            pointStyle = LineGraphPointStyle.NONE,
                            offset = 0.0,
                            scale = 1.0,
                            durationPlottingMode = DurationPlottingMode.HOURS
                        )
                    ),
                    sampleSize = null,
                    yRangeType = YRangeType.FIXED,
                    yFrom = 10.0,
                    yTo = 200.0,
                    endDate = GraphEndDate.Latest
                )
            )
        )

        // VERIFY
        val graphOrStat = fakeGraphDao.getGraphStatById(graphStatId)
        assertEquals("Test Graph", graphOrStat.name) // Name unchanged

        val lineGraph = fakeGraphDao.getLineGraphByGraphStatId(graphStatId)
        assertEquals(YRangeType.FIXED, lineGraph!!.yRangeType)
        assertEquals(10.0, lineGraph.yFrom, 0.001)
        assertEquals(200.0, lineGraph.yTo, 0.001)
        assertEquals(1, lineGraph.features.size)
        assertEquals("New Feature", lineGraph.features[0].name)
        assertEquals(20L, lineGraph.features[0].featureId)
        assertEquals(1, lineGraph.features[0].colorIndex)
        assertEquals(
            LineGraphAveragingModes.DAILY_MOVING_AVERAGE,
            lineGraph.features[0].averagingMode
        )
        assertEquals(
            LineGraphPlottingModes.GENERATE_HOURLY_TOTALS,
            lineGraph.features[0].plottingMode
        )
        assertEquals(LineGraphPointStyle.NONE, lineGraph.features[0].pointStyle)
        assertEquals(0.0, lineGraph.features[0].offset, 0.001)
        assertEquals(1.0, lineGraph.features[0].scale, 0.001)
        assertEquals(DurationPlottingMode.HOURS, lineGraph.features[0].durationPlottingMode)
    }

    // =========================================================================
    // Delete tests
    // =========================================================================

    @Test
    fun `deleteGraph removes graph from dao`() = runTest(dispatcher) {
        // PREPARE
        val graphStatId = uut.createLineGraph(
            LineGraphCreateRequest(
                name = "To Be Deleted",
                groupId = 1L,
                config = LineGraphConfig(
                    features = emptyList(),
                    sampleSize = null,
                    yRangeType = YRangeType.DYNAMIC,
                    yFrom = 0.0,
                    yTo = 100.0,
                    endDate = GraphEndDate.Latest
                )
            )
        )

        // Verify it exists
        assertNotNull(fakeGraphDao.tryGetGraphStatById(graphStatId))

        // EXECUTE
        uut.deleteGraph(GraphDeleteRequest(graphStatId = graphStatId))

        // VERIFY
        assertNull(fakeGraphDao.tryGetGraphStatById(graphStatId))
    }

    // =========================================================================
    // Duplicate tests
    // =========================================================================

    @Test
    fun `duplicateLineGraph creates copy with new graphStatId`() = runTest(dispatcher) {
        // PREPARE
        val originalGraphStatId = uut.createLineGraph(
            LineGraphCreateRequest(
                name = "Original Graph",
                groupId = 1L,
                config = LineGraphConfig(
                    features = listOf(
                        LineGraphFeatureConfig(
                            featureId = 10L,
                            name = "Feature",
                            colorIndex = 0,
                            averagingMode = LineGraphAveragingModes.NO_AVERAGING,
                            plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                            pointStyle = LineGraphPointStyle.CIRCLES_AND_NUMBERS,
                            offset = 0.0,
                            scale = 1.0,
                            durationPlottingMode = DurationPlottingMode.HOURS
                        )
                    ),
                    sampleSize = null,
                    yRangeType = YRangeType.FIXED,
                    yFrom = 5.0,
                    yTo = 50.0,
                    endDate = GraphEndDate.Latest
                )
            )
        )

        // EXECUTE
        val duplicatedGraphStatId = uut.duplicateLineGraph(originalGraphStatId)

        // VERIFY
        assertNotNull(duplicatedGraphStatId)
        assertTrue(duplicatedGraphStatId != originalGraphStatId)

        val original = fakeGraphDao.getGraphStatById(originalGraphStatId)
        val duplicate = fakeGraphDao.getGraphStatById(duplicatedGraphStatId!!)

        assertEquals(original.name, duplicate.name)
        assertEquals(original.groupId, duplicate.groupId)
        assertEquals(original.type, duplicate.type)

        val originalLineGraph = fakeGraphDao.getLineGraphByGraphStatId(originalGraphStatId)
        val duplicateLineGraph = fakeGraphDao.getLineGraphByGraphStatId(duplicatedGraphStatId)

        assertEquals(
            originalLineGraph!!.copy(
                id = 0L, graphStatId = 0L,
                features = originalLineGraph.features.map { it.copy(id = 0L, lineGraphId = 0L) }
            ),
            duplicateLineGraph!!.copy(
                id = 0L, graphStatId = 0L,
                features = duplicateLineGraph.features.map { it.copy(id = 0L, lineGraphId = 0L) }
            ),
        )
        assertNotEquals(originalLineGraph.id, duplicateLineGraph.id)
        assertNotEquals(originalLineGraph.graphStatId, duplicateLineGraph.graphStatId)
    }

    @Test
    fun `duplicateLineGraph returns null when graph does not exist`() = runTest(dispatcher) {
        // EXECUTE
        val result = uut.duplicateLineGraph(999L)

        // VERIFY
        assertNull(result)
    }

    // =========================================================================
    // Get tests
    // =========================================================================

    @Test
    fun `getGraphStatById returns dto from dao`() = runTest(dispatcher) {
        // PREPARE
        val graphStatId = uut.createLineGraph(
            LineGraphCreateRequest(
                name = "Test Graph",
                groupId = 1L,
                config = LineGraphConfig(
                    features = emptyList(),
                    sampleSize = null,
                    yRangeType = YRangeType.DYNAMIC,
                    yFrom = 0.0,
                    yTo = 100.0,
                    endDate = GraphEndDate.Latest
                )
            )
        )

        // EXECUTE
        val result = uut.getGraphStatById(graphStatId)

        // VERIFY
        assertEquals(graphStatId, result.id)
        assertEquals("Test Graph", result.name)
        assertEquals(GraphStatType.LINE_GRAPH, result.type)
    }

    @Test
    fun `tryGetGraphStatById returns null when not found`() = runTest(dispatcher) {
        // EXECUTE
        val result = uut.tryGetGraphStatById(999L)

        // VERIFY
        assertNull(result)
    }

    @Test
    fun `hasAnyGraphs returns false when empty`() = runTest(dispatcher) {
        // EXECUTE & VERIFY
        assertEquals(false, uut.hasAnyGraphs())
    }

    @Test
    fun `hasAnyGraphs returns true when graphs exist`() = runTest(dispatcher) {
        // PREPARE
        uut.createLineGraph(
            LineGraphCreateRequest(
                name = "Test",
                groupId = 1L,
                config = LineGraphConfig(
                    features = emptyList(),
                    sampleSize = null,
                    yRangeType = YRangeType.DYNAMIC,
                    yFrom = 0.0,
                    yTo = 100.0,
                    endDate = GraphEndDate.Latest
                )
            )
        )

        // EXECUTE & VERIFY
        assertEquals(true, uut.hasAnyGraphs())
    }

    @Test
    fun `getAllGraphStatsSync returns all graphs`() = runTest(dispatcher) {
        // PREPARE
        uut.createLineGraph(
            LineGraphCreateRequest(
                name = "Graph 1",
                groupId = 1L,
                config = LineGraphConfig(
                    features = emptyList(),
                    sampleSize = null,
                    yRangeType = YRangeType.DYNAMIC,
                    yFrom = 0.0,
                    yTo = 100.0,
                    endDate = GraphEndDate.Latest
                )
            )
        )
        uut.createPieChart(
            PieChartCreateRequest(
                name = "Graph 2",
                groupId = 1L,
                config = PieChartConfig(
                    featureId = 10L,
                    sampleSize = null,
                    endDate = GraphEndDate.Latest,
                    sumByCount = false
                )
            )
        )

        // EXECUTE
        val result = uut.getAllGraphStatsSync()

        // VERIFY
        assertEquals(2, result.size)
    }
}
