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

package com.samco.trackandgraph.group

import app.cash.turbine.test
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem
import com.samco.trackandgraph.data.interactor.DataUpdateType
import com.samco.trackandgraph.fixtures.TestGraphViewData
import com.samco.trackandgraph.fixtures.testDisplayTracker
import com.samco.trackandgraph.fixtures.testFunction
import com.samco.trackandgraph.fixtures.testGraphOrStat
import com.samco.trackandgraph.fixtures.testGraphViewData
import com.samco.trackandgraph.fixtures.testTracker
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchResultProcessorTest {

    @Test
    fun `empty input emits empty result list`() = runTest {
        val fixture = fixture()

        fixture.processor.process(emptyList()).test {
            assertEquals(emptyList<SearchResultItem>(), awaitItem())
            awaitComplete()
        }

        assertEquals(emptyList<Long>(), fixture.trackerCalls)
        assertEquals(emptyList<Long>(), fixture.graphCalls)
    }

    @Test
    fun `initial emission preserves ranked order and uses placeholders for uncached trackers and graphs`() =
        runTest {
            val fixture = fixture()
            val items = listOf(
                rankedGroup(groupItemId = 10L, groupId = 1L),
                rankedGraph(groupItemId = 11L, graphId = 2L),
                rankedTracker(groupItemId = 12L, trackerId = 3L, featureId = 30L),
                rankedFunction(groupItemId = 13L, functionId = 4L, featureId = 40L),
            )

            fixture.processor.process(items).test {
                val initial = awaitItem()

                assertEquals(listOf(10L, 11L, 12L, 13L), initial.map { it.child.groupItemId })
                assertTrue(initial[0].child is GroupChild.ChildGroup)
                assertGraphState(initial[1], IGraphStatViewData.State.LOADING)
                assertTrue(initial[2].child is GroupChild.ChildTrackerLoading)
                assertTrue(initial[3].child is GroupChild.ChildFunction)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `trackers in a batch are emitted before graph results from the same batch`() = runTest {
        val fixture = fixture()
        val graphResult = CompletableDeferred<IGraphStatViewData>()
        fixture.trackers[30L] = testDisplayTracker(id = 3L, featureId = 30L, name = "ready tracker")
        fixture.graphHandler = { graph, _ -> graphResult.await().also { assertEquals(graph.id, it.graphOrStat.id) } }

        val items = listOf(
            rankedTracker(groupItemId = 10L, trackerId = 3L, featureId = 30L),
            rankedGraph(groupItemId = 11L, graphId = 2L),
        )

        fixture.processor.process(items).test {
            val initial = awaitItem()
            assertTrue(initial[0].child is GroupChild.ChildTrackerLoading)
            assertGraphState(initial[1], IGraphStatViewData.State.LOADING)

            val trackerEmission = awaitItem()
            assertTrackerName(trackerEmission[0], "ready tracker")
            assertGraphState(trackerEmission[1], IGraphStatViewData.State.LOADING)

            graphResult.complete(readyGraph(graph = testGraphOrStat(2L), label = "ready graph"))
            val graphEmission = awaitItem()
            assertTrackerName(graphEmission[0], "ready tracker")
            assertGraphLabel(graphEmission[1], "ready graph")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `batches are processed sequentially so later batch trackers wait for earlier batch graphs`() = runTest {
        val fixture = fixture()
        val batchZeroGraph = testGraphOrStat(1L)
        val batchZeroGraphResult = CompletableDeferred<IGraphStatViewData>()
        fixture.trackers[130L] = testDisplayTracker(id = 13L, featureId = 130L, name = "batch 1 tracker")
        fixture.graphHandler = { graph, _ ->
            if (graph.id == batchZeroGraph.id) batchZeroGraphResult.await()
            else readyGraph(graph, label = "graph ${graph.id}")
        }

        val batchZero = listOf(rankedGraph(groupItemId = 1L, graphId = batchZeroGraph.id)) +
            (2L..12L).map { rankedGroup(groupItemId = it, groupId = it) }
        val batchOneTracker = rankedTracker(groupItemId = 13L, trackerId = 13L, featureId = 130L)

        fixture.processor.process(batchZero + batchOneTracker).test {
            val initial = awaitItem()
            assertGraphState(initial[0], IGraphStatViewData.State.LOADING)
            assertTrue(initial[12].child is GroupChild.ChildTrackerLoading)

            advanceUntilIdle()
            assertEquals(emptyList<Long>(), fixture.trackerCalls)

            batchZeroGraphResult.complete(readyGraph(batchZeroGraph, label = "batch 0 graph"))

            val batchZeroGraphEmission = awaitItem()
            assertGraphLabel(batchZeroGraphEmission[0], "batch 0 graph")
            assertTrue(batchZeroGraphEmission[12].child is GroupChild.ChildTrackerLoading)

            val batchOneTrackerEmission = awaitItem()
            assertTrackerName(batchOneTrackerEmission[12], "batch 1 tracker")
            assertEquals(listOf(130L), fixture.trackerCalls)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `graph calculation exception replaces loading placeholder with error view data`() = runTest {
        val fixture = fixture()
        val error = IllegalStateException("broken graph")
        fixture.graphHandler = { _, _ -> throw error }

        fixture.processor.process(listOf(rankedGraph(groupItemId = 10L, graphId = 2L))).test {
            assertGraphState(awaitItem().single(), IGraphStatViewData.State.LOADING)

            val errorEmission = awaitItem().single()
            assertGraphState(errorEmission, IGraphStatViewData.State.ERROR)
            assertSame(error, graphViewData(errorEmission).error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cache is reused across process calls and cleared by dispose`() = runTest {
        val fixture = fixture()
        val items = listOf(
            rankedTracker(groupItemId = 10L, trackerId = 3L, featureId = 30L),
            rankedGraph(groupItemId = 11L, graphId = 2L),
        )
        fixture.trackers[30L] = testDisplayTracker(id = 3L, featureId = 30L, name = "cached tracker")
        fixture.graphHandler = { graph, _ -> readyGraph(graph, label = "cached graph") }

        fixture.processor.process(items).test {
            awaitItem()
            awaitItem()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(30L), fixture.trackerCalls)
        assertEquals(listOf(2L), fixture.graphCalls)

        fixture.processor.process(items).test {
            val cachedInitial = awaitItem()
            assertTrackerName(cachedInitial[0], "cached tracker")
            assertGraphLabel(cachedInitial[1], "cached graph")
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(30L), fixture.trackerCalls)
        assertEquals(listOf(2L), fixture.graphCalls)

        fixture.processor.dispose()

        fixture.processor.process(items).test {
            assertTrue(awaitItem()[0].child is GroupChild.ChildTrackerLoading)
            awaitItem()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(30L, 30L), fixture.trackerCalls)
        assertEquals(listOf(2L, 2L), fixture.graphCalls)
    }

    @Test
    fun `tracker update event refreshes matching tracker and leaves other items unchanged`() = runTest {
        val fixture = fixture()
        fixture.trackers[30L] = testDisplayTracker(id = 3L, featureId = 30L, name = "first tracker")
        fixture.trackers[40L] = testDisplayTracker(id = 4L, featureId = 40L, name = "second tracker")
        fixture.graphHandler = { graph, _ -> readyGraph(graph, label = "stable graph") }

        val items = listOf(
            rankedTracker(groupItemId = 10L, trackerId = 3L, featureId = 30L),
            rankedTracker(groupItemId = 11L, trackerId = 4L, featureId = 40L),
            rankedGraph(groupItemId = 12L, graphId = 2L),
            rankedGroup(groupItemId = 13L, groupId = 5L),
            rankedFunction(groupItemId = 14L, functionId = 6L, featureId = 60L),
        )

        fixture.processor.process(items).test {
            awaitItem()

            val trackerEmission = awaitItem()
            assertTrackerName(trackerEmission[0], "first tracker")
            assertTrackerName(trackerEmission[1], "second tracker")

            val graphEmission = awaitItem()
            assertGraphLabel(graphEmission[2], "stable graph")
            assertTrue(graphEmission[3].child is GroupChild.ChildGroup)
            assertTrue(graphEmission[4].child is GroupChild.ChildFunction)

            fixture.trackers[30L] = testDisplayTracker(id = 3L, featureId = 30L, name = "refreshed tracker")
            fixture.events.emit(DataUpdateType.TrackerUpdated(trackerId = 3L, featureId = 30L))

            val refreshEmission = awaitItem()
            assertTrackerName(refreshEmission[0], "refreshed tracker")
            assertTrackerName(refreshEmission[1], "second tracker")
            assertGraphLabel(refreshEmission[2], "stable graph")
            assertTrue(refreshEmission[3].child is GroupChild.ChildGroup)
            assertTrue(refreshEmission[4].child is GroupChild.ChildFunction)
            assertEquals(listOf(30L, 40L, 30L), fixture.trackerCalls)
            assertEquals(listOf(2L), fixture.graphCalls)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `graph update event emits loading placeholder before refreshed graph data`() = runTest {
        val fixture = fixture()
        fixture.graphHandler = { graph, callIndex ->
            when (callIndex) {
                1 -> readyGraph(graph, label = "initial graph")
                2 -> readyGraph(graph, label = "refreshed graph")
                else -> error("Unexpected graph call $callIndex")
            }
        }

        fixture.processor.process(listOf(rankedGraph(groupItemId = 10L, graphId = 2L))).test {
            assertGraphState(awaitItem().single(), IGraphStatViewData.State.LOADING)
            assertGraphLabel(awaitItem().single(), "initial graph")

            fixture.events.emit(DataUpdateType.GraphOrStatUpdated(2L))

            assertGraphState(awaitItem().single(), IGraphStatViewData.State.LOADING)
            assertGraphLabel(awaitItem().single(), "refreshed graph")
            assertEquals(listOf(2L, 2L), fixture.graphCalls)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stale initial graph result cannot overwrite fresher event refresh result`() = runTest {
        val fixture = fixture()
        val graph = testGraphOrStat(2L)
        val initialResult = CompletableDeferred<IGraphStatViewData>()
        fixture.graphHandler = { requestedGraph, callIndex ->
            when (callIndex) {
                1 -> initialResult.await()
                2 -> readyGraph(requestedGraph, label = "refresh")
                else -> error("Unexpected graph call $callIndex")
            }
        }

        fixture.processor.process(listOf(rankedGraph(groupItemId = 10L, graphId = graph.id))).test {
            assertGraphState(awaitItem().single(), IGraphStatViewData.State.LOADING)
            advanceUntilIdle()

            fixture.events.emit(DataUpdateType.GraphOrStatUpdated(graph.id))
            assertGraphState(awaitItem().single(), IGraphStatViewData.State.LOADING)
            assertGraphLabel(awaitItem().single(), "refresh")

            initialResult.complete(readyGraph(graph, label = "stale initial"))
            assertGraphLabel(awaitItem().single(), "refresh")

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun TestScope.fixture() = ProcessorFixture(
        graphDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private class ProcessorFixture(
        graphDispatcher: CoroutineDispatcher,
    ) {
        val events = MutableSharedFlow<DataUpdateType>()
        val trackers = mutableMapOf<Long, DisplayTracker?>()
        val trackerCalls = mutableListOf<Long>()
        val graphCalls = mutableListOf<Long>()
        var graphHandler: suspend (GraphOrStat, Int) -> IGraphStatViewData =
            { graph, _ -> readyGraph(graph, label = "graph ${graph.id}") }

        val processor = SearchResultProcessor(
            getDataUpdateEvents = { events },
            tryGetTrackerByFeatureId = { featureId ->
                trackerCalls += featureId
                trackers[featureId]
            },
            getGraphViewData = { graph ->
                graphCalls += graph.id
                graphHandler(graph, graphCalls.count { it == graph.id })
            },
            graphDispatcher = graphDispatcher,
        )
    }

    private companion object {
        private fun rankedTracker(
            groupItemId: Long,
            trackerId: Long,
            featureId: Long,
            name: String = "tracker $trackerId",
        ) = RankedItem(
            groupItemId = groupItemId,
            item = GroupGraphItem.TrackerNode(
                groupItemId = groupItemId,
                tracker = testTracker(
                    id = trackerId,
                    name = name,
                    featureId = featureId,
                    description = "description",
                ),
            ),
            paths = emptyList(),
        )

        private fun rankedGraph(
            groupItemId: Long,
            graphId: Long,
            name: String = "graph $graphId",
        ) = RankedItem(
            groupItemId = groupItemId,
            item = GroupGraphItem.GraphNode(
                groupItemId = groupItemId,
                graph = testGraphOrStat(id = graphId, name = name),
            ),
            paths = emptyList(),
        )

        private fun rankedGroup(
            groupItemId: Long,
            groupId: Long,
            name: String = "group $groupId",
        ) = RankedItem(
            groupItemId = groupItemId,
            item = GroupGraphItem.GroupNode(
                groupItemId = groupItemId,
                groupGraph = GroupGraph(
                    group = Group(id = groupId, name = name, colorIndex = 0, unique = true),
                    children = emptyList(),
                ),
            ),
            paths = emptyList(),
        )

        private fun rankedFunction(
            groupItemId: Long,
            functionId: Long,
            featureId: Long,
            name: String = "function $functionId",
        ) = RankedItem(
            groupItemId = groupItemId,
            item = GroupGraphItem.FunctionNode(
                groupItemId = groupItemId,
                function = testFunction(
                    id = functionId,
                    featureId = featureId,
                    name = name,
                    description = "description",
                ),
            ),
            paths = emptyList(),
        )

        private fun readyGraph(
            graph: GraphOrStat,
            label: String,
        ) = testGraphViewData(graphOrStat = graph, label = label)

        private fun assertTrackerName(
            item: SearchResultItem,
            name: String,
        ) {
            assertEquals(name, (item.child as GroupChild.ChildTracker).displayTracker.name)
        }

        private fun assertGraphState(
            item: SearchResultItem,
            state: IGraphStatViewData.State,
        ) {
            assertEquals(state, graphViewData(item).state)
        }

        private fun assertGraphLabel(
            item: SearchResultItem,
            label: String,
        ) {
            assertEquals(label, (graphViewData(item) as TestGraphViewData).label)
        }

        private fun graphViewData(item: SearchResultItem): IGraphStatViewData =
            (item.child as GroupChild.ChildGraph).graph.viewData
    }

}
