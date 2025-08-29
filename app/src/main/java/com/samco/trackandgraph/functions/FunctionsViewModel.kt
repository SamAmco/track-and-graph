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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface FunctionsViewModel {
    val functionName: TextFieldValue
    val functionDescription: TextFieldValue
    val scriptText: TextFieldValue
    val errorText: StateFlow<Int?>
    val complete: ReceiveChannel<Unit>

    fun onFunctionNameChanged(name: TextFieldValue)
    fun onFunctionDescriptionChanged(description: TextFieldValue)
    fun onScriptTextChanged(script: TextFieldValue)
    fun onUpdateScriptFromClipboard(clipboardText: String)
    fun onCreateClicked()
    fun init(groupId: Long)
}

@HiltViewModel
class FunctionsViewModelImpl @Inject constructor() : ViewModel(), FunctionsViewModel {

    private var groupId: Long = -1L

    private val _functionName = mutableStateOf(TextFieldValue(""))
    override val functionName: TextFieldValue get() = _functionName.value

    private val _functionDescription = mutableStateOf(TextFieldValue(""))
    override val functionDescription: TextFieldValue get() = _functionDescription.value

    private val _scriptText = mutableStateOf(TextFieldValue(""))
    override val scriptText: TextFieldValue get() = _scriptText.value

    private val _errorText = MutableStateFlow<Int?>(null)
    override val errorText: StateFlow<Int?> = _errorText.asStateFlow()

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

    override fun onScriptTextChanged(script: TextFieldValue) {
        _scriptText.value = script
        validate()
    }

    override fun onUpdateScriptFromClipboard(clipboardText: String) {
        _scriptText.value = TextFieldValue(clipboardText)
        validate()
    }

    private fun validate() {
        //TODO implement validation
    }

    override fun onCreateClicked() {
        // TODO: Implement actual function creation logic
        // For now, just complete the flow
        complete.trySend(Unit)
    }

    override fun init(groupId: Long) {
        if (this.groupId == groupId) return

        this.groupId = groupId

        _functionName.value = TextFieldValue("")
        _functionDescription.value = TextFieldValue("")
        _scriptText.value = TextFieldValue("")
        _errorText.value = null
    }
}
