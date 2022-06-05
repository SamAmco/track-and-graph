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
import androidx.sqlite.db.SupportSQLiteQuery
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.base.database.sampling.DataSampler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.threeten.bp.OffsetDateTime

internal class DataInteractorImpl(
    private val database: TrackAndGraphDatabase,
    private val dao: TrackAndGraphDatabaseDao,
    private val io: CoroutineDispatcher
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
        dao.deleteGroup(id).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun updateGroup(group: Group) = withContext(io) {
        dao.updateGroup(group.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun updateGroups(groups: List<Group>) = withContext(io) {
        dao.updateGroups(groups.map { it.toEntity() }).also { dataUpdateEvents.emit(Unit) }
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

    override suspend fun updateFeatures(features: List<Feature>) = withContext(io) {
        dao.updateFeatures(features.map { it.toEntity() }).also { dataUpdateEvents.emit(Unit) }
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
    }

    override suspend fun deleteFeature(id: Long) = withContext(io) {
        dao.deleteFeature(id).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun deleteFeaturesForLineGraph(lineGraphId: Long) = withContext(io) {
        dao.deleteFeaturesForLineGraph(lineGraphId).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun insertReminder(reminder: Reminder) = withContext(io) {
        dao.insertReminder(reminder.toEntity())
    }

    override suspend fun deleteReminder(reminder: Reminder) = withContext(io) {
        dao.deleteReminder(reminder.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun updateReminder(reminder: Reminder) = withContext(io) {
        dao.updateReminder(reminder.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun updateReminders(reminders: List<Reminder>) = withContext(io) {
        dao.updateReminders(reminders.map { it.toEntity() }).also { dataUpdateEvents.emit(Unit) }
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

    override suspend fun insertLineGraphFeatures(lineGraphFeatures: List<LineGraphFeature>) =
        withContext(io) {
            dao.insertLineGraphFeatures(lineGraphFeatures.map { it.toEntity() })
        }

    override suspend fun insertLineGraph(lineGraph: LineGraph): Long = withContext(io) {
        dao.insertLineGraph(lineGraph.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun updateLineGraph(lineGraph: LineGraph) = withContext(io) {
        dao.updateLineGraph(lineGraph.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun insertPieChart(pieChart: PieChart): Long = withContext(io) {
        dao.insertPieChart(pieChart.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun updatePieChart(pieChart: PieChart) = withContext(io) {
        dao.updatePieChart(pieChart.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun insertAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat): Long =
        withContext(io) {
            dao.insertAverageTimeBetweenStat(averageTimeBetweenStat.toEntity())
                .also { dataUpdateEvents.emit(Unit) }
        }

    override suspend fun updateAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat) =
        withContext(io) {
            dao.updateAverageTimeBetweenStat(averageTimeBetweenStat.toEntity())
                .also { dataUpdateEvents.emit(Unit) }
        }

    override suspend fun insertTimeSinceLastStat(timeSinceLastStat: TimeSinceLastStat): Long =
        withContext(io) {
            dao.insertTimeSinceLastStat(timeSinceLastStat.toEntity())
                .also { dataUpdateEvents.emit(Unit) }
        }

    override suspend fun updateTimeSinceLastStat(timeSinceLastStat: TimeSinceLastStat) =
        withContext(io) {
            dao.updateTimeSinceLastStat(timeSinceLastStat.toEntity())
                .also { dataUpdateEvents.emit(Unit) }
        }

    override suspend fun insertGraphOrStat(graphOrStat: GraphOrStat): Long = withContext(io) {
        dao.insertGraphOrStat(graphOrStat.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun updateGraphOrStat(graphOrStat: GraphOrStat) = withContext(io) {
        dao.updateGraphOrStat(graphOrStat.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun updateGraphStats(graphStat: List<GraphOrStat>) = withContext(io) {
        dao.updateGraphStats(graphStat.map { it.toEntity() }).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun updateTimeHistogram(timeHistogram: TimeHistogram) = withContext(io) {
        dao.updateTimeHistogram(timeHistogram.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun insertTimeHistogram(timeHistogram: TimeHistogram) = withContext(io) {
        dao.insertTimeHistogram(timeHistogram.toEntity()).also { dataUpdateEvents.emit(Unit) }
    }

    override suspend fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram? =
        withContext(io) {
            dao.getTimeHistogramByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getGroupsForGroupSync(id: Long): List<Group> = withContext(io) {
        dao.getGroupsForGroupSync(id).map { it.toDto() }
    }
}