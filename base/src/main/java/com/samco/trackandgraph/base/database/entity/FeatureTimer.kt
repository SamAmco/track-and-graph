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

package com.samco.trackandgraph.base.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.threeten.bp.Instant

@Entity(
    tableName = "feature_timers_table",
    foreignKeys = [ForeignKey(
        entity = Feature::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("feature_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
internal data class FeatureTimer(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    //TODO should add index = true here but not sure if that requires a migration
    @ColumnInfo(name = "feature_id")
    val featureId: Long,

    @ColumnInfo(name = "start_instant", index = true)
    val startInstant: Instant
)