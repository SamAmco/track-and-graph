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
import com.samco.trackandgraph.data.database.dto.GraphOrStat
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
import com.samco.trackandgraph.util.flatMapLatestScan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
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
    fun onDeleteFeature(id: Long)
    fun onDeleteGraphStat(id: Long)
    fun onDeleteGroup(id: Long)
    fun onDeleteFunction(id: Long)

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

    private sealed class UpdateType {
        data object Trackers : UpdateType()
        data object Groups : UpdateType()
        data object Functions : UpdateType()
        data object AllGraphs : UpdateType()
        data object GraphDeleted : UpdateType()
        data class Graph(val graphId: Long) : UpdateType()
        data object All : UpdateType()
        data object DisplayIndices : UpdateType()
    }

    /**
     * Whenever the database is updated get the type of update to determine what to update in the UI
     */
    private fun asUpdateType(dataUpdateType: DataUpdateType) = when (dataUpdateType) {
        is DataUpdateType.DataPoint -> listOf(
            UpdateType.Trackers,
        )

        DataUpdateType.TrackerCreated -> listOf(
            UpdateType.Trackers,
            UpdateType.DisplayIndices
        )

        DataUpdateType.TrackerDeleted -> listOf(
            UpdateType.Trackers,
            UpdateType.DisplayIndices,
            UpdateType.AllGraphs
        )

        DataUpdateType.GroupCreated, DataUpdateType.GroupDeleted -> listOf(
            UpdateType.Groups,
            UpdateType.DisplayIndices
        )

        DataUpdateType.GroupDeleted -> listOf(
            UpdateType.Groups,
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

        is DataUpdateType.FunctionCreated -> listOf(
            UpdateType.Functions,
            UpdateType.DisplayIndices
        )

        is DataUpdateType.FunctionDeleted -> listOf(
            UpdateType.Functions,
            UpdateType.DisplayIndices
        )

        is DataUpdateType.FunctionUpdated -> listOf(
            UpdateType.Functions
        )

        DataUpdateType.Unknown -> listOf(UpdateType.All)

        DataUpdateType.GlobalNote, DataUpdateType.Reminder -> null
    }

    private val onUpdateChildrenForGroup =
        combine(
            groupId.filterNotNull(),
            dataInteractor.getDataUpdateEvents()
                .map { asUpdateType(it) }
                .filterNotNull()
                .flatMapConcat { it.asFlow() }
                .onStart { emit(UpdateType.All) }
        ) { a, b -> Pair(a, b) }
            .shareIn(viewModelScope, SharingStarted.Lazily, 1)


    private val graphChildren = onUpdateChildrenForGroup
        .filter {
            it.second in arrayOf(
                UpdateType.All,
                UpdateType.AllGraphs,
                UpdateType.DisplayIndices,
                UpdateType.GraphDeleted
            ) || it.second is UpdateType.Graph
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
            Pair(UpdateType.All, emptyList())
        ) { _, event ->
            val eventType = event.second
            val groupId = event.first

            //Don't need to get the graphs from the database again if the update type is
            //GraphsForFeature as this simply means a feature was updated and we need to
            //recalculate the inner graph data
            Pair(eventType, getGraphObjects(groupId))
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
                //Shouldn't ever happen
                else -> flowOf(viewData)
            }
        }
        .map { graphsToGroupChildren(it) }
        .flowOn(io)

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

        // Ideally we just get the view data for all the graphs and map them to the
        // new GraphOrStat objects.
        val dontUpdate = newGraphStats
            .map { Pair(it, oldGraphsById[it.id]?.viewData) }
            .filter { it.second?.viewData?.state == IGraphStatViewData.State.READY }
            .mapNotNull { pair -> pair.second?.let { GraphWithViewData(pair.first, it) } }

        // But any graphs that had not finished loading we have lost the opportunity to
        // get their view data
        val update = newGraphStats
            .map { Pair(it, oldGraphsById[it.id]?.viewData) }
            .filter { it.second?.viewData?.state != IGraphStatViewData.State.READY }
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

    private val functionChildren = onUpdateChildrenForGroup
        .filter {
            it.second in arrayOf(
                UpdateType.Functions,
                UpdateType.All,
                UpdateType.DisplayIndices,
            )
        }
        .debounce(10L)
        .map { getFunctionChildren(it.first) }
        .flowOn(io)

    private val allChildren: StateFlow<List<GroupChild>> =
        combine(
            graphChildren, trackersChildren, groupChildren, functionChildren
        ) { graphs, trackers, groups, functions ->
            listOf(graphs, trackers, groups, functions)
        }
            //This debounce should be longer than the children debounce
            .debounce(50L)
            .map { childrenLists ->
                val children = mutableListOf<GroupChild>().apply {
                    childrenLists.forEach { addAll(it) }
                }
                sortChildren(children)
                children
            }
            .flowOn(io)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val isDragging = MutableStateFlow(false)
    private val temporaryDragDropChildren = MutableStateFlow<List<GroupChild>>(emptyList())

    override val currentChildren: StateFlow<List<GroupChild>> = isDragging
        // We use a temporary copy of the current children for faster
        // responsive mutations while dragging, so we don't have to wait
        // for the database updates from a background thread
        .flatMapLatest { if (it) temporaryDragDropChildren else allChildren }
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

    private fun sortChildren(children: MutableList<GroupChild>) = children.sortWith { a, b ->
        val aInd = a.displayIndex
        val bInd = b.displayIndex
        when {
            aInd < bInd -> -1
            bInd < aInd -> 1
            else -> {
                val aId = a.idForGroupOrdering
                val bId = b.idForGroupOrdering
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

    private suspend fun getFunctionChildren(groupId: Long): List<GroupChild> {
        return dataInteractor.getFunctionsForGroupSync(groupId).map { function ->
            val displayFunction = DisplayFunction(
                id = function.id,
                featureId = function.featureId,
                name = function.name,
                description = function.description
            )
            GroupChild.ChildFunction(function.id, function.displayIndex, displayFunction)
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

    override fun onDeleteFeature(id: Long) {
        viewModelScope.launch(io) {
            dataInteractor.deleteFeature(id)
            timerServiceInteractor.requestWidgetsDisabledForFeatureId(id)
        }
    }

    override fun onDeleteGraphStat(id: Long) {
        viewModelScope.launch(io) {
            dataInteractor.deleteGraphOrStat(id)
        }
    }

    override fun onDeleteGroup(id: Long) {
        viewModelScope.launch(io) {
            val deletedFeatureIds = dataInteractor.deleteGroup(id).deletedFeatureIds
            deletedFeatureIds.forEach { timerServiceInteractor.requestWidgetsDisabledForFeatureId(it) }
        }
    }

    override fun onDeleteFunction(id: Long) {
        viewModelScope.launch(io) {
            dataInteractor.deleteFunction(id)
        }
    }

    override fun duplicateFunction(displayFunction: DisplayFunction) {
        viewModelScope.launch(io) {
            val function = dataInteractor.getFunctionById(displayFunction.id)
            function?.let { dataInteractor.duplicateFunction(it) }
        }
    }

    override fun duplicateGraphOrStat(graphOrStatViewData: IGraphStatViewData) {
        viewModelScope.launch(io) {
            val gs = graphOrStatViewData.graphOrStat
            gsiProvider.getDataSourceAdapter(gs.type).duplicateGraphOrStat(gs)
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
            dataInteractor.updateGroupChildOrder(
                groupId.value ?: return@launch,
                temporaryDragDropChildren.value.map(GroupChild::toDto)
            )

            val expectedOrder = temporaryDragDropChildren.value
                .map { Pair(it.idForGroupOrdering, it.type) }

            // Wait until all children have been updated in the database
            // before switching back to the real children or the UI could
            // glitch swapping back and forth.
            try {
                withTimeout(500) {
                    allChildren.first { allChildren ->
                        val actualOrder = allChildren.map { Pair(it.idForGroupOrdering, it.type) }
                        actualOrder == expectedOrder
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e("The database did not update the group child indexes within 500ms. Drag and drop update may have failed.")
            }

            // Switch back to showing the real children from the database
            isDragging.value = false
            temporaryDragDropChildren.value = emptyList()
        }
    }
}