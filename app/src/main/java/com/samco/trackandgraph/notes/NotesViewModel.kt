package com.samco.trackandgraph.notes

import androidx.lifecycle.*
import com.samco.trackandgraph.base.database.dto.DisplayNote
import com.samco.trackandgraph.base.database.dto.GlobalNote
import com.samco.trackandgraph.base.database.dto.NoteType
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.ui.FeaturePathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel() {
    private val notesFlow = dataInteractor.getAllDisplayNotes()
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val notes: LiveData<List<DisplayNote>> = notesFlow
        .asLiveData(viewModelScope.coroutineContext)

    val featureNameProvider: LiveData<FeaturePathProvider> = flow {
        val trackers = dataInteractor.getAllTrackersSync()
        val groups = dataInteractor.getAllGroupsSync()
        emit(FeaturePathProvider(trackers, groups))
    }.flowOn(io)
        .asLiveData(viewModelScope.coroutineContext)

    //Emits true and then false any time the top note in the list is changed so that the
    // view knows to scroll back to the top of the list
    val onNoteInsertedTop: LiveData<Boolean> =
        notesFlow.scan(Pair<DisplayNote?, DisplayNote?>(null, null)) { acc, value ->
            //Every time there's a new list of notes get the top note and save it along with
            // the top note from the last list
            return@scan Pair(acc.second, value.firstOrNull())
        }.flatMapLatest {
            //If the top note from the last list is not the same as the top note from this list
            // emit true/false to scroll to the top
            return@flatMapLatest if (it.first != it.second) flow {
                emit(true)
                emit(false)
            } else emptyFlow()
        }.asLiveData(viewModelScope.coroutineContext)


    fun deleteNote(note: DisplayNote) = viewModelScope.launch(io) {
        when (note.noteType) {
            NoteType.DATA_POINT -> note.trackerId?.let {
                dataInteractor.removeNote(note.timestamp, it)
            }
            NoteType.GLOBAL_NOTE -> {
                val globalNote = GlobalNote(note.timestamp, note.note)
                dataInteractor.deleteGlobalNote(globalNote)
            }
        }
    }
}