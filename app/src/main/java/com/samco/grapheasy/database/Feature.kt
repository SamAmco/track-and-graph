package com.samco.grapheasy.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "features_table")
data class Feature (
    @PrimaryKey(autoGenerate = true)
    var id: Long = -1L,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "type")
    val featureType: FeatureType = FeatureType.CONTINUOUS,

    @ColumnInfo(name = "discrete_values")
    val discreteValues: String = ""
)

enum class FeatureType constructor(val index: Int) {
    DISCRETE(0), CONTINUOUS(1)
}

