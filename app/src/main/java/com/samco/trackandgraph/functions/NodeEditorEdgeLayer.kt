package com.samco.trackandgraph.functions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

// ============================================================================
// EDGE LAYER STATE - Handles edge cubic computation and caching
// ============================================================================

internal data class Edge(
    val from: ConnectorId,
    val to: ConnectorId,
)

internal data class Cubic(val p0: Offset, val c1: Offset, val c2: Offset, val p1: Offset)

@Stable
internal class EdgeLayerState(
    val edges: List<Edge>,
    private val connectorState: ConnectorLayerState,
) {
    // Derived state that recomputes cubics only when edges change
    val cubics: Map<Edge, Pair<Cubic, List<Offset>>?> by derivedStateOf {
        edges.associateBy({ it }) { e ->
            val from = connectorState.connectorStates[e.from] ?: return@associateBy null
            val to = connectorState.connectorStates[e.to] ?: return@associateBy null
            val cubic = cubicWithHorizontalTangents(from.worldPosition, to.worldPosition)
            val poly = sampleCubicAsPolyline(cubic) // List<Offset> in WORLD units
            cubic to poly
        }
    }

    val draggingEdgeOverlay: Pair<Offset, Offset>? by derivedStateOf {
        val startConnector = connectorState.draggingConnectorId.value
        val startPos = connectorState.connectorStates[startConnector]?.worldPosition
        val endPos = connectorState.draggingConnectorWorldPosition.value
        if (startPos == null || endPos == null) return@derivedStateOf null
        return@derivedStateOf startPos to endPos
    }
}

@Composable
internal fun rememberEdgeLayerState(
    edges: List<Edge>,
    connectorState: ConnectorLayerState
): EdgeLayerState = remember {
    EdgeLayerState(
        edges = edges,
        connectorState = connectorState,
    )
}

@Composable
internal fun EdgeLayer(
    edgeLayerState: EdgeLayerState,
    selectedEdge: Edge?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Canvas(
        modifier
            .fillMaxSize()
            .zIndex(1f)
    ) {
        edgeLayerState.edges.forEach { edge ->
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

            val isSelected = edge == selectedEdge
            val strokeWidth = with(density) { (if (isSelected) 6.dp else 3.dp).toPx() }
            drawPath(
                path = path,
                color = if (isSelected) Color(0xFF3D7DFF) else Color(0xFF7A7A7A),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
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

