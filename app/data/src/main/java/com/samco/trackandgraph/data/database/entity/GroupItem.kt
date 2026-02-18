/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents the placement of an item (Feature, Graph, Group, or Reminder) within a group.
 * This junction table enables the "symlinks" feature where the same item can appear in multiple groups.
 *
 * @property id Auto-generated primary key for this placement
 * @property groupId The group this item is displayed in. Null for reminders that exist only in the reminders screen.
 * @property displayIndex The position of this item within the group
 * @property childId The ID of the item in its own entity table
 * @property type The type of the child item (FEATURE, GRAPH, GROUP, REMINDER)
 * @property createdAt Epoch milliseconds when this placement was created. 0 indicates legacy data
 *                     migrated before timestamps were tracked. The placement with the lowest non-zero
 *                     value (or any if all are 0) is considered the original location.
 */
@Entity(
    tableName = "group_items_table",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("group_id"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["group_id"]),
        Index(value = ["child_id", "type"]),
        Index(value = ["group_id", "child_id", "type"], unique = true)
    ]
)
internal data class GroupItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long = 0,

    @ColumnInfo(name = "group_id")
    val groupId: Long?,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "child_id")
    val childId: Long,

    @ColumnInfo(name = "type")
    val type: GroupItemType,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0
)

enum class GroupItemType {
    FEATURE,
    GRAPH,
    GROUP,
    REMINDER
}
