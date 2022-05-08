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
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.base.database.sampling.DataSampler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime

internal class DataInteractorImpl(
    private val database: TrackAndGraphDatabase,
    private val dao: TrackAndGraphDatabaseDao,
    private val io: CoroutineDispatcher
) : DataInteractor {

    private val ioJob = Job()
    private val ioScope = CoroutineScope(ioJob + io)

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

    override suspend fun <T> withTransaction(function: suspend () -> T): T {
        return database.withTransaction { function() }
    }

    override fun insertGroup(group: Group): Long {
        return dao.insertGroup(group.toEntity())
    }

    override fun deleteGroup(id: Long) {
        ioScope.launch {
            dao.deleteGroup(id)
            dataUpdateEvents.emit(Unit)
        }
    }

    override fun updateGroup(group: Group) {
        return dao.updateGroup(group.toEntity())
    }

    override fun updateGroups(groups: List<Group>) {
        return dao.updateGroups(groups.map { it.toEntity() })
    }

    override fun getGroupsForGroup(id: Long): LiveData<List<Group>> {
        return Transformations.map(dao.getGroupsForGroup(id)) { it.map { it.toDto() } }
    }

    override fun getAllGroups(): LiveData<List<Group>> {
        return Transformations.map(dao.getAllGroups()) { groups -> groups.map { it.toDto() } }
    }

    override fun getAllGroupsSync(): List<Group> {
        return dao.getAllGroupsSync().map { it.toDto() }
    }

    override fun getGroupById(id: Long): Group {
        return dao.getGroupById(id).toDto()
    }

    override fun getAllReminders(): LiveData<List<Reminder>> {
        return Transformations.map(dao.getAllReminders()) { reminders -> reminders.map { it.toDto() } }
    }

    override fun getAllRemindersSync(): List<Reminder> {
        return dao.getAllRemindersSync().map { it.toDto() }
    }

    override fun getAllFeatures(): LiveData<List<Feature>> {
        return Transformations.map(dao.getAllFeatures()) { features -> features.map { it.toDto() } }
    }

    override fun getAllFeaturesSync(): List<Feature> {
        return dao.getAllFeaturesSync().map { it.toDto() }
    }

    override fun updateFeatures(features: List<Feature>) {
        ioScope.launch {
            dao.updateFeatures(features.map { it.toEntity() })
            dataUpdateEvents.emit(Unit)
        }
    }

    override fun getDisplayFeaturesForGroup(groupId: Long): LiveData<List<DisplayFeature>> {
        return Transformations.map(dao.getDisplayFeaturesForGroup(groupId)) { displayFeatures ->
            displayFeatures.map { it.toDto() }
        }
    }

    override fun getFeaturesForGroupSync(groupId: Long): List<Feature> {
        return dao.getFeaturesForGroupSync(groupId).map { it.toDto() }
    }

    override fun getFeatureById(featureId: Long): Feature {
        return dao.getFeatureById(featureId).toDto()
    }

    override fun tryGetFeatureByIdSync(featureId: Long): Feature? {
        return dao.tryGetFeatureByIdSync(featureId)?.toDto()
    }

    override fun tryGetFeatureById(featureId: Long): LiveData<Feature?> {
        return Transformations.map(dao.tryGetFeatureById(featureId)) { it?.toDto() }
    }

    override fun getFeaturesByIdsSync(featureIds: List<Long>): List<Feature> {
        return dao.getFeaturesByIdsSync(featureIds).map { it.toDto() }
    }

    override fun insertFeature(feature: Feature): Long {
        return dao.insertFeature(feature.toEntity()).also {
            ioScope.launch { dataUpdateEvents.emit(Unit) }
        }
    }

    override fun updateFeature(feature: Feature) {
        ioScope.launch {
            dao.updateFeature(feature.toEntity())
            dataUpdateEvents.emit(Unit)
        }
    }

    override fun deleteFeature(id: Long) {
        ioScope.launch {
            dao.deleteFeature(id)
            dataUpdateEvents.emit(Unit)
        }
    }

    override fun deleteFeaturesForLineGraph(lineGraphId: Long) {
        return dao.deleteFeaturesForLineGraph(lineGraphId)
    }

    override fun insertReminder(reminder: Reminder) {
        return dao.insertReminder(reminder.toEntity())
    }

    override fun deleteReminder(reminder: Reminder) {
        return dao.deleteReminder(reminder.toEntity())
    }

    override fun updateReminder(reminder: Reminder) {
        return dao.updateReminder(reminder.toEntity())
    }

    override fun updateReminders(reminders: List<Reminder>) {
        return dao.updateReminders(reminders.map { it.toEntity() })
    }

    override fun deleteDataPoint(dataPoint: DataPoint) {
        ioScope.launch {
            dao.deleteDataPoint(dataPoint.toEntity())
            dataUpdateEvents.emit(Unit)
        }
    }

    override fun deleteAllDataPointsForDiscreteValue(featureId: Long, index: Double) {
        ioScope.launch {
            dao.deleteAllDataPointsForDiscreteValue(featureId, index)
            dataUpdateEvents.emit(Unit)
        }
    }

    override fun deleteGraphOrStat(id: Long) {
        return dao.deleteGraphOrStat(id)
    }

    override fun deleteGraphOrStat(graphOrStat: GraphOrStat) {
        return dao.deleteGraphOrStat(graphOrStat.toEntity())
    }

    override fun insertDataPoint(dataPoint: DataPoint): Long {
        return dao.insertDataPoint(dataPoint.toEntity()).also {
            ioScope.launch { dataUpdateEvents.emit(Unit) }
        }
    }

    override fun insertDataPoints(dataPoint: List<DataPoint>) {
        ioScope.launch {
            dao.insertDataPoints(dataPoint.map { it.toEntity() })
            dataUpdateEvents.emit(Unit)
        }
    }

    override fun updateDataPoints(dataPoint: List<DataPoint>) {
        ioScope.launch {
            dao.updateDataPoints(dataPoint.map { it.toEntity() })
            dataUpdateEvents.emit(Unit)
        }
    }

    //TODO probably can do better than this
    override fun getDataSampleForFeatureId(featureId: Long): DataSample {
        val dataSampler = DataSampler(dao)
        val dataSource = DataSource.FeatureDataSource(featureId)
        return dataSampler.getDataSampleForSource(dataSource)
    }

    override fun getDataUpdateEvents(): SharedFlow<Unit> = dataUpdateEvents

    override fun getDataPointsForFeatureSync(featureId: Long): List<DataPoint> {
        return dao.getDataPointsForFeatureSync(featureId).map { it.toDto() }
    }

    override fun getDataPointsCursorForFeatureSync(featureId: Long): Cursor {
        return dao.getDataPointsCursorForFeatureSync(featureId)
    }

    override fun getDataPointsForFeature(featureId: Long): LiveData<List<DataPoint>> {
        return Transformations.map(dao.getDataPointsForFeature(featureId)) { dataPoints -> dataPoints.map { it.toDto() } }
    }

    override fun getDataPointByTimestampAndFeatureSync(
        featureId: Long,
        timestamp: OffsetDateTime
    ): DataPoint {
        return dao.getDataPointByTimestampAndFeatureSync(featureId, timestamp).toDto()
    }

    override fun getGraphStatById(graphStatId: Long): GraphOrStat {
        return dao.getGraphStatById(graphStatId).toDto()
    }

    override fun tryGetGraphStatById(graphStatId: Long): GraphOrStat? {
        return dao.tryGetGraphStatById(graphStatId)?.toDto()
    }

    override fun getLineGraphByGraphStatId(graphStatId: Long): LineGraphWithFeatures? {
        return dao.getLineGraphByGraphStatId(graphStatId)?.toDto()
    }

    override fun getPieChartByGraphStatId(graphStatId: Long): PieChart? {
        return dao.getPieChartByGraphStatId(graphStatId)?.toDto()
    }

    override fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat? {
        return dao.getAverageTimeBetweenStatByGraphStatId(graphStatId)?.toDto()
    }

    override fun getTimeSinceLastStatByGraphStatId(graphStatId: Long): TimeSinceLastStat? {
        return dao.getTimeSinceLastStatByGraphStatId(graphStatId)?.toDto()
    }

    override fun getGraphsAndStatsByGroupId(groupId: Long): LiveData<List<GraphOrStat>> {
        return Transformations.map(dao.getGraphsAndStatsByGroupId(groupId)) { graphStats -> graphStats.map { it.toDto() } }
    }

    override fun getAllGraphStatsSync(): List<GraphOrStat> {
        return dao.getAllGraphStatsSync().map { it.toDto() }
    }

    override fun getAllDisplayNotes(): LiveData<List<DisplayNote>> {
        return Transformations.map(dao.getAllDisplayNotes()) { notes -> notes.map { it.toDto() } }
    }

    override fun removeNote(timestamp: OffsetDateTime, featureId: Long) {
        return dao.removeNote(timestamp, featureId).also {
            ioScope.launch { dataUpdateEvents.emit(Unit) }
        }
    }

    override fun deleteGlobalNote(note: GlobalNote) {
        return dao.deleteGlobalNote(note.toEntity()).also {
            ioScope.launch { dataUpdateEvents.emit(Unit) }
        }
    }

    override fun insertGlobalNote(note: GlobalNote): Long {
        return dao.insertGlobalNote(note.toEntity()).also {
            ioScope.launch { dataUpdateEvents.emit(Unit) }
        }
    }

    override fun getGlobalNoteByTimeSync(timestamp: OffsetDateTime?): GlobalNote? {
        return dao.getGlobalNoteByTimeSync(timestamp)?.toDto()
    }

    override fun getAllGlobalNotesSync(): List<GlobalNote> {
        return dao.getAllGlobalNotesSync().map { it.toDto() }
    }

    override fun insertLineGraphFeatures(lineGraphFeatures: List<LineGraphFeature>) {
        return dao.insertLineGraphFeatures(lineGraphFeatures.map { it.toEntity() })
    }

    override fun insertLineGraph(lineGraph: LineGraph): Long {
        return dao.insertLineGraph(lineGraph.toEntity())
    }

    override fun updateLineGraph(lineGraph: LineGraph) {
        return dao.updateLineGraph(lineGraph.toEntity())
    }

    override fun insertPieChart(pieChart: PieChart): Long {
        return dao.insertPieChart(pieChart.toEntity())
    }

    override fun updatePieChart(pieChart: PieChart) {
        return dao.updatePieChart(pieChart.toEntity())
    }

    override fun insertAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat): Long {
        return dao.insertAverageTimeBetweenStat(averageTimeBetweenStat.toEntity())
    }

    override fun updateAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat) {
        return dao.updateAverageTimeBetweenStat(averageTimeBetweenStat.toEntity())
    }

    override fun insertTimeSinceLastStat(timeSinceLastStat: TimeSinceLastStat): Long {
        return dao.insertTimeSinceLastStat(timeSinceLastStat.toEntity())
    }

    override fun updateTimeSinceLastStat(timeSinceLastStat: TimeSinceLastStat) {
        return dao.updateTimeSinceLastStat(timeSinceLastStat.toEntity())
    }

    override fun insertGraphOrStat(graphOrStat: GraphOrStat): Long {
        return dao.insertGraphOrStat(graphOrStat.toEntity())
    }

    override fun updateGraphOrStat(graphOrStat: GraphOrStat) {
        return dao.updateGraphOrStat(graphOrStat.toEntity())
    }

    override fun updateGraphStats(graphStat: List<GraphOrStat>) {
        return dao.updateGraphStats(graphStat.map { it.toEntity() })
    }

    override fun updateTimeHistogram(timeHistogram: TimeHistogram) {
        return dao.updateTimeHistogram(timeHistogram.toEntity())
    }

    override fun insertTimeHistogram(timeHistogram: TimeHistogram) {
        return dao.insertTimeHistogram(timeHistogram.toEntity())
    }

    override fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram? {
        return dao.getTimeHistogramByGraphStatId(graphStatId)?.toDto()
    }
}