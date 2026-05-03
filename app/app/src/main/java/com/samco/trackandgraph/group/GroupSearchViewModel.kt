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
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem
import com.samco.trackandgraph.data.di.DefaultDispatcher
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.navigation.GroupDescentPath
import com.samco.trackandgraph.util.FuzzyMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val processor: SearchResultProcessor,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel(), GroupSearchViewModel {

    // Structural snapshot of a searchable component, taken once when search opens.
    // `name` and `description` feed FuzzyMatcher; `typeBonus` tilts the score at
    // ranking time. `paths` and `item` are passed through to the processor as the
    // RankedItem payload — only the latter two matter for rendering.
    private data class SearchableItem(
        val groupItemId: Long,
        val item: GroupGraphItem,
        val name: String,
        val description: String?,
        val typeBonus: Double,
        val paths: List<ResolvedPath>,
    )

    private data class ScoredItem(
        val searchable: SearchableItem,
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

    // Built once when search opens; stable for the duration of the session.
    // Cleared (back to Loading) on hideSearch().
    private val searchState = MutableStateFlow<SearchState>(SearchState.Loading)

    private val queryText: Flow<CharSequence> = snapshotFlow { searchQuery.text }

    /**
     * Per-keystroke pipeline:
     * 1. Blank query → emit Empty.
     * 2. Non-empty query → emit Loading immediately, debounce, score, then hand
     *    the ranked list to [SearchResultProcessor] which streams progressively
     *    populated [SearchResultItem] lists wrapped in [SearchDisplayState.Results].
     *
     * flatMapLatest cancels the inner flow on every new query, which cascades to
     * cancel the processor's batch processing and event listener — only the
     * latest query's work runs. Cache hits inside the processor make repeated
     * queries within a session (e.g. typing then backspacing) feel instant.
     */
    override val displayResults: StateFlow<SearchDisplayState> = queryText
        .flatMapLatest { query ->
            val text = query.toString()
            if (text.isBlank()) {
                flowOf(SearchDisplayState.Empty)
            } else {
                flow<SearchDisplayState> {
                    emit(SearchDisplayState.Loading)
                    delay(DEBOUNCE_MS)
                    val items = searchState
                        .filterIsInstance<SearchState.Ready>()
                        .first()
                        .items
                    val ranked = withContext(defaultDispatcher) {
                        items.mapNotNull { scoreItem(text, it) }
                            .sortedByDescending { it.score }
                            .map { it.searchable.toRankedItem() }
                    }
                    emitAll(processor.process(ranked).map { SearchDisplayState.Results(it) })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), SearchDisplayState.Empty)

    override fun setGroupId(groupId: Long) {
        this.groupId = groupId
    }

    override fun showSearch() {
        _isSearchVisible.value = true
        searchState.value = SearchState.Loading
        viewModelScope.launch {
            val graph = dataInteractor.getGroupGraphSync(groupId)
            val items = buildSearchableItems(graph)
            searchState.value = SearchState.Ready(items)
        }
    }

    override fun hideSearch() {
        _isSearchVisible.value = false
        searchQuery.clearText()
        searchState.value = SearchState.Loading
        processor.dispose()
    }

    private fun scoreItem(query: String, item: SearchableItem): ScoredItem? {
        val nameScore = FuzzyMatcher.score(query, item.name)
        val descScore = item.description
            ?.let { FuzzyMatcher.score(query, it) }
            ?.times(DESCRIPTION_SCORE_MULTIPLIER)
        val baseScore = listOfNotNull(nameScore, descScore).maxOrNull() ?: return null
        return ScoredItem(item, baseScore + item.typeBonus)
    }

    private fun SearchableItem.toRankedItem() = RankedItem(
        groupItemId = groupItemId,
        item = item,
        paths = paths,
    )

    private fun buildSearchableItems(graph: GroupGraph): List<SearchableItem> {
        val pathsByComponent = buildResolvedPaths(graph)
        val items = mutableListOf<SearchableItem>()
        collectSearchableItems(graph, pathsByComponent, items)
        return items
    }

    /**
     * Walks the DAG below the current group, recording every reachable placement of every
     * component. Paths are descent-relative: the current group is the anchor and is not
     * included in [ResolvedPath.descent] or [ResolvedPath.displayString].
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

    private fun collectSearchableItems(
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
                            paths = pathsByComponent[key].orEmpty(),
                        ))
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
