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

import com.androidplot.xy.RectRegion
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.lua.dto.Line
import com.samco.trackandgraph.lua.dto.LinePointStyle
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject
import com.samco.trackandgraph.graphstatview.factories.viewdto.Line as LineViewData

class LineGraphLuaHelper @Inject constructor(
    private val androidPlotSeriesHelper: AndroidPlotSeriesHelper,
    private val dataDisplayIntervalHelper: DataDisplayIntervalHelper,
) {
    operator fun invoke(
        lineGraphData: LuaGraphResultData.LineGraphData,
        graphOrStat: GraphOrStat,
    ): ILuaGraphViewData? {
        val lines = lineGraphData.lines ?: return null

        val yRangeType =
            if (lineGraphData.yMax == null || lineGraphData.yMin == null) YRangeType.DYNAMIC
            else YRangeType.FIXED

        val endTime = lineGraphData.lines
            .mapNotNull { it.linePoints.firstOrNull() }
            .maxOfOrNull { it.timestamp }
            ?: return null

        val lineViewData = getLineViewData(lines, endTime)

        val bounds = RectRegion()
        lineViewData.forEach { line -> line.line?.let { bounds.union(it.minMax()) } }

        val yMin = lineGraphData.yMin ?: bounds.minY
        val yMax = lineGraphData.yMax ?: bounds.maxY

        bounds.minY = yMin
        bounds.maxY = yMax

        val parameters = dataDisplayIntervalHelper.getYParameters(
            yMin = yMin.toDouble(),
            yMax = yMax.toDouble(),
            isDurationBasedRange = lineGraphData.durationBasedRange,
            fixedBounds = yRangeType == YRangeType.FIXED
        )

        return object : ILuaGraphViewData {
            override val wrapped: IGraphStatViewData = object : ILineGraphViewData {
                override val durationBasedRange: Boolean = lineGraphData.durationBasedRange
                override val yRangeType: YRangeType = yRangeType
                override val bounds: RectRegion = bounds
                override val hasPlottableData: Boolean = true
                override val endTime: OffsetDateTime = endTime
                override val lines: List<LineViewData> = lineViewData
                override val yAxisSubdivides: Int = parameters.subdivides
                override val state: IGraphStatViewData.State = IGraphStatViewData.State.READY
                override val graphOrStat: GraphOrStat = graphOrStat.copy(type = GraphStatType.LINE_GRAPH)
            }
            override val state: IGraphStatViewData.State = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat = graphOrStat
        }
    }

    private fun getLineViewData(lines: List<Line>, endTime: OffsetDateTime): List<LineViewData> = lines
        .mapIndexed { index, line ->
            val pointsReversed = line.linePoints.asReversed()
            LineViewData(
                name = line.label ?: "",
                color = (line.lineColor ?: indexColorSpec(index)).toColorSpec(),
                pointStyle = line.pointStyle.toViewPointStyle(),
                line = androidPlotSeriesHelper.getFastXYSeries(
                    name = line.label ?: "",
                    xValues = pointsReversed.map { Duration.between(endTime, it.timestamp).toMillis() },
                    yValues = pointsReversed.map { it.value },
                )
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