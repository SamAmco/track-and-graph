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
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.PieChartSegment
import javax.inject.Inject

class PieChartLuaHelper @Inject constructor() {
    operator fun invoke(
        pieChartData: LuaGraphResultData.PieChartData,
        graphOrStat: GraphOrStat,
    ): ILuaGraphViewData = object : ILuaGraphViewData {
        override val wrapped: IGraphStatViewData = object : IPieChartViewData {
            override val segments: List<IPieChartViewData.Segment>? =
                pieChartData.segments?.let { segments ->
                    val total = segments.sumOf { segment -> segment.value }
                    val scale = 100 / total
                    return@let segments.map { it.toPieChartSegment(scale) }
                }
            override val state: IGraphStatViewData.State = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat = graphOrStat.copy(type = GraphStatType.PIE_CHART)
        }
        override val state: IGraphStatViewData.State = IGraphStatViewData.State.READY
        override val graphOrStat: GraphOrStat = graphOrStat
    }

    private fun PieChartSegment.toPieChartSegment(scale: Double): IPieChartViewData.Segment = IPieChartViewData.Segment(
        title = label,
        value = value * scale,
        color = color?.toColorSpec()
    )
}