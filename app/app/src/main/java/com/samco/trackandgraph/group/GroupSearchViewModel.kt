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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface GroupSearchViewModel {
    val isSearchVisible: StateFlow<Boolean>
    fun showSearch()
    fun hideSearch()
}

@HiltViewModel
class GroupSearchViewModelImpl @Inject constructor() : ViewModel(), GroupSearchViewModel {

    private val _isSearchVisible = MutableStateFlow(false)
    override val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    override fun showSearch() {
        _isSearchVisible.value = true
    }

    override fun hideSearch() {
        _isSearchVisible.value = false
    }
}
