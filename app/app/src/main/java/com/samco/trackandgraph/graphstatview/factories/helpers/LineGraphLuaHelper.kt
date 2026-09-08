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
package com.samco.trackandgraph.graphstatview.factories.helpers

import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.LineGraphPoint
import com.samco.trackandgraph.data.lua.dto.Line
import com.samco.trackandgraph.data.lua.dto.LinePointStyle
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject
import com.samco.trackandgraph.graphstatview.factories.viewdto.Line as LineViewData

class LineGraphLuaHelper @Inject constructor(
) {
    operator fun invoke(
        lineGraphData: LuaGraphResultData.LineGraphData,
        graphOrStat: GraphOrStat,
    ): ILuaGraphViewData? {
        val lines = lineGraphData.lines ?: return null

        val yRangeType =
            if (lineGraphData.yMax == null || lineGraphData.yMin == null) YRangeType.DYNAMIC
            else YRangeType.FIXED

        val endTime = lines
            .flatMap { it.linePoints }
            .maxOfOrNull { it.timestamp }
            ?: return null

        val lineViewData = getLineViewData(lines)

        return object : ILuaGraphViewData {
            override val wrapped: IGraphStatViewData = object : ILineGraphViewData {
                override val durationBasedRange: Boolean = lineGraphData.durationBasedRange
                override val yRangeType: YRangeType = yRangeType
                override val fixedYMin: Double? = lineGraphData.yMin
                override val fixedYMax: Double? = lineGraphData.yMax
                override val hasPlottableData: Boolean = lineViewData.any { it.points.size >= 2 }
                override val endTime: OffsetDateTime = endTime
                override val lines: List<LineViewData> = lineViewData
                override val state: IGraphStatViewData.State = IGraphStatViewData.State.READY
                override val graphOrStat: GraphOrStat = graphOrStat.copy(type = GraphStatType.LINE_GRAPH)
            }
            override val state: IGraphStatViewData.State = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat = graphOrStat
        }
    }

    private fun getLineViewData(lines: List<Line>): List<LineViewData> = lines
        .mapIndexed { index, line ->
            val pointsAscending = line.linePoints.sortedBy { it.timestamp }
            LineViewData(
                name = line.label ?: "",
                color = (line.lineColor ?: indexColorSpec(index)).toColorSpec(),
                pointStyle = line.pointStyle.toViewPointStyle(),
                points = pointsAscending.map { LineGraphPoint(it.timestamp, it.value) },
            )
        }

    private fun LinePointStyle?.toViewPointStyle() = when (this) {
        LinePointStyle.NONE -> LineGraphPointStyle.NONE
        LinePointStyle.CIRCLE -> LineGraphPointStyle.CIRCLES
        LinePointStyle.CIRCLE_VALUE -> LineGraphPointStyle.CIRCLES_AND_NUMBERS
        LinePointStyle.CIRCLES_ONLY -> LineGraphPointStyle.CIRCLES_ONLY
        null -> LineGraphPointStyle.NONE
    }
}
