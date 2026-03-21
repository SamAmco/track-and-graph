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
import com.samco.trackandgraph.data.database.dto.MoveComponentRequest
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.selectitemdialog.HiddenItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoveDialogConfig(
    val groupItemId: Long,
    val hiddenItems: Set<HiddenItem>
)

@HiltViewModel
class MoveItemViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    private val _moveDialogConfig = MutableStateFlow<MoveDialogConfig?>(null)
    val moveDialogConfig: StateFlow<MoveDialogConfig?> = _moveDialogConfig.asStateFlow()

    fun showMoveDialog(groupItemId: Long, hiddenItems: Set<HiddenItem> = emptySet()) {
        _moveDialogConfig.value = MoveDialogConfig(
            groupItemId = groupItemId,
            hiddenItems = hiddenItems,
        )
    }

    fun dismissMoveDialog() {
        _moveDialogConfig.value = null
    }

    fun moveItemToGroup(targetGroupId: Long) {
        val config = _moveDialogConfig.value ?: return

        // Dismiss dialog immediately
        _moveDialogConfig.value = null

        viewModelScope.launch(io) {
            dataInteractor.moveComponent(
                MoveComponentRequest(
                    groupItemId = config.groupItemId,
                    toGroupId = targetGroupId,
                )
            )
        }
    }
}
