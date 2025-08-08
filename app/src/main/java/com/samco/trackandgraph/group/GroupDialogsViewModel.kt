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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

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
}
