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
import com.samco.trackandgraph.FakeGroupItemDao
import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.dto.GraphEndDate
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.LineGraphConfig
import com.samco.trackandgraph.data.database.dto.LineGraphCreateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphFeatureConfig
import com.samco.trackandgraph.data.database.dto.LineGraphUpdateRequest
import com.samco.trackandgraph.data.database.dto.ComponentDeleteRequest
import com.samco.trackandgraph.data.database.dto.PieChartConfig
import com.samco.trackandgraph.data.database.dto.PieChartCreateRequest
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.DurationPlottingMode
import com.samco.trackandgraph.data.database.dto.LineGraphAveragingModes
import com.samco.trackandgraph.data.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
import com.samco.trackandgraph.data.time.TimeProvider
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
    private lateinit var fakeGroupItemDao: FakeGroupItemDao
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private lateinit var uut: GraphHelperImpl

    @Before
    fun before() {
        fakeGraphDao = FakeGraphDao()
        fakeGroupItemDao = FakeGroupItemDao()

        val transactionHelper = object : DatabaseTransactionHelper {
            override suspend fun <R> withTransaction(block: suspend () -> R): R = block()
        }

        val timeProvider = object : TimeProvider {
            override fun now(): org.threeten.bp.ZonedDateTime = org.threeten.bp.ZonedDateTime.now()
            override fun epochMilli(): Long = 1000L
            override fun defaultZone(): org.threeten.bp.ZoneId = org.threeten.bp.ZoneId.systemDefault()
        }

        uut = GraphHelperImpl(
            transactionHelper = transactionHelper,
            graphDao = fakeGraphDao,
            groupItemDao = fakeGroupItemDao,
            timeProvider = timeProvider,
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
            val graphStatId = uut.createLineGraph(request).componentId

            // VERIFY
            val graphOrStat = fakeGraphDao.getGraphStatById(graphStatId)
            assertEquals("Test Line Graph", graphOrStat.name)
            assertEquals(GraphStatType.LINE_GRAPH, graphOrStat.type)
            val groupItem = fakeGroupItemDao.getGroupItemsForChild(graphStatId, GroupItemType.GRAPH)
                .firstOrNull { it.groupId == 1L }
            assertNotNull(groupItem)

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
            val graphStatId = uut.createLineGraph(request).componentId

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
            val graphStatId = uut.createPieChart(request).componentId

            // VERIFY
            val graphOrStat = fakeGraphDao.getGraphStatById(graphStatId)
            assertEquals("Test Pie Chart", graphOrStat.name)
            assertEquals(GraphStatType.PIE_CHART, graphOrStat.type)
            val groupItem = fakeGroupItemDao.getGroupItemsForChild(graphStatId, GroupItemType.GRAPH)
                .firstOrNull { it.groupId == 2L }
            assertNotNull(groupItem)

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
        ).componentId

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
        ).componentId

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
        ).componentId

        // Verify it exists
        assertNotNull(fakeGraphDao.tryGetGraphStatById(graphStatId))

        // EXECUTE
        val groupItemId = fakeGroupItemDao.getGroupItemsForChild(graphStatId, GroupItemType.GRAPH)
            .first().id
        uut.deleteGraph(ComponentDeleteRequest(groupItemId = groupItemId))

        // VERIFY
        assertNull(fakeGraphDao.tryGetGraphStatById(graphStatId))
    }

    @Test
    fun `deleteGraph removes only symlink when deleteEverywhere is false and in multiple groups`() =
        runTest(dispatcher) {
            // PREPARE
            val graphStatId = uut.createLineGraph(
                LineGraphCreateRequest(
                    name = "Multi Group Graph",
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
            ).componentId
            // Add symlink to group 2
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = 2L,
                    displayIndex = 0,
                    childId = graphStatId,
                    type = GroupItemType.GRAPH,
                    createdAt = 1000L
                )
            )
            val group2ItemId = fakeGroupItemDao.getGroupItemsForChild(graphStatId, GroupItemType.GRAPH)
                .first { it.groupId == 2L }.id

            // EXECUTE
            uut.deleteGraph(
                ComponentDeleteRequest(
                    groupItemId = group2ItemId,
                    deleteEverywhere = false
                )
            )

            // VERIFY
            assertNotNull(fakeGraphDao.tryGetGraphStatById(graphStatId))
            val group1Items = fakeGroupItemDao.getGroupItemsForChild(graphStatId, GroupItemType.GRAPH)
                .filter { it.groupId == 1L }
            assertEquals(1, group1Items.size)
            val group2Items = fakeGroupItemDao.getGroupItemsForChild(graphStatId, GroupItemType.GRAPH)
                .filter { it.groupId == 2L }
            assertEquals(0, group2Items.size)
        }

    @Test
    fun `deleteGraph deletes graph entirely when deleteEverywhere is false but only one group`() =
        runTest(dispatcher) {
            // PREPARE
            val graphStatId = uut.createLineGraph(
                LineGraphCreateRequest(
                    name = "Single Group Graph",
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
            ).componentId
            val groupItemId = fakeGroupItemDao.getGroupItemsForChild(graphStatId, GroupItemType.GRAPH)
                .first().id

            // EXECUTE
            uut.deleteGraph(
                ComponentDeleteRequest(
                    groupItemId = groupItemId,
                    deleteEverywhere = false
                )
            )

            // VERIFY
            assertNull(fakeGraphDao.tryGetGraphStatById(graphStatId))
            val remainingItems = fakeGroupItemDao.getGroupItemsForChild(graphStatId, GroupItemType.GRAPH)
            assertEquals(0, remainingItems.size)
        }

    @Test
    fun `deleteGraph with deleteEverywhere removes graph and all symlinks`() =
        runTest(dispatcher) {
            // PREPARE
            val graphStatId = uut.createLineGraph(
                LineGraphCreateRequest(
                    name = "Everywhere Delete Graph",
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
            ).componentId
            // Add symlink to group 2
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = 2L,
                    displayIndex = 0,
                    childId = graphStatId,
                    type = GroupItemType.GRAPH,
                    createdAt = 1000L
                )
            )
            val groupItemId = fakeGroupItemDao.getGroupItemsForChild(graphStatId, GroupItemType.GRAPH)
                .first().id

            // EXECUTE
            uut.deleteGraph(
                ComponentDeleteRequest(
                    groupItemId = groupItemId,
                    deleteEverywhere = true
                )
            )

            // VERIFY
            assertNull(fakeGraphDao.tryGetGraphStatById(graphStatId))
            val remainingItems = fakeGroupItemDao.getGroupItemsForChild(graphStatId, GroupItemType.GRAPH)
            assertEquals(0, remainingItems.size)
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
        ).componentId

        // EXECUTE
        val originalGroupItemId = fakeGroupItemDao.getGroupItemsForChild(originalGraphStatId, GroupItemType.GRAPH)
            .first { it.groupId == 1L }.id
        val duplicateResult = uut.duplicateGraphOrStat(originalGroupItemId)

        // VERIFY
        assertNotNull(duplicateResult)
        val duplicatedGraphStatId = duplicateResult!!.componentId
        assertTrue(duplicatedGraphStatId != originalGraphStatId)

        val original = fakeGraphDao.getGraphStatById(originalGraphStatId)
        val duplicate = fakeGraphDao.getGraphStatById(duplicatedGraphStatId)

        assertEquals(original.name, duplicate.name)
        assertEquals(original.type, duplicate.type)
        // Both should be in the same group (groupId 1L)
        val originalGroupItem = fakeGroupItemDao.getGroupItemsForChild(originalGraphStatId, GroupItemType.GRAPH)
            .firstOrNull { it.groupId == 1L }
        val duplicateGroupItem = fakeGroupItemDao.getGroupItemsForChild(duplicatedGraphStatId, GroupItemType.GRAPH)
            .firstOrNull { it.groupId == 1L }
        assertNotNull(originalGroupItem)
        assertNotNull(duplicateGroupItem)

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
        // EXECUTE — pass a groupItemId that does not exist in the fake dao
        val result = uut.duplicateGraphOrStat(999L)

        // VERIFY
        assertNull(result)
    }

    @Test
    fun `duplicatePieChart creates copy with matching config`() = runTest(dispatcher) {
        // PREPARE
        val originalGraphStatId = uut.createPieChart(
            PieChartCreateRequest(
                name = "Original Pie",
                groupId = 2L,
                config = PieChartConfig(
                    featureId = 10L,
                    sampleSize = null,
                    endDate = GraphEndDate.Latest,
                    sumByCount = true
                )
            )
        ).componentId
        val groupItemId = fakeGroupItemDao.getGroupItemsForChild(originalGraphStatId, GroupItemType.GRAPH)
            .first().id

        // EXECUTE
        val duplicateResult = uut.duplicateGraphOrStat(groupItemId)

        // VERIFY
        assertNotNull(duplicateResult)
        val duplicatedGraphStatId = duplicateResult!!.componentId
        assertNotEquals(originalGraphStatId, duplicatedGraphStatId)

        val originalPie = fakeGraphDao.getPieChartByGraphStatId(originalGraphStatId)
        val duplicatePie = fakeGraphDao.getPieChartByGraphStatId(duplicatedGraphStatId)
        assertNotNull(originalPie)
        assertNotNull(duplicatePie)
        assertEquals(originalPie!!.featureId, duplicatePie!!.featureId)
        assertEquals(originalPie.sumByCount, duplicatePie.sumByCount)

        val duplicateGroupItem = fakeGroupItemDao.getGroupItemsForChild(duplicatedGraphStatId, GroupItemType.GRAPH)
            .firstOrNull { it.groupId == 2L }
        assertNotNull(duplicateGroupItem)
    }

    @Test
    fun `duplicateGraphOrStat places copy immediately after original in display order`() =
        runTest(dispatcher) {
            // PREPARE
            val originalGraphStatId = uut.createLineGraph(
                LineGraphCreateRequest(
                    name = "Original Graph",
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
            ).componentId
            val originalGroupItem = fakeGroupItemDao.getGroupItemsForChild(originalGraphStatId, GroupItemType.GRAPH)
                .first { it.groupId == 1L }

            // EXECUTE
            val duplicateResult = uut.duplicateGraphOrStat(originalGroupItem.id)

            // VERIFY
            assertNotNull(duplicateResult)
            val duplicateGroupItem = fakeGroupItemDao.getGroupItemsForChild(duplicateResult!!.componentId, GroupItemType.GRAPH)
                .first { it.groupId == 1L }
            assertEquals(originalGroupItem.displayIndex + 1, duplicateGroupItem.displayIndex)
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
        ).componentId

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

    // =========================================================================
    // Uniqueness tests
    // =========================================================================

    @Test
    fun `getGraphsAndStatsByGroupIdSync returns unique=true when graph in only one group`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = 1L
            val graphStatId = uut.createLineGraph(
                LineGraphCreateRequest(
                    name = "Single Group Graph",
                    groupId = groupId,
                    config = LineGraphConfig(
                        features = emptyList(),
                        sampleSize = null,
                        yRangeType = YRangeType.DYNAMIC,
                        yFrom = 0.0,
                        yTo = 100.0,
                        endDate = GraphEndDate.Latest
                    )
                )
            ).componentId

            // EXECUTE
            val result = uut.getGraphsAndStatsByGroupIdSync(groupId)

            // VERIFY
            assertEquals(1, result.size)
            assertEquals(graphStatId, result[0].id)
            assertEquals(true, result[0].unique)
        }

    @Test
    fun `getGraphsAndStatsByGroupIdSync returns unique=false when graph in multiple groups`() =
        runTest(dispatcher) {
            // PREPARE
            val group1 = 1L
            val group2 = 2L
            val graphStatId = uut.createLineGraph(
                LineGraphCreateRequest(
                    name = "Shared Graph",
                    groupId = group1,
                    config = LineGraphConfig(
                        features = emptyList(),
                        sampleSize = null,
                        yRangeType = YRangeType.DYNAMIC,
                        yFrom = 0.0,
                        yTo = 100.0,
                        endDate = GraphEndDate.Latest
                    )
                )
            ).componentId
            // Add symlink to group2
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = graphStatId,
                    type = GroupItemType.GRAPH,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            val result = uut.getGraphsAndStatsByGroupIdSync(group1)

            // VERIFY
            assertEquals(1, result.size)
            assertEquals(graphStatId, result[0].id)
            assertEquals(false, result[0].unique)
        }

    @Test
    fun `getGraphsAndStatsByGroupIdSync sets unique independently per graph`() =
        runTest(dispatcher) {
            // PREPARE - one unique graph, one non-unique graph in the same group
            val group1 = 1L
            val group2 = 2L

            val lineGraphConfig = LineGraphConfig(
                features = emptyList(),
                sampleSize = null,
                yRangeType = YRangeType.DYNAMIC,
                yFrom = 0.0,
                yTo = 100.0,
                endDate = GraphEndDate.Latest
            )
            val uniqueGraphId = uut.createLineGraph(
                LineGraphCreateRequest(name = "Unique Graph", groupId = group1, config = lineGraphConfig)
            ).componentId
            val sharedGraphId = uut.createLineGraph(
                LineGraphCreateRequest(name = "Shared Graph", groupId = group1, config = lineGraphConfig)
            ).componentId
            // Add symlink for sharedGraph to group2
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = sharedGraphId,
                    type = GroupItemType.GRAPH,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            val result = uut.getGraphsAndStatsByGroupIdSync(group1)

            // VERIFY
            assertEquals(2, result.size)
            val uniqueResult = result.first { it.id == uniqueGraphId }
            val sharedResult = result.first { it.id == sharedGraphId }
            assertEquals(true, uniqueResult.unique)
            assertEquals(false, sharedResult.unique)
        }

    @Test
    fun `getGraphStatById returns unique=true when graph in only one group`() = runTest(dispatcher) {
        val lineGraphConfig = LineGraphConfig(features = emptyList(), sampleSize = null, yRangeType = YRangeType.DYNAMIC, yFrom = 0.0, yTo = 100.0, endDate = GraphEndDate.Latest)
        val graphId = uut.createLineGraph(LineGraphCreateRequest(name = "Test", groupId = 1L, config = lineGraphConfig)).componentId
        val result = uut.getGraphStatById(graphId)
        assertEquals(true, result.unique)
    }

    @Test
    fun `getGraphStatById returns unique=false when graph in multiple groups`() = runTest(dispatcher) {
        val lineGraphConfig = LineGraphConfig(features = emptyList(), sampleSize = null, yRangeType = YRangeType.DYNAMIC, yFrom = 0.0, yTo = 100.0, endDate = GraphEndDate.Latest)
        val graphId = uut.createLineGraph(LineGraphCreateRequest(name = "Test", groupId = 1L, config = lineGraphConfig)).componentId
        fakeGroupItemDao.insertGroupItem(GroupItem(groupId = 2L, displayIndex = 0, childId = graphId, type = GroupItemType.GRAPH, createdAt = 1000L))
        val result = uut.getGraphStatById(graphId)
        assertEquals(false, result.unique)
    }

    @Test
    fun `tryGetGraphStatById returns unique=true when graph in only one group`() = runTest(dispatcher) {
        val lineGraphConfig = LineGraphConfig(features = emptyList(), sampleSize = null, yRangeType = YRangeType.DYNAMIC, yFrom = 0.0, yTo = 100.0, endDate = GraphEndDate.Latest)
        val graphId = uut.createLineGraph(LineGraphCreateRequest(name = "Test", groupId = 1L, config = lineGraphConfig)).componentId
        val result = uut.tryGetGraphStatById(graphId)
        assertNotNull(result)
        assertEquals(true, result!!.unique)
    }

    @Test
    fun `tryGetGraphStatById returns unique=false when graph in multiple groups`() = runTest(dispatcher) {
        val lineGraphConfig = LineGraphConfig(features = emptyList(), sampleSize = null, yRangeType = YRangeType.DYNAMIC, yFrom = 0.0, yTo = 100.0, endDate = GraphEndDate.Latest)
        val graphId = uut.createLineGraph(LineGraphCreateRequest(name = "Test", groupId = 1L, config = lineGraphConfig)).componentId
        fakeGroupItemDao.insertGroupItem(GroupItem(groupId = 2L, displayIndex = 0, childId = graphId, type = GroupItemType.GRAPH, createdAt = 1000L))
        val result = uut.tryGetGraphStatById(graphId)
        assertNotNull(result)
        assertEquals(false, result!!.unique)
    }
}
