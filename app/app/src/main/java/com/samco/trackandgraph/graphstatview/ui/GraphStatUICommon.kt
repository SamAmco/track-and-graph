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
package com.samco.trackandgraph.graphstatview.ui

import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import com.samco.trackandgraph.ui.ui.ColorCircle
import com.samco.trackandgraph.ui.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.dataVisColorList

private const val GRAPH_HEIGHT_WITH_LEGEND_MULTIPLIER = 0.8f
private const val GRAPH_HEIGHT_WITHOUT_LEGEND_MULTIPLIER = 0.9f

internal const val graphGridLineAlpha = 0.25f
internal const val graphXAxisLabelAngle = -28f
internal val graphGridLineThickness = 0.5.dp
internal val graphAxisTextStyle: TextStyle
    @Composable get() = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp,
    )

fun setGraphHeight(
    graphView: View,
    graphViewMode: GraphViewMode,
    hasLegend: Boolean,
) {
    if (graphViewMode is GraphViewMode.FullScreenMode) {
        val multiplier = graphHeightMultiplier(hasLegend)
        graphView.layoutParams.height = (graphViewMode.availableHeight * multiplier).toInt()
    } else {
        graphView.layoutParams.height = graphView.context.resources.getDimensionPixelSize(R.dimen.graph_height)
    }
}

@Composable
fun graphHeightFor(
    graphViewMode: GraphViewMode,
    hasLegend: Boolean,
): Dp = when (graphViewMode) {
    GraphViewMode.ListMode -> dimensionResource(R.dimen.graph_height)
    is GraphViewMode.FullScreenMode -> with(LocalDensity.current) {
        (graphViewMode.availableHeight * graphHeightMultiplier(hasLegend)).toDp()
    }
}

private fun graphHeightMultiplier(hasLegend: Boolean) =
    if (hasLegend) {
        GRAPH_HEIGHT_WITH_LEGEND_MULTIPLIER
    } else {
        GRAPH_HEIGHT_WITHOUT_LEGEND_MULTIPLIER
    }

@Composable
fun GraphErrorView(
    modifier: Modifier = Modifier,
    @StringRes error: Int,
) = GraphErrorView(
    modifier = modifier,
    error = stringResource(error)
)

@Composable
fun GraphErrorView(
    modifier: Modifier = Modifier,
    error: String,
) = Column(modifier = modifier) {
    Text(
        modifier = modifier
            .padding(vertical = inputSpacingLarge),
        text = error,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}

data class GraphLegendItem(
    val color: Color,
    val label: String
)

@ColorInt
fun getColorInt(
    colorSpec: ColorSpec,
) = when (colorSpec) {
    is ColorSpec.ColorIndex -> dataVisColorList[colorSpec.index].toArgb()
    is ColorSpec.ColorValue -> colorSpec.value
}

fun getColor(colorSpec: ColorSpec) = Color(getColorInt(colorSpec))

internal val graphLegendCircleSize = 20.dp
private val graphLegendTextStyle @Composable get() = MaterialTheme.typography.bodyMedium

@Composable
fun GraphLegend(
    modifier: Modifier = Modifier,
    items: List<GraphLegendItem>
) = FlowRow(
    modifier = modifier
        .fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
) {
    items.forEach {
        GraphLegendItemView(item = it)
        DialogInputSpacing()
    }
}

@Composable
fun GraphLegendItemView(
    modifier: Modifier = Modifier,
    item: GraphLegendItem
) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
) {

    ColorCircle(
        color = item.color,
        size = graphLegendCircleSize,
    )

    Spacer(modifier = Modifier.width(2.dp))

    Text(
        text = item.label,
        style = graphLegendTextStyle
    )
}
