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
package com.samco.trackandgraph.data.database

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.samco.trackandgraph.data.database.entity.AverageTimeBetweenStat
import com.samco.trackandgraph.data.database.entity.BarChart
import com.samco.trackandgraph.data.database.entity.DataPoint
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.FeatureTimer
import com.samco.trackandgraph.data.database.entity.GlobalNote
import com.samco.trackandgraph.data.database.entity.GraphOrStat
import com.samco.trackandgraph.data.database.entity.Group
import com.samco.trackandgraph.data.database.entity.LastValueStat
import com.samco.trackandgraph.data.database.entity.LineGraph
import com.samco.trackandgraph.data.database.entity.LineGraphFeature
import com.samco.trackandgraph.data.database.entity.LuaGraph
import com.samco.trackandgraph.data.database.entity.LuaGraphFeature
import com.samco.trackandgraph.data.database.entity.PieChart
import com.samco.trackandgraph.data.database.entity.Reminder
import com.samco.trackandgraph.data.database.entity.TimeHistogram
import com.samco.trackandgraph.data.database.entity.Tracker
import com.samco.trackandgraph.data.database.entity.queryresponse.DisplayNote
import com.samco.trackandgraph.data.database.entity.queryresponse.DisplayTracker
import com.samco.trackandgraph.data.database.entity.queryresponse.LineGraphWithFeatures
import com.samco.trackandgraph.data.database.entity.queryresponse.LuaGraphWithFeatures
import com.samco.trackandgraph.data.database.entity.queryresponse.TrackerWithFeature
import kotlinx.coroutines.flow.Flow

private const val getTrackersQuery = """
    SELECT 
        features_table.name as name,
        features_table.group_id as group_id,
        features_table.display_index as display_index,
        features_table.feature_description as feature_description,
        trackers_table.id as id,
        trackers_table.feature_id as feature_id,
        trackers_table.type as type,
        trackers_table.has_default_value as has_default_value,
        trackers_table.default_value as default_value,
        trackers_table.default_label as default_label,
        trackers_table.suggestion_type as suggestion_type,
        trackers_table.suggestion_order as suggestion_order
    FROM trackers_table
    LEFT JOIN features_table ON trackers_table.feature_id = features_table.id
            """

private const val getDisplayTrackersQuery = """ 
    SELECT
        features_table.name as name,
        features_table.group_id as group_id,
        features_table.display_index as display_index,
        features_table.feature_description as feature_description,
        trackers_table.id as id,
        trackers_table.feature_id as feature_id,
        trackers_table.type as type,
        trackers_table.has_default_value as has_default_value,
        trackers_table.default_value as default_value,
        trackers_table.default_label as default_label,
        last_epoch_milli,
        last_utc_offset_sec,
        start_instant 
        FROM (
            trackers_table
            LEFT JOIN features_table ON trackers_table.feature_id = features_table.id
            LEFT JOIN (
                SELECT feature_id, epoch_milli as last_epoch_milli, utc_offset_sec as last_utc_offset_sec
                FROM data_points_table as dpt
                INNER JOIN (
                    SELECT feature_id as fid, MAX(epoch_milli) as max_epoch_milli
                    FROM data_points_table 
                    GROUP BY feature_id
                ) as max_data ON max_data.fid = dpt.feature_id AND dpt.epoch_milli = max_data.max_epoch_milli
            ) as last_data ON last_data.feature_id = trackers_table.feature_id
            LEFT JOIN (
                SELECT * FROM feature_timers_table
            ) as timer_data ON timer_data.feature_id = trackers_table.feature_id
        )
    """


//TODO it would probably be better if we migrated from LiveData to flow here to remove lifecycle
// awareness from the model layer
@Dao
internal interface TrackAndGraphDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGroup(group: Group): Long

    @Query("DELETE FROM groups_table WHERE id = :id")
    fun deleteGroup(id: Long)

    @Update
    fun updateGroup(group: Group)

    @Update
    fun updateGroups(groups: List<Group>)

    @Query("""SELECT * FROM reminders_table ORDER BY display_index ASC, id DESC""")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("""SELECT * FROM reminders_table ORDER BY display_index ASC, id DESC""")
    fun getAllRemindersSync(): List<Reminder>

    @Query("""SELECT groups_table.* FROM groups_table ORDER BY display_index ASC, id DESC""")
    fun getAllGroups(): Flow<List<Group>>

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

    @Query("$getDisplayTrackersQuery WHERE group_id = :groupId ORDER BY features_table.display_index ASC, id DESC")
    fun getDisplayTrackersForGroupSync(groupId: Long): List<DisplayTracker>

    @Query("SELECT features_table.* FROM features_table WHERE group_id = :groupId ORDER BY features_table.display_index ASC")
    fun getFeaturesForGroupSync(groupId: Long): List<Feature>

    @Query(
        """
            SELECT
                features_table.name as name,
                features_table.group_id as group_id,
                features_table.display_index as display_index,
                features_table.feature_description as feature_description,
                trackers_table.id as id,
                trackers_table.feature_id as feature_id,
                trackers_table.type as type,
                trackers_table.has_default_value as has_default_value,
                trackers_table.default_value as default_value,
                trackers_table.default_label as default_label,
                trackers_table.suggestion_order as suggestion_order,
                trackers_table.suggestion_type as suggestion_type
            FROM trackers_table
            LEFT JOIN features_table ON features_table.id = trackers_table.feature_id
            WHERE features_table.group_id = :groupId ORDER BY features_table.display_index ASC
        """
    )
    fun getTrackersForGroupSync(groupId: Long): List<TrackerWithFeature>

    @Query("SELECT * FROM features_table WHERE id = :featureId LIMIT 1")
    fun getFeatureById(featureId: Long): Feature?

    @Query("""SELECT * from features_table WHERE id IN (:featureIds) ORDER BY display_index ASC, id DESC""")
    fun getFeaturesByIdsSync(featureIds: List<Long>): List<Feature>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY epoch_milli DESC")
    fun getDataPointsForFeatureSync(featureId: Long): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId AND epoch_milli = :epochMilli")
    fun getDataPointByTimestampAndFeatureSync(featureId: Long, epochMilli: Long): DataPoint?

    @Query("SELECT COUNT(*) FROM data_points_table WHERE feature_id = :featureId")
    fun getDataPointCount(featureId: Long): Int

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY epoch_milli DESC LIMIT :limit OFFSET :offset")
    fun getDataPoints(featureId: Long, limit: Int, offset: Int): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY epoch_milli DESC")
    fun getDataPointsCursor(featureId: Long): Cursor

    @Query("SELECT * FROM graphs_and_stats_table2 WHERE id = :graphStatId LIMIT 1")
    fun getGraphStatById(graphStatId: Long): GraphOrStat

    @Query("SELECT * FROM graphs_and_stats_table2 WHERE id = :graphStatId LIMIT 1")
    fun tryGetGraphStatById(graphStatId: Long): GraphOrStat?

    @Query("SELECT * FROM line_graphs_table3 WHERE graph_stat_id = :graphStatId LIMIT 1")
    @Transaction
    fun getLineGraphByGraphStatId(graphStatId: Long): LineGraphWithFeatures?

    @Query("SELECT * FROM lua_graphs_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    @Transaction
    fun getLuaGraphByGraphStatId(graphStatId: Long): LuaGraphWithFeatures?

    @Query("SELECT * FROM pie_charts_table2 WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getPieChartByGraphStatId(graphStatId: Long): PieChart?

    @Query("SELECT * FROM average_time_between_stat_table4 WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat?

    @Query("SELECT * FROM graphs_and_stats_table2 WHERE group_id = :groupId ORDER BY display_index ASC, id DESC")
    fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat>

    @Query("SELECT * FROM graphs_and_stats_table2 ORDER BY display_index ASC, id DESC")
    fun getAllGraphStatsSync(): List<GraphOrStat>

    @Query(
        """
        SELECT * FROM (
            SELECT dp.epoch_milli as epoch_milli, dp.utc_offset_sec as utc_offset_sec, t.id as tracker_id, dp.feature_id as feature_id, f.name as feature_name, g.id as group_id, dp.note as note
            FROM data_points_table as dp
            LEFT JOIN features_table as f ON dp.feature_id = f.id
            LEFT JOIN trackers_table as t ON dp.feature_id = t.feature_id
            LEFT JOIN groups_table as g ON f.group_id = g.id
            WHERE dp.note IS NOT NULL AND dp.note != ""
        ) UNION SELECT * FROM (
            SELECT n.epoch_milli as epoch_milli, n.utc_offset_sec as utc_offset_sec, NULL as tracker_id, NULL as feature_id, NULL as feature_name, NULL as group_id, n.note as note
            FROM notes_table as n
        ) ORDER BY epoch_milli DESC
        """
    )
    fun getAllDisplayNotes(): Flow<List<DisplayNote>>

    @Query("UPDATE data_points_table SET note = '' WHERE epoch_milli = :epochMilli AND feature_id = :featureId")
    fun removeNote(epochMilli: Long, featureId: Long)

    @Delete
    fun deleteGlobalNote(note: GlobalNote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGlobalNote(note: GlobalNote): Long

    @Query("SELECT * FROM notes_table WHERE epoch_milli = :epochMilli LIMIT 1")
    fun getGlobalNoteByTimeSync(epochMilli: Long): GlobalNote?

    @Query("SELECT * FROM notes_table")
    fun getAllGlobalNotesSync(): List<GlobalNote>

    @Query("DELETE FROM line_graph_features_table2 WHERE line_graph_id = :lineGraphId")
    fun deleteFeaturesForLineGraph(lineGraphId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLineGraphFeatures(lineGraphFeatures: List<LineGraphFeature>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLineGraph(lineGraph: LineGraph): Long

    @Update
    fun updateLineGraph(lineGraph: LineGraph)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPieChart(pieChart: PieChart): Long

    @Update
    fun updatePieChart(pieChart: PieChart)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat): Long

    @Update
    fun updateAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGraphOrStat(graphOrStat: GraphOrStat): Long

    @Update
    fun updateGraphOrStat(graphOrStat: GraphOrStat)

    @Update
    fun updateGraphStats(graphStat: List<GraphOrStat>)

    @Update
    fun updateTimeHistogram(timeHistogram: TimeHistogram)

    @Update
    fun updateLastValueStat(lastValueStat: LastValueStat)

    @Update
    fun updateBarChart(barChart: BarChart)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTimeHistogram(timeHistogram: TimeHistogram): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLastValueStat(lastValueStat: LastValueStat): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBarChart(barChart: BarChart): Long

    @Query("SELECT * FROM time_histograms_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram?

    @Query("SELECT * FROM last_value_stats_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getLastValueStatByGraphStatId(graphStatId: Long): LastValueStat?

    @Query("SELECT * FROM bar_charts_table WHERE graph_stat_id = :graphStatId LIMIT 1")
    fun getBarChartByGraphStatId(graphStatId: Long): BarChart?

    @Query("SELECT * FROM groups_table WHERE parent_group_id = :id")
    fun getGroupsForGroupSync(id: Long): List<Group>

    @Query("SELECT * FROM groups_table WHERE parent_group_id IS NULL LIMIT 1")
    fun getRootGroupSync(): Group?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFeatureTimer(featureTimer: FeatureTimer)

    @Query("DELETE FROM feature_timers_table WHERE feature_id=:featureId")
    fun deleteFeatureTimer(featureId: Long)

    @Query("SELECT * FROM feature_timers_table WHERE feature_id=:featureId LIMIT 1")
    fun getFeatureTimer(featureId: Long): FeatureTimer?

    @Query("$getDisplayTrackersQuery WHERE start_instant IS NOT NULL ORDER BY start_instant ASC, id DESC")
    fun getAllActiveTimerTrackers(): List<DisplayTracker>

    @Query("$getDisplayTrackersQuery WHERE trackers_table.feature_id=:featureId LIMIT 1")
    fun getDisplayTrackerByFeatureIdSync(featureId: Long): DisplayTracker?

    @Query(getTrackersQuery)
    fun getAllTrackersSync(): List<TrackerWithFeature>

    @Query("$getTrackersQuery WHERE trackers_table.id = :trackerId LIMIT 1")
    fun getTrackerById(trackerId: Long): TrackerWithFeature?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTracker(tracker: Tracker): Long

    @Update
    fun updateTracker(tracker: Tracker)

    @Query("$getTrackersQuery WHERE feature_id = :featureId LIMIT 1")
    fun getTrackerByFeatureId(featureId: Long): TrackerWithFeature?

    @Query("SELECT COUNT(*) FROM trackers_table")
    fun numTrackers(): Int

    @Query(
        """
            SELECT DISTINCT data_points_table.label 
            FROM data_points_table 
            WHERE data_points_table.feature_id = (
                SELECT trackers_table.feature_id 
                FROM trackers_table 
                WHERE trackers_table.id = :trackerId
            )
        """
    )
    fun getLabelsForTracker(trackerId: Long): List<String>

    @Query(" SELECT EXISTS ( SELECT 1 FROM data_points_table LIMIT 1 ) ")
    fun hasAtLeastOneDataPoint(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertLuaGraph(luaGraph: LuaGraph): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLuaGraphFeatures(map: List<LuaGraphFeature>)

    @Update
    fun updateLuaGraph(luaGraph: LuaGraph)

    @Query("DELETE FROM lua_graph_features_table WHERE lua_graph_id = :luaGraphId")
    fun deleteFeaturesForLuaGraph(luaGraphId: Long)

    @Query("SELECT EXISTS (SELECT 1 FROM lua_graphs_table LIMIT 1)")
    fun hasAnyLuaGraphs(): Boolean

    @Query("SELECT EXISTS (SELECT 1 FROM graphs_and_stats_table2 LIMIT 1)")
    fun hasAnyGraphs(): Boolean

    @Query("SELECT EXISTS (SELECT 1 FROM features_table LIMIT 1)")
    fun hasAnyFeatures(): Boolean

    @Query("SELECT EXISTS (SELECT 1 FROM groups_table WHERE parent_group_id IS NOT NULL LIMIT 1)")
    fun hasAnyGroups(): Boolean

    @Query("SELECT EXISTS (SELECT 1 FROM reminders_table LIMIT 1)")
    fun hasAnyReminders(): Boolean
}
