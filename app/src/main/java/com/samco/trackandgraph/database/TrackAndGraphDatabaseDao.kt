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
package com.samco.trackandgraph.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.samco.trackandgraph.database.dto.*
import com.samco.trackandgraph.database.entity.*
import org.threeten.bp.OffsetDateTime

private const val getFeatureByIdQuery =
    """SELECT * FROM features_table WHERE id = :featureId LIMIT 1"""

@Dao
interface TrackAndGraphDatabaseDao {
    @RawQuery
    fun doRawQuery(supportSQLiteQuery: SupportSQLiteQuery?): Int

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

    @Query("""SELECT features_table.* FROM features_table ORDER BY display_index ASC, id DESC""")
    fun getAllFeatures(): LiveData<List<Feature>>

    @Query("""SELECT features_table.* FROM features_table ORDER BY display_index ASC, id DESC""")
    fun getAllFeaturesSync(): List<Feature>

    @Query("""SELECT groups_table.* FROM groups_table ORDER BY display_index ASC, id DESC""")
    fun getAllGroupsSync(): List<Group>

    @Query("""SELECT * FROM data_points_table""")
    fun getAllDataPoints(): LiveData<List<DataPoint>>

    @Insert
    fun insertReminder(reminder: Reminder)

    @Delete
    fun deleteReminder(reminder: Reminder)

    @Update
    fun updateReminder(reminder: Reminder)

    @Update
    fun updateReminders(reminders: List<Reminder>)

    @Query("SELECT * FROM groups_table WHERE id = :id LIMIT 1")
    fun getGroupById(id: Long): Group

    @Update
    fun updateFeatures(features: List<Feature>)

    @Query(
        """SELECT features_table.*, num_data_points, last_timestamp from features_table 
        LEFT JOIN (
            SELECT feature_id as id, COUNT(*) as num_data_points, MAX(timestamp) as last_timestamp 
            FROM data_points_table GROUP BY feature_id
        ) as feature_data 
        ON feature_data.id = features_table.id
		WHERE group_id = :groupId
        ORDER BY features_table.display_index ASC, id DESC"""
    )
    fun getDisplayFeaturesForGroup(groupId: Long): LiveData<List<DisplayFeature>>

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

    //TODO make these descending. Eventually all access to data points will be mediated so all need to be in the same order
    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value IN (:values) AND timestamp < :endDateTime AND timestamp > :startDateTime ORDER BY timestamp""")
    fun getDataPointsWithValueInTimeRange(
        featureId: Long,
        values: List<Int>,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime
    ): List<DataPoint>

    //TODO make these descending. Eventually all access to data points will be mediated so all need to be in the same order
    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value >= :min AND value <= :max  AND timestamp < :endDateTime AND timestamp > :startDateTime ORDER BY timestamp""")
    fun getDataPointsBetweenInTimeRange(
        featureId: Long,
        min: String,
        max: String,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime
    ): List<DataPoint>

    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value >= :min AND value <= :max ORDER BY timestamp DESC LIMIT 1""")
    fun getLastDataPointBetween(
        featureId: Long,
        min: String,
        max: String
    ): DataPoint?

    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value IN (:values) ORDER BY timestamp DESC LIMIT 1""")
    fun getLastDataPointWithValue(
        featureId: Long,
        values: List<Int>
    ): DataPoint?

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC")
    fun getDataPointsForFeatureSync(featureId: Long): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId AND timestamp > :cutOff AND timestamp < :now ORDER BY timestamp DESC")
    fun getDataPointsForFeatureBetweenDescSync(
        featureId: Long,
        cutOff: OffsetDateTime,
        now: OffsetDateTime
    ): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC LIMIT 1")
    fun getLastDataPointForFeatureSync(featureId: Long): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC")
    fun getDataPointsForFeature(featureId: Long): LiveData<List<DataPoint>>

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

    @Query("SELECT * FROM average_time_between_stat_table2 WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat?

    @Query("SELECT * FROM time_since_last_stat_table2 WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getTimeSinceLastStatByGraphStatId(graphStatId: Long): TimeSinceLastStat?

    @Query("SELECT * FROM graphs_and_stats_table2 WHERE group_id = :groupId ORDER BY display_index ASC, id DESC")
    fun getGraphsAndStatsByGroupId(groupId: Long): LiveData<List<GraphOrStat>>

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
    fun insertTimeHistogram(timeHistogram: TimeHistogram)

    @Query("SELECT * FROM time_histograms_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram?

    @Query("SELECT * FROM groups_table WHERE parent_group_id = :id")
    fun getGroupsForGroup(id: Long): LiveData<List<Group>>
}
