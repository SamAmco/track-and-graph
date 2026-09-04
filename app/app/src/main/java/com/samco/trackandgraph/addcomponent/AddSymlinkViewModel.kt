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
package com.samco.trackandgraph.addcomponent

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
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface AddSymlinkViewModel {
    /** Ancestor groups are disabled because selecting one would create a cycle. */
    val disabledItems: StateFlow<Set<HiddenItem>>
    val saving: StateFlow<Boolean>

    fun initialize(groupId: Long)

    fun createSymlink(
        inGroupId: Long,
        childId: Long,
        childType: GroupChildType,
        onComplete: () -> Unit,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddSymlinkViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
) : ViewModel(), AddSymlinkViewModel {

    private val groupId = MutableStateFlow<Long?>(null)
    private val _saving = MutableStateFlow(false)
    override val saving: StateFlow<Boolean> = _saving.asStateFlow()

    override val disabledItems: StateFlow<Set<HiddenItem>> = groupId
        .filterNotNull()
        .map { id ->
            dataInteractor
                .getAncestorAndSelfGroupIds(id)
                .map { HiddenItem(SelectableItemType.GROUP, it) }
                .toSet()
        }
        .flowOn(io)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    override fun initialize(groupId: Long) {
        this.groupId.value = groupId
    }

    override fun createSymlink(
        inGroupId: Long,
        childId: Long,
        childType: GroupChildType,
        onComplete: () -> Unit,
    ) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                withContext(io) {
                    dataInteractor.createSymlink(inGroupId, childId, childType)
                }
            } finally {
                _saving.value = false
            }
            onComplete()
        }
    }
}
