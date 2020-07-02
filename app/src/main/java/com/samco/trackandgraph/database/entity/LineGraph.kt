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

package com.samco.trackandgraph.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.samco.trackandgraph.database.dto.YRangeType
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

@Entity(
    tableName = "line_graphs_table3",
    foreignKeys = [ForeignKey(
        entity = GraphOrStat::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("graph_stat_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class LineGraph(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "graph_stat_id", index = true)
    val graphStatId: Long,

    @ColumnInfo(name = "duration")
    val duration: Duration?,

    @ColumnInfo(name = "y_range_type")
    val yRangeType: YRangeType,

    @ColumnInfo(name = "y_from")
    val yFrom: Double,

    @ColumnInfo(name = "y_to")
    val yTo: Double,

    @ColumnInfo(name = "end_date")
    val endDate: OffsetDateTime?
) {
    companion object {
        fun create(
            id: Long, graphStatId: Long, duration: Duration?,
            yRangeType: YRangeType, yFrom: Double, yTo: Double, endDate: OffsetDateTime?
        ): LineGraph {
            return LineGraph(
                id,
                graphStatId,
                duration,
                yRangeType,
                yFrom,
                yTo,
                endDate
            )
        }
    }
}
