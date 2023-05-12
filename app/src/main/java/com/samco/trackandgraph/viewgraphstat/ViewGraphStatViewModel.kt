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
import com.samco.trackandgraph.base.database.dto.GlobalNote
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.util.FeatureDataProvider
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


interface ViewGraphStatViewModel {
    fun initFromGraphStatId(graphStatId: Long)

    //TODO This won't work when we implement features. FeatureDataProvider
    // iterates all data in every feature
    val featureDataProvider: LiveData<FeatureDataProvider>
    val graphStatViewData: LiveData<IGraphStatViewData>
    val showingNotes: LiveData<Boolean>
    val markedNote: LiveData<GraphNote?>
    val notes: LiveData<List<GraphNote>>

    fun showHideNotesClicked()
    fun noteClicked(note: GraphNote)
}

@HiltViewModel
class ViewGraphStatViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @MainDispatcher private val ui: CoroutineDispatcher,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel(), ViewGraphStatViewModel {
    override val featureDataProvider = MutableLiveData<FeatureDataProvider>()
    override val graphStatViewData = MutableLiveData<IGraphStatViewData>()
    override val showingNotes = MutableLiveData(false)

    private val globalNotes = MutableStateFlow<List<GlobalNote>>(emptyList())
    private val dataPoints = MutableStateFlow<List<DataPoint>>(emptyList())

    override val notes: LiveData<List<GraphNote>> =
        combine(dataPoints, globalNotes) { dataPoints, globalNotes ->
            val distinctDataPoints = dataPoints.distinct()
            val oldestTime = distinctDataPoints.minByOrNull { it.timestamp }?.timestamp
            val newestTime = distinctDataPoints.maxByOrNull { it.timestamp }?.timestamp

            if (oldestTime == null || newestTime == null) return@combine emptyList<GraphNote>()

            val filteredGlobalNotes = globalNotes
                .filter { g -> g.timestamp in oldestTime..newestTime }
                .map { GraphNote(it) }

            distinctDataPoints
                .filter { dp -> dp.note.isNotEmpty() }
                .distinct()
                .map { GraphNote(it) }
                .union(filteredGlobalNotes)
                .sortedByDescending { it.timestamp }
        }.asLiveData(viewModelScope.coroutineContext)

    override val markedNote = MutableLiveData<GraphNote?>(null)

    private var initialized = false

    override fun initFromGraphStatId(graphStatId: Long) {
        if (initialized) return
        initialized = true

        viewModelScope.launch(io) {
            emitGraphData(graphStatId)
            getAllDataSourceAttributes()
            getAllGlobalNotes()
        }
    }

    private suspend fun getAllGlobalNotes() = withContext(io) {
        val n = dataInteractor.getAllGlobalNotesSync()
        globalNotes.emit(n)
    }

    private suspend fun getAllDataSourceAttributes() {
        val allFeatures = dataInteractor.getAllFeaturesSync()
        val allGroups = dataInteractor.getAllGroupsSync()
        val dataSourceData = allFeatures.map { feature ->
            FeatureDataProvider.DataSourceData(
                feature,
                dataInteractor.getLabelsForFeatureId(feature.featureId).toSet(),
                dataInteractor.getDataSamplePropertiesForFeatureId(feature.featureId)
                    ?: return@map null
            )
        }.filterNotNull()
        FeatureDataProvider(dataSourceData, allGroups).let {
            withContext(ui) {
                featureDataProvider.value = it
            }
        }
    }

    private suspend fun emitGraphData(graphStatId: Long) {
        val graphStat = dataInteractor.getGraphStatById(graphStatId)
        withContext(ui) { graphStatViewData.value = IGraphStatViewData.loading(graphStat) }
        val viewData = gsiProvider
            .getDataFactory(graphStat.type)
            .getViewData(graphStat) { viewModelScope.launch(io) { dataPoints.emit(it) } }
        withContext(ui) { graphStatViewData.value = viewData }
    }

    override fun showHideNotesClicked() {
        showingNotes.value = showingNotes.value?.not()
    }

    override fun noteClicked(note: GraphNote) {
        markedNote.value = note
    }
}