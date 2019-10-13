package com.samco.grapheasy.database

import androidx.room.ColumnInfo
import org.threeten.bp.OffsetDateTime

data class DisplayFeature(
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

    @ColumnInfo(name = "last_timestamp")
    val timestamp: OffsetDateTime?,

    @ColumnInfo(name = "num_data_points")
    val numDataPoints: Long?
) {
    fun getDisplayTimestamp() = timestamp?.let { displayFeatureDateFormat.format(timestamp) }
}