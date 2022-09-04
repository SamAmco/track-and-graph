package com.samco.trackandgraph.base.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "functions_table",
    foreignKeys = [ForeignKey(
        entity = Feature::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("feature_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
//This is the only entity with the explicit name Entity because Function is too common of a name
// and causes naming conflicts with basic types
internal data class FunctionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "feature_id", index = true)
    val featureId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "data_sources")
    val dataSources: List<Feature>,

    @ColumnInfo(name = "script")
    val script: String
) {
    fun toDto() = com.samco.trackandgraph.base.database.dto.FunctionDto(
        id,
        name,
        dataSources.map { it.toDto() },
        script
    )
}
