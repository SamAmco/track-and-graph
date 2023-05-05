package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import org.threeten.bp.OffsetDateTime

@Composable
fun GraphStatView(
    modifier: Modifier = Modifier,
    graphStatViewData: IGraphStatViewData,
    listMode: Boolean,
    timeMarker: OffsetDateTime? = null,
    graphHeight: Int? = null
) {
    when (graphStatViewData.graphOrStat.type) {
        GraphStatType.LINE_GRAPH ->
            LineGraphView(
                modifier = modifier,
                viewData = graphStatViewData as ILineGraphViewData,
                timeMarker = timeMarker,
                listMode = listMode,
                graphHeight = graphHeight
            )
        GraphStatType.PIE_CHART ->
            PieChartView(
                modifier = modifier,
                viewData = graphStatViewData as IPieChartViewData,
                graphHeight = graphHeight
            )
        GraphStatType.AVERAGE_TIME_BETWEEN ->
            AverageTimeBetweenView(
                modifier = modifier,
                viewData = graphStatViewData as IAverageTimeBetweenViewData,
                graphHeight = graphHeight
            )
        GraphStatType.LAST_VALUE ->
            LastValueStatView(
                modifier = modifier,
                viewData = graphStatViewData as ILastValueData,
                listMode = listMode,
                graphHeight = graphHeight
            )
        GraphStatType.TIME_HISTOGRAM ->
            TimeHistogramView(
                modifier = modifier,
                viewData = graphStatViewData as ITimeHistogramViewData,
                graphHeight = graphHeight
            )
    }
}