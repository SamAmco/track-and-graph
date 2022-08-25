package com.samco.trackandgraph.base.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "functions_table",
    foreignKeys = [ForeignKey(
        entity = Group::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("group_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
//This is the only entity with the explicit name Entity because Function is too common of a name
// and causes naming conflicts with basic types
internal data class FunctionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "group_id", index = true)
    val groupId: Long,

    @ColumnInfo(name = "feature_description")
    val description: String,

    @ColumnInfo(name = "feature_ids")
    val featureIds: List<Long>,

    @ColumnInfo(name = "script")
    val script: String
)
