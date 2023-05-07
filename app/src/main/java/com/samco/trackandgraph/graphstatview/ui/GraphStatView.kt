package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall
import org.threeten.bp.OffsetDateTime

@Composable
fun GraphStatView(
    modifier: Modifier = Modifier,
    graphStatViewData: IGraphStatViewData,
    listMode: Boolean,
    timeMarker: OffsetDateTime? = null,
    graphHeight: Int? = null
) = Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(
        text = graphStatViewData.graphOrStat.name,
        style = MaterialTheme.typography.h6,
        textAlign = TextAlign.Center
    )
    SpacingSmall()
    if (graphStatViewData.state == IGraphStatViewData.State.LOADING) {
        CircularProgressIndicator()
    } else {
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
}