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
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.functions.repository.FunctionsRepository
import com.samco.trackandgraph.functions.repository.SignatureVerificationException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

enum class FetchError {
    VERIFICATION_FAILURE,
    NETWORK_FAILURE
}

sealed class NodeSelectionUiState {
    data object Loading : NodeSelectionUiState()
    data class Ready(
        val allFunctions: List<LuaFunctionMetadata>,
        val displayedFunctions: List<LuaFunctionMetadata>,
        val selectedCategory: String?,
        val allCategories: Map<String, TranslatedString>
    ) : NodeSelectionUiState()

    data class Error(val error: FetchError) : NodeSelectionUiState()
}

interface NodeSelectionViewModel {
    val state: StateFlow<NodeSelectionUiState>

    fun selectCategory(categoryId: String?)
    fun clearSelection()
    fun retry()
}

@HiltViewModel
class NodeSelectionViewModelImpl @Inject constructor(
    private val repository: FunctionsRepository,
) : ViewModel(), NodeSelectionViewModel {

    private val _state = MutableStateFlow<NodeSelectionUiState>(NodeSelectionUiState.Loading)
    override val state: StateFlow<NodeSelectionUiState> = _state

    private var allFunctions: List<LuaFunctionMetadata> = emptyList()
    private var selectedCategory: String? = null
    private var allCategories: Map<String, TranslatedString> = emptyMap()

    init {
        fetchFunctions()
    }

    override fun selectCategory(categoryId: String?) {
        selectedCategory = categoryId
        updateReadyState()
    }

    override fun clearSelection() {
        selectedCategory = null
        updateReadyState()
    }

    override fun retry() {
        _state.value = NodeSelectionUiState.Loading
        fetchFunctions()
    }

    private fun updateReadyState() {
        if (_state.value !is NodeSelectionUiState.Ready) return

        val displayedFunctions = if (selectedCategory == null) {
            allFunctions
        } else {
            allFunctions.filter { it.categories.containsKey(selectedCategory) }
        }

        _state.value = NodeSelectionUiState.Ready(
            allFunctions = allFunctions,
            displayedFunctions = displayedFunctions,
            selectedCategory = selectedCategory,
            allCategories = allCategories
        )
    }

    private fun fetchFunctions() {
        viewModelScope.launch {
            try {
                val functions = repository.fetchFunctions()
                allFunctions = functions

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
                allCategories = categoriesMap

                val displayedFunctions = if (selectedCategory == null) {
                    functions
                } else {
                    functions.filter { function ->
                        function.categories.containsKey(selectedCategory)
                    }
                }

                _state.value = NodeSelectionUiState.Ready(
                    allFunctions = functions,
                    displayedFunctions = displayedFunctions,
                    selectedCategory = selectedCategory,
                    allCategories = categoriesMap
                )
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
