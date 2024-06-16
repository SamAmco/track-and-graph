package com.samco.trackandgraph.graphstatview.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.androidplot.Plot
import com.androidplot.ui.Anchor
import com.androidplot.ui.HorizontalPositioning
import com.androidplot.ui.VerticalPositioning
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYGraphWidget
import com.androidplot.xy.XYPlot
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.helpers.formatTimeDuration
import com.samco.trackandgraph.databinding.GraphXyPlotBinding
import com.samco.trackandgraph.ui.compose.ui.ColorCircle
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.util.getColorFromAttr
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

    xyPlot.graph.gridBackgroundPaint.color = Color.TRANSPARENT
    xyPlot.backgroundPaint.color = Color.TRANSPARENT
    xyPlot.graph.backgroundPaint.color = Color.TRANSPARENT

    xyPlot.graph.paddingLeft = 0f
    xyPlot.graph.paddingBottom = 0f
    xyPlot.setRangeBoundaries(0, 1, BoundaryMode.AUTO)
    xyPlot.setDomainBoundaries(0, 1, BoundaryMode.GROW)
    xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).format = DecimalFormat("0.0")
}

fun setUpXYPlotYAxis(
    binding: GraphXyPlotBinding,
    yAxisRangeParameters: Pair<StepMode, Double>,
    durationBasedRange: Boolean
) {
    binding.xyPlot.setRangeStep(
        yAxisRangeParameters.first,
        yAxisRangeParameters.second
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

@Composable
fun GraphErrorView(
    modifier: Modifier = Modifier,
    @StringRes error: Int
) = Column(modifier = modifier) {
    Text(
        modifier = modifier
            .padding(vertical = dimensionResource(id = R.dimen.input_spacing_large)),
        text = stringResource(error),
        style = MaterialTheme.typography.body1,
        textAlign = TextAlign.Center
    )
}

data class GraphLegendItem(
    @ColorRes val color: Int,
    val label: String
)

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
        GraphLegentItemView(item = it)
        DialogInputSpacing()
    }
}

@Composable
fun GraphLegentItemView(
    modifier: Modifier = Modifier,
    item: GraphLegendItem
) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
) {

    ColorCircle(
        color = item.color,
        size = 20.dp
    )

    Spacer(modifier = Modifier.width(2.dp))

    Text(
        text = item.label,
        style = MaterialTheme.typography.body2
    )
}
