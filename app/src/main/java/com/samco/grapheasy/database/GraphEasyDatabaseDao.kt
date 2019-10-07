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

    @Query("SELECT * FROM features_table WHERE id IN(SELECT featureId FROM feature_track_group_join WHERE trackGroupId = :trackGroupId)")
    fun getFeaturesForTrackGroup(trackGroupId: Long): LiveData<List<Feature>>

    @Insert
    fun insertFeature(feature: Feature): Long

    @Update
    fun updateFeature(feature: Feature)

    @Delete
    fun deleteFeature(feature: Feature)

    @Insert
    fun insertFeatureTrackGroupJoin(featureTrackGroupJoin: FeatureTrackGroupJoin): Long

    @Insert
    fun insertDataPoint(dataPoint: DataPoint): Long

    @Query("SELECT * FROM data_points_table WHERE featureId = :featureId LIMIT 1")
    fun getLastDataPointForFeature(featureId: Long): LiveData<DataPoint>

    @Query("SELECT COUNT(id) FROM data_points_table")
    fun getNumDataPointsForFeature(): LiveData<Int>
}