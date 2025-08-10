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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.selectitemdialog.HiddenItem
import com.samco.trackandgraph.selectitemdialog.HiddenItemType
import com.samco.trackandgraph.util.GroupPathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SelectItemDialogState { LOADING, READY }

enum class SelectableItemType { GROUP }

sealed class SelectableItem {
    data class Group(
        val id: Long,
        val path: String,
        val colorIndex: Int,
    ) : SelectableItem()
}

@HiltViewModel
class SelectItemDialogViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(SelectItemDialogState.LOADING)
    val state: StateFlow<SelectItemDialogState> = _state.asStateFlow()

    private val _items = MutableStateFlow<List<SelectableItem>>(emptyList())
    val items: StateFlow<List<SelectableItem>> = _items.asStateFlow()

    private var initialized = false

    fun init(
        allowedTypes: Set<SelectableItemType> = setOf(SelectableItemType.GROUP),
        hiddenItems: Set<HiddenItem>,
    ) {
        if (initialized) return
        initialized = true

        viewModelScope.launch(io) {
            _state.value = SelectItemDialogState.LOADING

            //TODO we will generalize this later to handle allowed types and allow selecting trackers and/or graphs

            val allGroups = dataInteractor.getAllGroupsSync()
            
            // Extract hidden group IDs from the hidden items set
            val hiddenGroupIds = hiddenItems
                .filter { it.type == HiddenItemType.GROUP }
                .map { it.id }
                .toSet()

            _items.value = GroupPathProvider(allGroups, hiddenGroupIds)
                .filteredSortedGroups
                .map { groupInfo ->
                    SelectableItem.Group(
                        id = groupInfo.group.id,
                        path = groupInfo.path,
                        colorIndex = groupInfo.group.colorIndex,
                    )
                }

            _state.value = SelectItemDialogState.READY
        }
    }

    fun reset() {
        initialized = false
        _state.value = SelectItemDialogState.LOADING
        _items.value = emptyList()
    }
}
