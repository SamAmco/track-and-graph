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
package com.samco.trackandgraph.importexport

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.storage.PrefsPersistenceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

interface ImportExportDialogViewModel {
    val selectedTab: StateFlow<Int>
    fun initialize(groupId: Long)
    fun setSelectedTab(tab: Int)
}

@HiltViewModel
class ImportExportDialogViewModelImpl @Inject constructor(
    private val prefsPersistenceProvider: PrefsPersistenceProvider
) : ViewModel(), ImportExportDialogViewModel {

    private val dataStore = prefsPersistenceProvider.getDataStore(DATA_STORE_NAME)

    private val _selectedTab = MutableStateFlow(0)
    override val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private var currentGroupId: Long? = null

    override fun initialize(groupId: Long) {
        if (currentGroupId == groupId) return
        currentGroupId = groupId
        viewModelScope.launch {
            val key = intPreferencesKey(getKeyForGroup(groupId))
            val savedTab = dataStore.data.firstOrNull()?.get(key) ?: 0
            _selectedTab.value = savedTab
        }
    }

    override fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
        val groupId = currentGroupId ?: return
        viewModelScope.launch {
            val key = intPreferencesKey(getKeyForGroup(groupId))
            dataStore.edit { preferences ->
                preferences[key] = tab
            }
        }
    }

    private fun getKeyForGroup(groupId: Long): String = "last_tab_group_$groupId"

    companion object {
        private const val DATA_STORE_NAME = "import_export_dialog"
    }
}
