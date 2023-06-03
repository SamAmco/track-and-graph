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

@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.samco.trackandgraph.group

import androidx.lifecycle.*
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.util.Stopwatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel() {

    data class DurationInputDialogData(
        val trackerId: Long,
        val duration: Duration
    )

    private val _showDurationInputDialog = MutableLiveData<DurationInputDialogData?>()
    val showDurationInputDialog: LiveData<DurationInputDialogData?> = _showDurationInputDialog

    val hasTrackers: LiveData<Boolean> = dataInteractor
        .hasAtLeastOneTracker()
        .asLiveData(viewModelScope.coroutineContext)

    private val groupId = MutableSharedFlow<Long>(1, 1)

    val groupChildren: LiveData<List<GroupChild>> = groupId
        .distinctUntilChanged()
        .flatMapLatest { groupId ->
            dataInteractor.getDataUpdateEvents()
                .onStart { emit(Unit) }
                .debounce(100)
                .flatMapLatest { getGroupChildrenForGroupId(groupId) }
        }
        .flowOn(ui)
        .asLiveData(viewModelScope.coroutineContext)

    private fun getGroupChildrenForGroupId(groupId: Long) = flow {
        //Start getting the trackers and groups
        val trackerDataDeferred = getTrackerChildrenAsync(groupId)
        val groupDataDeferred = getGroupChildrenAsync(groupId)
        //Then get the graphs
        getGraphViewData(groupId).collect { graphDataPairs ->
            val trackerData = trackerDataDeferred.await()
            val groupData = groupDataDeferred.await()
            val graphData = graphsToGroupChildren(graphDataPairs)
            val children = mutableListOf<GroupChild>().apply {
                addAll(trackerData)
                addAll(groupData)
                addAll(graphData)
            }
            sortChildren(children)
            val next = children.toList()
            emit(next)
        }
    }

    val trackers
        get() = groupChildren.value
            ?.filter { it.type == GroupChildType.TRACKER }
            ?.map { it.obj as DisplayTracker }
            ?: emptyList()

    fun setGroup(groupId: Long) {
        viewModelScope.launch { this@GroupViewModel.groupId.emit(groupId) }
    }

    private fun sortChildren(children: MutableList<GroupChild>) = children.sortWith { a, b ->
        val aInd = a.displayIndex
        val bInd = b.displayIndex
        when {
            aInd < bInd -> -1
            bInd < aInd -> 1
            else -> {
                val aId = a.id
                val bId = b.id
                when {
                    aId > bId -> -1
                    bId > aId -> 1
                    else -> 0
                }
            }
        }
    }

    private fun getTrackerChildrenAsync(groupId: Long) = viewModelScope.async(io) {
        return@async dataInteractor.getDisplayTrackersForGroupSync(groupId).map {
            GroupChild(GroupChildType.TRACKER, it, it.id, it.displayIndex)
        }
    }

    private fun getGroupChildrenAsync(groupId: Long) = viewModelScope.async(io) {
        return@async dataInteractor.getGroupsForGroupSync(groupId).map {
            GroupChild(GroupChildType.GROUP, it, it.id, it.displayIndex)
        }
    }

    private suspend fun getGraphViewData(groupId: Long) =
        flow<List<Pair<Long, IGraphStatViewData>>> {
            val stopwatch = Stopwatch().apply { start() }
            val graphStats = dataInteractor.getGraphsAndStatsByGroupIdSync(groupId)
            graphStats.forEach {
                gsiProvider.getDataSourceAdapter(it.type).preen(it)
            }
            val loadingStates =
                graphStats.map { Pair(System.nanoTime(), IGraphStatViewData.loading(it)) }
            emit(loadingStates)

            val batch = mutableListOf<Deferred<IGraphStatViewData>>()
            for (index in graphStats.indices) {
                val graphOrStat = graphStats[index]
                val viewData = viewModelScope.async(defaultDispatcher) {
                    gsiProvider.getDataFactory(graphOrStat.type).getViewData(graphOrStat)
                }
                batch.add(index, viewData)
            }
            //Shouldn't really need to add one here but it just forces the times to be different
            // There was a bug previously where the loading and ready states had the same time using
            // Instant.now() which caused ready states to be missed and infinite loading to be shown
            emit(batch.map { Pair(System.nanoTime() + 1, it.await()) })
            stopwatch.stop()
            Timber.i("Took ${stopwatch.elapsedMillis}ms to generate view data for ${graphStats.size} graph(s)")
        }.flowOn(io)

    private fun graphsToGroupChildren(graphs: List<Pair<Long, IGraphStatViewData>>): List<GroupChild> {
        return graphs.map {
            GroupChild(
                GroupChildType.GRAPH,
                it,
                it.second.graphOrStat.id,
                it.second.graphOrStat.displayIndex
            )
        }
    }

    fun addDefaultTrackerValue(tracker: DisplayTracker) = viewModelScope.launch(io) {
        val newDataPoint = DataPoint(
            timestamp = OffsetDateTime.now(),
            featureId = tracker.featureId,
            value = tracker.defaultValue,
            label = tracker.defaultLabel,
            note = ""
        )
        dataInteractor.insertDataPoint(newDataPoint)
    }

    fun onDeleteFeature(id: Long) = viewModelScope.launch(io) {
        dataInteractor.deleteFeature(id)
    }

    fun adjustDisplayIndexes(items: List<GroupChild>) = viewModelScope.launch(io) {
        groupId.take(1).collect { dataInteractor.updateGroupChildOrder(it, items) }
    }

    fun onDeleteGraphStat(id: Long) =
        viewModelScope.launch(io) { dataInteractor.deleteGraphOrStat(id) }

    fun onDeleteGroup(id: Long) =
        viewModelScope.launch(io) { dataInteractor.deleteGroup(id) }

    fun duplicateGraphOrStat(graphOrStatViewData: IGraphStatViewData) {
        viewModelScope.launch(io) {
            val gs = graphOrStatViewData.graphOrStat
            gsiProvider.getDataSourceAdapter(gs.type).duplicateGraphOrStat(gs)
        }
    }

    fun onConsumedShowDurationInputDialog() {
        _showDurationInputDialog.value = null
    }

    fun stopTimer(tracker: DisplayTracker) = viewModelScope.launch(io) {
        dataInteractor.stopTimerForTracker(tracker.id)?.let {
            withContext(ui) {
                _showDurationInputDialog.value = DurationInputDialogData(tracker.id, it)
            }
        }
    }

    fun playTimer(tracker: DisplayTracker) = viewModelScope.launch(io) {
        dataInteractor.playTimerForTracker(tracker.id)
    }
}