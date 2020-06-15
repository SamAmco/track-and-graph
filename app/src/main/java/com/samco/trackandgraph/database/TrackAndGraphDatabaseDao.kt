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
import org.threeten.bp.OffsetDateTime

//TODO there is a lot of duplication in this file, you should extract all common queries (possibly all queries)
// to top level constants as below

private const val getAllFeaturesAndTrackGroupsQuery = """
        SELECT features_table.id, features_table.name, features_table.display_index,
            features_table.type, features_table.discrete_values, track_groups_table.id as track_group_id,
            track_groups_table.name as track_group_name 
        FROM features_table 
        LEFT JOIN track_groups_table 
        ON features_table.track_group_id = track_groups_table.id
        ORDER BY track_groups_table.display_index ASC, features_table.display_index ASC, features_table.id ASC"""

private const val getFeatureAndTrackGroupByFeatureId = """
    SELECT features_table.id, features_table.name, features_table.display_index,
            features_table.type, features_table.discrete_values, track_groups_table.id as track_group_id,
            track_groups_table.name as track_group_name
    FROM features_table
    LEFT JOIN track_groups_table
    ON features_table.track_group_id = track_groups_table.id
    WHERE features_table.id = :featureId LIMIT 1"""

private const val getFeatureByIdQuery =
    """SELECT * FROM features_table WHERE id = :featureId LIMIT 1"""

@Dao
interface TrackAndGraphDatabaseDao {
    @RawQuery
    fun doRawQuery(supportSQLiteQuery: SupportSQLiteQuery?): Int

    @Insert
    fun insertTrackGroup(trackGroup: TrackGroup): Long

    @Delete
    fun deleteTrackGroup(trackGroup: TrackGroup)

    @Query("""SELECT COUNT(*) FROM features_table""")
    fun getNumFeatures(): Long

    @Query("""SELECT * FROM reminders_table ORDER BY display_index ASC, id DESC""")
    fun getAllReminders(): LiveData<List<Reminder>>

    @Query("""SELECT * FROM reminders_table ORDER BY display_index ASC, id DESC""")
    fun getAllRemindersSync(): List<Reminder>

    @Query(
        """SELECT track_groups_table.*, 0 as type FROM track_groups_table 
        UNION SELECT graph_stat_groups_table.*, 1 as type FROM graph_stat_groups_table
        ORDER BY display_index ASC"""
    )
    fun getAllGroups(): LiveData<List<GroupItem>>

    @Query(
        """SELECT track_groups_table.*, 0 as type FROM track_groups_table 
        UNION SELECT graph_stat_groups_table.*, 1 as type FROM graph_stat_groups_table
        ORDER BY display_index ASC"""
    )
    fun getAllGroupsSync(): List<GroupItem>

    @Query("SELECT * FROM track_groups_table ORDER BY display_index ASC")
    fun getTrackGroups(): LiveData<List<TrackGroup>>

    @Query("SELECT * FROM graph_stat_groups_table ORDER BY display_index ASC")
    fun getGraphStatGroups(): LiveData<List<GraphStatGroup>>

    @Delete
    fun deleteGraphStatGroup(graphStatGroup: GraphStatGroup)

    @Update
    fun updateGraphStatGroup(graphStatGroup: GraphStatGroup)

    @Update
    fun updateGraphStatGroups(graphStatGroups: List<GraphStatGroup>)

    @Insert
    fun insertGraphStatGroup(graphStatGroup: GraphStatGroup)

    @Insert
    fun insertReminder(reminder: Reminder)

    @Delete
    fun deleteReminder(reminder: Reminder)

    @Update
    fun updateReminder(reminder: Reminder)

    @Update
    fun updateReminders(reminders: List<Reminder>)

    @Query("SELECT * FROM track_groups_table WHERE id = :id LIMIT 1")
    fun getTrackGroupById(id: Int): TrackGroup

    @Update
    fun updateTrackGroup(trackGroup: TrackGroup)

    @Update
    fun updateTrackGroups(trackGroups: List<TrackGroup>)

    @Update
    fun updateFeatures(features: List<Feature>)

    @Query(
        """SELECT features_table.*, num_data_points, last_timestamp from features_table 
        LEFT JOIN (
            SELECT feature_id as id, COUNT(*) as num_data_points, MAX(timestamp) as last_timestamp 
            FROM data_points_table GROUP BY feature_id
        ) as feature_data 
        ON feature_data.id = features_table.id
		WHERE track_group_id = :trackGroupId
        ORDER BY features_table.display_index ASC, id DESC"""
    )
    fun getDisplayFeaturesForTrackGroup(trackGroupId: Long): LiveData<List<DisplayFeature>>

    @Query("SELECT features_table.* FROM features_table WHERE track_group_id = :trackGroupId ORDER BY features_table.display_index ASC")
    fun getFeaturesForTrackGroupSync(trackGroupId: Long): List<Feature>

    @Query(getFeatureByIdQuery)
    fun getFeatureById(featureId: Long): Feature

    @Query(getFeatureByIdQuery)
    fun tryGetFeatureByIdSync(featureId: Long): Feature?

    @Query(getFeatureByIdQuery)
    fun tryGetFeatureById(featureId: Long): LiveData<Feature?>

    @Query(getAllFeaturesAndTrackGroupsQuery)
    fun getAllFeaturesAndTrackGroups(): LiveData<List<FeatureAndTrackGroup>>

    @Query(getAllFeaturesAndTrackGroupsQuery)
    fun getAllFeaturesAndTrackGroupsSync(): List<FeatureAndTrackGroup>

    @Query(getFeatureAndTrackGroupByFeatureId)
    fun getFeatureAndTrackGroupByFeatureId(featureId: Long): FeatureAndTrackGroup?

    @Query("""SELECT * from features_table WHERE id IN (:featureIds) ORDER BY display_index ASC, id DESC""")
    fun getFeaturesByIdsSync(featureIds: List<Long>): List<Feature>

    @Insert
    fun insertFeature(feature: Feature): Long

    @Update
    fun updateFeature(feature: Feature)

    @Delete
    fun deleteFeature(feature: Feature)

    @Delete
    fun deleteDataPoint(dataPoint: DataPoint)

    @Query("DELETE FROM data_points_table WHERE feature_id = :featureId AND value = :index")
    fun deleteAllDataPointsForDiscreteValue(featureId: Long, index: Double)

    @Delete
    fun deleteGraphOrStat(graphOrStat: GraphOrStat)

    @Query("DELETE FROM features_table WHERE id = :id")
    fun deleteFeature(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDataPoint(dataPoint: DataPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDataPoints(dataPoint: List<DataPoint>)

    @Update
    fun updateDataPoint(dataPoint: DataPoint)

    @Update
    fun updateDataPoints(dataPoint: List<DataPoint>)

    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value IN (:values) AND timestamp < :endDateTime AND timestamp > :startDateTime ORDER BY timestamp""")
    fun getDataPointsWithValueInTimeRange(
        featureId: Long,
        values: List<Int>,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime
    ): List<DataPoint>

    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value >= :min AND value <= :max  AND timestamp < :endDateTime AND timestamp > :startDateTime ORDER BY timestamp""")
    fun getDataPointsBetweenInTimeRange(
        featureId: Long,
        min: String,
        max: String,
        startDateTime: OffsetDateTime,
        endDateTime: OffsetDateTime
    ): List<DataPoint>

    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value >= :min AND value <= :max AND timestamp < :endDate ORDER BY timestamp DESC LIMIT 1""")
    fun getLastDataPointBetween(
        featureId: Long,
        min: String,
        max: String,
        endDate: OffsetDateTime
    ): DataPoint?

    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value IN (:values) AND timestamp < :endDate ORDER BY timestamp DESC LIMIT 1""")
    fun getLastDataPointWithValue(
        featureId: Long,
        values: List<Int>,
        endDate: OffsetDateTime
    ): DataPoint?

    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value = :value ORDER BY timestamp DESC LIMIT 1""")
    fun getLastDataPointWithValue(featureId: Long, value: String): DataPoint?

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC")
    fun getDataPointsForFeatureSync(featureId: Long): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId AND timestamp > :cutOff AND timestamp < :now ORDER BY timestamp ASC")
    fun getDataPointsForFeatureBetweenAscSync(
        featureId: Long,
        cutOff: OffsetDateTime,
        now: OffsetDateTime
    ): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC LIMIT 1")
    fun getLastDataPointForFeatureSync(featureId: Long): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp ASC")
    fun getDataPointsForFeatureAscSync(featureId: Long): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC")
    fun getDataPointsForFeature(featureId: Long): LiveData<List<DataPoint>>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId AND timestamp = :timestamp")
    fun getDataPointByTimestampAndFeatureSync(featureId: Long, timestamp: OffsetDateTime): DataPoint

    @Query("SELECT * FROM graphs_and_stats_table WHERE id = :graphStatId LIMIT 1")
    fun getGraphStatById(graphStatId: Long): GraphOrStat

    @Query("SELECT * FROM graphs_and_stats_table WHERE id = :graphStatId LIMIT 1")
    fun tryGetGraphStatById(graphStatId: Long): GraphOrStat?

    @Query("SELECT * FROM line_graphs_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getLineGraphByGraphStatId(graphStatId: Long): LineGraph?

    @Query("SELECT * FROM pie_chart_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getPieChartByGraphStatId(graphStatId: Long): PieChart?

    @Query("SELECT * FROM average_time_between_stat_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat?

    @Query("SELECT * FROM time_since_last_stat_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getTimeSinceLastStatByGraphStatId(graphStatId: Long): TimeSinceLastStat?

    @Query("SELECT * FROM graphs_and_stats_table WHERE graph_stat_group_id = :graphStatGroupId ORDER BY display_index ASC")
    fun getGraphsAndStatsByGroupId(graphStatGroupId: Long): LiveData<List<GraphOrStat>>

    @Query("SELECT * FROM graphs_and_stats_table ORDER BY display_index ASC")
    fun getAllGraphStatsSync(): List<GraphOrStat>

    @Query(
        """
            SELECT * FROM (
                SELECT dp.timestamp as timestamp, 0 as note_type, dp.feature_id as feature_id, f.name as feature_name, t.name as track_group_name, dp.note as note
                FROM data_points_table as dp 
                LEFT JOIN features_table as f ON dp.feature_id = f.id
                LEFT JOIN track_groups_table as t ON f.track_group_id = t.id
            ) UNION SELECT * FROM (
                SELECT n.timestamp as timestamp, 1 as note_type, NULL as feature_id, NULL as feature_name, NULL as track_group_name, n.note as note
                FROM notes_table as n
            ) ORDER BY timestamp DESC
        """
    )
    fun getAllDisplayNotes(): LiveData<List<DisplayNote>>

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
}
