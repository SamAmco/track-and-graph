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
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.samco.trackandgraph.notes

import androidx.lifecycle.*
import com.samco.trackandgraph.base.database.dto.GlobalNote
import com.samco.trackandgraph.base.database.odtFromString
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

interface GlobalNoteInputViewModel {
    val note: LiveData<GlobalNote>
    val dismiss: LiveData<Boolean>
    val updateMode: LiveData<Boolean>
    val addButtonEnabled: LiveData<Boolean>

    fun init(timestampStr: String?)
    fun updateNoteText(text: String)
    fun updateTimeStamp(timeStamp: OffsetDateTime)
    fun onAddClicked()
    fun onCancelClicked()
}

@HiltViewModel
class GlobalNoteInputViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel(), GlobalNoteInputViewModel {

    private val onInitFromTimestamp = MutableSharedFlow<OffsetDateTime>()
    private val onUpdateText = MutableSharedFlow<String?>()
    private val onUpdateTimeStamp = MutableSharedFlow<OffsetDateTime?>()
    private val onCancelClicked = MutableSharedFlow<Unit>()
    private val onAddClicked = MutableSharedFlow<Unit>()

    private val foundNote = onInitFromTimestamp
        .map { dataInteractor.getGlobalNoteByTimeSync(it) }
        .flowOn(io)
        .filterNotNull()
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    private val noteFlow = foundNote
        .onStart { emit(GlobalNote()) }
        .combine(onUpdateText.onStart { emit(null) }) { note, text ->
            text?.let { note.copy(note = it) } ?: note
        }
        .combine(onUpdateTimeStamp.onStart { emit(null) }) { note, timeStamp ->
            timeStamp?.let { note.copy(timestamp = it) } ?: note
        }
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    override val note = noteFlow.asLiveData(viewModelScope.coroutineContext)

    private val updateModeFlow = foundNote
        .map { true }
        .onStart { emit(false) }
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    override val updateMode = updateModeFlow.asLiveData(viewModelScope.coroutineContext)

    override val addButtonEnabled = noteFlow
        .map { it.note.isNotBlank() }
        .asLiveData(viewModelScope.coroutineContext)

    private val addCompleteEvents = onAddClicked
        .flatMapLatest { noteFlow }
        .combine(updateModeFlow) { note, updateMode ->
            if (updateMode) {
                foundNote.firstOrNull()?.let {
                    dataInteractor.deleteGlobalNote(it)
                }
            }
            dataInteractor.insertGlobalNote(note)
        }
        .flowOn(io)

    override val dismiss = merge(onCancelClicked, addCompleteEvents)
        .map { true }
        .asLiveData(viewModelScope.coroutineContext)

    private var initialized = false

    override fun init(timestampStr: String?) {
        if (initialized) return
        initialized = true

        viewModelScope.launch {
            timestampStr?.let {
                odtFromString(it)?.let { odt ->
                    onInitFromTimestamp.emit(odt)
                }
            }
        }
    }

    override fun updateNoteText(text: String) {
        viewModelScope.launch { onUpdateText.emit(text) }
    }

    override fun updateTimeStamp(timeStamp: OffsetDateTime) {
        viewModelScope.launch { onUpdateTimeStamp.emit(timeStamp) }
    }

    override fun onAddClicked() {
        viewModelScope.launch { onAddClicked.emit(Unit) }
    }

    override fun onCancelClicked() {
        viewModelScope.launch { onCancelClicked.emit(Unit) }
    }
}