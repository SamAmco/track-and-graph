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
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.samco.trackandgraph.data.database.dto.DurationPlottingMode
import com.samco.trackandgraph.data.database.dto.LineGraphAveragingModes
import com.samco.trackandgraph.data.database.dto.LineGraphFeature
import com.samco.trackandgraph.data.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle

@Entity(
    tableName = "line_graph_features_table2",
    foreignKeys = [
        ForeignKey(
            entity = LineGraph::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("line_graph_id"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Feature::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("feature_id"),
            onDelete = ForeignKey.CASCADE
        )]
)
internal data class LineGraphFeature(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "line_graph_id", index = true)
    val lineGraphId: Long,

    @ColumnInfo(name = "feature_id", index = true)
    val featureId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color_index")
    val colorIndex: Int,

    @ColumnInfo(name = "averaging_mode")
    val averagingMode: LineGraphAveragingModes,

    @ColumnInfo(name = "plotting_mode")
    val plottingMode: LineGraphPlottingModes,

    @ColumnInfo(name = "point_style")
    val pointStyle: LineGraphPointStyle,

    @ColumnInfo(name = "offset")
    val offset: Double,

    @ColumnInfo(name = "scale")
    val scale: Double,

    @ColumnInfo(name = "duration_plotting_mode")
    val durationPlottingMode: DurationPlottingMode
) {
    fun toDto() = LineGraphFeature(
        id,
        lineGraphId,
        featureId,
        name,
        colorIndex,
        averagingMode,
        plottingMode,
        pointStyle,
        offset,
        scale,
        durationPlottingMode
    )
}
