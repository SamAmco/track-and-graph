package com.samco.grapheasy.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "feature_track_group_join",
    foreignKeys = [
        ForeignKey(entity = Feature::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("feature_id"),
            onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackGroup::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("track_group_id"),
            onDelete = ForeignKey.CASCADE)
    ]
)
class FeatureTrackGroupJoin (
    @PrimaryKey(autoGenerate = true)
    var id: Long = -1L,

    @ColumnInfo(name = "feature_id")
    val featureId: Long,

    @ColumnInfo(name = "track_group_id")
    val trackGroupId: Long
)