package com.samco.grapheasy.database

import androidx.room.ColumnInfo

data class FeatureAndTrackGroup(
    @ColumnInfo(name = "id")
    var id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "track_group_id")
    val trackGroupId: Long,

    @ColumnInfo(name = "type")
    val featureType: FeatureType = FeatureType.CONTINUOUS,

    @ColumnInfo(name = "discrete_values")
    val discreteValues: List<DiscreteValue>,

    @ColumnInfo(name = "track_group_name")
    val trackGroupName: String
)
