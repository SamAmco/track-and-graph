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
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.di.DefaultDispatcher
import com.samco.trackandgraph.util.FuzzyMatcher
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class SearchResult(
    val groupItemId: Long,
    val item: GroupGraphItem,
    val score: Double,
)

sealed interface SearchDisplayState {
    /** Search is open but the query is empty — show "type to search". */
    data object Empty : SearchDisplayState
    /** A non-empty query is being processed — show a spinner. */
    data object Loading : SearchDisplayState
    /** Search completed. [children] may be empty — show "no results". */
    data class Results(val children: List<GroupChild>) : SearchDisplayState
}

interface GroupSearchViewModel {
    val isSearchVisible: StateFlow<Boolean>
    val searchQuery: TextFieldState
    val displayResults: StateFlow<SearchDisplayState>
    fun setGroupId(groupId: Long)
    fun showSearch()
    fun hideSearch()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupSearchViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel(), GroupSearchViewModel {

    // All strings and display data pre-fetched once when search opens.
    // displayTracker is non-null only for TrackerNode items.
    private data class SearchableItem(
        val groupItemId: Long,
        val item: GroupGraphItem,
        val name: String,
        val description: String?,
        val typeBonus: Double,
        val displayTracker: DisplayTracker?,
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

    // Empty/Loading pass through; Done is combined with the graph-view-data
    // cache so placeholder graphs get replaced as background calculations
    // complete. The upstream flatMapLatest already guarantees only the latest
    // query's Done reaches here, so a plain combine is sufficient — no
    // cancellation semantics needed at this stage.
    override val displayResults: StateFlow<SearchDisplayState> =
        combine(searchResults, graphViewDataCache) { state, cache ->
            when (state) {
                is ScoredResults.Empty -> SearchDisplayState.Empty
                is ScoredResults.Loading -> SearchDisplayState.Loading
                is ScoredResults.Done -> SearchDisplayState.Results(
                    mapResultsToGroupChildren(state.results, cache)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), SearchDisplayState.Empty)

    override fun setGroupId(groupId: Long) {
        this.groupId = groupId
    }

    override fun showSearch() {
        _isSearchVisible.value = true
        searchState.value = SearchState.Loading
        viewModelScope.launch {
            val graph = dataInteractor.getGroupGraphSync(groupId)
            searchState.value = SearchState.Ready(buildSearchableItems(graph))
        }
    }

    override fun hideSearch() {
        _isSearchVisible.value = false
        searchQuery.clearText()
        searchState.value = SearchState.Loading
        graphViewDataCache.value = emptyMap()
    }

    private fun scoreItem(query: String, item: SearchableItem): SearchResult? {
        val nameScore = FuzzyMatcher.score(query, item.name)
        val descScore = item.description
            ?.let { FuzzyMatcher.score(query, it) }
            ?.times(DESCRIPTION_SCORE_MULTIPLIER)
        val baseScore = listOfNotNull(nameScore, descScore).maxOrNull() ?: return null
        return SearchResult(item.groupItemId, item.item, baseScore + item.typeBonus)
    }

    private enum class EntityType { GROUP, TRACKER, GRAPH, FUNCTION }

    private suspend fun buildSearchableItems(graph: GroupGraph): List<SearchableItem> {
        val items = mutableListOf<SearchableItem>()
        collectSearchableItems(graph, items)
        return items
    }

    private suspend fun collectSearchableItems(
        graph: GroupGraph,
        items: MutableList<SearchableItem>,
        seen: MutableSet<Pair<EntityType, Long>> = mutableSetOf(),
    ) {
        for (child in graph.children) {
            when (child) {
                is GroupGraphItem.GroupNode -> {
                    if (seen.add(Pair(EntityType.GROUP, child.groupGraph.group.id))) {
                        items.add(SearchableItem(
                            groupItemId = child.groupItemId,
                            item = child,
                            name = child.groupGraph.group.name,
                            description = null,
                            typeBonus = 0.0,
                            displayTracker = null,
                        ))
                        collectSearchableItems(child.groupGraph, items, seen)
                    }
                }
                is GroupGraphItem.TrackerNode -> {
                    if (seen.add(Pair(EntityType.TRACKER, child.tracker.id))) {
                        items.add(SearchableItem(
                            groupItemId = child.groupItemId,
                            item = child,
                            name = child.tracker.name,
                            description = child.tracker.description,
                            typeBonus = TYPE_BONUS_TRACKER,
                            displayTracker = dataInteractor.tryGetTrackerByFeatureId(child.tracker.featureId),
                        ))
                    }
                }
                is GroupGraphItem.GraphNode -> {
                    if (seen.add(Pair(EntityType.GRAPH, child.graph.id))) {
                        items.add(SearchableItem(
                            groupItemId = child.groupItemId,
                            item = child,
                            name = child.graph.name,
                            description = null,
                            typeBonus = 0.0,
                            displayTracker = null,
                        ))
                    }
                }
                is GroupGraphItem.FunctionNode -> {
                    if (seen.add(Pair(EntityType.FUNCTION, child.function.id))) {
                        items.add(SearchableItem(
                            groupItemId = child.groupItemId,
                            item = child,
                            name = child.function.name,
                            description = child.function.description,
                            typeBonus = 0.0,
                            displayTracker = null,
                        ))
                    }
                }
            }
        }
    }

    private suspend fun mapResultsToGroupChildren(
        results: List<SearchResult>,
        cache: Map<Long, CalculatedGraphViewData>,
    ): List<GroupChild> = withContext(io) {
        val graphsToCalculate = mutableListOf<GroupGraphItem.GraphNode>()

        // Build a lookup so mapTracker can find pre-fetched display data without a DB call.
        val itemLookup = (searchState.value as? SearchState.Ready)?.items
            .orEmpty().associateBy { it.groupItemId }

        val children = results.mapNotNull { result ->
            when (val item = result.item) {
                is GroupGraphItem.TrackerNode -> mapTracker(result.groupItemId, item, itemLookup)
                is GroupGraphItem.GroupNode -> mapGroup(result.groupItemId, item)
                is GroupGraphItem.FunctionNode -> mapFunction(result.groupItemId, item)
                is GroupGraphItem.GraphNode -> mapGraph(
                    result.groupItemId,
                    item,
                    cache,
                    graphsToCalculate
                )
            }
        }

        // Launch async graph calculations for uncached graphs
        if (graphsToCalculate.isNotEmpty()) {
            launchGraphCalculations(graphsToCalculate)
        }

        children
    }

    private fun mapTracker(
        groupItemId: Long,
        item: GroupGraphItem.TrackerNode,
        itemLookup: Map<Long, SearchableItem>,
    ): GroupChild.ChildTracker? {
        val displayTracker = itemLookup[groupItemId]?.displayTracker ?: return null
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

    private fun launchGraphCalculations(graphs: List<GroupGraphItem.GraphNode>) {
        viewModelScope.launch(defaultDispatcher) {
            val results = graphs.map { node ->
                async {
                    try {
                        val viewData = gsiProvider.getDataFactory(node.graph.type)
                            .getViewData(node.graph)
                        node.graph.id to CalculatedGraphViewData(
                            time = System.nanoTime() + 1,
                            viewData = viewData,
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to calculate graph view data for ${node.graph.id}")
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            if (results.isNotEmpty()) {
                graphViewDataCache.value += results.toMap()
            }
        }
    }

    companion object {
        private const val DESCRIPTION_SCORE_MULTIPLIER = 0.8
        private const val TYPE_BONUS_TRACKER = 5.0
        private const val DEBOUNCE_MS = 150L
    }
}
