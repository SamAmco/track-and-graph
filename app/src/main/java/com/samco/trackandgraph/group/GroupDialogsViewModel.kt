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
import com.samco.trackandgraph.base.database.dto.DisplayTracker
import com.samco.trackandgraph.base.database.dto.Group
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Simple DTO for delete confirmation dialogs containing only the essential data needed.
 */
data class DeleteItemDto(
    val id: Long,
    val type: DeleteType
)

/**
 * Type of item being deleted to determine which delete operation to perform.
 */
enum class DeleteType {
    GROUP, GRAPH_STAT, TRACKER
}

/**
 * ViewModel responsible for managing dialog visibility states in GroupFragment.
 * Provides clean separation of concerns from GroupViewModel's business logic.
 */
@HiltViewModel
class GroupDialogsViewModel @Inject constructor() : ViewModel() {

    // Import Features Dialog
    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    fun showImportDialog() {
        _showImportDialog.value = true
    }

    fun hideImportDialog() {
        _showImportDialog.value = false
    }

    // Export Features Dialog - ready for future implementation
    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    fun showExportDialog() {
        _showExportDialog.value = true
    }

    fun hideExportDialog() {
        _showExportDialog.value = false
    }

    // Feature Description Dialog
    private val _featureForDescriptionDialog = MutableStateFlow<DisplayTracker?>(null)
    val featureForDescriptionDialog: StateFlow<DisplayTracker?> = _featureForDescriptionDialog.asStateFlow()

    fun showFeatureDescriptionDialog(feature: DisplayTracker) {
        _featureForDescriptionDialog.value = feature
    }

    fun hideFeatureDescriptionDialog() {
        _featureForDescriptionDialog.value = null
    }

    // Delete Confirmation Dialog - unified for all delete operations
    private val _itemForDeletion = MutableStateFlow<DeleteItemDto?>(null)
    val itemForDeletion: StateFlow<DeleteItemDto?> = _itemForDeletion.asStateFlow()

    fun showDeleteGroupDialog(group: Group) {
        _itemForDeletion.value = DeleteItemDto(
            id = group.id,
            type = DeleteType.GROUP
        )
    }

    fun showDeleteGraphStatDialog(graphStat: IGraphStatViewData) {
        _itemForDeletion.value = DeleteItemDto(
            id = graphStat.graphOrStat.id,
            type = DeleteType.GRAPH_STAT
        )
    }

    fun showDeleteTrackerDialog(tracker: DisplayTracker) {
        _itemForDeletion.value = DeleteItemDto(
            id = tracker.featureId,
            type = DeleteType.TRACKER
        )
    }

    fun hideDeleteDialog() {
        _itemForDeletion.value = null
    }

    // No Trackers Warning Dialog
    private val _showNoTrackersDialog = MutableStateFlow(false)
    val showNoTrackersDialog: StateFlow<Boolean> = _showNoTrackersDialog.asStateFlow()

    fun showNoTrackersDialog() {
        _showNoTrackersDialog.value = true
    }

    fun hideNoTrackersDialog() {
        _showNoTrackersDialog.value = false
    }
}
