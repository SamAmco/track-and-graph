package com.samco.grapheasy.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_sampler_specs_table")
data class DataSamplerSpec(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    var id: Long = -1L,

    @ColumnInfo(name = "name")
    val name: String = ""
)