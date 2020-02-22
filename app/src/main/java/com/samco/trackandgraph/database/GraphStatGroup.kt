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
            val validName = name.take(MAX_GROUP_NAME_LENGTH)
            return GraphStatGroup(id, validName, displayIndex)
        }
    }
}
