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
package com.samco.trackandgraph.functions

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.model.DataInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

interface FunctionsViewModel {
    val functionName: TextFieldValue
    val functionDescription: TextFieldValue
    val errorText: StateFlow<Int?>
    val loading: StateFlow<Boolean>
    val complete: ReceiveChannel<Unit>

    fun onFunctionNameChanged(name: TextFieldValue)
    fun onFunctionDescriptionChanged(description: TextFieldValue)
    fun onCreateClicked()
    fun init(groupId: Long, functionId: Long? = null)
}

@HiltViewModel
class FunctionsViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor
) : ViewModel(), FunctionsViewModel {

    private var groupId: Long = -1L
    private var functionId: Long? = null
    private var existingFunction: Function? = null

    private val _functionName = mutableStateOf(TextFieldValue(""))
    override val functionName: TextFieldValue get() = _functionName.value

    private val _functionDescription = mutableStateOf(TextFieldValue(""))
    override val functionDescription: TextFieldValue get() = _functionDescription.value

    private val _errorText = MutableStateFlow<Int?>(null)
    override val errorText: StateFlow<Int?> = _errorText.asStateFlow()

    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()

    override val complete = Channel<Unit>()

    override fun onFunctionNameChanged(name: TextFieldValue) {
        _functionName.value = name
        _errorText.value = null
        validate()
    }

    override fun onFunctionDescriptionChanged(description: TextFieldValue) {
        _functionDescription.value = description
        validate()
    }

    private fun validate() {
        _errorText.value = when {
            _functionName.value.text.isBlank() -> R.string.function_name_empty
            else -> null
        }
    }

    override fun onCreateClicked() {
        viewModelScope.launch {
            val existing = existingFunction
            if (existing != null) {
                // Update existing function
                val updatedFunction = existing.copy(
                    name = _functionName.value.text,
                    description = _functionDescription.value.text
                )
                dataInteractor.updateFunction(updatedFunction)
            } else {
                // Create new function
                val function = Function(
                    name = _functionName.value.text,
                    groupId = groupId,
                    description = _functionDescription.value.text,
                    functionGraph = FunctionGraph(), // Empty for now
                    inputFeatures = emptyList() // Empty for now
                )
                dataInteractor.insertFunction(function)
            }
            complete.trySend(Unit)
        }
    }

    override fun init(groupId: Long, functionId: Long?) {
        if (this.groupId == groupId && this.functionId == functionId) return

        this.groupId = groupId
        this.functionId = functionId

        if (functionId != null) {
            // Edit mode - load existing function
            _loading.value = true
            viewModelScope.launch {
                try {
                    val function = dataInteractor.getFunctionById(functionId)
                    if (function != null) {
                        existingFunction = function
                        _functionName.value = TextFieldValue(function.name)
                        _functionDescription.value = TextFieldValue(function.description)
                    }
                } finally {
                    _loading.value = false
                }
            }
        } else {
            // Create mode - reset fields
            existingFunction = null
            _functionName.value = TextFieldValue("")
            _functionDescription.value = TextFieldValue("")
        }
        _errorText.value = null
    }
}
