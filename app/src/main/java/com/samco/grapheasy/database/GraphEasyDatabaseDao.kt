package com.samco.grapheasy.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface GraphEasyDatabaseDao {
    @Insert
    fun insertTrackGroup(trackGroup: TrackGroup)

    @Delete
    fun deleteTrackGroup(trackGroup: TrackGroup)

    @Query("SELECT * FROM track_groups_table ORDER BY id DESC")
    fun getTrackGroups() : LiveData<List<TrackGroup>>

    @Query("SELECT * FROM track_groups_table WHERE id = :id LIMIT 1")
    fun getTrackGroupById(id: Int) : TrackGroup

    @Update
    fun updateTrackGroup(trackGroup: TrackGroup)
}