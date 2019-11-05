package com.samco.trackandgraph.database

import androidx.lifecycle.LiveData
import androidx.room.*
import org.threeten.bp.OffsetDateTime

@Dao
interface TrackAndGraphDatabaseDao {
    @Insert
    fun insertTrackGroup(trackGroup: TrackGroup): Long

    @Delete
    fun deleteTrackGroup(trackGroup: TrackGroup)

    @Query("SELECT * FROM track_groups_table ORDER BY display_index ASC")
    fun getTrackGroups() : LiveData<List<TrackGroup>>

    @Query("SELECT * FROM graph_stat_groups_table ORDER BY display_index ASC")
    fun getGraphStatGroups() : LiveData<List<GraphStatGroup>>

    @Delete
    fun deleteGraphStatGroup(graphStatGroup: GraphStatGroup)

    @Update
    fun updateGraphStatGroup(graphStatGroup: GraphStatGroup)

    @Update
    fun updateGraphStatGroups(graphStatGroups: List<GraphStatGroup>)

    @Insert
    fun insertGraphStatGroup(graphStatGroup: GraphStatGroup)

    @Query("SELECT * FROM track_groups_table WHERE id = :id LIMIT 1")
    fun getTrackGroupById(id: Int) : TrackGroup

    @Update
    fun updateTrackGroup(trackGroup: TrackGroup)

    @Update
    fun updateTrackGroups(trackGroups: List<TrackGroup>)

    @Query("""SELECT features_table.*, num_data_points, last_timestamp from features_table 
        LEFT JOIN (
            SELECT feature_id as id, COUNT(*) as num_data_points, MAX(timestamp) as last_timestamp 
            FROM data_points_table GROUP BY feature_id
        ) as feature_data 
        ON feature_data.id = features_table.id
		WHERE track_group_id = :trackGroupId""")
    fun getDisplayFeaturesForTrackGroup(trackGroupId: Long): LiveData<List<DisplayFeature>>

    @Query("SELECT features_table.* FROM features_table WHERE track_group_id = :trackGroupId")
    fun getFeaturesForTrackGroupSync(trackGroupId: Long): List<Feature>

    @Query("""SELECT * FROM features_table WHERE id = :featureId LIMIT 1""")
    fun getFeatureById(featureId: Long): Feature

    @Query("""SELECT * FROM features_table WHERE id = :featureId LIMIT 1""")
    fun tryGetFeatureById(featureId: Long): Feature?

    @Query("""SELECT features_table.*, track_groups_table.name as track_group_name 
        FROM features_table 
        LEFT JOIN track_groups_table 
        ON features_table.track_group_id = track_groups_table.id""")
    fun getAllFeaturesAndTrackGroups(): LiveData<List<FeatureAndTrackGroup>>

    @Query("""SELECT features_table.*, track_groups_table.name as track_group_name 
        FROM features_table 
        LEFT JOIN track_groups_table 
        ON features_table.track_group_id = track_groups_table.id
        WHERE features_table.id = :featureId LIMIT 1""")
    fun getFeatureAndTrackGroupByFeatureId(featureId: Long): FeatureAndTrackGroup?

    @Query("""SELECT * from features_table WHERE id IN (:featureIds)""")
    fun getFeaturesByIdsSync(featureIds: List<Long>): List<Feature>

    @Insert
    fun insertFeature(feature: Feature): Long

    @Update
    fun updateFeature(feature: Feature)

    @Delete
    fun deleteFeature(feature: Feature)

    @Delete
    fun deleteDataPoint(dataPoint: DataPoint)

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

    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value >= :min AND value <= :max ORDER BY timestamp""")
    fun getDataPointsBetween(featureId: Long, min: String, max: String): List<DataPoint>

    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value >= :min AND value <= :max  AND timestamp < :now AND timestamp > :cutOff ORDER BY timestamp""")
    fun getDataPointsBetweenInTimeRange(featureId: Long, min: String, max: String, cutOff: OffsetDateTime, now: OffsetDateTime): List<DataPoint>

    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value >= :min AND value <= :max ORDER BY timestamp DESC LIMIT 1""")
    fun getLastDataPointBetween(featureId: Long, min: String, max: String): DataPoint?

    @Query("""SELECT * FROM data_points_table WHERE feature_id = :featureId AND value = :value ORDER BY timestamp DESC LIMIT 1""")
    fun getLastDataPointWithValue(featureId: Long, value: String): DataPoint?

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC")
    fun getDataPointsForFeatureSync(featureId: Long): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId AND timestamp > :cutOff AND timestamp < :now ORDER BY timestamp ASC")
    fun getDataPointsForFeatureBetweenAscSync(featureId: Long, cutOff: OffsetDateTime, now: OffsetDateTime): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp ASC")
    fun getDataPointsForFeatureAscSync(featureId: Long): List<DataPoint>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC")
    fun getDataPointsForFeature(featureId: Long): LiveData<List<DataPoint>>

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId AND timestamp = :timestamp")
    fun getDataPointByTimestampAndFeatureSync(featureId: Long, timestamp: OffsetDateTime): DataPoint

    @Query("SELECT * FROM graphs_and_stats_table WHERE id = :graphStatId LIMIT 1")
    fun getGraphStatById(graphStatId: Long): GraphOrStat?

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
