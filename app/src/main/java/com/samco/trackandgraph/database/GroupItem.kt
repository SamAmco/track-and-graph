package com.samco.trackandgraph.database

import androidx.room.ColumnInfo

enum class GroupItemType { TRACK, GRAPH }

data class GroupItem(
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "type")
    val type: GroupItemType
)
