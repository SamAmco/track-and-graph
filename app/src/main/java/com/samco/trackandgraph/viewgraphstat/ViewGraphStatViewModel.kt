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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.helpers.getDisplayValue
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.util.FeatureDataProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

interface ViewGraphStatViewModel {
    fun initFromGraphStatId(graphStatId: Long)

    val graphStatViewData: StateFlow<IGraphStatViewData?>
    val showingNotes: StateFlow<Boolean>
    val timeMarker: StateFlow<OffsetDateTime?>
    val notes: StateFlow<List<GraphNote>>
    val selectedNoteForDialog: StateFlow<GraphNote?>

    fun showHideNotesClicked()
    fun setNotesVisibility(visible: Boolean)
    fun noteClicked(note: GraphNote)
    fun dismissNoteDialog()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ViewGraphStatViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel(), ViewGraphStatViewModel {

    private val graphStatId = MutableStateFlow<Long?>(null)

    override val showingNotes = MutableStateFlow(false)
    override val timeMarker = MutableStateFlow<OffsetDateTime?>(null)
    override val selectedNoteForDialog = MutableStateFlow<GraphNote?>(null)

    // Data structure to hold both view data and data points
    private data class GraphStatResult(
        val viewData: IGraphStatViewData,
        val dataPoints: List<DataPoint>
    )

    // Single expensive operation that produces both view data and data points
    private val graphStatResult = graphStatId
        .filterNotNull()
        .flatMapLatest { getGraphData(it) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    private fun getGraphData(id: Long) = flow {
        val graphStat = dataInteractor.getGraphStatById(id)
        emit(GraphStatResult(IGraphStatViewData.loading(graphStat), emptyList()))

        // Use a channel to handle the async callback
        val dataPointsChannel = Channel<List<DataPoint>>(Channel.UNLIMITED)

        val viewData = gsiProvider
            .getDataFactory(graphStat.type)
            .getViewData(graphStat) { dataPointsChannel.trySend(it) }

        // Listen for data points from the callback
        try {
            for (dataPoints in dataPointsChannel) {
                emit(GraphStatResult(viewData, dataPoints))
            }
        } finally {
            dataPointsChannel.close()
        }
    }.flowOn(io)

    // Derive individual flows from the shared result
    override val graphStatViewData: StateFlow<IGraphStatViewData?> = graphStatResult
        .map { it.viewData }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val dataPoints = graphStatResult
        .map { it.dataPoints }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    private val featureDataProvider = graphStatId
        .filterNotNull()
        .map {
            val allGroups = dataInteractor.getAllGroupsSync()
            val dataSourceData = getDataSourceData(dataInteractor.getAllFeaturesSync())
            FeatureDataProvider(dataSourceData, allGroups)
        }
        .flowOn(io)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    private suspend fun getDataSourceData(allFeatures: List<Feature>): List<FeatureDataProvider.DataSourceData> {
        return allFeatures.map { feature ->
            FeatureDataProvider.DataSourceData(
                feature,
                dataInteractor.getDataSamplePropertiesForFeatureId(feature.featureId)
                    ?: return@map null
            )
        }.filterNotNull()
    }

    private val globalNotes = graphStatId
        .filterNotNull()
        .map { dataInteractor.getAllGlobalNotesSync() }
        .flowOn(io)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    private val dataPointNotes = combine(dataPoints, featureDataProvider) { dataPoints, featureProvider ->
        dataPoints
            .distinct()
            .filter { dp -> dp.note.isNotEmpty() }
            .map { dp ->
                val featurePath = featureProvider.getPathForFeature(dp.featureId)
                val isDuration = featureProvider.getDataSampleProperties(dp.featureId)?.isDuration ?: false
                GraphNote.DataPointNote(
                    timestamp = dp.timestamp,
                    noteText = dp.note,
                    displayValue = dp.getDisplayValue(isDuration),
                    featurePath = featurePath
                )
            }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    override val notes: StateFlow<List<GraphNote>> =
        combine(dataPointNotes, globalNotes) { dataPointNotes, globalNotes ->
            val oldestTime = dataPointNotes.minByOrNull { it.timestamp }?.timestamp
            val newestTime = dataPointNotes.maxByOrNull { it.timestamp }?.timestamp

            if (oldestTime == null || newestTime == null) return@combine dataPointNotes

            val filteredGlobalNotes = globalNotes
                .filter { g -> g.timestamp in oldestTime..newestTime }
                .map { globalNote ->
                    GraphNote.GlobalNote(
                        timestamp = globalNote.timestamp,
                        noteText = globalNote.note
                    )
                }

            dataPointNotes
                .union(filteredGlobalNotes)
                .sortedByDescending { it.timestamp }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    override fun initFromGraphStatId(graphStatId: Long) {
        this.graphStatId.value = graphStatId
    }

    override fun showHideNotesClicked() {
        showingNotes.value = !showingNotes.value
    }

    override fun setNotesVisibility(visible: Boolean) {
        showingNotes.value = visible
    }

    override fun noteClicked(note: GraphNote) {
        timeMarker.value = note.timestamp
        selectedNoteForDialog.value = note
    }

    override fun dismissNoteDialog() {
        selectedNoteForDialog.value = null
    }
}