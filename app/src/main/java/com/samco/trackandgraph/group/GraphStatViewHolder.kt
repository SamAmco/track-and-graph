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

package com.samco.trackandgraph.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.graphstatview.factories.viewdto.*
import com.samco.trackandgraph.graphstatview.ui.*
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme

class GraphStatViewHolder(
    private val composeView: ComposeView,
) : GroupChildViewHolder(composeView) {

    private var isElevated by mutableStateOf(false)

    private lateinit var graphStatViewData: IGraphStatViewData
    private lateinit var clickListener: GraphStatClickListener

    fun bind(
        graphStat: IGraphStatViewData,
        clickListener: GraphStatClickListener
    ) {
        this.graphStatViewData = graphStat
        this.clickListener = clickListener

        composeView.setContent {
            TnGComposeTheme {
                GraphStatCard()
            }
        }
    }

    @Composable
    private fun GraphStatCard() = Card(
        elevation = if (isElevated)
            dimensionResource(id = R.dimen.card_elevation) * 3f
        else dimensionResource(R.dimen.card_elevation)
    ) {
        //TODO add a try catch around the graph views and show an error message if it fails
        Box(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.card_padding))
        ) {
            //TODO add menu button
            Column {
                Text(
                    text = graphStatViewData.graphOrStat.name,
                    style = MaterialTheme.typography.h6
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (graphStatViewData.state == IGraphStatViewData.State.LOADING) {
                        //TODO add loading indicator
                    } else when (graphStatViewData.graphOrStat.type) {
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
            }
        }
    }

    override fun elevateCard() {
        isElevated = true
    }

    override fun dropCard() {
        isElevated = false
    }
}

class GraphStatClickListener(
    private val onDelete: (graphStat: IGraphStatViewData) -> Unit,
    private val onEdit: (graphStat: IGraphStatViewData) -> Unit,
    private val onClick: (graphStat: IGraphStatViewData) -> Unit,
    private val onMoveGraphStat: (graphStat: IGraphStatViewData) -> Unit,
    private val onDuplicateGraphStat: (graphStat: IGraphStatViewData) -> Unit
) {
    fun onDelete(graphStat: IGraphStatViewData) = onDelete.invoke(graphStat)
    fun onEdit(graphStat: IGraphStatViewData) = onEdit.invoke(graphStat)
    fun onClick(graphStat: IGraphStatViewData) = onClick.invoke(graphStat)
    fun onMoveGraphStat(graphStat: IGraphStatViewData) = onMoveGraphStat.invoke(graphStat)
    fun onDuplicate(graphStat: IGraphStatViewData) = onDuplicateGraphStat.invoke(graphStat)
}
