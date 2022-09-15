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
package com.samco.trackandgraph.base.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.database.dto.FeatureDtoImpl

@Entity(
    tableName = "features_table",
    foreignKeys = [ForeignKey(
        entity = Group::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("group_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
internal data class Feature(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "group_id", index = true)
    val groupId: Long,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "feature_description")
    val description: String
) {
    fun toDto(): Feature = FeatureDtoImpl(
        id = this@Feature.id,
        name = this@Feature.name,
        groupId = this@Feature.groupId,
        displayIndex = this@Feature.displayIndex,
        description = this@Feature.description
    )
}


