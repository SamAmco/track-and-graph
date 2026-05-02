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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.interactor.DataUpdateType
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.di.DefaultDispatcher
import com.samco.trackandgraph.navigation.GroupDescentPath
import com.samco.trackandgraph.util.FuzzyMatcher
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * One of the routes the user can take to open a search result. [descent] drives navigation;
 * [displayString] is the slash-formatted form shown in the symlink disambiguation dialog.
 */
data class ResolvedPath(
    val descent: GroupDescentPath,
    val displayString: String,
)

/**
 * A search hit paired with every way to reach its component. Cards with more than one path
 * open the disambiguation dialog on tap; single-path cards navigate straight away.
 */
data class SearchResultItem(
    val child: GroupChild,
    val paths: List<ResolvedPath>,
)

sealed interface SearchDisplayState {
    /** Search is open but the query is empty — show "type to search". */
    data object Empty : SearchDisplayState
    /** A non-empty query is being processed — show a spinner. */
    data object Loading : SearchDisplayState
    /** Search completed. [items] may be empty — show "no results". */
    data class Results(val items: List<SearchResultItem>) : SearchDisplayState
}

interface GroupSearchViewModel {
    val isSearchVisible: StateFlow<Boolean>
    val searchQuery: TextFieldState
    val displayResults: StateFlow<SearchDisplayState>
    fun setGroupId(groupId: Long)
    fun showSearch()
    fun hideSearch()
}

private enum class ComponentType { GROUP, TRACKER, GRAPH, FUNCTION }

private data class ComponentKey(val type: ComponentType, val id: Long)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupSearchViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel(), GroupSearchViewModel {

    // Structural snapshot of a searchable component, taken once when search opens. Strings
    // and paths are captured here; live display state (DisplayTracker, graph view data) is
    // held in separate flows and merged in via `displayResults`'s combine. `featureId` is
    // non-null only for TrackerNode items — it's the correlation key for DataPoint events.
    // paths contains every route from the current group to this component — one entry per placement
    // (or more when any ancestor group is itself symlinked). At least one entry is always
    // present; size > 1 drives symlink disambiguation on tap.
    private data class SearchableItem(
        val groupItemId: Long,
        val item: GroupGraphItem,
        val name: String,
        val description: String?,
        val typeBonus: Double,
        val featureId: Long?,
        val paths: List<ResolvedPath>,
    )

    private data class SearchResult(
        val groupItemId: Long,
        val item: GroupGraphItem,
        val score: Double,
    )

    private val _isSearchVisible = MutableStateFlow(false)
    override val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    override val searchQuery: TextFieldState = TextFieldState()

    private var groupId = 0L

    private sealed interface SearchState {
        data object Loading : SearchState
        data class Ready(val items: List<SearchableItem>) : SearchState
    }

    // Populated once when search opens (graph is stable for the duration of a session).
    // Cleared when search closes.
    private val searchState = MutableStateFlow<SearchState>(SearchState.Loading)

    // Intermediate state between raw query and fully-rendered children.
    // `Done` here still needs to be combined with `graphViewDataCache` before
    // it can be shown, whereas `Empty` / `Loading` pass straight through.
    private sealed interface ScoredResults {
        data object Empty : ScoredResults
        data object Loading : ScoredResults
        data class Done(val results: List<SearchResult>) : ScoredResults
    }

    private val queryText: Flow<CharSequence> = snapshotFlow { searchQuery.text }

    // For each query: empty → Empty; non-empty → Loading immediately, wait for
    // the graph to be ready, debounce, score, emit Done. flatMapLatest cancels
    // the in-flight computation when the query changes (the RxJava switchMap
    // equivalent) so stale scoring never reaches the UI.
    private val searchResults: Flow<ScoredResults> = queryText
        .flatMapLatest { query ->
            val text = query.toString()
            if (text.isBlank()) {
                flowOf<ScoredResults>(ScoredResults.Empty)
            } else {
                flow<ScoredResults> {
                    emit(ScoredResults.Loading)
                    // Debounce inside the flow so flatMapLatest's cancellation
                    // gives us proper "only the latest query runs" semantics.
                    delay(DEBOUNCE_MS)
                    val items = searchState
                        .filterIsInstance<SearchState.Ready>()
                        .first()
                        .items
                    val scored = withContext(defaultDispatcher) {
                        items.mapNotNull { scoreItem(text, it) }
                            .sortedByDescending { it.score }
                    }
                    emit(ScoredResults.Done(scored))
                }
            }
        }

    // Cache for computed graph view data, keyed by GraphOrStat.id.
    // Cleared when search is hidden.
    private val graphViewDataCache =
        MutableStateFlow<Map<Long, CalculatedGraphViewData>>(emptyMap())

    // In-flight graph calculations keyed by graph.id. Same reconcile-against-desired
    // pattern as trackerFetchJobs: query changes cancel calculations no longer needed
    // by the result set. graphEventsJob also cancels + removes here when the
    // underlying data changes — without that, an in-flight calculation would
    // eventually write stale data to the cache.
    private val graphCalcJobs = mutableMapOf<Long, Job>()
    private val graphCalcJobsLock = Any()

    // Live per-tracker DisplayTracker, keyed by featureId (the correlation key on
    // DataPoint events). Populated lazily as trackers appear in result sets and
    // refreshed targetedly on DataUpdateType.DataPoint events for the duration of
    // the open session. Cleared when search closes.
    private val trackerDataMap = MutableStateFlow<Map<Long, DisplayTracker>>(emptyMap())

    // In-flight lazy fetches keyed by featureId. We track per-featureId jobs (not a
    // single shared job) so that a query change can cancel only the now-irrelevant
    // fetches while leaving still-relevant ones running. Reconciled in
    // launchTrackerFetches against the desired set produced by mapResultsToSearchItems.
    // Mutated from the combine path (via withContext(io)) and from the launched
    // fetch coroutines themselves (on completion), so all access goes through the lock.
    private val trackerFetchJobs = mutableMapOf<Long, Job>()
    private val trackerFetchJobsLock = Any()

    // Event-subscription jobs active only while search is open.
    private var trackerEventsJob: Job? = null
    private var graphEventsJob: Job? = null

    // Empty/Loading pass through; Done is combined with the graph-view-data
    // cache and the live tracker-display map so placeholder graphs get replaced
    // as background calculations complete and tracker cards reflect new
    // data-point writes. The upstream flatMapLatest already guarantees only the
    // latest query's Done reaches here, so a plain combine is sufficient — no
    // cancellation semantics needed at this stage.
    override val displayResults: StateFlow<SearchDisplayState> =
        combine(searchResults, graphViewDataCache, trackerDataMap) { state, cache, trackers ->
            when (state) {
                is ScoredResults.Empty -> SearchDisplayState.Empty
                is ScoredResults.Loading -> SearchDisplayState.Loading
                is ScoredResults.Done -> SearchDisplayState.Results(
                    mapResultsToSearchItems(state.results, cache, trackers)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), SearchDisplayState.Empty)

    override fun setGroupId(groupId: Long) {
        this.groupId = groupId
    }

    override fun showSearch() {
        _isSearchVisible.value = true
        searchState.value = SearchState.Loading
        trackerEventsJob?.cancel()
        graphEventsJob?.cancel()
        trackerEventsJob = null
        graphEventsJob = null
        viewModelScope.launch {
            val graph = dataInteractor.getGroupGraphSync(groupId)
            val items = buildSearchableItems(graph)
            // trackerDataMap stays empty — DisplayTrackers are fetched lazily by
            // mapTracker the first time a tracker appears in the result set, exactly
            // like the graph-view-data cache works for ChildGraph. Seeding all 100s
            // of trackers up-front blocked search startup for several seconds.
            searchState.value = SearchState.Ready(items)
            startTrackerEventsJob()
            startGraphEventsJob()
        }
    }

    override fun hideSearch() {
        _isSearchVisible.value = false
        searchQuery.clearText()
        searchState.value = SearchState.Loading
        graphViewDataCache.value = emptyMap()
        trackerEventsJob?.cancel()
        graphEventsJob?.cancel()
        trackerEventsJob = null
        graphEventsJob = null
        trackerDataMap.value = emptyMap()
        synchronized(trackerFetchJobsLock) {
            trackerFetchJobs.values.forEach { it.cancel() }
            trackerFetchJobs.clear()
        }
        synchronized(graphCalcJobsLock) {
            graphCalcJobs.values.forEach { it.cancel() }
            graphCalcJobs.clear()
        }
    }

    private fun startTrackerEventsJob() {
        trackerEventsJob = viewModelScope.launch {
            dataInteractor.getDataUpdateEvents()
                .collect { event ->
                    val featureId = when (event) {
                        is DataUpdateType.DataPoint -> event.featureId
                        is DataUpdateType.TrackerUpdated -> event.featureId
                        else -> return@collect
                    }
                    if (featureId !in trackerDataMap.value) return@collect
                    val refreshed = dataInteractor.tryGetTrackerByFeatureId(featureId)
                        ?: return@collect
                    trackerDataMap.update { it + (featureId to refreshed) }
                }
        }
    }

    private fun startGraphEventsJob() {
        graphEventsJob = viewModelScope.launch {
            dataInteractor.getDataUpdateEvents()
                .filterIsInstance<DataUpdateType.GraphOrStatUpdated>()
                .collect { event ->
                    val graphId = event.graphStatId
                    // Cancel any in-flight calculation for this graph — its inputs
                    // are now stale, and without this its eventual write would
                    // overwrite the fresh value we're about to recalculate.
                    val cancelled = synchronized(graphCalcJobsLock) {
                        graphCalcJobs.remove(graphId)?.also { it.cancel() } != null
                    }
                    val wasCached = graphId in graphViewDataCache.value
                    if (!cancelled && !wasCached) return@collect
                    if (wasCached) graphViewDataCache.value -= graphId
                    // Cache change re-fires the combine; mapGraph re-queues the
                    // graph and launchGraphCalculations starts a fresh job.
                }
        }
    }

    private fun scoreItem(query: String, item: SearchableItem): SearchResult? {
        val nameScore = FuzzyMatcher.score(query, item.name)
        val descScore = item.description
            ?.let { FuzzyMatcher.score(query, it) }
            ?.times(DESCRIPTION_SCORE_MULTIPLIER)
        val baseScore = listOfNotNull(nameScore, descScore).maxOrNull() ?: return null
        return SearchResult(item.groupItemId, item.item, baseScore + item.typeBonus)
    }

    private suspend fun buildSearchableItems(graph: GroupGraph): List<SearchableItem> {
        val pathsByComponent = buildResolvedPaths(graph)
        val items = mutableListOf<SearchableItem>()
        collectSearchableItems(graph, pathsByComponent, items)
        return items
    }

    /**
     * Walks the DAG below the current group, recording every reachable placement of every
     * component. Paths are descent-relative: the current group is the anchor and is not
     * included in [ResolvedPath.descent] or [ResolvedPath.displayString].
     *
     * [groupIds] and [groupNames] track the ancestor chain of the CURRENT recursion frame,
     * outer-to-inner, below the current group. [visitedGroupIds] guards against cycles on
     * the current descent path — symlinks allow the same group to be reached via multiple
     * parents, so we still visit it each time; we only bail when a group appears as its own
     * ancestor on the current path.
     */
    private fun buildResolvedPaths(graph: GroupGraph): Map<ComponentKey, List<ResolvedPath>> {
        val result = mutableMapOf<ComponentKey, MutableList<ResolvedPath>>()
        walkPaths(graph, emptyList(), emptyList(), result)
        return result.mapValues { (_, list) -> list.toList() }
    }

    private fun walkPaths(
        graph: GroupGraph,
        groupIds: List<Long>,
        groupNames: List<String>,
        result: MutableMap<ComponentKey, MutableList<ResolvedPath>>,
        visitedGroupIds: MutableSet<Long> = mutableSetOf(),
    ) {
        for (child in graph.children) {
            when (child) {
                is GroupGraphItem.GroupNode -> {
                    val childGroupId = child.groupGraph.group.id
                    val childName = child.groupGraph.group.name
                    result.getOrPut(ComponentKey(ComponentType.GROUP, childGroupId)) { mutableListOf() }
                        .add(ResolvedPath(
                            descent = GroupDescentPath(
                                groupIds = groupIds + childGroupId,
                                groupItemId = null,
                            ),
                            displayString = formatDisplay(groupNames + childName),
                        ))
                    if (visitedGroupIds.add(childGroupId)) {
                        walkPaths(
                            graph = child.groupGraph,
                            groupIds = groupIds + childGroupId,
                            groupNames = groupNames + childName,
                            result = result,
                            visitedGroupIds = visitedGroupIds,
                        )
                        visitedGroupIds.remove(childGroupId)
                    }
                }
                is GroupGraphItem.TrackerNode -> {
                    result.getOrPut(ComponentKey(ComponentType.TRACKER, child.tracker.id)) { mutableListOf() }
                        .add(ResolvedPath(
                            descent = GroupDescentPath(groupIds = groupIds, groupItemId = child.groupItemId),
                            displayString = formatDisplay(groupNames + child.tracker.name),
                        ))
                }
                is GroupGraphItem.GraphNode -> {
                    result.getOrPut(ComponentKey(ComponentType.GRAPH, child.graph.id)) { mutableListOf() }
                        .add(ResolvedPath(
                            descent = GroupDescentPath(groupIds = groupIds, groupItemId = child.groupItemId),
                            displayString = formatDisplay(groupNames + child.graph.name),
                        ))
                }
                is GroupGraphItem.FunctionNode -> {
                    result.getOrPut(ComponentKey(ComponentType.FUNCTION, child.function.id)) { mutableListOf() }
                        .add(ResolvedPath(
                            descent = GroupDescentPath(groupIds = groupIds, groupItemId = child.groupItemId),
                            displayString = formatDisplay(groupNames + child.function.name),
                        ))
                }
            }
        }
    }

    private fun formatDisplay(segments: List<String>): String =
        if (segments.isEmpty()) PATH_SEPARATOR
        else PATH_SEPARATOR + segments.joinToString(PATH_SEPARATOR)

    private suspend fun collectSearchableItems(
        graph: GroupGraph,
        pathsByComponent: Map<ComponentKey, List<ResolvedPath>>,
        items: MutableList<SearchableItem>,
        seen: MutableSet<ComponentKey> = mutableSetOf(),
    ) {
        for (child in graph.children) {
            when (child) {
                is GroupGraphItem.GroupNode -> {
                    val key = ComponentKey(ComponentType.GROUP, child.groupGraph.group.id)
                    if (seen.add(key)) {
                        items.add(SearchableItem(
                            groupItemId = child.groupItemId,
                            item = child,
                            name = child.groupGraph.group.name,
                            description = null,
                            typeBonus = 0.0,
                            featureId = null,
                            paths = pathsByComponent[key].orEmpty(),
                        ))
                        collectSearchableItems(child.groupGraph, pathsByComponent, items, seen)
                    }
                }
                is GroupGraphItem.TrackerNode -> {
                    val key = ComponentKey(ComponentType.TRACKER, child.tracker.id)
                    if (seen.add(key)) {
                        items.add(SearchableItem(
                            groupItemId = child.groupItemId,
                            item = child,
                            name = child.tracker.name,
                            description = child.tracker.description,
                            typeBonus = TYPE_BONUS_TRACKER,
                            featureId = child.tracker.featureId,
                            paths = pathsByComponent[key].orEmpty(),
                        ))
                    }
                }
                is GroupGraphItem.GraphNode -> {
                    val key = ComponentKey(ComponentType.GRAPH, child.graph.id)
                    if (seen.add(key)) {
                        items.add(SearchableItem(
                            groupItemId = child.groupItemId,
                            item = child,
                            name = child.graph.name,
                            description = null,
                            typeBonus = 0.0,
                            featureId = null,
                            paths = pathsByComponent[key].orEmpty(),
                        ))
                    }
                }
                is GroupGraphItem.FunctionNode -> {
                    val key = ComponentKey(ComponentType.FUNCTION, child.function.id)
                    if (seen.add(key)) {
                        items.add(SearchableItem(
                            groupItemId = child.groupItemId,
                            item = child,
                            name = child.function.name,
                            description = child.function.description,
                            typeBonus = 0.0,
                            featureId = null,
                            paths = pathsByComponent[key].orEmpty(),
                        ))
                    }
                }
            }
        }
    }

    private suspend fun mapResultsToSearchItems(
        results: List<SearchResult>,
        cache: Map<Long, CalculatedGraphViewData>,
        trackers: Map<Long, DisplayTracker>,
    ): List<SearchResultItem> = withContext(io) {
        val graphsToCalculate = mutableListOf<GroupGraphItem.GraphNode>()
        val trackersToFetch = mutableListOf<Long>()

        // Build a lookup so the mapper functions can find the pre-fetched searchable
        // (including paths) without another DB round-trip.
        val itemLookup = (searchState.value as? SearchState.Ready)?.items
            .orEmpty().associateBy { it.groupItemId }

        val searchItems = results.mapNotNull { result ->
            val searchable = itemLookup[result.groupItemId] ?: return@mapNotNull null
            val child: GroupChild = when (val item = result.item) {
                is GroupGraphItem.TrackerNode -> mapTracker(
                    groupItemId = result.groupItemId,
                    item = item,
                    trackers = trackers,
                    trackersToFetch = trackersToFetch,
                )
                is GroupGraphItem.GroupNode -> mapGroup(result.groupItemId, item)
                is GroupGraphItem.FunctionNode -> mapFunction(result.groupItemId, item)
                is GroupGraphItem.GraphNode -> mapGraph(
                    groupItemId = result.groupItemId,
                    item = item,
                    cache = cache,
                    graphsToCalculate = graphsToCalculate
                )
            }
            SearchResultItem(child = child, paths = searchable.paths)
        }

        if (graphsToCalculate.isNotEmpty()) {
            launchGraphCalculations(graphsToCalculate)
        }
        if (trackersToFetch.isNotEmpty()) {
            launchTrackerFetches(trackersToFetch)
        }

        searchItems
    }

    private fun mapTracker(
        groupItemId: Long,
        item: GroupGraphItem.TrackerNode,
        trackers: Map<Long, DisplayTracker>,
        trackersToFetch: MutableList<Long>,
    ): GroupChild {
        val displayTracker = trackers[item.tracker.featureId]
        if (displayTracker == null) {
            trackersToFetch.add(item.tracker.featureId)
            return GroupChild.ChildTrackerLoading(
                groupItemId = groupItemId,
                id = item.tracker.id,
                name = item.tracker.name,
            )
        }
        return GroupChild.ChildTracker(
            groupItemId = groupItemId,
            id = item.tracker.id,
            displayTracker = displayTracker,
        )
    }

    private fun mapGroup(
        groupItemId: Long,
        item: GroupGraphItem.GroupNode,
    ) = GroupChild.ChildGroup(
        groupItemId = groupItemId,
        id = item.groupGraph.group.id,
        group = item.groupGraph.group,
    )

    private fun mapFunction(
        groupItemId: Long,
        item: GroupGraphItem.FunctionNode,
    ) = GroupChild.ChildFunction(
        groupItemId = groupItemId,
        id = item.function.id,
        displayFunction = DisplayFunction(
            id = item.function.id,
            featureId = item.function.featureId,
            groupId = 0, // not meaningful in search context
            name = item.function.name,
            description = item.function.description,
            unique = item.function.unique,
        ),
    )

    private fun mapGraph(
        groupItemId: Long,
        item: GroupGraphItem.GraphNode,
        cache: Map<Long, CalculatedGraphViewData>,
        graphsToCalculate: MutableList<GroupGraphItem.GraphNode>,
    ): GroupChild.ChildGraph {
        val viewData = cache[item.graph.id] ?: run {
            graphsToCalculate.add(item)
            CalculatedGraphViewData(
                time = System.nanoTime(),
                viewData = IGraphStatViewData.loading(item.graph)
            )
        }
        return GroupChild.ChildGraph(
            groupItemId = groupItemId,
            id = item.graph.id,
            graph = viewData
        )
    }

    /**
     * Reconcile in-flight fetches against [desired] (the featureIds the current
     * result set wants displayed). Jobs for featureIds no longer desired are
     * cancelled; jobs already running for desired featureIds are left alone; new
     * jobs are launched for desired featureIds with no current job.
     */
    private fun launchTrackerFetches(desired: List<Long>) {
        synchronized(trackerFetchJobsLock) {
            val desiredSet = desired.toSet()
            val iter = trackerFetchJobs.entries.iterator()
            while (iter.hasNext()) {
                val (featureId, job) = iter.next()
                if (featureId !in desiredSet) {
                    job.cancel()
                    iter.remove()
                }
            }
            for (featureId in desired) {
                if (featureId in trackerFetchJobs) continue
                trackerFetchJobs[featureId] = viewModelScope.launch {
                    try {
                        dataInteractor.tryGetTrackerByFeatureId(featureId)?.let {
                            trackerDataMap.update { map -> map + (featureId to it) }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to fetch tracker for featureId=$featureId")
                    } finally {
                        synchronized(trackerFetchJobsLock) {
                            // Only remove if we're still the current job — a newer
                            // launch may have replaced us between cancellation and
                            // this finally running.
                            if (trackerFetchJobs[featureId] === coroutineContext[Job]) {
                                trackerFetchJobs.remove(featureId)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Reconcile in-flight graph calculations against [desired] (the graphs the
     * current result set wants displayed). Mirrors [launchTrackerFetches] — calcs
     * for graphs no longer desired are cancelled, in-flight calcs for desired
     * graphs are left alone, and new calcs are launched for desired graphs not
     * already running.
     */
    private fun launchGraphCalculations(desired: List<GroupGraphItem.GraphNode>) {
        synchronized(graphCalcJobsLock) {
            val desiredById = desired.associateBy { it.graph.id }
            val iter = graphCalcJobs.entries.iterator()
            while (iter.hasNext()) {
                val (graphId, job) = iter.next()
                if (graphId !in desiredById) {
                    job.cancel()
                    iter.remove()
                }
            }
            for ((graphId, node) in desiredById) {
                if (graphId in graphCalcJobs) continue
                graphCalcJobs[graphId] = viewModelScope.launch(defaultDispatcher) {
                    try {
                        val viewData = gsiProvider.getDataFactory(node.graph.type)
                            .getViewData(node.graph)
                        // Re-check before writing — if cancellation happened after
                        // getViewData returned but before we get here, the cache
                        // change would otherwise re-introduce stale data.
                        ensureActive()
                        graphViewDataCache.update {
                            it + (graphId to CalculatedGraphViewData(
                                time = System.nanoTime() + 1,
                                viewData = viewData,
                            ))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to calculate graph view data for $graphId")
                    } finally {
                        synchronized(graphCalcJobsLock) {
                            // Only remove if the map still holds this exact job —
                            // a newer job may have replaced us via the events
                            // path, and we mustn't disturb that entry.
                            if (graphCalcJobs[graphId] === coroutineContext[Job]) {
                                graphCalcJobs.remove(graphId)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val DESCRIPTION_SCORE_MULTIPLIER = 0.8
        private const val TYPE_BONUS_TRACKER = 5.0
        private const val DEBOUNCE_MS = 150L
        private const val PATH_SEPARATOR = "/"
    }
}
