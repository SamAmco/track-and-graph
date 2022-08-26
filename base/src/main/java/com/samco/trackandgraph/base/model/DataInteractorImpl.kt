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

package com.samco.trackandgraph.base.model

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.database.entity.FeatureTimer
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.base.database.sampling.DataSampler
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.service.ServiceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

internal class DataInteractorImpl @Inject constructor(
    private val database: TrackAndGraphDatabase,
    private val dao: TrackAndGraphDatabaseDao,
    @IODispatcher private val io: CoroutineDispatcher,
    private val featureUpdater: FeatureUpdater,
    private val csvReadWriter: CSVReadWriter,
    private val alarmInteractor: AlarmInteractor,
    private val serviceManager: ServiceManager
) : DataInteractor {

    private val dataUpdateEvents = MutableSharedFlow<Unit>()

    @Deprecated(message = "Create a function that performs the interaction for you in the model implementation")
    override fun doRawQuery(supportSQLiteQuery: SupportSQLiteQuery): Int {
        return dao.doRawQuery(supportSQLiteQuery)
    }

    override fun getDatabaseFilePath(): String {
        return database.openHelper.readableDatabase.path
    }

    override fun closeOpenHelper() {
        database.openHelper.close()
    }

    override suspend fun insertGroup(group: Group): Long = withContext(io) {
        dao.insertGroup(group.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun deleteGroup(id: Long) = withContext(io) {
        //Get all feature ids before we delete the group
        val allFeaturIdsBeforeDelete = dao.getAllFeaturesSync().map { it.id }.toSet()
        //Delete the group
        dao.deleteGroup(id)
        //Get all feature ids after deleting the group
        val allFeatureIdsAfterDelete = dao.getAllFeaturesSync().map { it.id }.toSet()
        val deletedFeatureIds = allFeaturIdsBeforeDelete.minus(allFeatureIdsAfterDelete)
        //Trigger a feature delete request for all deleted features
        deletedFeatureIds.forEach { serviceManager.requestWidgetsDisabledForFeatureId(it) }
        //Emit a data update event
        dataUpdateEvents.emit(Unit)
    }

    override suspend fun updateGroup(group: Group) = withContext(io) {
        dao.updateGroup(group.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override fun getAllGroups(): LiveData<List<Group>> {
        return Transformations.map(dao.getAllGroups()) { groups -> groups.map { it.toDto() } }
    }

    override suspend fun getAllGroupsSync(): List<Group> = withContext(io) {
        dao.getAllGroupsSync().map { it.toDto() }
    }

    override suspend fun getGroupById(id: Long): Group = withContext(io) {
        dao.getGroupById(id).toDto()
    }

    override fun getAllReminders(): LiveData<List<Reminder>> {
        return Transformations.map(dao.getAllReminders()) { reminders -> reminders.map { it.toDto() } }
    }

    override suspend fun getAllRemindersSync(): List<Reminder> = withContext(io) {
        dao.getAllRemindersSync().map { it.toDto() }
    }

    override fun getAllFeatures(): LiveData<List<Feature>> {
        return Transformations.map(dao.getAllFeatures()) { features -> features.map { it.toDto() } }
    }

    override suspend fun getAllFeaturesSync(): List<Feature> = withContext(io) {
        dao.getAllFeaturesSync().map { it.toDto() }
    }

    override suspend fun getDisplayFeaturesForGroupSync(groupId: Long): List<DisplayFeature> =
        withContext(io) {
            dao.getDisplayFeaturesForGroupSync(groupId).map { it.toDto() }
        }

    override suspend fun getFeaturesForGroupSync(groupId: Long): List<Feature> = withContext(io) {
        dao.getFeaturesForGroupSync(groupId).map { it.toDto() }
    }

    override suspend fun getFeatureById(featureId: Long): Feature = withContext(io) {
        dao.getFeatureById(featureId).toDto()
    }

    override suspend fun tryGetFeatureByIdSync(featureId: Long): Feature? = withContext(io) {
        dao.tryGetFeatureByIdSync(featureId)?.toDto()
    }

    override suspend fun tryGetDisplayFeatureByIdSync(
        featureId: Long
    ): DisplayFeature? = withContext(io) {
        dao.getDisplayFeatureByIdSync(featureId)?.toDto()
    }

    override fun tryGetFeatureById(featureId: Long): LiveData<Feature?> {
        return Transformations.map(dao.tryGetFeatureById(featureId)) { it?.toDto() }
    }

    override suspend fun getFeaturesByIdsSync(featureIds: List<Long>): List<Feature> =
        withContext(io) {
            dao.getFeaturesByIdsSync(featureIds).map { it.toDto() }
        }

    override suspend fun insertFeature(feature: Feature): Long = withContext(io) {
        dao.insertFeature(feature.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun updateFeature(feature: Feature) = withContext(io) {
        dao.updateFeature(feature.toEntity()).also { dataUpdateEvents.emit(Unit) }
        serviceManager.requestWidgetUpdatesForFeatureId(featureId = feature.id)
    }

    override suspend fun updateFeature(
        oldFeature: Feature,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>,
        durationNumericConversionMode: FeatureUpdater.DurationNumericConversionMode?,
        newName: String?,
        newFeatureType: DataType?,
        newDiscreteValues: List<DiscreteValue>?,
        hasDefaultValue: Boolean?,
        defaultValue: Double?,
        featureDescription: String?
    ) = withContext(io) {
        featureUpdater.updateFeature(
            oldFeature,
            discreteValueMap,
            durationNumericConversionMode,
            newName,
            newFeatureType,
            newDiscreteValues,
            hasDefaultValue,
            defaultValue,
            featureDescription
        ).also {
            serviceManager.requestWidgetUpdatesForFeatureId(featureId = oldFeature.id)
            dataUpdateEvents.emit(Unit)
        }
    }

    override suspend fun deleteFeature(id: Long) = withContext(io) {
        dao.deleteFeature(id)
        serviceManager.requestWidgetsDisabledForFeatureId(featureId = id)
        dataUpdateEvents.emit(Unit)
    }

    override suspend fun updateReminders(reminders: List<Reminder>) = withContext(io) {
        alarmInteractor.clearAlarms()
        dao.deleteReminders()
        reminders
            .map { it.toEntity() }
            .forEach { dao.insertReminder(it) }
        alarmInteractor.syncAlarms()
        dataUpdateEvents.emit(Unit)
    }

    override suspend fun deleteDataPoint(dataPoint: DataPoint) = withContext(io) {
        dao.deleteDataPoint(dataPoint.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun deleteAllDataPointsForDiscreteValue(featureId: Long, index: Double) =
        withContext(io) {
            dao.deleteAllDataPointsForDiscreteValue(featureId, index)
                .also { dataUpdateEvents.emit(Unit) }
        }

    override suspend fun deleteGraphOrStat(id: Long) = withContext(io) {
        dao.deleteGraphOrStat(id).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun deleteGraphOrStat(graphOrStat: GraphOrStat) = withContext(io) {
        dao.deleteGraphOrStat(graphOrStat.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun insertDataPoint(dataPoint: DataPoint): Long = withContext(io) {
        dao.insertDataPoint(dataPoint.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun insertDataPoints(dataPoint: List<DataPoint>) = withContext(io) {
        dao.insertDataPoints(dataPoint.map { it.toEntity() }).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun updateDataPoints(dataPoint: List<DataPoint>) = withContext(io) {
        dao.updateDataPoints(dataPoint.map { it.toEntity() }).also { dataUpdateEvents.emit(Unit) }
    }

    //TODO probably can do better than this
    override suspend fun getDataSampleForFeatureId(featureId: Long): DataSample = withContext(io) {
        val dataSampler = DataSampler(dao)
        val dataSource = DataSource.FeatureDataSource(featureId)
        dataSampler.getDataSampleForSource(dataSource)
    }

    override fun getDataUpdateEvents(): SharedFlow<Unit> = dataUpdateEvents

    override suspend fun getDataPointsCursorForFeatureSync(featureId: Long): Cursor =
        withContext(io) {
            dao.getDataPointsCursorForFeatureSync(featureId)
        }

    override fun getDataPointsForFeature(featureId: Long): LiveData<List<DataPoint>> {
        return Transformations.map(dao.getDataPointsForFeature(featureId)) { dataPoints -> dataPoints.map { it.toDto() } }
    }

    override suspend fun getDataPointByTimestampAndFeatureSync(
        featureId: Long,
        timestamp: OffsetDateTime
    ): DataPoint = withContext(io) {
        dao.getDataPointByTimestampAndFeatureSync(featureId, timestamp).toDto()
    }

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

    override suspend fun getTimeSinceLastStatByGraphStatId(graphStatId: Long): TimeSinceLastStat? =
        withContext(io) {
            dao.getTimeSinceLastStatByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat> =
        withContext(io) {
            dao.getGraphsAndStatsByGroupIdSync(groupId).map { it.toDto() }
        }

    override suspend fun getAllGraphStatsSync(): List<GraphOrStat> = withContext(io) {
        dao.getAllGraphStatsSync().map { it.toDto() }
    }

    override fun getAllDisplayNotes(): LiveData<List<DisplayNote>> {
        return Transformations.map(dao.getAllDisplayNotes()) { notes -> notes.map { it.toDto() } }
    }

    override suspend fun removeNote(timestamp: OffsetDateTime, featureId: Long) = withContext(io) {
        dao.removeNote(timestamp, featureId).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun deleteGlobalNote(note: GlobalNote) = withContext(io) {
        dao.deleteGlobalNote(note.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun insertGlobalNote(note: GlobalNote): Long = withContext(io) {
        dao.insertGlobalNote(note.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun getGlobalNoteByTimeSync(timestamp: OffsetDateTime?): GlobalNote? =
        withContext(io) {
            dao.getGlobalNoteByTimeSync(timestamp)?.toDto()
        }

    override suspend fun getAllGlobalNotesSync(): List<GlobalNote> = withContext(io) {
        dao.getAllGlobalNotesSync().map { it.toDto() }
    }

    private fun duplicateGraphOrStat(graphOrStat: GraphOrStat) =
        dao.insertGraphOrStat(graphOrStat.copy(id = 0L).toEntity())

    private suspend fun <R> performAtomicUpdate(block: suspend () -> R) = withContext(io) {
        database
            .withTransaction { block() }
            .also { dataUpdateEvents.emit(Unit) }
    }

    private suspend fun shiftUpGroupChildIndexes(groupId: Long) = performAtomicUpdate {
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

    override suspend fun duplicateLineGraph(graphOrStat: GraphOrStat): Long? = performAtomicUpdate {
        shiftUpGroupChildIndexes(graphOrStat.groupId)
        val newGraphStat = duplicateGraphOrStat(graphOrStat)
        dao.getLineGraphByGraphStatId(graphOrStat.id)?.let {
            val copy = dao.insertLineGraph(
                it.toLineGraph().copy(id = 0L, graphStatId = newGraphStat)
            )
            dao.insertLineGraphFeatures(it.features.map { f ->
                f.copy(id = 0L, lineGraphId = copy)
            })
            copy
        }
    }

    override suspend fun duplicatePieChart(graphOrStat: GraphOrStat): Long? = performAtomicUpdate {
        shiftUpGroupChildIndexes(graphOrStat.groupId)
        val newGraphStat = duplicateGraphOrStat(graphOrStat)
        dao.getPieChartByGraphStatId(graphOrStat.id)?.let {
            dao.insertPieChart(it.copy(id = 0L, graphStatId = newGraphStat))
        }
    }

    override suspend fun duplicateAverageTimeBetweenStat(graphOrStat: GraphOrStat): Long? =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(graphOrStat.groupId)
            val newGraphStat = duplicateGraphOrStat(graphOrStat)
            dao.getAverageTimeBetweenStatByGraphStatId(graphOrStat.id)?.let {
                dao.insertAverageTimeBetweenStat(it.copy(id = 0L, graphStatId = newGraphStat))
            }
        }

    override suspend fun duplicateTimeSinceLastStat(graphOrStat: GraphOrStat): Long? =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(graphOrStat.groupId)
            val newGraphStat = duplicateGraphOrStat(graphOrStat)
            dao.getTimeSinceLastStatByGraphStatId(graphOrStat.id)?.let {
                dao.insertTimeSinceLastStat(it.copy(id = 0L, graphStatId = newGraphStat))
            }
        }

    override suspend fun duplicateTimeHistogram(graphOrStat: GraphOrStat): Long? =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(graphOrStat.groupId)
            val newGraphStat = duplicateGraphOrStat(graphOrStat)
            dao.getTimeHistogramByGraphStatId(graphOrStat.id)?.let {
                dao.insertTimeHistogram(it.copy(id = 0L, graphStatId = newGraphStat))
            }
        }

    private fun insertGraphStat(graphOrStat: GraphOrStat) =
        dao.insertGraphOrStat(graphOrStat.copy(id = 0L, displayIndex = 0).toEntity())

    override suspend fun insertLineGraph(
        graphOrStat: GraphOrStat,
        lineGraph: LineGraphWithFeatures
    ): Long = performAtomicUpdate {
        shiftUpGroupChildIndexes(graphOrStat.groupId)
        val id = insertGraphStat(graphOrStat)
        val lineGraphId =
            dao.insertLineGraph(lineGraph.toLineGraph().copy(graphStatId = id).toEntity())
        val features = lineGraph.features.map { it.copy(lineGraphId = lineGraphId).toEntity() }
        dao.insertLineGraphFeatures(features)
        lineGraphId
    }

    override suspend fun insertPieChart(graphOrStat: GraphOrStat, pieChart: PieChart): Long =
        performAtomicUpdate {
            shiftUpGroupChildIndexes(graphOrStat.groupId)
            val id = insertGraphStat(graphOrStat)
            dao.insertPieChart(pieChart.copy(graphStatId = id).toEntity())
        }

    override suspend fun insertAverageTimeBetweenStat(
        graphOrStat: GraphOrStat,
        averageTimeBetweenStat: AverageTimeBetweenStat
    ): Long = performAtomicUpdate {
        shiftUpGroupChildIndexes(graphOrStat.groupId)
        val id = insertGraphStat(graphOrStat)
        dao.insertAverageTimeBetweenStat(
            averageTimeBetweenStat.copy(graphStatId = id).toEntity()
        )
    }

    override suspend fun insertTimeSinceLastStat(
        graphOrStat: GraphOrStat,
        timeSinceLastStat: TimeSinceLastStat
    ): Long = performAtomicUpdate {
        shiftUpGroupChildIndexes(graphOrStat.groupId)
        val id = insertGraphStat(graphOrStat)
        dao.insertTimeSinceLastStat(timeSinceLastStat.copy(graphStatId = id).toEntity())
    }

    override suspend fun insertTimeHistogram(
        graphOrStat: GraphOrStat,
        timeHistogram: TimeHistogram
    ) = performAtomicUpdate {
        shiftUpGroupChildIndexes(graphOrStat.groupId)
        val id = insertGraphStat(graphOrStat)
        dao.insertTimeHistogram(timeHistogram.copy(graphStatId = id).toEntity())
    }

    override suspend fun updatePieChart(graphOrStat: GraphOrStat, pieChart: PieChart) =
        performAtomicUpdate {
            dao.updateGraphOrStat(graphOrStat.toEntity())
            dao.updatePieChart(pieChart.toEntity())
        }

    override suspend fun updateAverageTimeBetweenStat(
        graphOrStat: GraphOrStat,
        averageTimeBetweenStat: AverageTimeBetweenStat
    ) = performAtomicUpdate {
        dao.updateGraphOrStat(graphOrStat.toEntity())
        dao.updateAverageTimeBetweenStat(averageTimeBetweenStat.toEntity())
    }

    override suspend fun updateLineGraph(
        graphOrStat: GraphOrStat,
        lineGraph: LineGraphWithFeatures
    ) = performAtomicUpdate {
        dao.updateGraphOrStat(graphOrStat.toEntity())
        dao.updateLineGraph(lineGraph.toLineGraph().toEntity())
        dao.deleteFeaturesForLineGraph(lineGraph.id)
        dao.insertLineGraphFeatures(lineGraph.features.map {
            it.copy(lineGraphId = lineGraph.id).toEntity()
        })
    }

    override suspend fun updateTimeSinceLastStat(
        graphOrStat: GraphOrStat,
        timeSinceLastStat: TimeSinceLastStat
    ) = performAtomicUpdate {
        dao.updateGraphOrStat(graphOrStat.toEntity())
        dao.updateTimeSinceLastStat(timeSinceLastStat.toEntity())
    }

    override suspend fun updateGraphOrStat(graphOrStat: GraphOrStat) = performAtomicUpdate {
        dao.updateGraphOrStat(graphOrStat.toEntity())
    }

    override suspend fun updateTimeHistogram(
        graphOrStat: GraphOrStat,
        timeHistogram: TimeHistogram
    ) = performAtomicUpdate {
        dao.updateGraphOrStat(graphOrStat.toEntity())
        dao.updateTimeHistogram(timeHistogram.toEntity())
    }

    override suspend fun updateGroupChildOrder(groupId: Long, children: List<GroupChild>) =
        performAtomicUpdate {
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

    override suspend fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram? =
        withContext(io) {
            dao.getTimeHistogramByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getGroupsForGroupSync(id: Long): List<Group> = withContext(io) {
        dao.getGroupsForGroupSync(id).map { it.toDto() }
    }

    override suspend fun writeFeaturesToCSV(outStream: OutputStream, featureIds: List<Long>) =
        withContext(io) {
            val featureMap =
                featureIds.associate { getFeatureById(it) to getDataSampleForFeatureId(it) }
            csvReadWriter.writeFeaturesToCSV(outStream, featureMap)
        }

    override suspend fun readFeaturesFromCSV(inputStream: InputStream, trackGroupId: Long) =
        withContext(io) {
            csvReadWriter.readFeaturesFromCSV(inputStream, trackGroupId)
            dataUpdateEvents.emit(Unit)
        }

    override suspend fun playTimerForFeature(featureId: Long) = performAtomicUpdate {
        dao.deleteFeatureTimer(featureId)
        dao.insertFeatureTimer(FeatureTimer(0L, featureId, Instant.now()))
    }.also {
        serviceManager.startTimerNotificationService()
        serviceManager.requestWidgetUpdatesForFeatureId(featureId)
    }

    override suspend fun stopTimerForFeature(featureId: Long): Duration? = performAtomicUpdate {
        val timer = dao.getFeatureTimer(featureId)
        dao.deleteFeatureTimer(featureId)
        timer?.let { Duration.between(it.startInstant, Instant.now()) }
    }.also {
        serviceManager.requestWidgetUpdatesForFeatureId(featureId)
    }

    override suspend fun getAllActiveTimerFeatures(): List<DisplayFeature> = withContext(io) {
        dao.getAllActiveTimerFeatures().map { it.toDto() }
    }
}