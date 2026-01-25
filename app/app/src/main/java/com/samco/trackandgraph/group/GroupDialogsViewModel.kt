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
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Simple DTO for delete confirmation dialogs containing only the essential data needed.
 *
 * @param groupItemId the GroupItem placement to act on
 * @param type determines which confirmation text to show
 * @param unique whether the item exists in only one group. If false, the user gets the
 *   option to remove just this placement or delete everywhere.
 */
data class DeleteItemDto(
    val groupItemId: Long,
    val type: DeleteType,
    val unique: Boolean = true,
)

/**
 * Type of item being deleted to determine which delete operation to perform.
 */
enum class DeleteType {
    GROUP, GRAPH_STAT, TRACKER, FUNCTION
}

/**
 * ViewModel responsible for managing dialog visibility states in GroupFragment.
 * Provides clean separation of concerns from GroupViewModel's business logic.
 */
@HiltViewModel
class GroupDialogsViewModel @Inject constructor() : ViewModel() {

    // Import/Export Dialog
    private val _showImportExportDialog = MutableStateFlow(false)
    val showImportExportDialog: StateFlow<Boolean> = _showImportExportDialog.asStateFlow()

    fun showImportExportDialog() {
        _showImportExportDialog.value = true
    }

    fun hideImportExportDialog() {
        _showImportExportDialog.value = false
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

    fun showDeleteDialog(groupItemId: Long, type: DeleteType, unique: Boolean) {
        _itemForDeletion.value = DeleteItemDto(
            groupItemId = groupItemId,
            type = type,
            unique = unique,
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

    // No Trackers for Functions Warning Dialog
    private val _showNoTrackersFunctionsDialog = MutableStateFlow(false)
    val showNoTrackersFunctionsDialog: StateFlow<Boolean> = _showNoTrackersFunctionsDialog.asStateFlow()

    fun showNoTrackersFunctionsDialog() {
        _showNoTrackersFunctionsDialog.value = true
    }

    fun hideNoTrackersFunctionsDialog() {
        _showNoTrackersFunctionsDialog.value = false
    }
}
