package com.samco.trackandgraph.graphstatview.factories.helpers

import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.PieChartSegment
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