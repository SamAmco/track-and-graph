/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.selectitemdialog

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.IODispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.samco.trackandgraph.data.database.dto.GroupGraph as ModelGroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem as ModelGroupGraphItem

enum class SelectItemDialogState { LOADING, READY }

enum class SelectableItemType { GROUP, TRACKER, FEATURE, GRAPH, FUNCTION }

data class HiddenItem(
    val type: SelectableItemType,
    val id: Long
)

sealed class GraphNode {
    abstract val name: String

    data class Group(
        val id: Long,
        override val name: String,
        val colorIndex: Int,
        val expanded: MutableState<Boolean>,
        val children: List<GraphNode>,
        val isRoot: Boolean = false,
    ) : GraphNode()

    data class Tracker(
        val trackerId: Long,
        val featureId: Long,
        override val name: String,
    ) : GraphNode()

    data class Graph(
        val id: Long,
        override val name: String,
    ) : GraphNode()

    data class Function(
        val functionId: Long,
        val featureId: Long,
        override val name: String,
    ) : GraphNode()
}

interface SelectItemDialogViewModel {
    val state: StateFlow<SelectItemDialogState>
    val groupTree: StateFlow<GraphNode?>
    val selectedItem: StateFlow<GraphNode?>
    val lazyListState: LazyListState
    val horizontalScrollState: ScrollState

    fun init(selectableTypes: Set<SelectableItemType>, hiddenItems: Set<HiddenItem>)
    fun onItemClicked(item: GraphNode)
    fun reset()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SelectItemDialogViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel(), SelectItemDialogViewModel {

    private val _selectedItem = MutableStateFlow<GraphNode?>(null)
    override val selectedItem: StateFlow<GraphNode?> = _selectedItem.asStateFlow()

    override val lazyListState = LazyListState()
    override val horizontalScrollState = ScrollState(0)

    private data class Config(
        val selectableTypes: Set<SelectableItemType>,
        val hiddenItems: Set<HiddenItem>
    )

    private val config = MutableStateFlow<Config?>(null)
    override val groupTree: StateFlow<GraphNode?> = config
        .filterNotNull()
        .flatMapLatest { config ->
            dataInteractor.getDataUpdateEvents()
                .map { config }
                .onStart { emit(config) }
        }
        .map {
            buildGraphNodeTree(
                groupGraph = dataInteractor.getGroupGraphSync(rootGroupId = null),
                selectableTypes = it.selectableTypes,
                hiddenItems = it.hiddenItems
            )
        }
        .flowOn(io)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    override val state: StateFlow<SelectItemDialogState> =
        combine(listOf(groupTree, config)) { deps ->
            when {
                deps.any { it == null } -> SelectItemDialogState.LOADING
                else -> SelectItemDialogState.READY
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SelectItemDialogState.LOADING)

    override fun init(
        selectableTypes: Set<SelectableItemType>,
        hiddenItems: Set<HiddenItem>
    ) {
        config.value = Config(selectableTypes, hiddenItems)
    }

    override fun onItemClicked(item: GraphNode) {
        val selectableTypes = config.value?.selectableTypes ?: return
        when (item) {
            is GraphNode.Group -> {
                // Always toggle expansion when a group is clicked unless it is the root
                if (!item.isRoot) item.expanded.value = !item.expanded.value

                // Only set as selected if groups are selectable
                if (SelectableItemType.GROUP in selectableTypes) {
                    _selectedItem.value = item
                }
            }

            is GraphNode.Tracker -> {
                if (SelectableItemType.TRACKER !in selectableTypes
                    && SelectableItemType.FEATURE !in selectableTypes
                ) return
                _selectedItem.value = item
            }

            is GraphNode.Graph -> {
                if (SelectableItemType.GRAPH !in selectableTypes) return
                _selectedItem.value = item
            }

            is GraphNode.Function -> {
                if (SelectableItemType.FUNCTION !in selectableTypes
                    && SelectableItemType.FEATURE !in selectableTypes
                ) return
                _selectedItem.value = item
            }
        }
    }

    override fun reset() {
        config.value = null
        _selectedItem.value = null
    }

    private fun buildGraphNodeTree(
        groupGraph: ModelGroupGraph,
        selectableTypes: Set<SelectableItemType>,
        hiddenItems: Set<HiddenItem>
    ): GraphNode? {
        // If the group is hidden, skip its entire subgraph
        if (HiddenItem(SelectableItemType.GROUP, groupGraph.group.id) in hiddenItems) {
            return null
        }

        // Process children of this group
        val children = groupGraph.children.mapNotNull { child ->
            when (child) {
                is ModelGroupGraphItem.GroupNode -> {
                    if (groupHidden(groupId = child.groupGraph.group.id, hiddenItems)) {
                        return@mapNotNull null
                    }
                    // Recursively build child group nodes
                    buildGraphNodeTree(
                        groupGraph = child.groupGraph,
                        selectableTypes = selectableTypes,
                        hiddenItems = hiddenItems
                    )
                }

                is ModelGroupGraphItem.TrackerNode -> {
                    if (trackerHidden(
                            trackerId = child.tracker.id,
                            featureId = child.tracker.featureId,
                            selectableTypes = selectableTypes,
                            hiddenItems = hiddenItems
                        )
                    ) {
                        return@mapNotNull null
                    }
                    child.toGraphNode()
                }

                is ModelGroupGraphItem.GraphNode -> {
                    if (graphHidden(graphId = child.graph.id, selectableTypes, hiddenItems)) {
                        return@mapNotNull null
                    }
                    child.toGraphNode()
                }

                is ModelGroupGraphItem.FunctionNode -> {
                    if (functionHidden(
                            functionId = child.function.id,
                            featureId = child.function.featureId,
                            selectableTypes = selectableTypes,
                            hiddenItems = hiddenItems
                        )
                    ) {
                        return@mapNotNull null
                    }
                    child.toGraphNode()
                }
            }
        }

        val group = GraphNode.Group(
            isRoot = groupGraph.group.parentGroupId == null,
            id = groupGraph.group.id,
            name = if (groupGraph.group.parentGroupId == null) "/" else groupGraph.group.name,
            colorIndex = groupGraph.group.colorIndex,
            expanded = mutableStateOf(groupGraph.group.parentGroupId == null),
            children = children.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
        )

        // If groups are not selectable, only include this group if it has selectable descendants
        return if (
            SelectableItemType.GROUP in selectableTypes ||
            hasSelectableDescendants(group, selectableTypes)
        ) group else null
    }

    /**
     * Recursively checks if a group has any selectable descendants in its subtree
     */
    private fun hasSelectableDescendants(
        group: GraphNode.Group,
        selectableTypes: Set<SelectableItemType>
    ): Boolean {
        return group.children.any { child ->
            when (child) {
                is GraphNode.Group -> hasSelectableDescendants(child, selectableTypes)
                is GraphNode.Tracker -> SelectableItemType.TRACKER in selectableTypes || SelectableItemType.FEATURE in selectableTypes
                is GraphNode.Function -> SelectableItemType.FUNCTION in selectableTypes || SelectableItemType.FEATURE in selectableTypes
                is GraphNode.Graph -> SelectableItemType.GRAPH in selectableTypes
            }
        }
    }

    private fun groupHidden(
        groupId: Long,
        hiddenItems: Set<HiddenItem>
    ): Boolean {
        return HiddenItem(SelectableItemType.GROUP, groupId) in hiddenItems
    }

    private fun trackerHidden(
        trackerId: Long,
        featureId: Long,
        selectableTypes: Set<SelectableItemType>,
        hiddenItems: Set<HiddenItem>,
    ): Boolean {
        val selectable = (SelectableItemType.TRACKER in selectableTypes
                || SelectableItemType.FEATURE in selectableTypes)
        val hidden = HiddenItem(SelectableItemType.TRACKER, trackerId) in hiddenItems
                || HiddenItem(SelectableItemType.FEATURE, featureId) in hiddenItems
        return !selectable || hidden
    }

    private fun graphHidden(
        graphId: Long,
        selectableTypes: Set<SelectableItemType>,
        hiddenItems: Set<HiddenItem>
    ): Boolean {
        return SelectableItemType.GRAPH !in selectableTypes
                || HiddenItem(SelectableItemType.GRAPH, graphId) in hiddenItems
    }

    private fun ModelGroupGraphItem.TrackerNode.toGraphNode() = GraphNode.Tracker(
        trackerId = tracker.id,
        featureId = tracker.featureId,
        name = tracker.name,
    )

    private fun ModelGroupGraphItem.GraphNode.toGraphNode() = GraphNode.Graph(
        id = graph.id,
        name = graph.name,
    )

    private fun ModelGroupGraphItem.FunctionNode.toGraphNode() = GraphNode.Function(
        functionId = function.id,
        featureId = function.featureId,
        name = function.name,
    )

    private fun functionHidden(
        functionId: Long,
        featureId: Long,
        selectableTypes: Set<SelectableItemType>,
        hiddenItems: Set<HiddenItem>
    ): Boolean {
        val selectable = (SelectableItemType.FUNCTION in selectableTypes
                || SelectableItemType.FEATURE in selectableTypes)
        val hidden = HiddenItem(SelectableItemType.FUNCTION, functionId) in hiddenItems
                || HiddenItem(SelectableItemType.FEATURE, featureId) in hiddenItems
        return !selectable || hidden
    }
}
