package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.runtime.Composable
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.graphstatview.factories.viewdto.*

@Composable
fun GraphStatView(
    graphStatViewData: IGraphStatViewData
) {
    when (graphStatViewData.graphOrStat.type) {
        GraphStatType.LINE_GRAPH ->
            LineGraphView(
                viewData = graphStatViewData as ILineGraphViewData,
                timeMarker = null,
                listMode = true
            )
        GraphStatType.PIE_CHART ->
            PieChartView(viewData = graphStatViewData as IPieChartViewData)
        GraphStatType.AVERAGE_TIME_BETWEEN ->
            AverageTimeBetweenView(viewData = graphStatViewData as IAverageTimeBetweenViewData)
        GraphStatType.LAST_VALUE ->
            LastValueStatView(viewData = graphStatViewData as ILastValueData)
        GraphStatType.TIME_HISTOGRAM ->
            TimeHistogramView(viewData = graphStatViewData as ITimeHistogramViewData)
    }
}