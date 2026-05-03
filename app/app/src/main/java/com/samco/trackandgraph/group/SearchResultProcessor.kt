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

import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.GroupGraphItem
import com.samco.trackandgraph.data.di.DefaultDispatcher
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.interactor.DataUpdateType
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * One ranked search hit handed to the processor — minimum surface needed to
 * render the card and (for trackers/graphs) fetch the live data. Carries the
 * navigable paths so [SearchResultProcessor] can build [SearchResultItem]s
 * without re-walking the group graph.
 */
data class RankedItem(
    val groupItemId: Long,
    val item: GroupGraphItem,
    val paths: List<ResolvedPath>,
)

private data class TrackerResult(
    val ranked: RankedItem,
    val featureId: Long,
    val cacheVersion: Long,
    val tracker: DisplayTracker,
)

private data class GraphResult(
    val ranked: RankedItem,
    val graphId: Long,
    val cacheVersion: Long,
    val viewData: IGraphStatViewData,
)

/**
 * Owns the per-session cache of fetched [DisplayTracker]s and computed graph
 * view data, and turns a ranked list of [RankedItem]s into an evolving
 * [Flow]<[List]<[SearchResultItem]>>.
 *
 * Behaviour:
 * - First emission is the full list with cache hits filled in and cache
 *   misses rendered as loading placeholders, so the grid renders immediately
 *   in rank order.
 * - The remaining work is processed in rank-order batches of [BATCH_SIZE].
 *   Within each batch, trackers are fetched in parallel first,
 *   then graphs in parallel. The list is re-emitted twice per batch —
 *   once after the trackers, once after the graphs — so the user
 *   sees populated cards as soon as their data lands
 *   and a slow graph never holds up cheap tracker cards.
 * - Data-update listening starts as soon as the initial placeholder list is
 *   emitted, not after the initial fill. This is what makes a `+` tap on a
 *   tracker card update its last-value/timestamp in place even while lower
 *   priority results are still being populated.
 *
 * Cancellation: the consumer cancelling (via flatMapLatest on a query
 * change, or unsubscribe on hideSearch) cancels the channelFlow and all
 * in-flight work as siblings. Only entries that fully completed live in the
 * cache; partially-fetched items are simply re-fetched on the next process()
 * call. [dispose] clears the cache (called from hideSearch).
 */
class SearchResultProcessor @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    private val cache = SearchResultCache()

    fun process(items: List<RankedItem>): Flow<List<SearchResultItem>> = channelFlow {
        if (items.isEmpty()) {
            send(emptyList())
            return@channelFlow
        }

        // Build the initial list against the current cache snapshot — every
        // item gets a card immediately (placeholders where no data yet).
        val currentList = items.map { renderItem(it, cache.snapshot()) }.toMutableList()
        val indexByGroupItemId = currentList.withIndex()
            .associate { (idx, item) -> item.child.groupItemId to idx }
        val listMutex = Mutex()
        send(currentList.toList())

        launch {
            listenForUpdates(items, currentList, indexByGroupItemId, listMutex)
        }

        // Process the missing data in rank-order batches, trackers-first
        // within each batch.
        items.chunked(BATCH_SIZE).forEach { batch ->
            ensureActive()

            val trackerSnapshot = cache.snapshot()
            val trackerWork = batch.mapNotNull { ranked ->
                val node = ranked.item as? GroupGraphItem.TrackerNode ?: return@mapNotNull null
                if (node.tracker.featureId in trackerSnapshot.trackers) return@mapNotNull null
                ranked
            }
            if (trackerWork.isNotEmpty()) {
                val resolved = fetchTrackers(trackerWork)
                val nextList = listMutex.withLock {
                    applyTrackerResults(resolved, currentList, indexByGroupItemId)
                    currentList.toList()
                }
                send(nextList)
            }

            val graphSnapshot = cache.snapshot()
            val graphWork = batch.mapNotNull { ranked ->
                val node = ranked.item as? GroupGraphItem.GraphNode ?: return@mapNotNull null
                if (node.graph.id in graphSnapshot.graphs) return@mapNotNull null
                ranked
            }
            if (graphWork.isNotEmpty()) {
                val resolved = calculateGraphs(graphWork)
                val nextList = listMutex.withLock {
                    applyGraphResults(resolved, currentList, indexByGroupItemId)
                    currentList.toList()
                }
                send(nextList)
            }
        }
    }

    /** Drop everything cached — called from hideSearch. */
    fun dispose() {
        cache.clear()
    }

    // ===== Helpers =====

    private suspend fun fetchTrackers(
        work: List<RankedItem>,
    ): List<TrackerResult> = coroutineScope {
        work.map { ranked ->
            async {
                val node = ranked.item as GroupGraphItem.TrackerNode
                val featureId = node.tracker.featureId
                val version = cache.trackerVersion(featureId)
                val tracker = try {
                    dataInteractor.tryGetTrackerByFeatureId(featureId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch tracker featureId=$featureId")
                    null
                }
                tracker?.let { TrackerResult(ranked, featureId, version, it) }
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun calculateGraphs(
        work: List<RankedItem>,
    ): List<GraphResult> = coroutineScope {
        work.map { ranked ->
            async(defaultDispatcher) {
                val node = ranked.item as GroupGraphItem.GraphNode
                val graphId = node.graph.id
                val version = cache.graphVersion(graphId)
                val viewData = try {
                    gsiProvider.getDataFactory(node.graph.type).getViewData(node.graph)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to calculate graph view data for ${node.graph.id}")
                    null
                }
                viewData?.let { GraphResult(ranked, graphId, version, it) }
            }
        }.awaitAll().filterNotNull()
    }

    private fun applyTrackerResults(
        resolved: List<TrackerResult>,
        list: MutableList<SearchResultItem>,
        index: Map<Long, Int>,
    ) {
        for ((ranked, featureId, version, tracker) in resolved) {
            if (!cache.putTrackerIfVersion(featureId, version, tracker)) continue
            val idx = index[ranked.groupItemId] ?: continue
            list[idx] = trackerItem(ranked, tracker)
        }
    }

    private fun applyGraphResults(
        resolved: List<GraphResult>,
        list: MutableList<SearchResultItem>,
        index: Map<Long, Int>,
    ) {
        for ((ranked, graphId, version, viewData) in resolved) {
            if (!cache.putGraphIfVersion(graphId, version, viewData)) continue
            val idx = index[ranked.groupItemId] ?: continue
            list[idx] = graphItem(ranked, viewData)
        }
    }

    private suspend fun ProducerScope<List<SearchResultItem>>.listenForUpdates(
        items: List<RankedItem>,
        currentList: MutableList<SearchResultItem>,
        index: Map<Long, Int>,
        listMutex: Mutex,
    ) {
        val byFeatureId = items
            .mapNotNull { ranked ->
                val node = ranked.item as? GroupGraphItem.TrackerNode ?: return@mapNotNull null
                node.tracker.featureId to ranked
            }
            .toMap()
        val byGraphId = items
            .mapNotNull { ranked ->
                val node = ranked.item as? GroupGraphItem.GraphNode ?: return@mapNotNull null
                node.graph.id to ranked
            }
            .toMap()

        dataInteractor.getDataUpdateEvents().collect { event ->
            val featureId: Long? = when (event) {
                is DataUpdateType.DataPoint -> event.featureId
                is DataUpdateType.TrackerUpdated -> event.featureId
                else -> null
            }
            if (featureId != null) {
                val ranked = byFeatureId[featureId] ?: return@collect
                cache.invalidateTracker(featureId)
                val version = cache.trackerVersion(featureId)
                val refreshed = fetchTrackerForEvent(featureId) ?: return@collect
                val nextList = listMutex.withLock {
                    if (!cache.putTrackerIfVersion(featureId, version, refreshed)) {
                        return@withLock null
                    }
                    val idx = index[ranked.groupItemId] ?: return@withLock null
                    currentList[idx] = trackerItem(ranked, refreshed)
                    currentList.toList()
                }
                if (nextList != null) send(nextList)
                return@collect
            }
            if (event is DataUpdateType.GraphOrStatUpdated) {
                val ranked = byGraphId[event.graphStatId] ?: return@collect
                val node = ranked.item as GroupGraphItem.GraphNode
                cache.invalidateGraph(event.graphStatId)
                val version = cache.graphVersion(event.graphStatId)
                val placeholderList = listMutex.withLock {
                    val idx = index[ranked.groupItemId] ?: return@withLock null
                    currentList[idx] = graphPlaceholder(ranked)
                    currentList.toList()
                }
                if (placeholderList != null) send(placeholderList)
                val viewData = try {
                    withContext(defaultDispatcher) {
                        gsiProvider.getDataFactory(node.graph.type).getViewData(node.graph)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to recalculate graph view data for ${event.graphStatId}")
                    return@collect
                }
                val nextList = listMutex.withLock {
                    if (!cache.putGraphIfVersion(event.graphStatId, version, viewData)) {
                        return@withLock null
                    }
                    val idx = index[ranked.groupItemId] ?: return@withLock null
                    currentList[idx] = graphItem(ranked, viewData)
                    currentList.toList()
                }
                if (nextList != null) send(nextList)
            }
        }
    }

    private suspend fun fetchTrackerForEvent(
        featureId: Long,
    ): DisplayTracker? =
        try {
            dataInteractor.tryGetTrackerByFeatureId(featureId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh tracker featureId=$featureId")
            null
        }

    // ===== Item rendering =====

    private fun renderItem(ranked: RankedItem, snapshot: SearchResultCache.Snapshot): SearchResultItem =
        when (val node = ranked.item) {
            is GroupGraphItem.TrackerNode -> {
                val cached = snapshot.trackers[node.tracker.featureId]
                if (cached != null) trackerItem(ranked, cached) else trackerPlaceholder(ranked)
            }
            is GroupGraphItem.GraphNode -> {
                val cached = snapshot.graphs[node.graph.id]
                if (cached != null) graphItem(ranked, cached) else graphPlaceholder(ranked)
            }
            is GroupGraphItem.GroupNode -> groupItem(ranked, node)
            is GroupGraphItem.FunctionNode -> functionItem(ranked, node)
        }

    private fun trackerItem(ranked: RankedItem, tracker: DisplayTracker): SearchResultItem {
        val node = ranked.item as GroupGraphItem.TrackerNode
        return SearchResultItem(
            child = GroupChild.ChildTracker(
                groupItemId = ranked.groupItemId,
                id = node.tracker.id,
                displayTracker = tracker,
            ),
            paths = ranked.paths,
        )
    }

    private fun trackerPlaceholder(ranked: RankedItem): SearchResultItem {
        val node = ranked.item as GroupGraphItem.TrackerNode
        return SearchResultItem(
            child = GroupChild.ChildTrackerLoading(
                groupItemId = ranked.groupItemId,
                id = node.tracker.id,
                name = node.tracker.name,
            ),
            paths = ranked.paths,
        )
    }

    private fun graphItem(ranked: RankedItem, viewData: IGraphStatViewData): SearchResultItem {
        val node = ranked.item as GroupGraphItem.GraphNode
        return SearchResultItem(
            child = GroupChild.ChildGraph(
                groupItemId = ranked.groupItemId,
                id = node.graph.id,
                graph = CalculatedGraphViewData(time = System.nanoTime(), viewData = viewData),
            ),
            paths = ranked.paths,
        )
    }

    private fun graphPlaceholder(ranked: RankedItem): SearchResultItem {
        val node = ranked.item as GroupGraphItem.GraphNode
        return SearchResultItem(
            child = GroupChild.ChildGraph(
                groupItemId = ranked.groupItemId,
                id = node.graph.id,
                graph = CalculatedGraphViewData(
                    time = System.nanoTime(),
                    viewData = IGraphStatViewData.loading(node.graph),
                ),
            ),
            paths = ranked.paths,
        )
    }

    private fun groupItem(ranked: RankedItem, node: GroupGraphItem.GroupNode) =
        SearchResultItem(
            child = GroupChild.ChildGroup(
                groupItemId = ranked.groupItemId,
                id = node.groupGraph.group.id,
                group = node.groupGraph.group,
            ),
            paths = ranked.paths,
        )

    private fun functionItem(ranked: RankedItem, node: GroupGraphItem.FunctionNode) =
        SearchResultItem(
            child = GroupChild.ChildFunction(
                groupItemId = ranked.groupItemId,
                id = node.function.id,
                displayFunction = DisplayFunction(
                    id = node.function.id,
                    featureId = node.function.featureId,
                    groupId = 0,
                    name = node.function.name,
                    description = node.function.description,
                    unique = node.function.unique,
                ),
            ),
            paths = ranked.paths,
        )

    companion object {
        // 12 items per batch matches what's typically visible on a phone in
        // the search grid; tablets may need to wait for the second batch but
        // see the first batch's data quickly. Tune if needed.
        private const val BATCH_SIZE = 12
    }
}

/**
 * Thread-safe per-session cache of fetched tracker / computed graph data.
 * Reads are lock-free snapshots; writes are atomic CAS over the whole
 * snapshot, so concurrent batches and the events listener never produce a
 * torn read. Per-item versions let event invalidations reject older batch
 * results that finish late.
 */
private class SearchResultCache {
    data class Snapshot(
        val trackers: Map<Long, DisplayTracker>,
        val graphs: Map<Long, IGraphStatViewData>,
        val trackerVersions: Map<Long, Long>,
        val graphVersions: Map<Long, Long>,
    ) {
        companion object {
            val EMPTY = Snapshot(emptyMap(), emptyMap(), emptyMap(), emptyMap())
        }
    }

    private val ref = AtomicReference(Snapshot.EMPTY)

    fun snapshot(): Snapshot = ref.get()

    fun trackerVersion(featureId: Long): Long =
        ref.get().trackerVersions[featureId] ?: 0L

    fun graphVersion(graphId: Long): Long =
        ref.get().graphVersions[graphId] ?: 0L

    fun putTrackerIfVersion(
        featureId: Long,
        version: Long,
        tracker: DisplayTracker,
    ): Boolean = updateIfVersion(
        versionOf = { it.trackerVersions[featureId] ?: 0L },
        transform = { it.copy(trackers = it.trackers + (featureId to tracker)) },
        expectedVersion = version,
    )

    fun putGraphIfVersion(
        graphId: Long,
        version: Long,
        viewData: IGraphStatViewData,
    ): Boolean = updateIfVersion(
        versionOf = { it.graphVersions[graphId] ?: 0L },
        transform = { it.copy(graphs = it.graphs + (graphId to viewData)) },
        expectedVersion = version,
    )

    fun invalidateTracker(featureId: Long) {
        update {
            it.copy(
                trackers = it.trackers - featureId,
                trackerVersions = it.trackerVersions + (
                    featureId to ((it.trackerVersions[featureId] ?: 0L) + 1L)
                ),
            )
        }
    }

    fun invalidateGraph(graphId: Long) {
        update {
            it.copy(
                graphs = it.graphs - graphId,
                graphVersions = it.graphVersions + (
                    graphId to ((it.graphVersions[graphId] ?: 0L) + 1L)
                ),
            )
        }
    }

    fun clear() {
        ref.set(Snapshot.EMPTY)
    }

    private inline fun update(transform: (Snapshot) -> Snapshot) {
        while (true) {
            val current = ref.get()
            val next = transform(current)
            if (ref.compareAndSet(current, next)) return
        }
    }

    private inline fun updateIfVersion(
        expectedVersion: Long,
        versionOf: (Snapshot) -> Long,
        transform: (Snapshot) -> Snapshot,
    ): Boolean {
        while (true) {
            val current = ref.get()
            if (versionOf(current) != expectedVersion) return false
            val next = transform(current)
            if (ref.compareAndSet(current, next)) return true
        }
    }
}
