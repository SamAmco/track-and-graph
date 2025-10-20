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
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import com.samco.trackandgraph.functions.repository.FunctionsRepository
import com.samco.trackandgraph.functions.repository.SignatureVerificationException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

enum class FetchError {
    VERIFICATION_FAILURE,
    NETWORK_FAILURE
}

sealed class NodeSelectionUiState {
    data object Loading : NodeSelectionUiState()
    data class Ready(val items: List<LuaFunctionMetadata>) : NodeSelectionUiState()
    data class Error(val error: FetchError) : NodeSelectionUiState()
}

interface NodeSelectionViewModel {
    val state: StateFlow<NodeSelectionUiState>
    val displayedFunctions: StateFlow<List<LuaFunctionMetadata>>
    val selectedCategory: StateFlow<String?>
    val allCategories: StateFlow<Map<String, TranslatedString>>

    fun selectCategory(categoryId: String?)
    fun retry()
}

@HiltViewModel
class NodeSelectionViewModelImpl @Inject constructor(
    private val repository: FunctionsRepository,
) : ViewModel(), NodeSelectionViewModel {

    private val _state = MutableStateFlow<NodeSelectionUiState>(NodeSelectionUiState.Loading)
    override val state: StateFlow<NodeSelectionUiState> = _state

    private val _allFunctions = MutableStateFlow<List<LuaFunctionMetadata>>(emptyList())
    private val _selectedCategory = MutableStateFlow<String?>(null)
    override val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _allCategories = MutableStateFlow<Map<String, TranslatedString>>(emptyMap())
    override val allCategories: StateFlow<Map<String, TranslatedString>> = _allCategories

    override val displayedFunctions: StateFlow<List<LuaFunctionMetadata>> =
        combine(_allFunctions, _selectedCategory) { functions, category ->
            if (category == null) {
                functions
            } else {
                functions.filter { function ->
                    function.categories.containsKey(category)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        fetchFunctions()
    }

    override fun selectCategory(categoryId: String?) {
        _selectedCategory.value = categoryId
    }

    override fun retry() {
        _state.value = NodeSelectionUiState.Loading
        fetchFunctions()
    }

    private fun fetchFunctions() {
        viewModelScope.launch {
            try {
                val functions = repository.fetchFunctions()
                _allFunctions.value = functions
                
                // Build the categories map from all functions
                val categoriesMap = mutableMapOf<String, TranslatedString>()
                functions.forEach { function ->
                    function.categories.forEach { (id, translatedString) ->
                        // Assume translations for the same key are always the same
                        if (!categoriesMap.containsKey(id)) {
                            categoriesMap[id] = translatedString
                        }
                    }
                }
                _allCategories.value = categoriesMap
                
                _state.value = NodeSelectionUiState.Ready(functions)
            } catch (e: SignatureVerificationException) {
                Timber.e(e)
                _state.value = NodeSelectionUiState.Error(FetchError.VERIFICATION_FAILURE)
            } catch (t: Throwable) {
                Timber.e(t)
                _state.value = NodeSelectionUiState.Error(FetchError.NETWORK_FAILURE)
            }
        }
    }
}
