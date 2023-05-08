/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
            GraphStatType.BAR_CHART ->
                BarChartView(
                    viewData = graphStatViewData as IBarChartData,
                    timeMarker = timeMarker,
                    listMode = listMode,
                    graphHeight = graphHeight
                )
        }
    }
}