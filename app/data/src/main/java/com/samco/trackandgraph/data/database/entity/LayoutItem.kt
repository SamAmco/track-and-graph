/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.samco.trackandgraph.data.database.dto.LayoutItem
import com.samco.trackandgraph.data.database.dto.LayoutItemType

@Entity(
    tableName = "layout_items_table",
    indices = [
        Index(value = ["group_id"]),
        Index(value = ["item_id", "type"], unique = true)
    ]
)
internal data class LayoutItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "type")
    val type: LayoutItemType,

    @ColumnInfo(name = "item_id")
    val itemId: Long,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int
) {
    fun toDto() = LayoutItem(
        id = id,
        groupId = groupId,
        type = type,
        itemId = itemId,
        displayIndex = displayIndex
    )
}
