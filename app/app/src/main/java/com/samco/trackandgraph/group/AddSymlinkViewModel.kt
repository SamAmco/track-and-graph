/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.selectitemdialog.HiddenItem
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

interface AddSymlinkViewModel {
    /** The group ID for which the symlink dialog is currently shown, or null if hidden. */
    val showDialogForGroupId: StateFlow<Long?>

    /**
     * The set of items that should be disabled (visible but not selectable) in the SelectItemDialog
     * to prevent cycles. Contains all ancestor group IDs (including self) as GROUP disabled items.
     */
    val disabledItems: StateFlow<Set<HiddenItem>>

    fun show(groupId: Long)
    fun hide()
    fun createSymlink(inGroupId: Long, childId: Long, childType: GroupChildType)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddSymlinkViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel(), AddSymlinkViewModel {

    private val _showDialogForGroupId = MutableStateFlow<Long?>(null)
    override val showDialogForGroupId: StateFlow<Long?> = _showDialogForGroupId.asStateFlow()

    override val disabledItems: StateFlow<Set<HiddenItem>> = _showDialogForGroupId
        .filterNotNull()
        .map { groupId ->
            dataInteractor
                .getAncestorAndSelfGroupIds(groupId)
                .map { HiddenItem(SelectableItemType.GROUP, it) }
                .toSet()
        }
        .flowOn(io)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    override fun show(groupId: Long) {
        _showDialogForGroupId.value = groupId
    }

    override fun hide() {
        _showDialogForGroupId.value = null
    }

    override fun createSymlink(inGroupId: Long, childId: Long, childType: GroupChildType) {
        hide()
        viewModelScope.launch(io) {
            dataInteractor.createSymlink(inGroupId, childId, childType)
        }
    }
}
