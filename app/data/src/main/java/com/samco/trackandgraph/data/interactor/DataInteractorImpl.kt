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
import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStat
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStatCreateRequest
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStatUpdateRequest
import com.samco.trackandgraph.data.database.dto.BarChart
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
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupChildOrderData
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.data.database.dto.GroupCreateRequest
import com.samco.trackandgraph.data.database.dto.GroupDeleteRequest
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem
import com.samco.trackandgraph.data.database.dto.GroupUpdateRequest
import com.samco.trackandgraph.data.database.dto.LastValueStat
import com.samco.trackandgraph.data.database.dto.LastValueStatCreateRequest
import com.samco.trackandgraph.data.database.dto.LastValueStatUpdateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphCreateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphFeature
import com.samco.trackandgraph.data.database.dto.LineGraphUpdateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.data.database.dto.LuaGraphCreateRequest
import com.samco.trackandgraph.data.database.dto.LuaGraphFeature
import com.samco.trackandgraph.data.database.dto.LuaGraphUpdateRequest
import com.samco.trackandgraph.data.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.data.database.dto.MoveComponentRequest
import com.samco.trackandgraph.data.database.dto.PieChart
import com.samco.trackandgraph.data.database.dto.PieChartCreateRequest
import com.samco.trackandgraph.data.database.dto.PieChartUpdateRequest
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.TimeHistogram
import com.samco.trackandgraph.data.database.dto.TimeHistogramCreateRequest
import com.samco.trackandgraph.data.database.dto.TimeHistogramUpdateRequest
import com.samco.trackandgraph.data.database.dto.TrackerCreateRequest
import com.samco.trackandgraph.data.database.dto.TrackerDeleteRequest
import com.samco.trackandgraph.data.database.dto.TrackerUpdateRequest
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
    @IODispatcher private val io: CoroutineDispatcher,
    private val trackerHelper: TrackerHelper,
    private val functionHelper: FunctionHelper,
    private val reminderHelper: ReminderHelper,
    private val groupHelper: GroupHelper,
    private val dependencyAnalyserProvider: DependencyAnalyserProvider,
) : DataInteractor,
    TrackerHelper by trackerHelper,
    FunctionHelper by functionHelper,
    ReminderHelper by reminderHelper,
    GroupHelper by groupHelper {

    private val dataUpdateEvents = MutableSharedFlow<DataUpdateType>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // GroupHelper method overrides with event emission
    override suspend fun insertGroup(request: GroupCreateRequest): Long = withContext(io) {
        groupHelper.insertGroup(request)
            .also { dataUpdateEvents.emit(DataUpdateType.GroupCreated) }
    }

    override suspend fun deleteGroup(request: GroupDeleteRequest): DeletedGroupInfo = withContext(io) {
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
            dao.getGroupById(rootGroupId).toDto()
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
        return@withContext id
    }

    override suspend fun updateTracker(request: TrackerUpdateRequest) = withContext(io) {
        trackerHelper.updateTracker(request)
        dataUpdateEvents.emit(DataUpdateType.TrackerUpdated)
    }

    override suspend fun deleteTracker(request: TrackerDeleteRequest) = withContext(io) {
        val tracker = dao.getTrackerById(request.trackerId)
            ?: throw IllegalArgumentException("Tracker not found: ${request.trackerId}")
        deleteFeature(tracker.featureId, isTracker = true)
    }

    private suspend fun deleteFeature(featureId: Long, isTracker: Boolean) {
        val allDependentGraphs = dependencyAnalyserProvider.create()
            .getDependentGraphs(featureId)

        // We need to make sure all cascade deletes are complete before we continue
        // hence the atomic update
        performAtomicUpdate { dao.deleteFeature(featureId) }

        if (isTracker) dataUpdateEvents.emit(DataUpdateType.TrackerDeleted)
        else dataUpdateEvents.emit(DataUpdateType.FunctionDeleted(featureId))

        val orphanedGraphs = dependencyAnalyserProvider.create().getOrphanedGraphs()
        for (graphStatId in orphanedGraphs.graphStatIds) {
            dao.deleteGraphOrStat(graphStatId)
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatDeleted)
        }

        val graphsNeedingUpdate = allDependentGraphs.graphStatIds - orphanedGraphs.graphStatIds
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

    override suspend fun insertReminder(reminder: Reminder): Long = withContext(io) {
        val id = reminderHelper.insertReminder(reminder)
        dataUpdateEvents.emit(DataUpdateType.Reminder)
        return@withContext id
    }

    override suspend fun updateReminder(reminder: Reminder) = withContext(io) {
        reminderHelper.updateReminder(reminder)
        dataUpdateEvents.emit(DataUpdateType.Reminder)
    }

    override suspend fun deleteReminder(id: Long) = withContext(io) {
        reminderHelper.deleteReminder(id)
        dataUpdateEvents.emit(DataUpdateType.Reminder)
    }

    override suspend fun duplicateReminder(reminder: Reminder): Long = withContext(io) {
        val id = reminderHelper.duplicateReminder(reminder)
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

    // =========================================================================
    // GraphHelper - Get methods
    // =========================================================================

    override suspend fun getGraphStatById(graphStatId: Long): GraphOrStat = withContext(io) {
        dao.getGraphStatById(graphStatId).toDto()
    }

    override suspend fun tryGetGraphStatById(graphStatId: Long): GraphOrStat? = withContext(io) {
        dao.tryGetGraphStatById(graphStatId)?.toDto()
    }

    override suspend fun getLineGraphByGraphStatId(graphStatId: Long): LineGraphWithFeatures? =
        withContext(io) {
            dao.getLineGraphByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getPieChartByGraphStatId(graphStatId: Long): PieChart? = withContext(io) {
        dao.getPieChartByGraphStatId(graphStatId)?.toDto()
    }

    override suspend fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat? =
        withContext(io) {
            dao.getAverageTimeBetweenStatByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram? =
        withContext(io) {
            dao.getTimeHistogramByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getLastValueStatByGraphStatId(graphStatId: Long): LastValueStat? =
        withContext(io) {
            dao.getLastValueStatByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getBarChartByGraphStatId(graphStatId: Long): BarChart? =
        withContext(io) {
            dao.getBarChartByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getLuaGraphByGraphStatId(graphStatId: Long): LuaGraphWithFeatures? =
        withContext(io) {
            dao.getLuaGraphByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat> =
        withContext(io) {
            dao.getGraphsAndStatsByGroupIdSync(groupId).map { it.toDto() }
        }

    override suspend fun getAllGraphStatsSync(): List<GraphOrStat> = withContext(io) {
        dao.getAllGraphStatsSync().map { it.toDto() }
    }

    override suspend fun hasAnyGraphs(): Boolean = withContext(io) { dao.hasAnyGraphs() }

    override suspend fun hasAnyLuaGraphs(): Boolean = withContext(io) { dao.hasAnyLuaGraphs() }

    override suspend fun hasAnyFeatures(): Boolean = withContext(io) { dao.hasAnyFeatures() }

    // =========================================================================
    // GraphHelper - Delete method
    // =========================================================================

    override suspend fun deleteGraph(request: GraphDeleteRequest) = withContext(io) {
        dao.deleteGraphOrStat(request.graphStatId)
        dataUpdateEvents.emit(DataUpdateType.GraphOrStatDeleted)
    }

    // =========================================================================
    // GraphHelper - Create methods
    // =========================================================================

    override suspend fun createLineGraph(request: LineGraphCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            val graphStatId = insertGraphOrStat(request.name, request.groupId, GraphStatType.LINE_GRAPH)
            val lineGraphId = dao.insertLineGraph(
                com.samco.trackandgraph.data.database.entity.LineGraph(
                    id = 0L,
                    graphStatId = graphStatId,
                    sampleSize = request.config.sampleSize,
                    yRangeType = request.config.yRangeType,
                    yFrom = request.config.yFrom,
                    yTo = request.config.yTo,
                    endDate = request.config.endDate
                )
            )
            val features = request.config.features.map { config ->
                com.samco.trackandgraph.data.database.entity.LineGraphFeature(
                    id = 0L,
                    lineGraphId = lineGraphId,
                    featureId = config.featureId,
                    name = config.name,
                    colorIndex = config.colorIndex,
                    averagingMode = config.averagingMode,
                    plottingMode = config.plottingMode,
                    pointStyle = config.pointStyle,
                    offset = config.offset,
                    scale = config.scale,
                    durationPlottingMode = config.durationPlottingMode
                )
            }
            dao.insertLineGraphFeatures(features)
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(graphStatId))
            graphStatId
        }

    override suspend fun createPieChart(request: PieChartCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            val graphStatId = insertGraphOrStat(request.name, request.groupId, GraphStatType.PIE_CHART)
            dao.insertPieChart(
                com.samco.trackandgraph.data.database.entity.PieChart(
                    id = 0L,
                    graphStatId = graphStatId,
                    featureId = request.config.featureId,
                    sampleSize = request.config.sampleSize,
                    endDate = request.config.endDate,
                    sumByCount = request.config.sumByCount
                )
            )
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(graphStatId))
            graphStatId
        }

    override suspend fun createAverageTimeBetweenStat(request: AverageTimeBetweenStatCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            val graphStatId = insertGraphOrStat(request.name, request.groupId, GraphStatType.AVERAGE_TIME_BETWEEN)
            dao.insertAverageTimeBetweenStat(
                com.samco.trackandgraph.data.database.entity.AverageTimeBetweenStat(
                    id = 0L,
                    graphStatId = graphStatId,
                    featureId = request.config.featureId,
                    fromValue = request.config.fromValue,
                    toValue = request.config.toValue,
                    sampleSize = request.config.sampleSize,
                    labels = request.config.labels,
                    endDate = request.config.endDate,
                    filterByRange = request.config.filterByRange,
                    filterByLabels = request.config.filterByLabels
                )
            )
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(graphStatId))
            graphStatId
        }

    override suspend fun createTimeHistogram(request: TimeHistogramCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            val graphStatId = insertGraphOrStat(request.name, request.groupId, GraphStatType.TIME_HISTOGRAM)
            dao.insertTimeHistogram(
                com.samco.trackandgraph.data.database.entity.TimeHistogram(
                    id = 0L,
                    graphStatId = graphStatId,
                    featureId = request.config.featureId,
                    sampleSize = request.config.sampleSize,
                    window = request.config.window,
                    sumByCount = request.config.sumByCount,
                    endDate = request.config.endDate
                )
            )
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(graphStatId))
            graphStatId
        }

    override suspend fun createLastValueStat(request: LastValueStatCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            val graphStatId = insertGraphOrStat(request.name, request.groupId, GraphStatType.LAST_VALUE)
            dao.insertLastValueStat(
                com.samco.trackandgraph.data.database.entity.LastValueStat(
                    id = 0L,
                    graphStatId = graphStatId,
                    featureId = request.config.featureId,
                    endDate = request.config.endDate,
                    fromValue = request.config.fromValue,
                    toValue = request.config.toValue,
                    labels = request.config.labels,
                    filterByRange = request.config.filterByRange,
                    filterByLabels = request.config.filterByLabels
                )
            )
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(graphStatId))
            graphStatId
        }

    override suspend fun createBarChart(request: BarChartCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            val graphStatId = insertGraphOrStat(request.name, request.groupId, GraphStatType.BAR_CHART)
            dao.insertBarChart(
                com.samco.trackandgraph.data.database.entity.BarChart(
                    id = 0L,
                    graphStatId = graphStatId,
                    featureId = request.config.featureId,
                    endDate = request.config.endDate,
                    sampleSize = request.config.sampleSize,
                    yRangeType = request.config.yRangeType,
                    yTo = request.config.yTo,
                    scale = request.config.scale,
                    barPeriod = request.config.barPeriod,
                    sumByCount = request.config.sumByCount
                )
            )
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(graphStatId))
            graphStatId
        }

    override suspend fun createLuaGraph(request: LuaGraphCreateRequest): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(request.groupId)
            val graphStatId = insertGraphOrStat(request.name, request.groupId, GraphStatType.LUA_SCRIPT)
            val luaGraphId = dao.insertLuaGraph(
                com.samco.trackandgraph.data.database.entity.LuaGraph(
                    id = 0L,
                    graphStatId = graphStatId,
                    script = request.config.script
                )
            )
            val features = request.config.features.map { config ->
                com.samco.trackandgraph.data.database.entity.LuaGraphFeature(
                    id = 0L,
                    luaGraphId = luaGraphId,
                    featureId = config.featureId,
                    name = config.name
                )
            }
            dao.insertLuaGraphFeatures(features)
            dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(graphStatId))
            graphStatId
        }

    // =========================================================================
    // GraphHelper - Update methods
    // =========================================================================

    override suspend fun updateLineGraph(request: LineGraphUpdateRequest) =
        performAtomicUpdate(DataUpdateType.GraphOrStatUpdated(request.graphStatId)) {
            val existing = dao.getGraphStatById(request.graphStatId)
            val existingLineGraph = dao.getLineGraphByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Line graph not found for graphStatId: ${request.graphStatId}")

            // Update GraphOrStat if name changed
            if (request.name != null) {
                dao.updateGraphOrStat(existing.copy(name = request.name))
            }

            // Update config if provided
            if (request.config != null) {
                dao.updateLineGraph(
                    existingLineGraph.toLineGraph().copy(
                        sampleSize = request.config.sampleSize,
                        yRangeType = request.config.yRangeType,
                        yFrom = request.config.yFrom,
                        yTo = request.config.yTo,
                        endDate = request.config.endDate
                    )
                )
                dao.deleteFeaturesForLineGraph(existingLineGraph.id)
                val features = request.config.features.map { config ->
                    com.samco.trackandgraph.data.database.entity.LineGraphFeature(
                        id = 0L,
                        lineGraphId = existingLineGraph.id,
                        featureId = config.featureId,
                        name = config.name,
                        colorIndex = config.colorIndex,
                        averagingMode = config.averagingMode,
                        plottingMode = config.plottingMode,
                        pointStyle = config.pointStyle,
                        offset = config.offset,
                        scale = config.scale,
                        durationPlottingMode = config.durationPlottingMode
                    )
                }
                dao.insertLineGraphFeatures(features)
            }
        }

    override suspend fun updatePieChart(request: PieChartUpdateRequest) =
        performAtomicUpdate(DataUpdateType.GraphOrStatUpdated(request.graphStatId)) {
            val existing = dao.getGraphStatById(request.graphStatId)
            val existingPieChart = dao.getPieChartByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Pie chart not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                dao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                dao.updatePieChart(
                    existingPieChart.copy(
                        featureId = request.config.featureId,
                        sampleSize = request.config.sampleSize,
                        endDate = request.config.endDate,
                        sumByCount = request.config.sumByCount
                    )
                )
            }
        }

    override suspend fun updateAverageTimeBetweenStat(request: AverageTimeBetweenStatUpdateRequest) =
        performAtomicUpdate(DataUpdateType.GraphOrStatUpdated(request.graphStatId)) {
            val existing = dao.getGraphStatById(request.graphStatId)
            val existingStat = dao.getAverageTimeBetweenStatByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Average time between stat not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                dao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                dao.updateAverageTimeBetweenStat(
                    existingStat.copy(
                        featureId = request.config.featureId,
                        fromValue = request.config.fromValue,
                        toValue = request.config.toValue,
                        sampleSize = request.config.sampleSize,
                        labels = request.config.labels,
                        endDate = request.config.endDate,
                        filterByRange = request.config.filterByRange,
                        filterByLabels = request.config.filterByLabels
                    )
                )
            }
        }

    override suspend fun updateTimeHistogram(request: TimeHistogramUpdateRequest) =
        performAtomicUpdate(DataUpdateType.GraphOrStatUpdated(request.graphStatId)) {
            val existing = dao.getGraphStatById(request.graphStatId)
            val existingStat = dao.getTimeHistogramByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Time histogram not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                dao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                dao.updateTimeHistogram(
                    existingStat.copy(
                        featureId = request.config.featureId,
                        sampleSize = request.config.sampleSize,
                        window = request.config.window,
                        sumByCount = request.config.sumByCount,
                        endDate = request.config.endDate
                    )
                )
            }
        }

    override suspend fun updateLastValueStat(request: LastValueStatUpdateRequest) =
        performAtomicUpdate(DataUpdateType.GraphOrStatUpdated(request.graphStatId)) {
            val existing = dao.getGraphStatById(request.graphStatId)
            val existingStat = dao.getLastValueStatByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Last value stat not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                dao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                dao.updateLastValueStat(
                    existingStat.copy(
                        featureId = request.config.featureId,
                        endDate = request.config.endDate,
                        fromValue = request.config.fromValue,
                        toValue = request.config.toValue,
                        labels = request.config.labels,
                        filterByRange = request.config.filterByRange,
                        filterByLabels = request.config.filterByLabels
                    )
                )
            }
        }

    override suspend fun updateBarChart(request: BarChartUpdateRequest) =
        performAtomicUpdate(DataUpdateType.GraphOrStatUpdated(request.graphStatId)) {
            val existing = dao.getGraphStatById(request.graphStatId)
            val existingStat = dao.getBarChartByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Bar chart not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                dao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                dao.updateBarChart(
                    existingStat.copy(
                        featureId = request.config.featureId,
                        endDate = request.config.endDate,
                        sampleSize = request.config.sampleSize,
                        yRangeType = request.config.yRangeType,
                        yTo = request.config.yTo,
                        scale = request.config.scale,
                        barPeriod = request.config.barPeriod,
                        sumByCount = request.config.sumByCount
                    )
                )
            }
        }

    override suspend fun updateLuaGraph(request: LuaGraphUpdateRequest) =
        performAtomicUpdate(DataUpdateType.GraphOrStatUpdated(request.graphStatId)) {
            val existing = dao.getGraphStatById(request.graphStatId)
            val existingLuaGraph = dao.getLuaGraphByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Lua graph not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                dao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                dao.updateLuaGraph(
                    existingLuaGraph.toLuaGraph().copy(
                        script = request.config.script
                    )
                )
                dao.deleteFeaturesForLuaGraph(existingLuaGraph.id)
                val features = request.config.features.map { config ->
                    com.samco.trackandgraph.data.database.entity.LuaGraphFeature(
                        id = 0L,
                        luaGraphId = existingLuaGraph.id,
                        featureId = config.featureId,
                        name = config.name
                    )
                }
                dao.insertLuaGraphFeatures(features)
            }
        }

    // =========================================================================
    // GraphHelper - Duplicate methods
    // =========================================================================

    override suspend fun duplicateLineGraph(graphStatId: Long): Long? =
        performAtomicUpdate {
            val graphOrStat = dao.tryGetGraphStatById(graphStatId)?.toDto() ?: return@performAtomicUpdate null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat)
            dao.getLineGraphByGraphStatId(graphStatId)?.let {
                val copy = dao.insertLineGraph(
                    it.toLineGraph().copy(id = 0L, graphStatId = newGraphStatId)
                )
                dao.insertLineGraphFeatures(it.features.map { f ->
                    f.copy(id = 0L, lineGraphId = copy)
                })
                copy
            }.also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(newGraphStatId)) }
            newGraphStatId
        }

    override suspend fun duplicatePieChart(graphStatId: Long): Long? =
        performAtomicUpdate {
            val graphOrStat = dao.tryGetGraphStatById(graphStatId)?.toDto() ?: return@performAtomicUpdate null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat)
            dao.getPieChartByGraphStatId(graphStatId)?.let {
                dao.insertPieChart(it.copy(id = 0L, graphStatId = newGraphStatId))
            }.also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(newGraphStatId)) }
            newGraphStatId
        }

    override suspend fun duplicateAverageTimeBetweenStat(graphStatId: Long): Long? =
        performAtomicUpdate {
            val graphOrStat = dao.tryGetGraphStatById(graphStatId)?.toDto() ?: return@performAtomicUpdate null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat)
            dao.getAverageTimeBetweenStatByGraphStatId(graphStatId)?.let {
                dao.insertAverageTimeBetweenStat(it.copy(id = 0L, graphStatId = newGraphStatId))
            }.also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(newGraphStatId)) }
            newGraphStatId
        }

    override suspend fun duplicateTimeHistogram(graphStatId: Long): Long? =
        performAtomicUpdate {
            val graphOrStat = dao.tryGetGraphStatById(graphStatId)?.toDto() ?: return@performAtomicUpdate null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat)
            dao.getTimeHistogramByGraphStatId(graphStatId)?.let {
                dao.insertTimeHistogram(it.copy(id = 0L, graphStatId = newGraphStatId))
            }.also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(newGraphStatId)) }
            newGraphStatId
        }

    override suspend fun duplicateLastValueStat(graphStatId: Long): Long? =
        performAtomicUpdate {
            val graphOrStat = dao.tryGetGraphStatById(graphStatId)?.toDto() ?: return@performAtomicUpdate null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat)
            dao.getLastValueStatByGraphStatId(graphStatId)?.let {
                dao.insertLastValueStat(it.copy(id = 0L, graphStatId = newGraphStatId))
            }.also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(newGraphStatId)) }
            newGraphStatId
        }

    override suspend fun duplicateBarChart(graphStatId: Long): Long? =
        performAtomicUpdate {
            val graphOrStat = dao.tryGetGraphStatById(graphStatId)?.toDto() ?: return@performAtomicUpdate null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat)
            dao.getBarChartByGraphStatId(graphStatId)?.let {
                dao.insertBarChart(it.copy(id = 0L, graphStatId = newGraphStatId))
            }.also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(newGraphStatId)) }
            newGraphStatId
        }

    override suspend fun duplicateLuaGraph(graphStatId: Long): Long? =
        performAtomicUpdate {
            val graphOrStat = dao.tryGetGraphStatById(graphStatId)?.toDto() ?: return@performAtomicUpdate null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat)
            dao.getLuaGraphByGraphStatId(graphStatId)?.let {
                val copy = dao.insertLuaGraph(
                    it.toLuaGraph().copy(id = 0L, graphStatId = newGraphStatId)
                )
                dao.insertLuaGraphFeatures(it.features.map { f ->
                    f.copy(id = 0L, luaGraphId = copy)
                })
                copy
            }.also { dataUpdateEvents.emit(DataUpdateType.GraphOrStatCreated(newGraphStatId)) }
            newGraphStatId
        }

    // =========================================================================
    // Private helper methods
    // =========================================================================

    private fun duplicateGraphOrStat(graphOrStat: GraphOrStat) =
        dao.insertGraphOrStat(graphOrStat.copy(id = 0L).toEntity())

    private suspend fun <R> performAtomicUpdate(
        updateType: DataUpdateType? = null,
        block: suspend () -> R
    ) = withContext(io) {
        transactionHelper
            .withTransaction { block() }
            .also { updateType?.let { dataUpdateEvents.emit(it) } }
    }

    private suspend fun shiftUpGroupChildIndexes(groupId: Long) =
        performAtomicUpdate(DataUpdateType.DisplayIndex) {
            //Update features
            dao.getFeaturesForGroupSync(groupId).let { features ->
                dao.updateFeatures(features.map { it.copy(displayIndex = it.displayIndex + 1) })
            }

            //Update graphs
            dao.getGraphsAndStatsByGroupIdSync(groupId).let { graphs ->
                dao.updateGraphStats(graphs.map { it.copy(displayIndex = it.displayIndex + 1) })
            }

            //Update groups
            dao.getGroupsForGroupSync(groupId).let { groups ->
                dao.updateGroups(groups.map { it.copy(displayIndex = it.displayIndex + 1) })
            }
        }

    private fun insertGraphOrStat(name: String, groupId: Long, type: GraphStatType): Long =
        dao.insertGraphOrStat(
            com.samco.trackandgraph.data.database.entity.GraphOrStat(
                id = 0L,
                groupId = groupId,
                name = name,
                type = type,
                displayIndex = 0
            )
        )

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

    override suspend fun updateGroupChildOrder(groupId: Long, children: List<GroupChildOrderData>) =
        performAtomicUpdate(DataUpdateType.DisplayIndex) {
            //Update features
            dao.getFeaturesForGroupSync(groupId).let { features ->
                val updates = features.map { feature ->
                    val newDisplayIndex = children.indexOfFirst {
                        it.type == GroupChildType.FEATURE && it.id == feature.id
                    }
                    feature.copy(displayIndex = newDisplayIndex)
                }
                dao.updateFeatures(updates)
            }

            //Update graphs
            dao.getGraphsAndStatsByGroupIdSync(groupId).let { graphs ->
                val updates = graphs.map { graph ->
                    val newDisplayIndex = children.indexOfFirst {
                        it.type == GroupChildType.GRAPH && it.id == graph.id
                    }
                    graph.copy(displayIndex = newDisplayIndex)
                }
                dao.updateGraphStats(updates)
            }

            //Update groups
            dao.getGroupsForGroupSync(groupId).let { groups ->
                val updates = groups.map { group ->
                    val newDisplayIndex = children.indexOfFirst {
                        it.type == GroupChildType.GROUP && it.id == group.id
                    }
                    group.copy(displayIndex = newDisplayIndex)
                }
                dao.updateGroups(updates)
            }
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
        if (id != null) dataUpdateEvents.emit(DataUpdateType.FunctionCreated(id))
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

    override suspend fun duplicateFunction(function: Function): Long? = withContext(io) {
        val newFunctionId = functionHelper.duplicateFunction(function)
        if (newFunctionId != null) {
            dataUpdateEvents.emit(DataUpdateType.FunctionCreated(newFunctionId))
        }
        return@withContext newFunctionId
    }

    override suspend fun deleteFunction(request: FunctionDeleteRequest) = withContext(io) {
        val function = dao.getFunctionById(request.functionId) ?: return@withContext
        // TODO: When multi-group support is added, check request.groupId
        // to determine if we should remove from one group or delete entirely
        deleteFeature(function.featureId, isTracker = false)
    }

    override suspend fun getFeatureIdsDependingOn(featureId: Long): Set<Long> = withContext(io) {
        dependencyAnalyserProvider.create().getFeaturesDependingOn(featureId).featureIds
    }

    override suspend fun getDependencyFeatureIdsOf(featureId: Long): Set<Long> = withContext(io) {
        dependencyAnalyserProvider.create().getDependenciesOf(featureId).featureIds
    }

    override suspend fun moveComponent(request: MoveComponentRequest) = withContext(io) {
        // TODO: When features can exist in multiple groups, this will need to handle
        // adding/removing group associations rather than just updating the single groupId
        when (request.type) {
            ComponentType.TRACKER -> {
                val tracker = dao.getTrackerById(request.id)
                    ?: throw IllegalArgumentException("Tracker not found: ${request.id}")
                val feature = dao.getFeatureById(tracker.featureId)
                    ?: throw IllegalArgumentException("Feature not found: ${tracker.featureId}")
                dao.updateFeature(feature.copy(groupId = request.toGroupId))
                dataUpdateEvents.emit(DataUpdateType.TrackerUpdated)
            }
            ComponentType.FUNCTION -> {
                val function = dao.getFunctionById(request.id)
                    ?: throw IllegalArgumentException("Function not found: ${request.id}")
                val feature = dao.getFeatureById(function.featureId)
                    ?: throw IllegalArgumentException("Feature not found: ${function.featureId}")
                dao.updateFeature(feature.copy(groupId = request.toGroupId))
                dataUpdateEvents.emit(DataUpdateType.FunctionUpdated(function.featureId))
            }
            ComponentType.GROUP -> {
                val group = dao.getGroupById(request.id)

                // Validate: ensure we're not moving a group to its own descendant
                val visited = mutableListOf(request.id)
                var currentParentId: Long? = request.toGroupId
                while (currentParentId != null && currentParentId != 0L) {
                    if (visited.contains(currentParentId)) {
                        throw IllegalArgumentException("Cannot move group to its own descendant")
                    }
                    visited.add(currentParentId)
                    currentParentId = dao.getGroupById(currentParentId).parentGroupId
                }

                dao.updateGroup(group.copy(parentGroupId = request.toGroupId))
                dataUpdateEvents.emit(DataUpdateType.GroupUpdated)
            }
            ComponentType.GRAPH -> {
                val graphStat = dao.getGraphStatById(request.id)
                dao.updateGraphOrStat(graphStat.copy(groupId = request.toGroupId))
                dataUpdateEvents.emit(DataUpdateType.GraphOrStatUpdated(request.id))
            }
        }
    }
}
