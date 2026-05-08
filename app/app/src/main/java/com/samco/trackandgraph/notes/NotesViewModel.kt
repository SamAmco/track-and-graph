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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.database.dto.DisplayNote
import com.samco.trackandgraph.data.database.dto.GlobalNote
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.storage.PrefsPersistenceProvider
import com.samco.trackandgraph.ui.ui.Datable
import com.samco.trackandgraph.ui.ui.DateDisplayResolution
import com.samco.trackandgraph.ui.ui.DateScrollData
import com.samco.trackandgraph.util.FeaturePathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

data class NoteInfo(
    override val date: OffsetDateTime,
    val trackerId: Long?,
    val featureId: Long?,
    val featureName: String?,
    val featurePath: String,
    val note: String
) : Datable

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    prefsPersistenceProvider: PrefsPersistenceProvider,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel() {
    private val _selectedNoteForDialog = MutableStateFlow<NoteInfo?>(null)
    val selectedNoteForDialog: StateFlow<NoteInfo?> = _selectedNoteForDialog.asStateFlow()

    private val dataStore = prefsPersistenceProvider.getDataStore(DATA_STORE_NAME)

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()
    val searchQuery = TextFieldState()
    private val queryText = snapshotFlow { searchQuery.text.toString() }

    val showGlobalNotes: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[SHOW_GLOBAL_NOTES_KEY] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showDataPointNotes: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[SHOW_DATA_POINT_NOTES_KEY] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val notesFlow = dataInteractor.getAllDisplayNotes()
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private val featureNameProvider = flow {
        emit(FeaturePathProvider(dataInteractor.getGroupGraphSync()))
    }.flowOn(io)

    val dateScrollData: StateFlow<DateScrollData<NoteInfo>?> = combine(
        notesFlow,
        featureNameProvider,
        showGlobalNotes,
        showDataPointNotes,
        queryText
    ) { list, featurePathProvider, showGlobalNotes, showDataPointNotes, query ->
        if (list.isEmpty()) return@combine null

        val filteredList = list.filter { note ->
            when {
                note.trackerId == null -> showGlobalNotes
                else -> showDataPointNotes
            }
        }.map { it.asNoteInfo(featurePathProvider) }
            .filter { note -> query.isBlank() || note.matchesSearchQuery(query) }

        val range = Duration.between(list.last().timestamp, list.first().timestamp).abs()

        val dateDisplayResolution = when {
            range.toDays() > 365 -> DateDisplayResolution.MONTH_YEAR
            else -> DateDisplayResolution.MONTH_DAY
        }

        DateScrollData(
            items = filteredList,
            dateDisplayResolution = dateDisplayResolution
        )
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun showSearch() {
        _isSearchVisible.value = true
    }

    fun hideSearch() {
        _isSearchVisible.value = false
        searchQuery.clearText()
    }

    fun onNoteClicked(note: NoteInfo) {
        _selectedNoteForDialog.value = note
    }

    fun onDialogDismissed() {
        _selectedNoteForDialog.value = null
    }

    fun toggleShowGlobalNotes() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SHOW_GLOBAL_NOTES_KEY] = !showGlobalNotes.value
            }
        }
    }

    fun toggleShowDataPointNotes() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SHOW_DATA_POINT_NOTES_KEY] = !showDataPointNotes.value
            }
        }
    }

    fun deleteNote(note: NoteInfo) = viewModelScope.launch(io) {
        note.trackerId?.let {
            dataInteractor.removeNote(note.date, it)
        } ?: run {
            dataInteractor.deleteGlobalNote(GlobalNote(note.date, note.note))
        }
    }

    private fun DisplayNote.asNoteInfo(
        featurePathProvider: FeaturePathProvider,
    ) = NoteInfo(
        date = timestamp,
        trackerId = trackerId,
        featureId = featureId,
        featureName = featureName,
        featurePath = featureId
            ?.let { featurePathProvider.getPathForFeature(it) }
            ?: featureName
            ?: "",
        note = note
    )

    private fun NoteInfo.matchesSearchQuery(query: String): Boolean =
        searchTargets().any { it.contains(query, ignoreCase = true) }

    private fun NoteInfo.searchTargets(): List<String> = listOfNotNull(
        note,
        featureName,
        featurePath,
    )

    companion object {
        private const val DATA_STORE_NAME = "notes_screen"
        private val SHOW_GLOBAL_NOTES_KEY = booleanPreferencesKey("show_global_notes")
        private val SHOW_DATA_POINT_NOTES_KEY = booleanPreferencesKey("show_data_point_notes")
    }
}
