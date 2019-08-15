package com.samco.grapheasy.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_groups_table")
class TrackGroup(
    @PrimaryKey(autoGenerate = true)
    var id: Long = -1L,

    @ColumnInfo(name = "name")
    val name: String = ""
)