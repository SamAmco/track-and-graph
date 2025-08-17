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

import android.content.Context
import android.util.TypedValue
import android.graphics.Color as GColor
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getColor
import com.androidplot.Plot
import com.androidplot.ui.Anchor
import com.androidplot.ui.HorizontalPositioning
import com.androidplot.ui.VerticalPositioning
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYGraphWidget
import com.androidplot.xy.XYPlot
import com.samco.trackandgraph.R
import com.samco.trackandgraph.helpers.formatTimeDuration
import com.samco.trackandgraph.databinding.GraphXyPlotBinding
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import com.samco.trackandgraph.ui.compose.ui.ColorCircle
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.dataVisColorList
import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import kotlin.math.roundToLong

fun xyPlotSetup(
    context: Context,
    xyPlot: XYPlot
) {
    xyPlot.layoutManager.remove(xyPlot.legend)
    xyPlot.layoutManager.remove(xyPlot.rangeTitle)
    xyPlot.layoutManager.remove(xyPlot.title)
    xyPlot.domainTitle.position(
        0f,
        HorizontalPositioning.ABSOLUTE_FROM_CENTER,
        0f,
        VerticalPositioning.ABSOLUTE_FROM_BOTTOM,
        Anchor.BOTTOM_MIDDLE
    )
    xyPlot.setBorderStyle(Plot.BorderStyle.NONE, null, null)
    xyPlot.graph.position(
        0f,
        HorizontalPositioning.ABSOLUTE_FROM_LEFT,
        0f,
        VerticalPositioning.ABSOLUTE_FROM_TOP
    )
    xyPlot.setPlotMargins(0f, 0f, 0f, 0f)
    xyPlot.setPlotPadding(0f, 0f, 0f, 0f)
    xyPlot.graph.setPadding(0f, 0f, 0f, 10f)
    xyPlot.graph.setMargins(0f, 20f, 0f, 0f)

    val colorOnSurface = context.getColorFromAttr(R.attr.colorOnSurface)

    xyPlot.domainTitle.labelPaint.color = colorOnSurface
    xyPlot.graph.domainGridLinePaint.color = colorOnSurface
    xyPlot.graph.rangeGridLinePaint.color = colorOnSurface
    xyPlot.graph.domainSubGridLinePaint.color = colorOnSurface
    xyPlot.graph.rangeSubGridLinePaint.color = colorOnSurface
    xyPlot.graph.domainOriginLinePaint.color = colorOnSurface
    xyPlot.graph.domainOriginLinePaint.strokeWidth = 1f
    xyPlot.graph.rangeOriginLinePaint.color = colorOnSurface
    xyPlot.graph.rangeOriginLinePaint.strokeWidth = 1f

    //Setting the layer type enables transparent backgrounds, I don't know why
    // I discovered it here: https://groups.google.com/g/androidplot/c/5QnJXD0uIIU
    xyPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

    xyPlot.graph.gridBackgroundPaint.color = GColor.TRANSPARENT
    xyPlot.backgroundPaint.color = GColor.TRANSPARENT
    xyPlot.graph.backgroundPaint.color = GColor.TRANSPARENT

    xyPlot.graph.paddingLeft = 0f
    xyPlot.graph.paddingBottom = 0f
    xyPlot.setRangeBoundaries(0, 1, BoundaryMode.AUTO)
    xyPlot.setDomainBoundaries(0, 1, BoundaryMode.GROW)
    xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).format = DecimalFormat("0.0")
}

fun setUpXYPlotYAxis(
    binding: GraphXyPlotBinding,
    yAxisSubdivides: Int,
    durationBasedRange: Boolean
) {
    binding.xyPlot.setRangeStep(
        StepMode.SUBDIVIDE,
        yAxisSubdivides.toDouble(),
    )
    if (durationBasedRange) {
        binding.xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).format =
            object : Format() {
                override fun format(
                    obj: Any,
                    toAppendTo: StringBuffer,
                    pos: FieldPosition
                ): StringBuffer {
                    val sec = (obj as Number).toDouble().roundToLong()
                    return toAppendTo.append(formatTimeDuration(sec))
                }

                override fun parseObject(source: String, pos: ParsePosition) = null
            }
    }
}

fun setGraphHeight(
    graphView: View,
    graphViewMode: GraphViewMode,
    hasLegend: Boolean,
) {
    if (graphViewMode is GraphViewMode.FullScreenMode) {
        val multiplier = if (hasLegend) 0.85 else 0.9
        graphView.layoutParams.height = (graphViewMode.availableHeight * multiplier).toInt()
    } else {
        graphView.layoutParams.height = graphView.context.resources.getDimensionPixelSize(R.dimen.graph_height)
    }
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
    context: Context,
    colorSpec: ColorSpec,
) = when (colorSpec) {
    is ColorSpec.ColorIndex -> getColor(context, dataVisColorList[colorSpec.index])
    is ColorSpec.ColorValue -> colorSpec.value
}

fun getColor(
    context: Context,
    colorSpec: ColorSpec,
) = Color(getColorInt(context, colorSpec))

private val graphLegendCircleSize = 20.dp
private val graphLegendTextStyle @Composable get() = MaterialTheme.typography.bodyMedium

@Composable
fun legendItemLineHeight(): Int {
    val density = LocalDensity.current
    val typography = graphLegendTextStyle

    return remember(density, typography) {
        val body2LineHeight = with(density) {
            typography.lineHeight.toPx()
        }
        val circleSize = with(density) {
            graphLegendCircleSize.toPx()
        }

        maxOf(body2LineHeight, circleSize).toInt()
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}