package com.samco.trackandgraph.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_groups_table")
data class TrackGroup(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int
) {
    companion object {
        fun create(id: Long, name: String, displayIndex: Int): TrackGroup {
            val validName = name.take(MAX_TRACK_GROUP_NAME_LENGTH)
                .replace(splitChars1, " ")
                .replace(splitChars2, " ")
            return TrackGroup(id, validName, displayIndex)
        }
    }
}