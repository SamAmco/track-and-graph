package com.samco.grapheasy.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GraphEasyDatabaseDao {
    @Insert
    fun insertTrackGroup(trackGroup: TrackGroup)

    @Query("SELECT * FROM track_groups_table ORDER BY id DESC")
    fun getTrackGroups() : LiveData<List<TrackGroup>>
}