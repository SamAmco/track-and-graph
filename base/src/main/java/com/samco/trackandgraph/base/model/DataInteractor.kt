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

import android.content.Context
import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SupportSQLiteQuery
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
import com.samco.trackandgraph.base.database.dto.*
import org.threeten.bp.OffsetDateTime

interface DataInteractor {
    fun getInstance(context: Context): DataInteractor {
        val database = TrackAndGraphDatabase.getInstance(context)
        return DataInteractorImpl(database, database.trackAndGraphDatabaseDao)
    }

    //TODO get rid of this
    @Deprecated(message = "Create a function that performs the interaction for you in the model implementation")
    fun doRawQuery(supportSQLiteQuery: SupportSQLiteQuery): Int

    fun insertGroup(group: Group): Long

    fun deleteGroup(id: Long)

    fun updateGroup(group: Group)

    fun updateGroups(groups: List<Group>)

    fun getAllReminders(): LiveData<List<Reminder>>

    fun getAllRemindersSync(): List<Reminder>

    fun getAllGroups(): LiveData<List<Group>>

    fun getAllGroupsSync(): List<Group>

    fun getAllFeatures(): LiveData<List<Feature>>

    fun getAllFeaturesSync(): List<Feature>

    fun insertReminder(reminder: Reminder)

    fun deleteReminder(reminder: Reminder)

    fun updateReminder(reminder: Reminder)

    fun updateReminders(reminders: List<Reminder>)

    fun getGroupById(id: Long): Group

    fun updateFeatures(features: List<Feature>)

    fun getDisplayFeaturesForGroup(groupId: Long): LiveData<List<DisplayFeature>>

    fun getFeaturesForGroupSync(groupId: Long): List<Feature>

    fun getFeatureById(featureId: Long): Feature

    fun tryGetFeatureByIdSync(featureId: Long): Feature?

    fun tryGetFeatureById(featureId: Long): LiveData<Feature?>

    fun getFeaturesByIdsSync(featureIds: List<Long>): List<Feature>

    fun insertFeature(feature: Feature): Long

    fun updateFeature(feature: Feature)

    fun deleteDataPoint(dataPoint: DataPoint)

    fun deleteAllDataPointsForDiscreteValue(featureId: Long, index: Double)

    fun deleteGraphOrStat(id: Long)

    fun deleteGraphOrStat(graphOrStat: GraphOrStat)

    fun deleteFeature(id: Long)

    fun insertDataPoint(dataPoint: DataPoint): Long

    fun insertDataPoints(dataPoint: List<DataPoint>)

    fun updateDataPoints(dataPoint: List<DataPoint>)

    //TODO remove this once we have a model layer that can track updates and emit events for us
    fun getAllDataPoints(): LiveData<List<DataPoint>>

    //TODO get rid of this and only return DataSample for a feature
    fun getDataPointsForFeatureSync(featureId: Long): List<DataPoint>

    //TODO get rid of this and only return DataSample for a feature
    fun getDataPointsCursorForFeatureSync(featureId: Long): Cursor

    //TODO get rid of this and only return DataSample for a feature
    fun getDataPointsForFeature(featureId: Long): LiveData<List<DataPoint>>

    fun getDataPointByTimestampAndFeatureSync(featureId: Long, timestamp: OffsetDateTime): DataPoint

    fun getGraphStatById(graphStatId: Long): GraphOrStat

    fun tryGetGraphStatById(graphStatId: Long): GraphOrStat?

    fun getLineGraphByGraphStatId(graphStatId: Long): LineGraphWithFeatures?

    fun getPieChartByGraphStatId(graphStatId: Long): PieChart?

    fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat?

    fun getTimeSinceLastStatByGraphStatId(graphStatId: Long): TimeSinceLastStat?

    fun getGraphsAndStatsByGroupId(groupId: Long): LiveData<List<GraphOrStat>>

    fun getAllGraphStatsSync(): List<GraphOrStat>

    fun getAllDisplayNotes(): LiveData<List<DisplayNote>>

    fun removeNote(timestamp: OffsetDateTime, featureId: Long)

    fun deleteGlobalNote(note: GlobalNote)

    fun insertGlobalNote(note: GlobalNote): Long

    fun getGlobalNoteByTimeSync(timestamp: OffsetDateTime?): GlobalNote?

    fun getAllGlobalNotesSync(): List<GlobalNote>

    fun deleteFeaturesForLineGraph(lineGraphId: Long)

    fun insertLineGraphFeatures(lineGraphFeatures: List<LineGraphFeature>)

    fun insertLineGraph(lineGraph: LineGraph): Long

    fun updateLineGraph(lineGraph: LineGraph)

    fun insertPieChart(pieChart: PieChart): Long

    fun updatePieChart(pieChart: PieChart)

    fun insertAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat): Long

    fun updateAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat)

    fun insertTimeSinceLastStat(timeSinceLastStat: TimeSinceLastStat): Long

    fun updateTimeSinceLastStat(timeSinceLastStat: TimeSinceLastStat)

    //TODO consider managing GraphOrStat automatically in the model
    fun insertGraphOrStat(graphOrStat: GraphOrStat): Long

    fun updateGraphOrStat(graphOrStat: GraphOrStat)

    fun updateGraphStats(graphStat: List<GraphOrStat>)

    fun updateTimeHistogram(timeHistogram: TimeHistogram)

    fun insertTimeHistogram(timeHistogram: TimeHistogram)

    fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram?

    fun getGroupsForGroup(id: Long): LiveData<List<Group>>
}