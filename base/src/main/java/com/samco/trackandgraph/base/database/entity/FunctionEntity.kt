package com.samco.trackandgraph.base.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

internal enum class DataSourceType {
    FEATURE, FUNCTION;

    fun toDto() = when (this) {
        FEATURE -> com.samco.trackandgraph.base.database.dto.DataSourceType.FEATURE
        FUNCTION -> com.samco.trackandgraph.base.database.dto.DataSourceType.FUNCTION
    }
}

@JsonClass(generateAdapter = true)
internal data class DataSourceDescriptor(
    val name: String,
    val type: DataSourceType,
    val id: Long,
    val groupId: Long
) {
    fun toDto() = com.samco.trackandgraph.base.database.dto.DataSourceDescriptor(
        name, type.toDto(), id, groupId
    )
}

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

    @ColumnInfo(name = "function_description")
    val description: String,

    @ColumnInfo(name = "data_sources")
    val dataSources: List<DataSourceDescriptor>,

    @ColumnInfo(name = "script")
    val script: String
) {
    fun toDto() = com.samco.trackandgraph.base.database.dto.FunctionDto(
        id,
        name,
        groupId,
        description,
        dataSources.map { it.toDto() },
        script
    )
}
