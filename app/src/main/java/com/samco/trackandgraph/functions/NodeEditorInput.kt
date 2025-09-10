package com.samco.trackandgraph.functions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlin.math.max

// This should be placed outside the world transform container at the back
// of the z-order so that it doesn't consume input from cards
@Composable
fun EditorInputOverlay(
    state: ViewportState,
    edges: List<Edge>,
    onSelectEdge: (String?) -> Unit,
    onLongPressEmpty: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val screenTolPx = with(density) { 12.dp.toPx() }

    // Values that may change across recompositions:
    val cubicsState = rememberUpdatedState(
        edges.associateBy({ it.id }) { e ->
            val c = cubicWithHorizontalTangents(e.from, e.to)
            c to sampleCubicAsPolyline(c)
        }
    )
    val onSelectEdgeState = rememberUpdatedState(onSelectEdge)
    val onLongPressEmptyState = rememberUpdatedState(onLongPressEmpty)

    val focusManager = LocalFocusManager.current

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    worldPointerInputEventHandler(
                        state = state,
                        cubics = cubicsState.value,
                        onSelectEdge = onSelectEdgeState.value,
                        onLongPressEmpty = onLongPressEmptyState.value,
                        edgeTapToleranceWorld = screenTolPx / state.scale,
                        onClearFocus = { focusManager.clearFocus() }
                    )
                }
            }
    )
}

private suspend fun AwaitPointerEventScope.worldPointerInputEventHandler(
    state: ViewportState,
    cubics: Map<String, Pair<Cubic, List<Offset>>>,
    onSelectEdge: (String?) -> Unit,
    onLongPressEmpty: (Offset) -> Unit,
    edgeTapToleranceWorld: Float,
    onClearFocus: () -> Unit,
) {
    val down = awaitFirstDown(requireUnconsumed = true)
    val start = down.position // screen coords

    var last = start
    var maxPointerCount = 1
    var up = false
    var maxDelta = Offset.Zero
    val downTime = System.nanoTime()
    var checkedLongPress = false
    val slop = viewConfiguration.touchSlop
    val longPressMs = viewConfiguration.longPressTimeoutMillis

    while (!up) {
        val ev = withTimeoutOrNull(32) { awaitPointerEvent() }

        val totalPassedMs = (System.nanoTime() - downTime) / 1_000_000
        if (!checkedLongPress && maxPointerCount == 1 && totalPassedMs > longPressMs) {
            if (maxDelta.getDistance() < slop) {
                onLongPressEmpty(state.screenToWorld(start))
            }
            checkedLongPress = true
        }

        if (ev == null) continue

        if (ev.changes.size > 1) {
            val zoomChange = ev.calculateZoom()
            val centroid = ev.calculateCentroid(useCurrent = false)
            if (zoomChange != 1f) state.zoomBy(zoomChange, centroid)
            maxPointerCount = max(ev.changes.size, maxPointerCount)
        } else {
            val current = ev.changes.firstOrNull { it.id == down.id } ?: return
            val delta = current.position - last

            state.panBy(delta)
            current.consume()

            maxDelta = max(current.position - start, maxDelta)
            last = current.position
        }
        up = ev.changes.all { !it.pressed }
    }

    val totalPassedMs = (System.nanoTime() - downTime) / 1_000_000
    if (totalPassedMs < longPressMs && maxPointerCount == 1 && maxDelta.getDistance() < slop) {
        onClearFocus()
        handleEdgeSelection(
            offset = state.screenToWorld(start),
            cubics = cubics,
            onSelectEdge = onSelectEdge,
            toleranceWorld = edgeTapToleranceWorld,
        )
    }
}

private fun max(a: Offset, b: Offset): Offset {
    if ((a.x * a.x) + (a.y * a.y) > (b.x * b.x) + (b.y * b.y)) {
        return a
    } else {
        return b
    }
}

private fun handleEdgeSelection(
    offset: Offset,
    cubics: Map<String, Pair<Cubic, List<Offset>>>,
    onSelectEdge: (String?) -> Unit,
    toleranceWorld: Float,
) {
    // Edge candidate?
    val hit = findClosestEdgeHit(offset, cubics, toleranceWorld)
    if (hit != null) {
        onSelectEdge(hit.first)
    } else {
        onSelectEdge(null)
    }
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

/**
 * Calls onDrag with a drag delta in world coordinates.
 */
fun Modifier.worldDraggable(
    onDragBy: (Offset) -> Unit
) = this.pointerInput(onDragBy) {
    detectDragGestures(
        onDrag = { change, dragAmount ->
            change.consume()
            onDragBy(dragAmount)
        }
    )
}
