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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IAverageTimeBetweenViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IBarChartViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILastValueViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITextViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeHistogramViewData
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
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
    DialogInputSpacing()
    when (graphStatViewData.state) {
        IGraphStatViewData.State.LOADING -> {
            Box(
                modifier = Modifier.heightIn(min = guessHeight(graphStatViewData)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        IGraphStatViewData.State.ERROR -> GraphError(modifier, graphStatViewData)

        else -> GraphStatInnerViewOrLuaGraph(
            modifier = modifier,
            graphStatViewData = graphStatViewData,
            listMode = listMode,
            timeMarker = timeMarker,
            graphHeight = graphHeight
        )
    }
}

@Composable
private fun GraphError(
    modifier: Modifier,
    graphStatViewData: IGraphStatViewData,
) {
    if (graphStatViewData.graphOrStat.type == GraphStatType.LUA_SCRIPT) {
        GraphErrorView(
            modifier = modifier,
            error = graphStatViewData.error?.message
                ?: stringResource(R.string.graph_stat_validation_unknown)
        )
    } else {
        GraphErrorView(
            modifier = modifier,
            error = (graphStatViewData.error as? GraphStatInitException)
                ?.errorTextId
                ?: R.string.graph_stat_validation_unknown
        )
    }
}

private fun guessHeight(graphStatViewData: IGraphStatViewData): Dp {
    return when (graphStatViewData.graphOrStat.type) {
        GraphStatType.AVERAGE_TIME_BETWEEN, GraphStatType.LAST_VALUE -> 125.dp
        GraphStatType.LINE_GRAPH,
        GraphStatType.PIE_CHART,
        GraphStatType.TIME_HISTOGRAM,
        GraphStatType.LUA_SCRIPT,
        GraphStatType.BAR_CHART -> 240.dp
    }
}

@Composable
private fun GraphStatInnerViewOrLuaGraph(
    modifier: Modifier,
    graphStatViewData: IGraphStatViewData,
    listMode: Boolean,
    timeMarker: OffsetDateTime? = null,
    graphHeight: Int? = null
) {
    var unwrapped = graphStatViewData

    if (graphStatViewData is ILuaGraphViewData) {
        if (!graphStatViewData.hasData) {
            GraphErrorView(
                modifier = modifier,
                error = R.string.graph_stat_view_not_enough_data_graph
            )
            return
        }
        unwrapped = graphStatViewData.wrapped ?: return
    }

    if (unwrapped is ITextViewData) {
        LuaTextView(textData = unwrapped)
        return
    }

    GraphStatInnerView(
        graphStatViewData = unwrapped,
        listMode = listMode,
        timeMarker = timeMarker,
        graphHeight = graphHeight
    )
}

@Composable
private fun GraphStatInnerView(
    modifier: Modifier = Modifier,
    graphStatViewData: IGraphStatViewData,
    listMode: Boolean,
    timeMarker: OffsetDateTime? = null,
    graphHeight: Int? = null
) = when (graphStatViewData.graphOrStat.type) {
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
            viewData = graphStatViewData as ILastValueViewData,
            listMode = listMode,
            graphHeight = graphHeight
        )

    GraphStatType.TIME_HISTOGRAM ->
        TimeHistogramView(
            modifier = modifier,
            viewData = graphStatViewData as ITimeHistogramViewData,
            graphHeight = graphHeight
        )

    GraphStatType.BAR_CHART ->
        BarChartView(
            modifier = modifier,
            viewData = graphStatViewData as IBarChartViewData,
            timeMarker = timeMarker,
            listMode = listMode,
            graphHeight = graphHeight
        )

    GraphStatType.LUA_SCRIPT -> {
        // This should never happen, unwrap the lua graph before calling this function
    }
}