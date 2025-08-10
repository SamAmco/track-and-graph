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

package com.samco.trackandgraph.widgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.util.FeaturePathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TrackWidgetConfigureDialogViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel() {

    private val _featureMap = MutableStateFlow<Map<Long, String>?>(null)
    val featureMap: StateFlow<Map<Long, String>?> get() = _featureMap.asStateFlow()

    private val _onCreateWidget = Channel<Long>()
    val onCreateWidget: ReceiveChannel<Long> get() = _onCreateWidget

    private var selectedFeatureId: Long? = null

    init {
        viewModelScope.launch(io) {
            val groups = dataInteractor.getAllGroupsSync()
            val trackers = dataInteractor.getAllTrackersSync()
            val featurePathProvider = FeaturePathProvider(trackers, groups)
            withContext(ui) {
                _featureMap.value = featurePathProvider.sortedFeatureMap()
            }
        }
    }

    fun onCreateClicked() {
        _onCreateWidget.trySend(selectedFeatureId ?: return)
    }

    fun onFeatureSelected(featureId: Long) {
        selectedFeatureId = featureId
    }
}