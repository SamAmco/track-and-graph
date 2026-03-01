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

package com.samco.trackandgraph.data.interactor

import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.GroupItemDao
import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStatCreateRequest
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStatUpdateRequest
import com.samco.trackandgraph.data.database.dto.BarChartCreateRequest
import com.samco.trackandgraph.data.database.dto.BarChartUpdateRequest
import com.samco.trackandgraph.data.database.dto.ComponentType
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.DeletedGroupInfo
import com.samco.trackandgraph.data.database.dto.DisplayNote
import com.samco.trackandgraph.data.database.dto.Feature
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionCreateRequest
import com.samco.trackandgraph.data.database.dto.FunctionDeleteRequest
import com.samco.trackandgraph.data.database.dto.FunctionUpdateRequest
import com.samco.trackandgraph.data.database.dto.GlobalNote
import com.samco.trackandgraph.data.database.dto.GraphDeleteRequest
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupChildDisplayIndex
import com.samco.trackandgraph.data.database.dto.GroupCreateRequest
import com.samco.trackandgraph.data.database.dto.GroupDeleteRequest
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem
import com.samco.trackandgraph.data.database.dto.GroupUpdateRequest
import com.samco.trackandgraph.data.database.dto.LastValueStatCreateRequest
import com.samco.trackandgraph.data.database.dto.LastValueStatUpdateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphCreateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphUpdateRequest
import com.samco.trackandgraph.data.database.dto.LuaGraphCreateRequest
import com.samco.trackandgraph.data.database.dto.LuaGraphUpdateRequest
import com.samco.trackandgraph.data.database.dto.MoveComponentRequest
import com.samco.trackandgraph.data.database.dto.PieChartCreateRequest
import com.samco.trackandgraph.data.database.dto.PieChartUpdateRequest
import com.samco.trackandgraph.data.database.dto.ReminderCreateRequest
import com.samco.trackandgraph.data.database.dto.ReminderDeleteRequest
import com.samco.trackandgraph.data.database.dto.ReminderDisplayOrderData
import com.samco.trackandgraph.data.database.dto.ReminderUpdateRequest
import com.samco.trackandgraph.data.database.dto.TimeHistogramCreateRequest
import com.samco.trackandgraph.data.database.dto.TimeHistogramUpdateRequest
import com.samco.trackandgraph.data.database.dto.TrackerCreateRequest
import com.samco.trackandgraph.data.database.dto.TrackerDeleteRequest
import com.samco.trackandgraph.data.database.dto.TrackerUpdateRequest
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
import com.samco.trackandgraph.data.dependencyanalyser.DependencyAnalyserProvider
import com.samco.trackandgraph.data.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

internal class DataInteractorImpl @Inject constructor(
    private val transactionHelper: DatabaseTransactionHelper,
    private val dao: TrackAndGraphDatabaseDao,
    private val groupItemDao: GroupItemDao,
    @IODispatcher private val io: CoroutineDispatcher,
    private val trackerHelper: TrackerHelper,
    private val functionHelper: FunctionHelper,
    private val reminderHelper: ReminderHelper,
    private val groupHelper: GroupHelper,
    private val graphHelper: GraphHelper,
    private val dependencyAnalyserProvider: DependencyAnalyserProvider,
) : DataInteractor,
    TrackerHelper by trackerHelper,
    FunctionHelper by functionHelper,
    ReminderHelper by reminderHelper,
    GroupHelper by groupHelper,
    GraphHelper by graphHelper {

    private val dataUpdateEvents = MutableSharedFlow<DataUpdateType>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // GroupHelper method overrides with event emission
    override suspend fun insertGroup(request: GroupCreateRequest): Long = withContext(io) {
        groupHelper.insertGroup(request).also {
            dataUpdateEvents.emit(DataUpdateType.GroupCreated)
            dataUpdateEvents.emit(DataUpdateType.DisplayIndex(request.parentGroupId))
        }
    }

    override suspend fun deleteGroup(request: GroupDeleteRequest): DeletedGroupInfo =
        withContext(io) {
            val deletedGroupInfo = groupHelper.deleteGroup(request)

            // Delete any orphaned graphs
            val orphanedGraphs = dependencyAnalyserProvider.create().getOrphanedGraphs()
            for (graphStatId in orphanedGraphs.graphStatIds) {
                dao.deleteGraphOrStat(graphStatId)
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatDeleted)
            }

            dataUpdateEvents.emit(DataUpdateType.GroupDeleted)
            return@withContext deletedGroupInfo
        }

    override suspend fun updateGroup(request: GroupUpdateRequest) = withContext(io) {
        groupHelper.updateGroup(request)
        dataUpdateEvents.emit(DataUpdateType.GroupUpdated)
    }

    override suspend fun getGroupGraphSync(rootGroupId: Long?): GroupGraph = withContext(io) {
        val rootGroup = if (rootGroupId != null) {
            dao.getGroupById(rootGroupId)?.toDto()
                ?: throw IllegalStateException("Group with id $rootGroupId not found")
        } else {
            // Get the root group (group with no parent)
            dao.getRootGroupSync()?.toDto() ?: throw IllegalStateException("No root group found")
        }

        buildGroupGraph(rootGroup)
    }

    private suspend fun buildGroupGraph(group: Group): GroupGraph {
        val children = mutableListOf<GroupGraphItem>()

        // Get child groups for this specific group
        val childGroups = dao.getGroupsForGroupSync(group.id)
            .map { it.toDto() }

        // Get trackers for this specific group using TrackerHelper
        val trackers = getTrackersForGroupSync(group.id)

        // Get graphs for this specific group
        val graphs = dao.getGraphsAndStatsByGroupIdSync(group.id)
            .map { it.toDto() }

        // Get functions for this specific group using FunctionHelper
        val functions = getFunctionsForGroupSync(group.id)

        // Add child groups recursively
        for (childGroup in childGroups) {
            children.add(GroupGraphItem.GroupNode(buildGroupGraph(childGroup)))
        }

        // Add trackers
        for (tracker in trackers) {
            children.add(GroupGraphItem.TrackerNode(tracker))
        }

        // Add graphs
        for (graph in graphs) {
            children.add(GroupGraphItem.GraphNode(graph))
        }

        // Add functions
        for (function in functions) {
            children.add(GroupGraphItem.FunctionNode(function))
        }

        return GroupGraph(group, children)
    }

    override suspend fun getFeaturesForGroupSync(groupId: Long): List<Feature> = withContext(io) {
        dao.getFeaturesForGroupSync(groupId).map { it.toDto() }
    }

    override suspend fun getFeatureById(featureId: Long): Feature? = withContext(io) {
        dao.getFeatureById(featureId)?.toDto()
    }

    override suspend fun createTracker(request: TrackerCreateRequest): Long = withContext(io) {
        val id = trackerHelper.createTracker(request)
        dataUpdateEvents.emit(DataUpdateType.TrackerCreated)
        dataUpdateEvents.emit(DataUpdateType.DisplayIndex(request.groupId))
        return@withContext id
    }

    override suspend fun updateTracker(request: TrackerUpdateRequest) = withContext(io) {
        trackerHelper.updateTracker(request)
        dataUpdateEvents.emit(DataUpdateType.TrackerUpdated)
    }

    override suspend fun deleteTracker(request: TrackerDeleteRequest) = withContext(io) {
        val tracker = dao.getTrackerById(request.trackerId) ?: return@withContext
        val featureId = tracker.featureId

        val dependentGraphs = dependencyAnalyserProvider.create().getDependentGraphs(featureId)

        trackerHelper.deleteTracker(request)

        dataUpdateEvents.emit(DataUpdateType.TrackerDeleted)
        handleOrphanedAndDependentGraphsForFeatureDelete(dependentGraphs.graphStatIds)
    }

    private suspend fun handleOrphanedAndDependentGraphsForFeatureDelete(dependentGraphIds: Set<Long>) {
        val orphanedGraphs = dependencyAnalyserProvider.create().getOrphanedGraphs()
        for (graphStatId in orphanedGraphs.graphStatIds) {
            dao.deleteGraphOrStat(graphStatId)
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatDeleted)
        }

        val graphsNeedingUpdate = dependentGraphIds - orphanedGraphs.graphStatIds
        for (graphStatId in graphsNeedingUpdate) {
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(graphStatId))
        }
    }

    override suspend fun updateDataPoints(
        trackerId: Long,
        whereValue: Double?,
        whereLabel: String?,
        toValue: Double?,
        toLabel: String?
    ) = withContext(io) {
        trackerHelper.updateDataPoints(
            trackerId = trackerId,
            whereValue = whereValue,
            whereLabel = whereLabel,
            toValue = toValue,
            toLabel = toLabel
        )
        val featureId = dao.getTrackerById(trackerId)?.featureId ?: return@withContext
        dataUpdateEvents.emit(DataUpdateType.DataPoint(featureId))
        val graphsNeedingUpdate = dependencyAnalyserProvider.create()
            .getDependentGraphs(featureId)
        for (graphStatId in graphsNeedingUpdate.graphStatIds) {
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(graphStatId))
        }
    }

    override suspend fun createReminder(request: ReminderCreateRequest): Long = withContext(io) {
        val id = reminderHelper.createReminder(request)
        dataUpdateEvents.emit(DataUpdateType.Reminder)
        return@withContext id
    }

    override suspend fun updateReminder(request: ReminderUpdateRequest) = withContext(io) {
        reminderHelper.updateReminder(request)
        dataUpdateEvents.emit(DataUpdateType.Reminder)
    }

    override suspend fun updateReminderScreenDisplayOrder(orders: List<ReminderDisplayOrderData>) =
        withContext(io) {
            reminderHelper.updateReminderScreenDisplayOrder(orders)
            dataUpdateEvents.emit(DataUpdateType.ReminderScreenDisplayOrder)
        }

    override suspend fun deleteReminder(request: ReminderDeleteRequest) = withContext(io) {
        reminderHelper.deleteReminder(request)
        dataUpdateEvents.emit(DataUpdateType.Reminder)
    }

    override suspend fun duplicateReminder(id: Long): Long = withContext(io) {
        val id = reminderHelper.duplicateReminder(id)
        dataUpdateEvents.emit(DataUpdateType.Reminder)
        return@withContext id
    }

    override suspend fun deleteDataPoint(dataPoint: DataPoint) = withContext(io) {
        dao.deleteDataPoint(dataPoint.toEntity())
        dataUpdateEvents.emit(DataUpdateType.DataPoint(dataPoint.featureId))
        val graphsNeedingUpdate = dependencyAnalyserProvider.create()
            .getDependentGraphs(dataPoint.featureId)
        for (graphStatId in graphsNeedingUpdate.graphStatIds) {
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(graphStatId))
        }
    }

    override suspend fun deleteDataPoints(dataPoints: List<DataPoint>) = withContext(io) {
        if (dataPoints.isEmpty()) return@withContext
        dao.deleteDataPoints(dataPoints.map { it.toEntity() })
        val featureIds = dataPoints.map { it.featureId }.toSet()
        for (featureId in featureIds) {
            dataUpdateEvents.emit(DataUpdateType.DataPoint(featureId))
            val graphsNeedingUpdate = dependencyAnalyserProvider.create()
                .getDependentGraphs(featureId)
            for (graphStatId in graphsNeedingUpdate.graphStatIds) {
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(graphStatId))
            }
        }
    }

    override suspend fun insertDataPoint(dataPoint: DataPoint): Long = withContext(io) {
        dao.insertDataPoint(dataPoint.toEntity()).also {
            dataUpdateEvents.emit(DataUpdateType.DataPoint(dataPoint.featureId))
            val graphsNeedingUpdate = dependencyAnalyserProvider.create()
                .getDependentGraphs(dataPoint.featureId)
            for (graphStatId in graphsNeedingUpdate.graphStatIds) {
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(graphStatId))
            }
        }
    }

    override suspend fun insertDataPoints(dataPoints: List<DataPoint>) = withContext(io) {
        if (dataPoints.isEmpty()) return@withContext
        dao.insertDataPoints(dataPoints.map { it.toEntity() }).also {
            dataUpdateEvents.emit(DataUpdateType.DataPoint(dataPoints.first().featureId))
            val depProvider = dependencyAnalyserProvider.create()
            val affectedGraphs = dataPoints.fold(emptySet<Long>()) { acc, dataPoint ->
                acc + depProvider.getDependentGraphs(dataPoint.featureId).graphStatIds
            }
            for (graphStatId in affectedGraphs) {
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(graphStatId))
            }
        }
    }

    override fun getDataUpdateEvents(): SharedFlow<DataUpdateType> = dataUpdateEvents

    override suspend fun hasAnyFeatures(): Boolean = withContext(io) { dao.hasAnyFeatures() }

    // =========================================================================
    // GraphHelper method overrides with event emission
    // =========================================================================

    override suspend fun deleteGraph(request: GraphDeleteRequest) = withContext(io) {
        graphHelper.deleteGraph(request)
        dataUpdateEvents.emit(DataUpdateType.GraphOrStatDeleted)
    }

    override suspend fun createLineGraph(request: LineGraphCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            graphHelper.createLineGraph(request)
                .also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it)) }
        }

    override suspend fun createPieChart(request: PieChartCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            graphHelper.createPieChart(request)
                .also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it)) }
        }

    override suspend fun createAverageTimeBetweenStat(request: AverageTimeBetweenStatCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            graphHelper.createAverageTimeBetweenStat(request)
                .also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it)) }
        }

    override suspend fun createTimeHistogram(request: TimeHistogramCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            graphHelper.createTimeHistogram(request)
                .also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it)) }
        }

    override suspend fun createLastValueStat(request: LastValueStatCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            graphHelper.createLastValueStat(request)
                .also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it)) }
        }

    override suspend fun createBarChart(request: BarChartCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            graphHelper.createBarChart(request)
                .also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it)) }
        }

    override suspend fun createLuaGraph(request: LuaGraphCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            graphHelper.createLuaGraph(request)
                .also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it)) }
        }

    override suspend fun updateLineGraph(request: LineGraphUpdateRequest) = withContext(io) {
        graphHelper.updateLineGraph(request)
        dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(request.graphStatId))
    }

    override suspend fun updatePieChart(request: PieChartUpdateRequest) = withContext(io) {
        graphHelper.updatePieChart(request)
        dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(request.graphStatId))
    }

    override suspend fun updateAverageTimeBetweenStat(request: AverageTimeBetweenStatUpdateRequest) =
        withContext(io) {
            graphHelper.updateAverageTimeBetweenStat(request)
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(request.graphStatId))
        }

    override suspend fun updateTimeHistogram(request: TimeHistogramUpdateRequest) =
        withContext(io) {
            graphHelper.updateTimeHistogram(request)
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(request.graphStatId))
        }

    override suspend fun updateLastValueStat(request: LastValueStatUpdateRequest) =
        withContext(io) {
            graphHelper.updateLastValueStat(request)
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(request.graphStatId))
        }

    override suspend fun updateBarChart(request: BarChartUpdateRequest) = withContext(io) {
        graphHelper.updateBarChart(request)
        dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(request.graphStatId))
    }

    override suspend fun updateLuaGraph(request: LuaGraphUpdateRequest) = withContext(io) {
        graphHelper.updateLuaGraph(request)
        dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(request.graphStatId))
    }

    override suspend fun duplicateLineGraph(graphStatId: Long, groupId: Long): Long? =
        withContext(io) {
            graphHelper.duplicateLineGraph(graphStatId, groupId)?.also {
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it))
                dataUpdateEvents.emit(DataUpdateType.DisplayIndex(groupId))
            }
        }

    override suspend fun duplicatePieChart(graphStatId: Long, groupId: Long): Long? =
        withContext(io) {
            graphHelper.duplicatePieChart(graphStatId, groupId)?.also {
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it))
                dataUpdateEvents.emit(DataUpdateType.DisplayIndex(groupId))
            }
        }

    override suspend fun duplicateAverageTimeBetweenStat(graphStatId: Long, groupId: Long): Long? =
        withContext(io) {
            graphHelper.duplicateAverageTimeBetweenStat(graphStatId, groupId)?.also {
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it))
                dataUpdateEvents.emit(DataUpdateType.DisplayIndex(groupId))
            }
        }

    override suspend fun duplicateTimeHistogram(graphStatId: Long, groupId: Long): Long? =
        withContext(io) {
            graphHelper.duplicateTimeHistogram(graphStatId, groupId)?.also {
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it))
                dataUpdateEvents.emit(DataUpdateType.DisplayIndex(groupId))
            }
        }

    override suspend fun duplicateLastValueStat(graphStatId: Long, groupId: Long): Long? =
        withContext(io) {
            graphHelper.duplicateLastValueStat(graphStatId, groupId)?.also {
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it))
                dataUpdateEvents.emit(DataUpdateType.DisplayIndex(groupId))
            }
        }

    override suspend fun duplicateBarChart(graphStatId: Long, groupId: Long): Long? =
        withContext(io) {
            graphHelper.duplicateBarChart(graphStatId, groupId)?.also {
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it))
                dataUpdateEvents.emit(DataUpdateType.DisplayIndex(groupId))
            }
        }

    override suspend fun duplicateLuaGraph(graphStatId: Long, groupId: Long): Long? =
        withContext(io) {
            graphHelper.duplicateLuaGraph(graphStatId, groupId)?.also {
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(it))
                dataUpdateEvents.emit(DataUpdateType.DisplayIndex(groupId))
            }
        }

    // =========================================================================
    // Private helper methods
    // =========================================================================

    private suspend fun <R> performAtomicUpdate(
        updateType: DataUpdateType? = null,
        block: suspend () -> R
    ) = withContext(io) {
        transactionHelper
            .withTransaction { block() }
            .also { updateType?.let { dataUpdateEvents.emit(it) } }
    }

    private suspend fun shiftUpGroupChildIndexes(groupId: Long) =
        performAtomicUpdate(DataUpdateType.DisplayIndex(groupId)) {
            groupItemDao.shiftDisplayIndexesDown(groupId)
            dataUpdateEvents.emit(DataUpdateType.DisplayIndex(groupId))
        }

    // =========================================================================
    // Other DataInteractor methods
    // =========================================================================

    override fun getAllDisplayNotes(): Flow<List<DisplayNote>> {
        return dao.getAllDisplayNotes().map { notes ->
            notes.map { it.toDto() }
        }
    }

    override suspend fun removeNote(timestamp: OffsetDateTime, trackerId: Long) = withContext(io) {
        val featureId = dao.getTrackerById(trackerId)?.featureId ?: return@withContext
        dao.removeNote(timestamp.toInstant().toEpochMilli(), featureId).also {
            dataUpdateEvents.emit(DataUpdateType.DataPoint(featureId))
        }
        val dependentGraphs = dependencyAnalyserProvider.create().getDependentGraphs(featureId)
        for (graphStatId in dependentGraphs.graphStatIds) {
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(graphStatId))
        }
    }

    override suspend fun deleteGlobalNote(note: GlobalNote) = withContext(io) {
        dao.deleteGlobalNote(note.toEntity())
            .also { dataUpdateEvents.emit(DataUpdateType.GlobalNote) }
    }

    override suspend fun insertGlobalNote(note: GlobalNote): Long = withContext(io) {
        dao.insertGlobalNote(note.toEntity())
            .also { dataUpdateEvents.emit(DataUpdateType.GlobalNote) }
    }

    override suspend fun getGlobalNoteByTimeSync(timestamp: OffsetDateTime?): GlobalNote? =
        withContext(io) {
            timestamp?.let {
                dao.getGlobalNoteByTimeSync(it.toInstant().toEpochMilli())?.toDto()
            }
        }

    override suspend fun getAllGlobalNotesSync(): List<GlobalNote> = withContext(io) {
        dao.getAllGlobalNotesSync().map { it.toDto() }
    }

    override suspend fun updateGroupChildOrder(
        groupId: Long,
        children: List<GroupChildDisplayIndex>
    ) {
        groupHelper.updateGroupChildOrder(groupId, children)
        dataUpdateEvents.emit(DataUpdateType.DisplayIndex(groupId))

    }

    override fun onImportedExternalData() {
        // Emit data update event to notify observers that external data was imported
        dataUpdateEvents.tryEmit(DataUpdateType.Unknown)
    }

    override suspend fun playTimerForTracker(trackerId: Long): Long? {
        return trackerHelper.playTimerForTracker(trackerId)?.also {
            dataUpdateEvents.emit(DataUpdateType.TrackerUpdated)
        }
    }

    override suspend fun stopTimerForTracker(trackerId: Long): Duration? =
        trackerHelper.stopTimerForTracker(trackerId).also {
            dataUpdateEvents.emit(DataUpdateType.TrackerUpdated)
        }

    override suspend fun getAllFeaturesSync(): List<Feature> = withContext(io) {
        dao.getAllFeaturesSync().map { it.toDto() }
    }

    // FunctionHelper method overrides with event emission
    override suspend fun insertFunction(request: FunctionCreateRequest): Long? = withContext(io) {
        val id = functionHelper.insertFunction(request)
        if (id != null) {
            dataUpdateEvents.emit(DataUpdateType.FunctionCreated(id))
            dataUpdateEvents.emit(DataUpdateType.DisplayIndex(request.groupId))
        }
        return@withContext id
    }

    override suspend fun updateFunction(request: FunctionUpdateRequest) = withContext(io) {
        val existingFunction = dao.getFunctionById(request.id) ?: return@withContext
        functionHelper.updateFunction(request)
        dataUpdateEvents.emit(DataUpdateType.FunctionUpdated(existingFunction.featureId))
        val dependentGraphs = dependencyAnalyserProvider.create()
            .getDependentGraphs(existingFunction.featureId)
        for (graphStatId in dependentGraphs.graphStatIds) {
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(graphStatId))
        }
    }

    override suspend fun duplicateFunction(function: Function, groupId: Long): Long? =
        withContext(io) {
            val newFunctionId = functionHelper.duplicateFunction(function, groupId)
            if (newFunctionId != null) {
                dataUpdateEvents.emit(DataUpdateType.FunctionCreated(newFunctionId))
                dataUpdateEvents.emit(DataUpdateType.DisplayIndex(groupId))
            }
            return@withContext newFunctionId
        }

    override suspend fun deleteFunction(request: FunctionDeleteRequest) = withContext(io) {
        val function = dao.getFunctionById(request.functionId) ?: return@withContext
        val featureId = function.featureId

        val dependentGraphs = dependencyAnalyserProvider.create().getDependentGraphs(featureId)

        functionHelper.deleteFunction(request)

        dataUpdateEvents.emit(DataUpdateType.FunctionDeleted(featureId))
        handleOrphanedAndDependentGraphsForFeatureDelete(dependentGraphs.graphStatIds)
    }

    override suspend fun getFeatureIdsDependingOn(featureId: Long): Set<Long> = withContext(io) {
        dependencyAnalyserProvider.create().getFeaturesDependingOn(featureId).featureIds
    }

    override suspend fun getDependencyFeatureIdsOf(featureId: Long): Set<Long> = withContext(io) {
        dependencyAnalyserProvider.create().getDependenciesOf(featureId).featureIds
    }

    override suspend fun moveComponent(request: MoveComponentRequest) = withContext(io) {
        if (request.fromGroupId == request.toGroupId) return@withContext

        val itemType = when (request.type) {
            ComponentType.TRACKER -> GroupItemType.TRACKER
            ComponentType.FUNCTION -> GroupItemType.FUNCTION
            ComponentType.GROUP -> GroupItemType.GROUP
            ComponentType.GRAPH -> GroupItemType.GRAPH
        }

        if (request.type == ComponentType.GROUP) {
            validateGroupNotMovingToDescendant(request.id, request.toGroupId)
        }

        val existingItem = groupItemDao.getGroupItem(request.fromGroupId, request.id, itemType)
            ?: return@withContext

        groupItemDao.deleteGroupItem(existingItem.id)
        groupItemDao.shiftDisplayIndexesDown(request.toGroupId)
        groupItemDao.insertGroupItem(
            GroupItem(
                groupId = request.toGroupId,
                displayIndex = 0,
                childId = request.id,
                type = itemType,
            )
        )

        when (request.type) {
            ComponentType.TRACKER -> dataUpdateEvents.emit(DataUpdateType.TrackerUpdated)
            ComponentType.FUNCTION -> dataUpdateEvents.emit(DataUpdateType.FunctionUpdated(request.id))
            ComponentType.GROUP -> dataUpdateEvents.emit(DataUpdateType.GroupUpdated)
            ComponentType.GRAPH -> dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(request.id))
        }
        dataUpdateEvents.emit(DataUpdateType.DisplayIndex(request.toGroupId))
    }

    private fun validateGroupNotMovingToDescendant(groupId: Long, toGroupId: Long) {
        val toVisit = ArrayDeque<Long>()
        toVisit.add(groupId)
        while (toVisit.isNotEmpty()) {
            val current = toVisit.removeFirst()
            val childGroups = groupItemDao.getGroupItemsByType(current, GroupItemType.GROUP)
            for (item in childGroups) {
                if (item.childId == toGroupId) {
                    throw IllegalArgumentException("Cannot move group to its own descendant")
                }
                toVisit.add(item.childId)
            }
        }
    }

}
