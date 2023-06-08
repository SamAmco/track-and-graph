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
import com.samco.trackandgraph.base.model.DataUpdateType
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.util.Stopwatch
import com.samco.trackandgraph.util.bufferWithTimeout
import com.samco.trackandgraph.util.flatMapLatestScan
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

    private enum class UpdateType {
        TRACKERS, GROUPS, GRAPHS, ALL, DISPLAY_INDICES, PREEN
    }

    private fun asUpdateType(dataUpdateType: DataUpdateType) = when (dataUpdateType) {
        DataUpdateType.DataPoint -> listOf(UpdateType.TRACKERS, UpdateType.GRAPHS)
        DataUpdateType.TrackerCreated -> listOf(UpdateType.TRACKERS, UpdateType.DISPLAY_INDICES)
        DataUpdateType.TrackerDeleted -> listOf(
            UpdateType.TRACKERS,
            UpdateType.DISPLAY_INDICES,
            UpdateType.PREEN,
            UpdateType.GRAPHS
        )

        DataUpdateType.GroupCreated, DataUpdateType.GroupDeleted -> listOf(
            UpdateType.GROUPS,
            UpdateType.DISPLAY_INDICES
        )

        DataUpdateType.GroupDeleted -> listOf(UpdateType.GROUPS, UpdateType.PREEN)
        DataUpdateType.DisplayIndex -> listOf(UpdateType.DISPLAY_INDICES)
        DataUpdateType.Function, DataUpdateType.GlobalNote, DataUpdateType.Reminder -> null
        DataUpdateType.GraphOrStatCreated, DataUpdateType.GraphOrStatDeleted -> listOf(
            UpdateType.DISPLAY_INDICES
        )

        DataUpdateType.GraphOrStatUpdated -> listOf(UpdateType.GRAPHS)
        DataUpdateType.GroupUpdated -> listOf(UpdateType.GROUPS)
        DataUpdateType.TrackerUpdated -> listOf(UpdateType.TRACKERS)
    }

    private val onUpdateChildrenForGroup =
        combine(
            groupId,
            dataInteractor.getDataUpdateEvents()
                .map { asUpdateType(it) }
                .filterNotNull()
                .flatMapConcat { it.asFlow() }
                .onStart { emit(UpdateType.ALL) }
        ) { a, b -> Pair(a, b) }
            .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    init {
        viewModelScope.launch {
            onUpdateChildrenForGroup
                .filter { it.second == UpdateType.PREEN }
                .debounce(20)
                .collect { pair -> preenGraphs(pair.first) }
        }
    }

    private suspend fun preenGraphs(groupId: Long) = withContext(io) {
        getGraphObjects(groupId).forEach {
            !gsiProvider.getDataSourceAdapter(it.type).preen(it)
        }
    }

    private val graphChildren = onUpdateChildrenForGroup
        .filter {
            it.second in arrayOf(
                UpdateType.GRAPHS,
                UpdateType.ALL,
                UpdateType.DISPLAY_INDICES
            )
        }
        .bufferWithTimeout(10)
        .map { bufferedEvents ->
            //If the update type is all or graphs then do a full update
            //otherwise the type will be display indices and we will do a partial update
            bufferedEvents.firstOrNull {
                it.second == UpdateType.GRAPHS || it.second == UpdateType.ALL
            } ?: bufferedEvents.first()
        }
        .map { Pair(it.second, getGraphObjects(it.first)) }
        .flatMapLatestScan(emptyList<GraphWithViewData>()) { viewData, graphUpdate ->
            val (type, graphStats) = graphUpdate
            //Only get graph data for graphs that were not already loaded
            if (type == UpdateType.DISPLAY_INDICES) mapNewGraphsToOldViewData(viewData, graphStats)
            //Get graph data for all graphs
            else getGraphViewData(graphStats)
        }
        .map { graphsToGroupChildren(it) }
        .flowOn(io)

    private suspend fun getGraphObjects(groupId: Long): List<GraphOrStat> {
        val graphStats = dataInteractor.getGraphsAndStatsByGroupIdSync(groupId)
        return graphStats.filter { !gsiProvider.getDataSourceAdapter(it.type).preen(it) }
    }

    private fun graphsToGroupChildren(graphs: List<GraphWithViewData>) = graphs
        .map { GroupChild.ChildGraph(it.graph.id, it.graph.displayIndex, it.viewData) }

    private suspend fun mapNewGraphsToOldViewData(
        viewData: List<GraphWithViewData>,
        graphStats: List<GraphOrStat>
    ): Flow<List<GraphWithViewData>> {
        val oldGraphsById = viewData.associateBy { it.graph.id }

        val dontUpdate = graphStats
            .map { Pair(it, oldGraphsById[it.id]?.viewData) }
            .filter { it.second?.viewData?.state == IGraphStatViewData.State.READY }
            .mapNotNull { pair -> pair.second?.let { GraphWithViewData(pair.first, it) } }

        val update = graphStats
            .map { Pair(it, oldGraphsById[it.id]?.viewData) }
            .filter { it.second?.viewData?.state != IGraphStatViewData.State.READY }
            .map { it.first }

        return combine(
            flow { emit(dontUpdate) },
            getGraphViewData(update)
        ) { a, b -> a + b }
    }

    private fun GraphOrStat.asLoading() = GraphWithViewData(
        this,
        CalculatedGraphViewData(
            System.nanoTime(),
            IGraphStatViewData.loading(this)
        )
    )

    private suspend fun getGraphViewData(graphs: List<GraphOrStat>): Flow<List<GraphWithViewData>> =
        flow {
            val stopwatch = Stopwatch().apply { start() }

            emit(graphs.map { it.asLoading() })

            val batch = mutableListOf<Deferred<GraphWithViewData>>()
            for (graph in graphs) {
                val viewData = viewModelScope.async(defaultDispatcher) {
                    val calculatedData = gsiProvider.getDataFactory(graph.type).getViewData(graph)
                    GraphWithViewData(
                        graph,
                        //Shouldn't really need to add one here but it just forces the times to be different
                        // There was a bug previously where the loading and ready states had the same time using
                        // Instant.now() which caused ready states to be missed and infinite loading to be shown
                        CalculatedGraphViewData(System.nanoTime() + 1, calculatedData)
                    )
                }
                batch.add(viewData)
            }

            emit(batch.awaitAll())

            stopwatch.stop()
            Timber.i("Took ${stopwatch.elapsedMillis}ms to generate view data for ${graphs.size} graph(s)")
        }

    private val trackersChildren = onUpdateChildrenForGroup
        .filter {
            it.second in arrayOf(
                UpdateType.TRACKERS,
                UpdateType.ALL,
                UpdateType.DISPLAY_INDICES,
            )
        }
        .debounce(10L)
        .map { getTrackerChildren(it.first) }
        .flowOn(io)

    private val groupChildren = onUpdateChildrenForGroup
        .filter {
            it.second in arrayOf(
                UpdateType.GROUPS,
                UpdateType.ALL,
                UpdateType.DISPLAY_INDICES,
            )
        }
        .debounce(10L)
        .map { getGroupChildren(it.first) }
        .flowOn(io)

    val allChildren: LiveData<List<GroupChild>> =
        combine(graphChildren, trackersChildren, groupChildren) { a, b, c -> Triple(a, b, c) }
            //This debounce should be longer than the children debounce
            .debounce(50L)
            .map { (graphs, trackers, groups) ->
                val children = mutableListOf<GroupChild>().apply {
                    addAll(graphs)
                    addAll(trackers)
                    addAll(groups)
                }
                sortChildren(children)
                children
            }.flowOn(io).asLiveData(viewModelScope.coroutineContext)

    val trackers
        get() = allChildren.value
            ?.filterIsInstance<GroupChild.ChildTracker>()
            ?.map { it.displayTracker }
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

    private suspend fun getTrackerChildren(groupId: Long): List<GroupChild> {
        return dataInteractor.getDisplayTrackersForGroupSync(groupId).map {
            GroupChild.ChildTracker(it.id, it.displayIndex, it)
        }
    }

    private suspend fun getGroupChildren(groupId: Long): List<GroupChild> {
        return dataInteractor.getGroupsForGroupSync(groupId).map {
            GroupChild.ChildGroup(it.id, it.displayIndex, it)
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
        groupId.take(1).collect {
            dataInteractor.updateGroupChildOrder(it, items.map(GroupChild::toDto))
        }
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