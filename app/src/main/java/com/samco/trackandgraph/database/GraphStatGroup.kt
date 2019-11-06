package com.samco.trackandgraph.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "graph_stat_groups_table")
data class GraphStatGroup(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int
) {
    companion object {
        fun create(id: Long, name: String, displayIndex: Int): GraphStatGroup {
            val validName = name.take(MAX_GRAPH_STAT_GROUP_NAME_LENGTH)
            return GraphStatGroup(id, validName, displayIndex)
        }
    }
}
