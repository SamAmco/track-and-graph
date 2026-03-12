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

@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.samco.trackandgraph.group

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.FunctionDeleteRequest
import com.samco.trackandgraph.data.database.dto.GraphDeleteRequest
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GroupChildDisplayIndex
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.data.database.dto.GroupDeleteRequest
import com.samco.trackandgraph.data.database.dto.TrackerDeleteRequest
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.interactor.DataUpdateType
import com.samco.trackandgraph.data.di.DefaultDispatcher
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.di.MainDispatcher
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.timers.TimerServiceInteractor
import com.samco.trackandgraph.util.Stopwatch
import com.samco.trackandgraph.util.debounceBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import timber.log.Timber
import javax.inject.Inject

interface GroupViewModel {
    data class DurationInputDialogData(
        val trackerId: Long,
        val duration: Duration
    )

    val showDurationInputDialog: StateFlow<DurationInputDialogData?>
    val groupHasAnyTrackers: StateFlow<Boolean>
    val currentChildren: StateFlow<List<GroupChild>>
    val showEmptyGroupText: StateFlow<Boolean>
    val hasAnyReminders: StateFlow<Boolean>
    val loading: StateFlow<Boolean>
    val lazyGridState: LazyGridState

    fun setGroup(groupId: Long)
    suspend fun userHasAnyTrackers(): Boolean
    fun getTrackersInGroup(): List<DisplayTracker>
    fun addDefaultTrackerValue(tracker: DisplayTracker)
    /**
     * Delete a tracker. If [groupId] is provided and the tracker exists in multiple groups,
     * only the GroupItem link for that group is removed (symlink removal). If [groupId] is null
     * or the tracker is unique, the tracker is deleted entirely from all groups.
     */
    fun onDeleteTracker(trackerId: Long, groupId: Long? = null)
    /**
     * Delete a graph/stat. If [groupId] is provided and the graph exists in multiple groups,
     * only the GroupItem link for that group is removed. If [groupId] is null or the graph is
     * unique, it is deleted entirely.
     */
    fun onDeleteGraphStat(id: Long, groupId: Long? = null)
    /**
     * Delete a group. If [parentGroupId] is provided and the group exists in multiple parent
     * groups, only the GroupItem link for that parent is removed. If null or unique, it is
     * deleted entirely along with all contents.
     */
    fun onDeleteGroup(id: Long, parentGroupId: Long? = null)
    /**
     * Delete a function. If [groupId] is provided and the function exists in multiple groups,
     * only the GroupItem link for that group is removed. If null or unique, it is deleted entirely.
     */
    fun onDeleteFunction(id: Long, groupId: Long? = null)

    fun duplicateFunction(displayFunction: DisplayFunction)
    fun duplicateGraphOrStat(graphOrStatViewData: IGraphStatViewData)
    fun onConsumedShowDurationInputDialog()
    fun stopTimer(tracker: DisplayTracker)
    fun playTimer(tracker: DisplayTracker)

    fun onDragStart()
    fun onDragSwap(from: Int, to: Int)
    fun onDragEnd()
}

@HiltViewModel
class GroupViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    private val timerServiceInteractor: TimerServiceInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel(), GroupViewModel {

    override val lazyGridState = LazyGridState()

    private val _showDurationInputDialog =
        MutableStateFlow<GroupViewModel.DurationInputDialogData?>(null)
    override val showDurationInputDialog: StateFlow<GroupViewModel.DurationInputDialogData?> =
        _showDurationInputDialog

    override suspend fun userHasAnyTrackers() = dataInteractor.hasAtLeastOneTracker()

    private val groupId = MutableStateFlow<Long?>(null)

    private val onUpdateChildrenForGroup: SharedFlow<Pair<Long, DataUpdateType>> =
        combine(
            groupId.filterNotNull(),
            dataInteractor.getDataUpdateEvents()
                .filter { it !is DataUpdateType.GlobalNote }
                .onStart { emit(DataUpdateType.Unknown) }
        ) { groupId, event -> Pair(groupId, event) }
            .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    private val graphChildren: Flow<List<GroupChild>> = channelFlow {
        var currentViewData = emptyList<GraphWithViewData>()
        var innerJob: Job? = null

        onUpdateChildrenForGroup
            .filter { (_, event) ->
                event is DataUpdateType.Unknown ||
                event is DataUpdateType.SymlinkCreated ||
                event is DataUpdateType.GraphOrStatDeleted ||
                event is DataUpdateType.GraphOrStatCreated ||
                event is DataUpdateType.GraphOrStatUpdated
            }
            .debounceBuffer(10)
            .collect { bufferedEvents ->
                val groupId = bufferedEvents[0].first
                val events = bufferedEvents.map { it.second }
                val graphStats = getGraphObjects(groupId)

                // Cancel any in-flight view data computation for the previous batch
                innerJob?.cancel()
                innerJob?.join()

                // Unknown event = full recalculation; otherwise reuse surviving view data
                // and only recalculate created/updated IDs (empty set = delete-only)
                val viewDataFlow = if (events.any { it is DataUpdateType.Unknown }) {
                    getGraphViewData(graphStats)
                } else {
                    val forceIds = events.mapNotNull {
                        when (it) {
                            is DataUpdateType.GraphOrStatCreated -> it.graphStatId
                            is DataUpdateType.GraphOrStatUpdated -> it.graphStatId
                            else -> null
                        }
                    }.toSet()
                    mapNewGraphsToOldViewData(currentViewData, graphStats, forceIds)
                }

                innerJob = launch {
                    viewDataFlow.collect { viewData ->
                        currentViewData = viewData
                        send(graphsToGroupChildren(viewData))
                    }
                }
            }
    }.flowOn(io)

    private suspend fun getGraphObjects(groupId: Long): List<GraphOrStat> = dataInteractor
        .getGraphsAndStatsByGroupIdSync(groupId)

    private fun graphsToGroupChildren(graphs: List<GraphWithViewData>) = graphs
        .map { GroupChild.ChildGraph(it.graph.id, it.viewData) }

    private fun mapNewGraphsToOldViewData(
        viewData: List<GraphWithViewData>,
        newGraphStats: List<GraphOrStat>,
        forceUpdateIds: Set<Long> = emptySet(),
    ): Flow<List<GraphWithViewData>> {
        val oldGraphsById = viewData.associateBy { it.graph.id }

        // Ideally we just get the view data for all the graphs and map them to the
        // new GraphOrStat objects.
        val dontUpdate = newGraphStats
            .map { Pair(it, oldGraphsById[it.id]?.viewData) }
            .filter { it.first.id !in forceUpdateIds && it.second?.isReady() == true }
            .mapNotNull { pair -> pair.second?.let { GraphWithViewData(pair.first, it) } }

        // But any graphs that had not finished loading we have lost the opportunity to
        // get their view data
        val update = newGraphStats
            .map { Pair(it, oldGraphsById[it.id]?.viewData) }
            .filter { forceUpdateIds.contains(it.first.id) || it.second?.isLoading() != false }
            .map { it.first }

        // So we ensure we get the view data for any graphs that have not finished loading
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
                        //Shouldn't really need to add one here, but it just forces the times to be different
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
        .filter { (_, event) ->
            event is DataUpdateType.Unknown ||
            event is DataUpdateType.SymlinkCreated ||
            event is DataUpdateType.DataPoint ||
            event is DataUpdateType.TrackerCreated ||
            event is DataUpdateType.TrackerDeleted ||
            event is DataUpdateType.TrackerUpdated
        }
        .debounce(10L)
        .map { (groupId, _) -> getTrackerChildren(groupId) }
        .flowOn(io)

    private val groupChildren = onUpdateChildrenForGroup
        .filter { (_, event) ->
            event is DataUpdateType.Unknown ||
            event is DataUpdateType.SymlinkCreated ||
            event is DataUpdateType.GroupCreated ||
            event is DataUpdateType.GroupDeleted ||
            event is DataUpdateType.GroupUpdated
        }
        .debounce(10L)
        .map { (groupId, _) -> getGroupChildren(groupId) }
        .flowOn(io)

    private val functionChildren = onUpdateChildrenForGroup
        .filter { (_, event) ->
            event is DataUpdateType.Unknown ||
            event is DataUpdateType.SymlinkCreated ||
            event is DataUpdateType.FunctionCreated ||
            event is DataUpdateType.FunctionDeleted ||
            event is DataUpdateType.FunctionUpdated
        }
        .debounce(10L)
        .map { (groupId, _) -> getFunctionChildren(groupId) }
        .flowOn(io)

    /**
     * A flow of display indices from the database, represented as a map from
     * (type, childId) to displayIndex for O(1) lookups during sorting.
     */
    private val dbDisplayIndices: StateFlow<Map<Pair<GroupChildType, Long>, Int>?> =
        onUpdateChildrenForGroup
            .filter { (_, event) ->
                event is DataUpdateType.DisplayIndex ||
                event is DataUpdateType.SymlinkCreated ||
                event is DataUpdateType.Unknown
            }
            .debounce(10L)
            .map { (groupId, _) ->
                dataInteractor.getDisplayIndicesForGroup(groupId)
                    .associate { Pair(it.type, it.id) to it.displayIndex }
            }
            .flowOn(io)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Merging dbDisplayIndices into the same combine and filtering to only items present in
    // indices prevents the race where dbDisplayIndices updates before allChildren (or vice versa),
    // which would cause removed items to temporarily sort to Int.MAX_VALUE (visual flicker).
    private val allChildren: StateFlow<List<GroupChild>> =
        combine(
            graphChildren, trackersChildren, groupChildren, functionChildren, dbDisplayIndices.filterNotNull()
        ) { graphs, trackers, groups, functions, indices ->
            listOf(graphs, trackers, groups, functions).flatten()
                .filter { child -> indices.containsKey(Pair(child.type, child.id)) }
                .sortedBy { child -> indices[Pair(child.type, child.id)] }
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val isDragging = MutableStateFlow(false)
    private val temporaryDragDropChildren = MutableStateFlow<List<GroupChild>>(emptyList())

    override val currentChildren: StateFlow<List<GroupChild>> = isDragging
        .flatMapLatest { dragging ->
            if (dragging) temporaryDragDropChildren
            else allChildren
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    override val groupHasAnyTrackers: StateFlow<Boolean> = allChildren
        .map { children -> children.any { it is GroupChild.ChildTracker } }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)


    override val showEmptyGroupText: StateFlow<Boolean> = currentChildren
        .map {
            if (!inRootGroup()) return@map false
            return@map listOf(
                dataInteractor.hasAnyFeatures(),
                dataInteractor.hasAnyGraphs(),
                dataInteractor.hasAnyGroups(),
                dataInteractor.hasAnyFunctions(),
            ).none { it }
        }
        .flowOn(io)
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    override val hasAnyReminders: StateFlow<Boolean> = flow {
        emit(dataInteractor.hasAnyReminders())
    }
        .flowOn(io)
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    override val loading = combine(
        showEmptyGroupText,
        currentChildren
    ) { showEmptyGroupText, allChildren ->
        inRootGroup() && allChildren.isEmpty() && !showEmptyGroupText
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    private suspend fun inRootGroup() = groupId.first() == 0L

    override fun getTrackersInGroup(): List<DisplayTracker> {
        return currentChildren.value
            .filterIsInstance<GroupChild.ChildTracker>()
            .map { it.displayTracker }
    }

    override fun setGroup(groupId: Long) {
        viewModelScope.launch { this@GroupViewModelImpl.groupId.emit(groupId) }
    }

    private suspend fun getTrackerChildren(groupId: Long): List<GroupChild> {
        return dataInteractor.getDisplayTrackersForGroupSync(groupId).map {
            GroupChild.ChildTracker(it.id, it)
        }
    }

    private suspend fun getGroupChildren(groupId: Long): List<GroupChild> {
        return dataInteractor.getGroupsForGroupSync(groupId).map {
            GroupChild.ChildGroup(it.id, it)
        }
    }

    private suspend fun getFunctionChildren(groupId: Long): List<GroupChild> {
        return dataInteractor.getFunctionsForGroupSync(groupId).map { function ->
            val displayFunction = DisplayFunction(
                id = function.id,
                featureId = function.featureId,
                groupId = groupId,
                name = function.name,
                description = function.description,
                unique = function.unique,
            )
            GroupChild.ChildFunction(function.id, displayFunction)
        }
    }

    override fun addDefaultTrackerValue(tracker: DisplayTracker) {
        viewModelScope.launch(io) {
            val newDataPoint = DataPoint(
                timestamp = OffsetDateTime.now(),
                featureId = tracker.featureId,
                value = tracker.defaultValue,
                label = tracker.defaultLabel,
                note = ""
            )
            dataInteractor.insertDataPoint(newDataPoint)
        }
    }

    override fun onDeleteTracker(trackerId: Long, groupId: Long?) {
        viewModelScope.launch(io) {
            val tracker = dataInteractor.getTrackerById(trackerId) ?: return@launch
            dataInteractor.deleteTracker(TrackerDeleteRequest(trackerId = trackerId, groupId = groupId))
            // Only disable widgets when the tracker is fully deleted (groupId = null means delete everywhere)
            if (groupId == null) {
                timerServiceInteractor.requestWidgetsDisabledForFeatureId(tracker.featureId)
            }
        }
    }

    override fun onDeleteGraphStat(id: Long, groupId: Long?) {
        viewModelScope.launch(io) {
            dataInteractor.deleteGraph(GraphDeleteRequest(graphStatId = id, groupId = groupId))
        }
    }

    override fun onDeleteGroup(id: Long, parentGroupId: Long?) {
        viewModelScope.launch(io) {
            val deletedFeatureIds =
                dataInteractor.deleteGroup(GroupDeleteRequest(groupId = id, parentGroupId = parentGroupId)).deletedFeatureIds
            deletedFeatureIds.forEach { timerServiceInteractor.requestWidgetsDisabledForFeatureId(it) }
        }
    }

    override fun onDeleteFunction(id: Long, groupId: Long?) {
        viewModelScope.launch(io) {
            dataInteractor.deleteFunction(FunctionDeleteRequest(functionId = id, groupId = groupId))
        }
    }

    override fun duplicateFunction(displayFunction: DisplayFunction) {
        viewModelScope.launch(io) {
            val currentGroupId = groupId.value ?: return@launch
            val function = dataInteractor.getFunctionById(displayFunction.id)
            function?.let { dataInteractor.duplicateFunction(it, currentGroupId) }
        }
    }

    override fun duplicateGraphOrStat(graphOrStatViewData: IGraphStatViewData) {
        viewModelScope.launch(io) {
            val currentGroupId = groupId.value ?: return@launch
            val gs = graphOrStatViewData.graphOrStat
            gsiProvider.getDataSourceAdapter(gs.type).duplicateGraphOrStat(gs, currentGroupId)
        }
    }

    override fun onConsumedShowDurationInputDialog() {
        _showDurationInputDialog.value = null
    }

    override fun stopTimer(tracker: DisplayTracker) {
        viewModelScope.launch(io) {
            dataInteractor.stopTimerForTracker(tracker.id)?.let {
                withContext(ui) {
                    _showDurationInputDialog.value =
                        GroupViewModel.DurationInputDialogData(tracker.id, it)
                }
            }
            timerServiceInteractor.requestWidgetUpdatesForFeatureId(tracker.featureId)
        }
    }

    override fun playTimer(tracker: DisplayTracker) {
        viewModelScope.launch(io) {
            dataInteractor.playTimerForTracker(tracker.id)
            timerServiceInteractor.startTimerNotificationService()
            timerServiceInteractor.requestWidgetUpdatesForFeatureId(tracker.featureId)
        }
    }

    override fun onDragStart() {
        if (isDragging.value) return
        // Create a temporary copy of the current children for faster
        // responsive mutations while dragging
        temporaryDragDropChildren.value = currentChildren.value.toMutableList()
        isDragging.value = true
    }

    override fun onDragSwap(from: Int, to: Int) {
        if (!isDragging.value) return
        if (from !in temporaryDragDropChildren.value.indices) return
        if (to !in temporaryDragDropChildren.value.indices) return
        // Swap the temporary children in place synchronously
        temporaryDragDropChildren.value = temporaryDragDropChildren.value.toMutableList()
            .apply { add(to, removeAt(from)) }
    }

    override fun onDragEnd() {
        if (!isDragging.value) return

        viewModelScope.launch {
            val children = temporaryDragDropChildren.value
            dataInteractor.updateGroupChildOrder(
                groupId.value ?: return@launch,
                children.mapIndexed { index, child ->
                    GroupChildDisplayIndex(
                        type = child.type,
                        id = child.id,
                        displayIndex = index
                    )
                }
            )

            val expectedIndices = children
                .mapIndexed { index, child -> Pair(child.type, child.id) to index }
                .toMap()

            // Wait until dbDisplayIndices reflects the new order before switching back
            // to the real children, otherwise the UI could glitch swapping back and forth.
            try {
                withTimeout(500) {
                    dbDisplayIndices.filterNotNull().first { indices ->
                        expectedIndices.all { (key, expectedIndex) ->
                            indices[key] == expectedIndex
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e(
                    e,
                    "The database did not update the group child indexes within 500ms. Drag and drop update may have failed."
                )
            }

            // Switch back to showing the real children from the database
            isDragging.value = false
            temporaryDragDropChildren.value = emptyList()
        }
    }
}