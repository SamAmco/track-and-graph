/* 
* This file is part of Track & Graph
* 
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.base.database

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.samco.trackandgraph.base.database.entity.*
import com.samco.trackandgraph.base.database.entity.queryresponse.DisplayFeature
import com.samco.trackandgraph.base.database.entity.queryresponse.DisplayNote
import com.samco.trackandgraph.base.database.entity.queryresponse.LineGraphWithFeatures
import org.threeten.bp.OffsetDateTime

private const val getFeatureByIdQuery =
    """SELECT * FROM features_table WHERE id = :featureId LIMIT 1"""

private const val getDisplayFeaturesQuery =
    """SELECT features_table.*, num_data_points, last_timestamp, start_instant from features_table 
        LEFT JOIN (
            SELECT feature_id as id, COUNT(*) as num_data_points, MAX(timestamp) as last_timestamp 
            FROM data_points_table GROUP BY feature_id
        ) as feature_data 
        ON feature_data.id = features_table.id
        LEFT JOIN (
            SELECT * FROM feature_timers_table
        ) as timer_data
        ON timer_data.feature_id = features_table.id 
        """


//TODO it would probably be better if we migrated from LiveData to flow here to remove lifecycle
// awareness from the model layer
@Dao
internal interface TrackAndGraphDatabaseDao {
    @RawQuery
    fun doRawQuery(supportSQLiteQuery: SupportSQLiteQuery): Int

    @Insert
    fun insertGroup(group: Group): Long

    @Query("DELETE FROM groups_table WHERE id = :id")
    fun deleteGroup(id: Long)

    @Update
    fun updateGroup(group: Group)

    @Update
    fun updateGroups(groups: List<Group>)

    @Query("""SELECT * FROM reminders_table ORDER BY display_index ASC, id DESC""")
    fun getAllReminders(): LiveData<List<Reminder>>

    @Query("""SELECT * FROM reminders_table ORDER BY display_index ASC, id DESC""")
    fun getAllRemindersSync(): List<Reminder>

    @Query("""SELECT groups_table.* FROM groups_table ORDER BY display_index ASC, id DESC""")
    fun getAllGroups(): LiveData<List<Group>>

    @Query("""SELECT groups_table.* FROM groups_table ORDER BY display_index ASC, id DESC""")
    fun getAllGroupsSync(): List<Group>

    @Query("""SELECT features_table.* FROM features_table ORDER BY display_index ASC, id DESC""")
    fun getAllFeatures(): LiveData<List<Feature>>

    @Query("""SELECT features_table.* FROM features_table ORDER BY display_index ASC, id DESC""")
    fun getAllFeaturesSync(): List<Feature>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReminder(reminder: Reminder)

    @Query("DELETE FROM reminders_table")
    fun deleteReminders()

    @Query("SELECT * FROM groups_table WHERE id = :id LIMIT 1")
    fun getGroupById(id: Long): Group

    @Update
    fun updateFeatures(features: List<Feature>)

    @Query(getDisplayFeaturesQuery + """WHERE group_id = :groupId ORDER BY features_table.display_index ASC, id DESC""")
    fun getDisplayFeaturesForGroupSync(groupId: Long): List<DisplayFeature>

    @Query("SELECT features_table.* FROM features_table WHERE group_id = :groupId ORDER BY features_table.display_index ASC")
    fun getFeaturesForGroupSync(groupId: Long): List<Feature>

    @Query(getFeatureByIdQuery)
    fun getFeatureById(featureId: Long): Feature

    @Query(getFeatureByIdQuery)
    fun tryGetFeatureByIdSync(featureId: Long): Feature?

    @Query(getFeatureByIdQuery)
    fun tryGetFeatureById(featureId: Long): LiveData<Feature?>

    @Query("""SELECT * from features_table WHERE id IN (:featureIds) ORDER BY display_index ASC, id DESC""")
    fun getFeaturesByIdsSync(featureIds: List<Long>): List<Feature>

    @Insert
    fun insertFeature(feature: Feature): Long

    @Update
    fun updateFeature(feature: Feature)

    @Delete
    fun deleteDataPoint(dataPoint: DataPoint)

    @Query("DELETE FROM data_points_table WHERE feature_id = :featureId AND value = :index")
    fun deleteAllDataPointsForDiscreteValue(featureId: Long, index: Double)

    @Query("DELETE FROM graphs_and_stats_table2 WHERE id = :id")
    fun deleteGraphOrStat(id: Long)

    @Delete
    fun deleteGraphOrStat(graphOrStat: GraphOrStat)

    @Query("DELETE FROM features_table WHERE id = :id")
    fun deleteFeature(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDataPoint(dataPoint: DataPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDataPoints(dataPoint: List<DataPoint>)

    @Update
    fun updateDataPoints(dataPoint: List<DataPoint>)

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC")
    fun getDataPointsCursorForFeatureSync(featureId: Long): Cursor

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC")
    fun getDataPointsForFeature(featureId: Long): LiveData<List<DataPoint>>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC")
    fun getDataPointsForFeatureSync(featureId: Long): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC LIMIT :size OFFSET :startIndex")
    fun getDataPointsForFeatureSync(featureId: Long, startIndex: Int, size: Int): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId AND timestamp = :timestamp")
    fun getDataPointByTimestampAndFeatureSync(featureId: Long, timestamp: OffsetDateTime): DataPoint

    @Query("SELECT * FROM graphs_and_stats_table2 WHERE id = :graphStatId LIMIT 1")
    fun getGraphStatById(graphStatId: Long): GraphOrStat

    @Query("SELECT * FROM graphs_and_stats_table2 WHERE id = :graphStatId LIMIT 1")
    fun tryGetGraphStatById(graphStatId: Long): GraphOrStat?

    @Query("SELECT * FROM line_graphs_table3 WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getLineGraphByGraphStatId(graphStatId: Long): LineGraphWithFeatures?

    @Query("SELECT * FROM pie_charts_table2 WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getPieChartByGraphStatId(graphStatId: Long): PieChart?

    @Query("SELECT * FROM average_time_between_stat_table4 WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat?

    @Query("SELECT * FROM time_since_last_stat_table4 WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getTimeSinceLastStatByGraphStatId(graphStatId: Long): TimeSinceLastStat?

    @Query("SELECT * FROM graphs_and_stats_table2 WHERE group_id = :groupId ORDER BY display_index ASC, id DESC")
    fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat>

    @Query("SELECT * FROM graphs_and_stats_table2 ORDER BY display_index ASC, id DESC")
    fun getAllGraphStatsSync(): List<GraphOrStat>

    @Query(
        """
            SELECT * FROM (
                SELECT dp.timestamp as timestamp, 0 as note_type, dp.feature_id as feature_id, f.name as feature_name, t.id as group_id, dp.note as note
                FROM data_points_table as dp 
                LEFT JOIN features_table as f ON dp.feature_id = f.id
                LEFT JOIN groups_table as t ON f.group_id = t.id
                WHERE dp.note IS NOT NULL AND dp.note != ""
            ) UNION SELECT * FROM (
                SELECT n.timestamp as timestamp, 1 as note_type, NULL as feature_id, NULL as feature_name, NULL as group_id, n.note as note
                FROM notes_table as n
            ) ORDER BY timestamp DESC
        """
    )
    fun getAllDisplayNotes(): LiveData<List<DisplayNote>>

    @Query("UPDATE data_points_table SET note = '' WHERE timestamp = :timestamp AND feature_id = :featureId")
    fun removeNote(timestamp: OffsetDateTime, featureId: Long)

    @Delete
    fun deleteGlobalNote(note: GlobalNote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGlobalNote(note: GlobalNote): Long

    @Query("SELECT * FROM notes_table WHERE timestamp = :timestamp LIMIT 1")
    fun getGlobalNoteByTimeSync(timestamp: OffsetDateTime?): GlobalNote?

    @Query("SELECT * FROM notes_table")
    fun getAllGlobalNotesSync(): List<GlobalNote>

    @Query("DELETE FROM line_graph_features_table2 WHERE line_graph_id = :lineGraphId")
    fun deleteFeaturesForLineGraph(lineGraphId: Long)

    @Insert
    fun insertLineGraphFeatures(lineGraphFeatures: List<LineGraphFeature>)

    @Insert
    fun insertLineGraph(lineGraph: LineGraph): Long

    @Update
    fun updateLineGraph(lineGraph: LineGraph)

    @Insert
    fun insertPieChart(pieChart: PieChart): Long

    @Update
    fun updatePieChart(pieChart: PieChart)

    @Insert
    fun insertAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat): Long

    @Update
    fun updateAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat)

    @Insert
    fun insertTimeSinceLastStat(timeSinceLastStat: TimeSinceLastStat): Long

    @Update
    fun updateTimeSinceLastStat(timeSinceLastStat: TimeSinceLastStat)

    @Insert
    fun insertGraphOrStat(graphOrStat: GraphOrStat): Long

    @Update
    fun updateGraphOrStat(graphOrStat: GraphOrStat)

    @Update
    fun updateGraphStats(graphStat: List<GraphOrStat>)

    @Update
    fun updateTimeHistogram(timeHistogram: TimeHistogram)

    @Insert
    fun insertTimeHistogram(timeHistogram: TimeHistogram): Long

    @Query("SELECT * FROM time_histograms_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram?

    @Query("SELECT * FROM groups_table WHERE parent_group_id = :id")
    fun getGroupsForGroupSync(id: Long): List<Group>

    @Insert
    fun insertFeatureTimer(featureTimer: FeatureTimer)

    @Query("DELETE FROM feature_timers_table WHERE feature_id=:featureId")
    fun deleteFeatureTimer(featureId: Long)

    @Query("SELECT * FROM feature_timers_table WHERE feature_id=:featureId LIMIT 1")
    fun getFeatureTimer(featureId: Long): FeatureTimer?

    @Query(getDisplayFeaturesQuery + """WHERE start_instant IS NOT NULL ORDER BY start_instant ASC, id DESC""")
    fun getAllActiveTimerFeatures(): List<DisplayFeature>

    @Query(getDisplayFeaturesQuery + """WHERE features_table.id=:featureId LIMIT 1""")
    fun getDisplayFeatureByIdSync(featureId: Long): DisplayFeature?

    @Query("SELECT COUNT(*) FROM data_points_table WHERE feature_id = :id")
    fun getNumberOfDataPointsForFeature(id: Long): Int

    @Query("SELECT * FROM functions_table WHERE id = :functionId LIMIT 1")
    fun getFunctionById(functionId: Long): FunctionEntity?

    @Update
    fun updateFunction(function: FunctionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun createFunction(function: FunctionEntity)
}
