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

@file:OptIn(FlowPreview::class)

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
import com.samco.trackandgraph.util.debounceBuffer
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

    private val hasTrackersFlow = dataInteractor.hasAtLeastOneTracker()

    /**
     * Show a loading screen until the database has been loaded in case we're in the middle of a
     * heavy migration.
     */
    private val databaseLoading = hasTrackersFlow
        .map { false }
        .flowOn(io)
        .onStart { emit(true) }

    val loading = databaseLoading.asLiveData(viewModelScope.coroutineContext)

    val hasTrackers: LiveData<Boolean> = hasTrackersFlow
        .asLiveData(viewModelScope.coroutineContext)

    private val groupId = MutableSharedFlow<Long>(1, 1)

    private sealed class UpdateType {
        object Trackers : UpdateType()
        object Groups : UpdateType()
        object AllGraphs : UpdateType()
        object GraphDeleted : UpdateType()
        data class GraphsForFeature(val featureId: Long) : UpdateType()
        data class Graph(val graphId: Long) : UpdateType()
        object All : UpdateType()
        object DisplayIndices : UpdateType()
        object Preen : UpdateType()
    }

    /**
     * Whenever the database is updated get the type of update to determine what to update in the UI
     */
    private fun asUpdateType(dataUpdateType: DataUpdateType) = when (dataUpdateType) {
        is DataUpdateType.DataPoint -> listOf(
            UpdateType.Trackers,
            UpdateType.GraphsForFeature(dataUpdateType.featureId)
        )

        DataUpdateType.TrackerCreated -> listOf(
            UpdateType.Trackers,
            UpdateType.DisplayIndices
        )

        DataUpdateType.TrackerDeleted -> listOf(
            UpdateType.Trackers,
            UpdateType.DisplayIndices,
            UpdateType.Preen,
            UpdateType.AllGraphs
        )

        DataUpdateType.GroupCreated, DataUpdateType.GroupDeleted -> listOf(
            UpdateType.Groups,
            UpdateType.DisplayIndices
        )

        DataUpdateType.GroupDeleted -> listOf(
            UpdateType.Groups,
            UpdateType.Preen,
            UpdateType.DisplayIndices
        )

        DataUpdateType.DisplayIndex -> listOf(
            UpdateType.DisplayIndices
        )

        is DataUpdateType.GraphOrStatCreated -> listOf(
            UpdateType.Graph(dataUpdateType.graphStatId),
            UpdateType.DisplayIndices
        )

        DataUpdateType.GraphOrStatDeleted -> listOf(
            UpdateType.DisplayIndices,
            UpdateType.GraphDeleted
        )

        is DataUpdateType.GraphOrStatUpdated -> listOf(
            UpdateType.Graph(dataUpdateType.graphStatId)
        )

        DataUpdateType.GroupUpdated -> listOf(
            UpdateType.Groups
        )

        DataUpdateType.TrackerUpdated -> listOf(
            UpdateType.Trackers
        )

        DataUpdateType.Function, DataUpdateType.GlobalNote, DataUpdateType.Reminder -> null
    }

    private val onUpdateChildrenForGroup =
        combine(
            groupId.distinctUntilChanged(),
            dataInteractor.getDataUpdateEvents()
                .map { asUpdateType(it) }
                .filterNotNull()
                .flatMapConcat { it.asFlow() }
                .onStart { emit(UpdateType.All) }
        ) { a, b -> Pair(a, b) }
            .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    init {
        viewModelScope.launch {
            onUpdateChildrenForGroup
                .filter { it.second == UpdateType.Preen }
                .debounce(20)
                .collect { pair -> preenGraphs(pair.first) }
        }
    }

    private suspend fun preenGraphs(groupId: Long) = withContext(io) {
        getGraphObjects(groupId).forEach {
            gsiProvider.getDataSourceAdapter(it.type).preen(it)
        }
    }

    private val graphChildren = onUpdateChildrenForGroup
        .filter {
            it.second in arrayOf(
                UpdateType.All,
                UpdateType.AllGraphs,
                UpdateType.DisplayIndices,
                UpdateType.GraphDeleted
            )
                    || it.second is UpdateType.GraphsForFeature
                    || it.second is UpdateType.Graph
        }
        .debounceBuffer(10)
        //Get any buffered events and only emit the most significant one
        .mapNotNull { bufferedEvents ->
            sequence {
                val types = bufferedEvents.associateBy { it.second }
                types[UpdateType.All]?.let { yield(it) }
                types[UpdateType.AllGraphs]?.let { yield(it) }

                //Special case if you delete a graph you typically get both display indices and
                //graph deleted events. Graph deleted is more significant so we only emit that
                if (types.size == 2
                    && types.containsKey(UpdateType.GraphDeleted)
                    && types.containsKey(UpdateType.DisplayIndices)
                ) {
                    yield(types[UpdateType.GraphDeleted])
                }

                //If we missed more than one specific event just update all
                //graphs. This should happen very rarely
                if (types.size > 1) yield(Pair(bufferedEvents[0].first, UpdateType.AllGraphs))
                else if (types.size == 1) yield(bufferedEvents[0])
            }.firstOrNull()
        }
        //Get the graph objects for the group
        .scan<Pair<Long, UpdateType>, Pair<UpdateType, List<GraphOrStat>>>(
            Pair(
                UpdateType.All,
                emptyList()
            )
        ) { pair, event ->
            val lastGraphList = pair.second
            val eventType = event.second
            val groupId = event.first

            if (lastGraphList.isEmpty() || eventType !is UpdateType.GraphsForFeature) {
                Pair(eventType, getGraphObjects(groupId))
            }
            //Don't need to get the graphs from the database again if the update type is
            //GraphsForFeature as this simply means a feature was updated and we need to
            //recalculate the inner graph data
            else Pair(eventType, lastGraphList)
        }
        //Get the graph view data for the graphs
        .flatMapLatestScan(emptyList<GraphWithViewData>()) { viewData, graphUpdate ->
            val (type, graphStats) = graphUpdate

            //Get the graph data for any graphs that need updating
            return@flatMapLatestScan when (type) {
                UpdateType.All, UpdateType.AllGraphs -> getGraphViewData(graphStats)
                UpdateType.DisplayIndices, UpdateType.GraphDeleted -> {
                    mapNewGraphsToOldViewData(viewData, graphStats)
                }

                is UpdateType.Graph -> withUpdatedGraph(viewData, graphStats, type.graphId)
                is UpdateType.GraphsForFeature ->
                    withUpdatedIfAffected(viewData, graphStats, type.featureId)
                //Shouldn't ever happen
                else -> flowOf(viewData)
            }
        }
        .map { graphsToGroupChildren(it) }
        .flowOn(io)

    private fun withUpdatedIfAffected(
        viewData: List<GraphWithViewData>,
        graphStats: List<GraphOrStat>,
        featureId: Long
    ): Flow<List<GraphWithViewData>> = flow {

        val affected = viewData
            .map { it.graph }
            .filter { gsiProvider.getDataFactory(it.type).affectedBy(it.id, featureId) }
            .map { it.id }

        val dontUpdate = viewData.filter { it.graph.id !in affected }

        getGraphViewData(graphStats.filter { it.id in affected })
            .collect { emit(dontUpdate + it) }
    }

    private fun withUpdatedGraph(
        viewData: List<GraphWithViewData>,
        newGraphStats: List<GraphOrStat>,
        updateGraphId: Long
    ): Flow<List<GraphWithViewData>> {
        val dontUpdate = viewData.filter { it.graph.id != updateGraphId }
        return getGraphViewData(newGraphStats.filter { it.id == updateGraphId })
            .map { dontUpdate + it }
    }

    private suspend fun getGraphObjects(groupId: Long): List<GraphOrStat> = dataInteractor
        .getGraphsAndStatsByGroupIdSync(groupId)

    private fun graphsToGroupChildren(graphs: List<GraphWithViewData>) = graphs
        .map { GroupChild.ChildGraph(it.graph.id, it.graph.displayIndex, it.viewData) }

    private fun mapNewGraphsToOldViewData(
        viewData: List<GraphWithViewData>,
        newGraphStats: List<GraphOrStat>
    ): Flow<List<GraphWithViewData>> {
        val oldGraphsById = viewData.associateBy { it.graph.id }

        val dontUpdate = newGraphStats
            .map { Pair(it, oldGraphsById[it.id]?.viewData) }
            .filter { it.second?.viewData?.state == IGraphStatViewData.State.READY }
            .mapNotNull { pair -> pair.second?.let { GraphWithViewData(pair.first, it) } }

        val update = newGraphStats
            .map { Pair(it, oldGraphsById[it.id]?.viewData) }
            .filter { it.second?.viewData?.state != IGraphStatViewData.State.READY }
            .map { it.first }

        return getGraphViewData(update).map { dontUpdate + it }
    }

    private fun GraphOrStat.asLoading() = GraphWithViewData(
        this,
        CalculatedGraphViewData(
            System.nanoTime(),
            IGraphStatViewData.loading(this)
        )
    )

    private fun getGraphViewData(graphs: List<GraphOrStat>): Flow<List<GraphWithViewData>> =
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
                UpdateType.Trackers,
                UpdateType.All,
                UpdateType.DisplayIndices,
            )
        }
        .debounce(10L)
        .map { getTrackerChildren(it.first) }
        .flowOn(io)

    private val groupChildren = onUpdateChildrenForGroup
        .filter {
            it.second in arrayOf(
                UpdateType.Groups,
                UpdateType.All,
                UpdateType.DisplayIndices,
            )
        }
        .debounce(10L)
        .map { getGroupChildren(it.first) }
        .flowOn(io)

    private val allChildrenFlow: StateFlow<List<GroupChild>> =
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
            }
            .flowOn(io)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allChildren = allChildrenFlow.asLiveData(viewModelScope.coroutineContext)

    val showEmptyGroupText: LiveData<Boolean> = combine(
        allChildrenFlow.map { it.isEmpty() },
        databaseLoading,
        groupId
    ) { childrenEmpty, loading, groupId ->
        childrenEmpty && !loading && groupId == 0L
    }
        .onStart { emit(false) }
        .distinctUntilChanged()
        .asLiveData(viewModelScope.coroutineContext)

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