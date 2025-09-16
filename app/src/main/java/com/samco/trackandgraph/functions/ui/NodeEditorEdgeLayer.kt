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
package com.samco.trackandgraph.functions.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.functions.Edge
import kotlinx.coroutines.flow.StateFlow

internal data class Cubic(val p0: Offset, val c1: Offset, val c2: Offset, val p1: Offset)

/**
 * Manages edge rendering state and cubic Bézier curve computation.
 * Handles edge connections between nodes with caching for performance.
 */
@Stable
internal class EdgeLayerState(
    val edges: State<List<Edge>>,
    val selectedEdge: State<Edge?>,
    private val connectorState: ConnectorLayerState,
) {
    // Derived state that recomputes cubics only when edges change
    val cubics: Map<Edge, Pair<Cubic, List<Offset>>?> by derivedStateOf {
        edges.value.associateBy({ it }) { e ->
            val from = connectorState.worldPosOf(e.from) ?: return@associateBy null
            val to = connectorState.worldPosOf(e.to) ?: return@associateBy null
            val cubic = cubicWithHorizontalTangents(from, to)
            val poly = sampleCubicAsPolyline(cubic) // List<Offset> in WORLD units
            cubic to poly
        }
    }

    val draggingEdgeOverlay: Pair<Offset, Offset>? by derivedStateOf {
        val startPos = connectorState.draggingConnectorId.value
            ?.let { connectorState.worldPosOf(it) }
        val endPos = connectorState.draggingConnectorWorldPosition.value
        if (startPos == null || endPos == null) return@derivedStateOf null
        return@derivedStateOf startPos to endPos
    }
}

@Composable
internal fun rememberEdgeLayerState(
    edges: StateFlow<List<Edge>>,
    selectedEdge: StateFlow<Edge?>,
    connectorState: ConnectorLayerState
): EdgeLayerState {
    val edgesAsState = edges.collectAsStateWithLifecycle()
    val selectedEdgeAsState = selectedEdge.collectAsStateWithLifecycle()
    return remember {
        EdgeLayerState(
            edges = edgesAsState,
            selectedEdge = selectedEdgeAsState,
            connectorState = connectorState,
        )
    }
}

@Composable
internal fun EdgeLayer(
    edgeLayerState: EdgeLayerState,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedColor = MaterialTheme.colorScheme.primary
    val edgeWidth = 3.dp
    val selectedEdgeWidth = 6.dp
    val arrowHeadSize = 14.dp

    Canvas(
        modifier
            .fillMaxSize()
            .zIndex(1f)
    ) {
        edgeLayerState.edges.value.forEach { edge ->
            val (cubic, _) = edgeLayerState.cubics[edge] ?: return@forEach

            // Build a Path for drawing
            val path = Path().apply {
                moveTo(cubic.p0.x, cubic.p0.y)
                cubicTo(
                    cubic.c1.x, cubic.c1.y,
                    cubic.c2.x, cubic.c2.y,
                    cubic.p1.x, cubic.p1.y
                )
            }

            val isSelected = edge == edgeLayerState.selectedEdge.value
            val strokeWidth = (if (isSelected) selectedEdgeWidth else edgeWidth).toPx()
            val currentColor = if (isSelected) selectedColor else color

            drawPath(
                path = path,
                color = currentColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw arrow head at the end of the edge
            drawArrowHead(
                cubic = cubic,
                color = currentColor,
                size = arrowHeadSize.toPx(),
                strokeWidth = strokeWidth
            )
        }

        edgeLayerState.draggingEdgeOverlay?.let { (start, end) ->
            val cubic = cubicWithHorizontalTangents(start, end)
            val path = Path().apply {
                moveTo(start.x, start.y)
                cubicTo(
                    cubic.c1.x, cubic.c1.y,
                    cubic.c2.x, cubic.c2.y,
                    cubic.p1.x, cubic.p1.y
                )
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = edgeWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw arrow head for dragging edge
            drawArrowHead(
                cubic = cubic,
                color = color,
                size = arrowHeadSize.toPx(),
                strokeWidth = edgeWidth.toPx()
            )
        }
    }
}

/**
 * Horizontal tangents at both ends:
 *   c1 = (p0.x + h0, p0.y)
 *   c2 = (p1.x - h1, p1.y)
 *
 * For forward edges (to.x >= from.x), h ~ 0.35*dx.
 * For backward edges (to.x < from.x), we keep h based on |dx| so the curve
 * exits to the right and re-enters from the left (loop-back).
 * We also enforce min/max handles for aesthetics.
 */
internal fun cubicWithHorizontalTangents(
    from: Offset,
    to: Offset,
    k: Float = 0.35f,
    minHandle: Float = 40f,
    maxHandle: Float = 600f
): Cubic {
    val dx = to.x - from.x
    val hBase = (k * kotlin.math.abs(dx)).coerceIn(minHandle, maxHandle)
    val c1 = Offset(from.x + hBase, from.y)
    val c2 = Offset(to.x - hBase, to.y)
    return Cubic(from, c1, c2, to)
}

internal fun sampleCubicAsPolyline(c: Cubic, maxSegLen: Float = 40f): List<Offset> {
    // Choose segment count based on curve "size"
    val approxLen = (c.p1 - c.p0).getDistance() + (c.c1 - c.p0).getDistance() + (c.c2 - c.p1).getDistance()
    val segments = (approxLen / maxSegLen).coerceIn(12f, 64f).toInt()

    return (0..segments).map { i ->
        val t = i.toFloat() / segments
        cubicPoint(c, t)
    }
}

private fun cubicPoint(c: Cubic, t: Float): Offset {
    val u = 1f - t
    val uu = u * u
    val tt = t * t
    val uuu = uu * u
    val ttt = tt * t
    // Bernstein basis
    val x = uuu * c.p0.x + 3f * uu * t * c.c1.x + 3f * u * tt * c.c2.x + ttt * c.p1.x
    val y = uuu * c.p0.y + 3f * uu * t * c.c1.y + 3f * u * tt * c.c2.y + ttt * c.p1.y
    return Offset(x, y)
}

/**
 * Draws an arrow head at the end of a cubic Bézier curve using two converging lines.
 * Simplified for left-to-right horizontal edges - arrows always point right.
 */
private fun DrawScope.drawArrowHead(
    cubic: Cubic,
    color: Color,
    size: Float,
    strokeWidth: Float,
    arrowAngle: Float = 0.5f // Angle of arrow head in radians (about 30 degrees)
) {
    // Arrow tip is at the visual end of the line (accounting for rounded cap)
    // Since edges always go left-to-right, arrow always points right
    val arrowTipPoint = Offset(
        cubic.p1.x + strokeWidth / 2f,
        cubic.p1.y
    )

    // Calculate the two arrow line endpoints for a rightward-pointing arrow
    val cosAngle = cos(arrowAngle)
    val sinAngle = sin(arrowAngle)

    val leftPoint = Offset(
        arrowTipPoint.x - size * cosAngle,
        arrowTipPoint.y - size * sinAngle
    )

    val rightPoint = Offset(
        arrowTipPoint.x - size * cosAngle,
        arrowTipPoint.y + size * sinAngle
    )

    // Draw two lines that converge at the arrow tip
    drawLine(
        color = color,
        start = leftPoint,
        end = arrowTipPoint,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color,
        start = rightPoint,
        end = arrowTipPoint,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

