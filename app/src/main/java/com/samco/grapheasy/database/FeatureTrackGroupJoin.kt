package com.samco.grapheasy.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "feature_track_group_join",
    foreignKeys = [
        ForeignKey(entity = Feature::class, parentColumns = arrayOf("id"),
            childColumns = arrayOf("featureId"), onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackGroup::class, parentColumns = arrayOf("id"),
            childColumns = arrayOf("trackGroupId"), onDelete = ForeignKey.CASCADE)
    ]
)
class FeatureTrackGroupJoin (
    @PrimaryKey(autoGenerate = true)
    var id: Long = -1L,

    @ColumnInfo(name = "featureId")
    val featureId: Long,

    @ColumnInfo(name = "trackGroupId")
    val trackGroupId: Long
)