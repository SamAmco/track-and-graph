package com.samco.grapheasy.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "features_table",
    foreignKeys = [ForeignKey(entity = TrackGroup::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("track_group_id"),
        onDelete = ForeignKey.CASCADE)]
)
data class Feature (
    @PrimaryKey(autoGenerate = true)
    var id: Long = -1L,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "track_group_id")
    val trackGroupId: Long,

    @ColumnInfo(name = "type")
    val featureType: FeatureType = FeatureType.CONTINUOUS,

    @ColumnInfo(name = "discrete_values")
    val discreteValues: List<String>
)

enum class FeatureType constructor(val index: Int) {
    DISCRETE(0), CONTINUOUS(1)
}

