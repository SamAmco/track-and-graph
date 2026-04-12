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
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.di.DefaultDispatcher
import com.samco.trackandgraph.util.FuzzyMatcher
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
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

interface GroupSearchViewModel {
    val isSearchVisible: StateFlow<Boolean>
    val searchQuery: TextFieldState
    val displayResults: StateFlow<List<GroupChild>>
    fun setGroupId(groupId: Long)
    fun showSearch()
    fun hideSearch()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class GroupSearchViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel(), GroupSearchViewModel {

    private val _isSearchVisible = MutableStateFlow(false)
    override val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    override val searchQuery: TextFieldState = TextFieldState()

    private var groupId = 0L
    private val groupGraph = MutableStateFlow<GroupGraph?>(null)

    private val queryText = snapshotFlow { searchQuery.text }
        .debounce(150)

    private val searchResults = combine(queryText, groupGraph) { query, graph ->
        val text = query.toString()
        if (text.isBlank() || graph == null) return@combine emptyList()
        val results = mutableListOf<SearchResult>()
        collectMatches(graph, text, results)
        results.sortedByDescending { it.score }
    }

    // Cache for computed graph view data, keyed by GraphOrStat.id.
    // Cleared when search is hidden.
    private val graphViewDataCache =
        MutableStateFlow<Map<Long, CalculatedGraphViewData>>(emptyMap())

    override val displayResults: StateFlow<List<GroupChild>> =
        combine(searchResults, graphViewDataCache) { results, cache ->
            if (results.isEmpty()) return@combine emptyList()
            mapResultsToGroupChildren(results, cache)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), emptyList())

    override fun setGroupId(groupId: Long) {
        this.groupId = groupId
    }

    override fun showSearch() {
        _isSearchVisible.value = true
        viewModelScope.launch {
            groupGraph.value = dataInteractor.getGroupGraphSync(groupId)
        }
    }

    override fun hideSearch() {
        _isSearchVisible.value = false
        searchQuery.clearText()
        groupGraph.value = null
        graphViewDataCache.value = emptyMap()
    }

    private suspend fun mapResultsToGroupChildren(
        results: List<SearchResult>,
        cache: Map<Long, CalculatedGraphViewData>,
    ): List<GroupChild> = withContext(io) {
        val graphsToCalculate = mutableListOf<GroupGraphItem.GraphNode>()

        val children = results.mapNotNull { result ->
            when (val item = result.item) {
                is GroupGraphItem.TrackerNode -> mapTracker(result.groupItemId, item)
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

    private suspend fun mapTracker(
        groupItemId: Long,
        item: GroupGraphItem.TrackerNode,
    ): GroupChild.ChildTracker? = dataInteractor
        .tryGetTrackerByFeatureId(item.tracker.featureId)
        ?.let {
            GroupChild.ChildTracker(
                groupItemId = groupItemId,
                id = item.tracker.id,
                displayTracker = it
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

    private fun collectMatches(
        graph: GroupGraph,
        query: String,
        results: MutableList<SearchResult>,
    ) {
        for (child in graph.children) {
            when (child) {
                is GroupGraphItem.GroupNode -> matchGroup(child, query, results)
                is GroupGraphItem.TrackerNode -> matchTracker(child, query, results)
                is GroupGraphItem.GraphNode -> matchGraph(child, query, results)
                is GroupGraphItem.FunctionNode -> matchFunction(child, query, results)
            }
        }
    }

    private fun matchGroup(
        child: GroupGraphItem.GroupNode,
        query: String,
        results: MutableList<SearchResult>,
    ) {
        FuzzyMatcher.score(query, child.groupGraph.group.name)?.let { score ->
            results.add(SearchResult(groupItemId = child.groupItemId, item = child, score = score))
        }
        collectMatches(child.groupGraph, query, results)
    }

    private fun matchTracker(
        child: GroupGraphItem.TrackerNode,
        query: String,
        results: MutableList<SearchResult>,
    ) {
        val titleScore = FuzzyMatcher.score(query, child.tracker.name)
        val descScore = FuzzyMatcher.score(query, child.tracker.description)?.times(DESCRIPTION_SCORE_MULTIPLIER)
        val baseScore = listOfNotNull(titleScore, descScore).maxOrNull() ?: return
        results.add(SearchResult(
            groupItemId = child.groupItemId,
            item = child,
            score = baseScore + TYPE_BONUS_TRACKER,
        ))
    }

    private fun matchGraph(
        child: GroupGraphItem.GraphNode,
        query: String,
        results: MutableList<SearchResult>,
    ) {
        FuzzyMatcher.score(query, child.graph.name)?.let { score ->
            results.add(SearchResult(groupItemId = child.groupItemId, item = child, score = score))
        }
    }

    private fun matchFunction(
        child: GroupGraphItem.FunctionNode,
        query: String,
        results: MutableList<SearchResult>,
    ) {
        val titleScore = FuzzyMatcher.score(query, child.function.name)
        val descScore = FuzzyMatcher.score(query, child.function.description)?.times(DESCRIPTION_SCORE_MULTIPLIER)
        val baseScore = listOfNotNull(titleScore, descScore).maxOrNull() ?: return
        results.add(SearchResult(
            groupItemId = child.groupItemId,
            item = child,
            score = baseScore,
        ))
    }

    companion object {
        private const val DESCRIPTION_SCORE_MULTIPLIER = 0.8
        private const val TYPE_BONUS_TRACKER = 5.0
    }
}
