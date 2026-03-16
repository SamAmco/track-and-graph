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
package com.samco.trackandgraph.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.util.ComponentPathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SymlinksDialogData(
    val componentName: String,
    val paths: List<String>,
)

@HiltViewModel
class SymlinksDialogViewModel @Inject constructor(
    private val dataInteractor: DataInteractor
) : ViewModel() {

    private val _dialogData = MutableStateFlow<SymlinksDialogData?>(null)
    val dialogData: StateFlow<SymlinksDialogData?> = _dialogData.asStateFlow()

    fun showSymlinks(componentId: Long, componentType: GroupChildType, componentName: String) {
        viewModelScope.launch {
            val groupGraph = dataInteractor.getGroupGraphSync()
            val pathProvider = ComponentPathProvider(groupGraph)

            val paths = when (componentType) {
                GroupChildType.GROUP -> pathProvider.getAllPathsForGroup(componentId)
                GroupChildType.TRACKER -> pathProvider.getAllPathsForTracker(componentId)
                GroupChildType.FUNCTION -> pathProvider.getAllPathsForFunction(componentId)
                GroupChildType.GRAPH -> pathProvider.getAllPathsForGraph(componentId)
                GroupChildType.REMINDER -> emptyList()
            }

            _dialogData.value = SymlinksDialogData(
                componentName = componentName,
                paths = paths,
            )
        }
    }

    fun dismiss() {
        _dialogData.value = null
    }
}
