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

package com.samco.trackandgraph.viewgraphstat

import androidx.lifecycle.*
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.GlobalNote
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.ui.FeaturePathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class ViewGraphStatViewModelState { INITIALIZING, WAITING }

@HiltViewModel
class ViewGraphStatViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @MainDispatcher private val ui: CoroutineDispatcher,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel() {
    var featurePathProvider: FeaturePathProvider = FeaturePathProvider(emptyMap())
        private set
    var featureTypes: Map<Long, DataType>? = null
        private set

    val state: LiveData<ViewGraphStatViewModelState>
        get() = _state
    private val _state = MutableLiveData(ViewGraphStatViewModelState.INITIALIZING)

    val graphStatViewData: LiveData<IGraphStatViewData>
        get() = _graphStatViewData
    private val _graphStatViewData = MutableLiveData<IGraphStatViewData>()

    val showingNotes: LiveData<Boolean>
        get() = _showingNotes
    private val _showingNotes = MutableLiveData(false)

    private val globalNotes = MutableStateFlow<List<GlobalNote>>(emptyList())
    private val dataPoints = MutableStateFlow<List<DataPoint>>(emptyList())

    val notes: LiveData<List<GraphNote>> =
        combine(dataPoints, globalNotes) { dataPoints, globalNotes ->
            val filteredGlobalNotes = globalNotes
                .filter { g ->
                    val afterOldest =
                        dataPoints.lastOrNull()?.let { g.timestamp >= it.timestamp } ?: false
                    val beforeNewest =
                        dataPoints.firstOrNull()?.let { g.timestamp <= it.timestamp } ?: false
                    afterOldest && beforeNewest
                }
                .map { GraphNote(it) }
            dataPoints
                .filter { dp -> dp.note.isNotEmpty() }
                .distinct()
                .map { GraphNote(it) }
                .union(filteredGlobalNotes)
                .sortedByDescending { it.timestamp }
        }.asLiveData(viewModelScope.coroutineContext)

    val markedNote: LiveData<GraphNote?>
        get() = _markedNote
    private val _markedNote = MutableLiveData<GraphNote?>(null)

    private var initialized = false

    fun setGraphStatId(graphStatId: Long) {
        if (initialized) return
        initialized = true

        viewModelScope.launch(io) {
            initFromGraphStatId(graphStatId)
            getAllDataSourceAttributes()
            getAllGlobalNotes()
            withContext(ui) { _state.value = ViewGraphStatViewModelState.WAITING }
        }
    }

    private suspend fun getAllGlobalNotes() = withContext(io) {
        globalNotes.emit(dataInteractor.getAllGlobalNotesSync())
    }

    private suspend fun getAllDataSourceAttributes() {
        val allDataSources = dataInteractor.getAllDataSourcesSync()
        val allGroups = dataInteractor.getAllGroupsSync()
        val allFeatures = dataInteractor.getAllFeaturesSync()
        val dataSourceMap = allDataSources.mapNotNull { dataSource ->
            val group = allGroups.firstOrNull { it.id == dataSource.groupId }
                ?: return@mapNotNull null
            dataSource to group
        }.toMap()
        featurePathProvider = FeaturePathProvider(dataSourceMap)
        featureTypes = allFeatures.associate { it.id to it.featureType }
    }

    private suspend fun initFromGraphStatId(graphStatId: Long) {
        val graphStat = dataInteractor.getGraphStatById(graphStatId)
        val viewData = gsiProvider
            .getDataFactory(graphStat.type)
            .getViewData(graphStat) {
                viewModelScope.launch(io) { dataPoints.emit(it) }
            }
        withContext(ui) { _graphStatViewData.value = viewData }
    }

    fun showHideNotesClicked() {
        _showingNotes.value = _showingNotes.value?.not()
    }

    fun noteClicked(note: GraphNote) {
        _markedNote.value = note
    }
}