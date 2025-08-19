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
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.di.IODispatcher
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

enum class MovableItemType { GROUP, GRAPH, TRACKER, }

data class MoveDialogConfig(
    val itemId: Long,
    val itemType: MovableItemType,
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

    fun showMoveGroupDialog(group: Group) {
        _moveDialogConfig.value = MoveDialogConfig(
            itemId = group.id,
            itemType = MovableItemType.GROUP,
            hiddenItems = setOf(HiddenItem(SelectableItemType.GROUP, group.id))
        )
    }

    fun showMoveGraphDialog(graphOrStat: IGraphStatViewData) {
        _moveDialogConfig.value = MoveDialogConfig(
            itemId = graphOrStat.graphOrStat.id,
            itemType = MovableItemType.GRAPH,
            hiddenItems = emptySet()
        )
    }

    fun showMoveTrackerDialog(tracker: DisplayTracker) {
        _moveDialogConfig.value = MoveDialogConfig(
            itemId = tracker.id,
            itemType = MovableItemType.TRACKER,
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
                        tracker?.copy(groupId = targetGroupId)?.let {
                            dataInteractor.updateTracker(it)
                            timerServiceInteractor.requestWidgetUpdatesForFeatureId(it.featureId)
                        }
                    }

                    MovableItemType.GRAPH -> {
                        val graphStat = dataInteractor.getGraphStatById(config.itemId)
                        graphStat.copy(groupId = targetGroupId).let {
                            dataInteractor.updateGraphOrStat(it)
                        }
                    }

                    MovableItemType.GROUP -> {
                        val group = dataInteractor.getGroupById(config.itemId)
                        group.copy(parentGroupId = targetGroupId).let {
                            dataInteractor.updateGroup(it)
                        }
                    }
                }
            } catch (e: Exception) {
                // On error, do nothing - move operation failed silently
            }
        }
    }
}
