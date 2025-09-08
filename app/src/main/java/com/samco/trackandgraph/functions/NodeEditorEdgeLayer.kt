package com.samco.trackandgraph.functions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun EdgeLayer(
    state: ViewportState,           // your camera state (scale/pan; used only for tolerance)
    edges: List<Edge>,
    selectedId: String?,
    onSelect: (String?) -> Unit,    // pass null to clear selection
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // Hit tolerance: ~12dp in SCREEN px -> convert to WORLD units
    val toleranceWorld by remember(state.scale, density) {
        mutableFloatStateOf(with(density) { 12.dp.toPx() } / state.scale)
    }

    // Precompute cubic + polyline for each edge (cheap but keeps code tidy)
    val cubics = remember(edges) {
        edges.associateBy({ it.id }) { e ->
            val cubic = cubicWithHorizontalTangents(e.from, e.to)
            val poly = sampleCubicAsPolyline(cubic) // List<Offset> in WORLD units
            cubic to poly
        }
    }

    // Tap selection first, then parent pan/zoom (child gets first shot)
    Box(
        modifier
            .fillMaxSize()
            .edgeTapHandler(edges, state, cubics, onSelect, toleranceWorld)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            edges.forEach { edge ->
                val (cubic, _) = cubics.getValue(edge.id)

                // Build a Path for drawing
                val path = Path().apply {
                    moveTo(cubic.p0.x, cubic.p0.y)
                    cubicTo(
                        cubic.c1.x, cubic.c1.y,
                        cubic.c2.x, cubic.c2.y,
                        cubic.p1.x, cubic.p1.y
                    )
                }

                val isSelected = edge.id == selectedId
                val strokeWidth = with(density) { (if (isSelected) 6.dp else 3.dp).toPx() }
                drawPath(
                    path = path,
                    color = if (isSelected) Color(0xFF3D7DFF) else Color(0xFF7A7A7A),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

// Custom tap detector that only consumes when we actually hit an edge.
private fun Modifier.edgeTapHandler(
    edges: List<Edge>,
    state: ViewportState,
    cubics: Map<String, Pair<Cubic, List<Offset>>>,
    onSelect: (String?) -> Unit,
    toleranceWorld: Float,
) = pointerInput(edges, state.scale) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val downWorld = down.position // already world coords (inside PanZoomHost)
        val hit = findClosestEdgeHit(downWorld, cubics, toleranceWorld)

        // Track for a tap (no move beyond slop, single finger)
        val viewConfig = viewConfiguration
        var isTap = hit != null
        var pointerUp = false
        var exceededSlop = false
        var last = down.position

        // If we *might* tap an edge, consume DOWN so cards/background don’t react.
        if (isTap) down.consume()

        while (!pointerUp) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
            if (change.positionChanged()) {
                exceededSlop = exceededSlop ||
                    (change.position - last).getDistance() > viewConfig.touchSlop
                last = change.position
            }
            pointerUp = event.changes.all { !it.pressed }

            // If they start dragging or add fingers, cancel the edge tap candidate and stop consuming.
            val pointers = event.changes.count { it.pressed }
            if (isTap && (exceededSlop || pointers > 1)) {
                isTap = false
            }

            // Only keep consuming while we believe it's an edge tap
            if (isTap) event.changes.forEach { it.consume() }
        }

        if (isTap) onSelect(hit!!.first) else onSelect(null)
    }
}

data class Edge(
    val id: String,
    val from: Offset, // WORLD coords (exit point on source card)
    val to: Offset    // WORLD coords (entry point on target card)
)

private data class Cubic(val p0: Offset, val c1: Offset, val c2: Offset, val p1: Offset)

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
private fun cubicWithHorizontalTangents(
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

private fun sampleCubicAsPolyline(c: Cubic, maxSegLen: Float = 40f): List<Offset> {
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

private fun distancePointToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val ap = p - a
    val denom = ab.x * ab.x + ab.y * ab.y
    if (denom == 0f) return (p - a).getDistance()
    val t = (ap.x * ab.x + ap.y * ab.y) / denom
    val clamped = t.coerceIn(0f, 1f)
    val proj = Offset(a.x + clamped * ab.x, a.y + clamped * ab.y)
    return (p - proj).getDistance()
}

/** Returns (edgeId, distance) if within tolerance, else null. */
private fun findClosestEdgeHit(
    tapWorld: Offset,
    cubics: Map<String, Pair<Cubic, List<Offset>>>,
    toleranceWorld: Float
): Pair<String, Float>? {
    var bestId: String? = null
    var bestDist = Float.MAX_VALUE

    for ((id, pair) in cubics) {
        val poly = pair.second
        // quick bbox prune
        val minX = poly.minOf { it.x } - toleranceWorld
        val maxX = poly.maxOf { it.x } + toleranceWorld
        val minY = poly.minOf { it.y } - toleranceWorld
        val maxY = poly.maxOf { it.y } + toleranceWorld
        if (tapWorld.x !in minX..maxX || tapWorld.y !in minY..maxY) continue

        // segment distances
        var i = 0
        while (i < poly.size - 1) {
            val d = distancePointToSegment(tapWorld, poly[i], poly[i + 1])
            if (d < bestDist) {
                bestDist = d
                bestId = id
            }
            i++
        }
    }
    return if (bestId != null && bestDist <= toleranceWorld) bestId to bestDist else null
}
