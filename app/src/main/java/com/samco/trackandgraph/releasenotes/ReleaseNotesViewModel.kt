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
package com.samco.trackandgraph.releasenotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.localisation.TranslatedString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

interface ReleaseNotesViewModel {
    val showReleaseNotesButton: StateFlow<Boolean>
    val showReleaseNotesDialog: StateFlow<Boolean>

    val releaseNotes: StateFlow<List<ReleaseNoteViewData>>

    fun onClickReleaseNotesButton()
    fun onDismissReleaseNotesButton()
}

data class ReleaseNoteViewData(
    val version: String,
    val text: TranslatedString,
)

@HiltViewModel
class ReleaseNotesViewModelImpl @Inject constructor(
    private val repository: ReleaseNotesRepository
) : ViewModel(), ReleaseNotesViewModel {
    override val showReleaseNotesDialog = MutableStateFlow(false)

    override val releaseNotes: StateFlow<List<ReleaseNoteViewData>> = channelFlow {
        send(repository.getNewReleaseNotes().map { it.toViewData() })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    override val showReleaseNotesButton: StateFlow<Boolean> = merge(
        releaseNotes.map { it.isNotEmpty() },
        showReleaseNotesDialog.filter { it }.map { false }
    ).stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun onClickReleaseNotesButton() {
        showReleaseNotesDialog.update { true }
    }

    override fun onDismissReleaseNotesButton() {
        showReleaseNotesDialog.update { false }
    }

    private fun ReleaseNote.toViewData() = ReleaseNoteViewData(
        version = version.toString(),
        text = text
    )
}