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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResult(
    val groupItemId: Long,
    val name: String,
    val description: String,
    val type: SearchResultType,
)

enum class SearchResultType {
    GROUP, TRACKER, GRAPH, FUNCTION
}

interface GroupSearchViewModel {
    val isSearchVisible: StateFlow<Boolean>
    val searchQuery: TextFieldState
    val searchResults: StateFlow<List<SearchResult>>
    fun setGroupId(groupId: Long)
    fun showSearch()
    fun hideSearch()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class GroupSearchViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
) : ViewModel(), GroupSearchViewModel {

    private val _isSearchVisible = MutableStateFlow(false)
    override val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    override val searchQuery: TextFieldState = TextFieldState()

    private var groupId = 0L
    private val groupGraph = MutableStateFlow<GroupGraph?>(null)

    private val queryText = snapshotFlow { searchQuery.text }
        .debounce(150)

    // TODO: Improve search ordering (e.g. rank by match quality, prefix matches first)
    override val searchResults: StateFlow<List<SearchResult>> =
        combine(queryText, groupGraph) { query, graph ->
            val text = query.toString()
            if (text.isBlank() || graph == null) return@combine emptyList()
            val results = mutableListOf<SearchResult>()
            collectMatches(graph, text, results)
            results
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }

    private fun collectMatches(
        graph: GroupGraph,
        query: String,
        results: MutableList<SearchResult>,
    ) {
        for (child in graph.children) {
            when (child) {
                is GroupGraphItem.GroupNode -> {
                    val group = child.groupGraph.group
                    if (group.name.contains(query, ignoreCase = true)) {
                        results.add(
                            SearchResult(
                                groupItemId = child.groupItemId,
                                name = group.name,
                                description = "",
                                type = SearchResultType.GROUP,
                            )
                        )
                    }
                    collectMatches(child.groupGraph, query, results)
                }

                is GroupGraphItem.TrackerNode -> {
                    val tracker = child.tracker
                    if (tracker.name.contains(query, ignoreCase = true) ||
                        tracker.description.contains(query, ignoreCase = true)
                    ) {
                        results.add(
                            SearchResult(
                                groupItemId = child.groupItemId,
                                name = tracker.name,
                                description = tracker.description,
                                type = SearchResultType.TRACKER,
                            )
                        )
                    }
                }

                is GroupGraphItem.GraphNode -> {
                    val graphOrStat = child.graph
                    if (graphOrStat.name.contains(query, ignoreCase = true)) {
                        results.add(
                            SearchResult(
                                groupItemId = child.groupItemId,
                                name = graphOrStat.name,
                                description = "",
                                type = SearchResultType.GRAPH,
                            )
                        )
                    }
                }

                is GroupGraphItem.FunctionNode -> {
                    val function = child.function
                    if (function.name.contains(query, ignoreCase = true) ||
                        function.description.contains(query, ignoreCase = true)
                    ) {
                        results.add(
                            SearchResult(
                                groupItemId = child.groupItemId,
                                name = function.name,
                                description = function.description,
                                type = SearchResultType.FUNCTION,
                            )
                        )
                    }
                }
            }
        }
    }
}
