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
package com.samco.trackandgraph.functions.node_selector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.functions.repository.FunctionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class NodeSelectionUiState {
    data object Loading : NodeSelectionUiState()
    data class Ready(val items: List<LuaFunctionMetadata>) : NodeSelectionUiState()
}

interface NodeSelectionViewModel {
    val state: StateFlow<NodeSelectionUiState>
}

@HiltViewModel
class NodeSelectionViewModelImpl @Inject constructor(
    private val repository: FunctionsRepository,
) : ViewModel(), NodeSelectionViewModel {

    private val _state = MutableStateFlow<NodeSelectionUiState>(NodeSelectionUiState.Loading)
    override val state: StateFlow<NodeSelectionUiState> = _state

    init {
        viewModelScope.launch {
            val functions = repository.fetchFunctions()
            _state.value = NodeSelectionUiState.Ready(functions)
        }
    }
}
