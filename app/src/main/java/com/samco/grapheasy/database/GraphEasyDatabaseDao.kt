package com.samco.grapheasy.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface GraphEasyDatabaseDao {
    @Insert
    fun insertTrackGroup(trackGroup: TrackGroup): Long

    @Delete
    fun deleteTrackGroup(trackGroup: TrackGroup)

    @Query("SELECT * FROM track_groups_table ORDER BY id DESC")
    fun getTrackGroups() : LiveData<List<TrackGroup>>

    @Query("SELECT * FROM track_groups_table WHERE id = :id LIMIT 1")
    fun getTrackGroupById(id: Int) : TrackGroup

    @Update
    fun updateTrackGroup(trackGroup: TrackGroup)

    @Query("""SELECT track_group_id, features_table.id, features_table.name, features_table.type, features_table.discrete_values, num_data_points, last_timestamp from features_table 
        LEFT JOIN (
            SELECT feature_id as id, COUNT(id) as num_data_points, MAX(timestamp) as last_timestamp 
            FROM data_points_table GROUP BY feature_id
        ) as feature_data 
        ON feature_data.id = features_table.id
		LEFT JOIN feature_track_group_join ON feature_track_group_join.feature_id = features_table.id 
		WHERE track_group_id = :trackGroupId""")
    fun getDisplayFeaturesForTrackGroup(trackGroupId: Long): LiveData<List<DisplayFeature>>

    @Insert
    fun insertFeature(feature: Feature): Long

    @Update
    fun updateFeature(feature: Feature)

    @Delete
    fun deleteFeature(feature: Feature)

    @Query("DELETE FROM features_table WHERE id = :id")
    fun deleteFeature(id: Long)

    @Insert
    fun insertFeatureTrackGroupJoin(featureTrackGroupJoin: FeatureTrackGroupJoin): Long

    @Insert
    fun insertDataPoint(dataPoint: DataPoint): Long

    @Query("SELECT * FROM data_points_table WHERE feature_id = :featureId ORDER BY timestamp DESC")
    fun getDataPointsForFeature(featureId: Long): LiveData<List<DataPoint>>
}
