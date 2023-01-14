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

package com.samco.trackandgraph.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.*
import com.samco.trackandgraph.base.database.dto.GlobalNote
import com.samco.trackandgraph.base.database.odtFromString
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

interface GlobalNoteInputViewModel {
    val note: TextFieldValue
    val dateTime: LiveData<OffsetDateTime>
    val dismiss: LiveData<Boolean>
    val updateMode: LiveData<Boolean>
    val addButtonEnabled: LiveData<Boolean>

    fun init(timestampStr: String?)
    fun updateNoteText(text: TextFieldValue)
    fun updateTimeStamp(timeStamp: OffsetDateTime)
    fun onAddClicked()
    fun onCancelClicked()
}

@HiltViewModel
class GlobalNoteInputViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel(), GlobalNoteInputViewModel {

    private val onCancelClicked = MutableSharedFlow<Unit>()

    private var foundNote: GlobalNote? = null

    override var note by mutableStateOf(TextFieldValue())
    override val dateTime = MutableLiveData<OffsetDateTime>()

    override val updateMode = MutableLiveData(false)

    override val addButtonEnabled = snapshotFlow { note }
        .map { it.text.isNotBlank() }
        .asLiveData(viewModelScope.coroutineContext)

    private val addCompleteEvents = MutableSharedFlow<Unit>()

    override val dismiss = merge(onCancelClicked, addCompleteEvents)
        .map { true }
        .asLiveData(viewModelScope.coroutineContext)

    private var initialized = false

    override fun init(timestampStr: String?) {
        if (initialized) return
        initialized = true

        viewModelScope.launch(io) {
            val globalNote = timestampStr?.let {
                odtFromString(it)?.let { odt ->
                    dataInteractor.getGlobalNoteByTimeSync(odt)
                }
            }
            withContext(ui) {
                globalNote?.let { initFromGlobalNote(it) }
            }
        }
    }

    private fun initFromGlobalNote(note: GlobalNote) {
        foundNote = note
        this.note = TextFieldValue(note.note, TextRange(note.note.length))
        this.dateTime.value = note.timestamp
        this.updateMode.value = true
    }

    override fun updateNoteText(text: TextFieldValue) {
        note = text
    }

    override fun updateTimeStamp(timeStamp: OffsetDateTime) {
        dateTime.value = timeStamp
    }

    override fun onAddClicked() {
        viewModelScope.launch(io) {
            foundNote?.let { dataInteractor.deleteGlobalNote(it) }
            dataInteractor.insertGlobalNote(
                GlobalNote(
                    note = note.text,
                    timestamp = dateTime.value ?: OffsetDateTime.now()
                )
            )

            withContext(ui) { addCompleteEvents.emit(Unit) }
        }
    }

    override fun onCancelClicked() {
        viewModelScope.launch { onCancelClicked.emit(Unit) }
    }
}