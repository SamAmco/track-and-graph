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
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.ComponentType
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.MoveComponentRequest
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.selectitemdialog.HiddenItem
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.timers.TimerServiceInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MovableItemType { GROUP, GRAPH, TRACKER, FUNCTION }

data class MoveDialogConfig(
    val itemId: Long,
    val itemType: MovableItemType,
    val fromGroupId: Long?,
    val hiddenItems: Set<HiddenItem>
)

@HiltViewModel
class MoveItemViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val timerServiceInteractor: TimerServiceInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    private val _moveDialogConfig = MutableStateFlow<MoveDialogConfig?>(null)
    val moveDialogConfig: StateFlow<MoveDialogConfig?> = _moveDialogConfig.asStateFlow()

    fun showMoveGroupDialog(fromGroupId: Long, group: Group) {
        _moveDialogConfig.value = MoveDialogConfig(
            itemId = group.id,
            itemType = MovableItemType.GROUP,
            fromGroupId = fromGroupId,
            hiddenItems = setOf(HiddenItem(SelectableItemType.GROUP, group.id))
        )
    }

    fun showMoveGraphDialog(fromGroupId: Long, graphOrStat: IGraphStatViewData) {
        _moveDialogConfig.value = MoveDialogConfig(
            itemId = graphOrStat.graphOrStat.id,
            itemType = MovableItemType.GRAPH,
            fromGroupId = fromGroupId,
            hiddenItems = emptySet()
        )
    }

    fun showMoveTrackerDialog(tracker: DisplayTracker) {
        _moveDialogConfig.value = MoveDialogConfig(
            itemId = tracker.id,
            itemType = MovableItemType.TRACKER,
            fromGroupId = tracker.groupId,
            hiddenItems = emptySet()
        )
    }

    fun showMoveFunctionDialog(displayFunction: DisplayFunction) {
        _moveDialogConfig.value = MoveDialogConfig(
            itemId = displayFunction.id,
            itemType = MovableItemType.FUNCTION,
            fromGroupId = displayFunction.groupId,
            hiddenItems = emptySet()
        )
    }

    fun dismissMoveDialog() {
        _moveDialogConfig.value = null
    }

    fun moveItemToGroup(targetGroupId: Long) {
        val config = _moveDialogConfig.value ?: return

        // Dismiss dialog immediately
        _moveDialogConfig.value = null

        // Launch coroutine to move the item
        viewModelScope.launch(io) {
            try {
                when (config.itemType) {
                    MovableItemType.TRACKER -> {
                        val tracker = dataInteractor.getTrackerById(config.itemId)
                        tracker?.let {
                            val request = MoveComponentRequest(
                                type = ComponentType.TRACKER,
                                id = it.id,
                                fromGroupId = config.fromGroupId,
                                toGroupId = targetGroupId
                            )
                            dataInteractor.moveComponent(request)
                            timerServiceInteractor.requestWidgetUpdatesForFeatureId(it.featureId)
                        }
                    }

                    MovableItemType.GRAPH -> {
                        val request = MoveComponentRequest(
                            type = ComponentType.GRAPH,
                            id = config.itemId,
                            fromGroupId = config.fromGroupId,
                            toGroupId = targetGroupId
                        )
                        dataInteractor.moveComponent(request)
                    }

                    MovableItemType.GROUP -> {
                        val request = MoveComponentRequest(
                            type = ComponentType.GROUP,
                            id = config.itemId,
                            fromGroupId = config.fromGroupId,
                            toGroupId = targetGroupId
                        )
                        dataInteractor.moveComponent(request)
                    }

                    MovableItemType.FUNCTION -> {
                        val function = dataInteractor.getFunctionById(config.itemId)
                        function?.let {
                            val request = MoveComponentRequest(
                                type = ComponentType.FUNCTION,
                                id = it.id,
                                fromGroupId = config.fromGroupId,
                                toGroupId = targetGroupId
                            )
                            dataInteractor.moveComponent(request)
                        }
                    }
                }
            } catch (e: Exception) {
                // On error, do nothing - move operation failed silently
            }
        }
    }
}
