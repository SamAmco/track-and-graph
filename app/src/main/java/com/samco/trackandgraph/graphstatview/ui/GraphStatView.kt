package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
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
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(
        text = graphStatViewData.graphOrStat.name,
        style = MaterialTheme.typography.h6,
        textAlign = TextAlign.Center
    )
    SpacingSmall()
    if (graphStatViewData.state == IGraphStatViewData.State.LOADING) {
        Box(
            modifier = Modifier.heightIn(min = 160.dp),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
    } else {
        when (graphStatViewData.graphOrStat.type) {
            GraphStatType.LINE_GRAPH ->
                LineGraphView(
                    viewData = graphStatViewData as ILineGraphViewData,
                    timeMarker = timeMarker,
                    listMode = listMode,
                    graphHeight = graphHeight
                )
            GraphStatType.PIE_CHART ->
                PieChartView(
                    viewData = graphStatViewData as IPieChartViewData,
                    graphHeight = graphHeight
                )
            GraphStatType.AVERAGE_TIME_BETWEEN ->
                AverageTimeBetweenView(
                    viewData = graphStatViewData as IAverageTimeBetweenViewData,
                    graphHeight = graphHeight
                )
            GraphStatType.LAST_VALUE ->
                LastValueStatView(
                    viewData = graphStatViewData as ILastValueData,
                    listMode = listMode,
                    graphHeight = graphHeight
                )
            GraphStatType.TIME_HISTOGRAM ->
                TimeHistogramView(
                    viewData = graphStatViewData as ITimeHistogramViewData,
                    graphHeight = graphHeight
                )
        }
    }
}