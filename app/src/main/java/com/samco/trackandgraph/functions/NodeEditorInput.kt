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
package com.samco.trackandgraph.functions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

// This should be placed outside the world transform container at the back
// of the z-order so that it doesn't consume input from cards
@Composable
internal fun NodeEditorInputWrapper(
    modifier: Modifier = Modifier,
    state: ViewportState,
    edgeLayerState: EdgeLayerState,
    onSelectEdge: (Edge?) -> Unit,
    onLongPressEmpty: (Offset) -> Unit,
    content: @Composable () -> Unit,
) {
    // Values that may change across recompositions:
    val onSelectEdgeState = rememberUpdatedState(onSelectEdge)
    val onLongPressEmptyState = rememberUpdatedState(onLongPressEmpty)

    val focusManager = rememberUpdatedState(LocalFocusManager.current)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zoomPointerInput(state)
            .tapPanLongPressPointerInput(
                state = state,
                edgeLayerState = edgeLayerState,
                onSelectEdge = onSelectEdgeState.value,
                onLongPressEmpty = onLongPressEmptyState.value,
                edgeTapToleranceWorld = 12.dp / state.scale,
                onClearFocus = { focusManager.value.clearFocus() }
            ),
    ) { content() }
}

/**
 * Modifier that handles zoom gestures in the initial pass.
 * Consumes input if multiple touches are detected.
 */
private fun Modifier.zoomPointerInput(state: ViewportState) = this.pointerInput(state) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var up = false

        while (!up) {
            val ev = withTimeoutOrNull(32) { awaitPointerEvent(pass = PointerEventPass.Initial) }
            if (ev == null) continue

            if (ev.changes.size > 1) {
                // Multi-touch detected - handle zoom and consume all inputs
                val zoomChange = ev.calculateZoom()
                val centroid = ev.calculateCentroid(useCurrent = false)
                if (zoomChange != 1f) {
                    state.zoomBy(zoomChange, centroid)
                }
                // Consume all changes to prevent other modifiers from handling them
                ev.changes.forEach { it.consume() }
            }

            up = ev.changes.all { !it.pressed }
        }
    }
}

/**
 * Modifier that handles tap, pan, and long press gestures in the final pass.
 * Only processes input if it hasn't been consumed by the zoom modifier.
 */
private fun Modifier.tapPanLongPressPointerInput(
    state: ViewportState,
    edgeLayerState: EdgeLayerState,
    onSelectEdge: (Edge?) -> Unit,
    onLongPressEmpty: (Offset) -> Unit,
    edgeTapToleranceWorld: Dp,
    onClearFocus: () -> Unit
) = this.pointerInput(state, edgeLayerState, onSelectEdge, onLongPressEmpty, edgeTapToleranceWorld, onClearFocus) {
    awaitEachGesture {
        tapPanLongPressEventHandler(
            state = state,
            edgeLayerState = edgeLayerState,
            onSelectEdge = onSelectEdge,
            onLongPressEmpty = onLongPressEmpty,
            edgeTapToleranceWorld = edgeTapToleranceWorld.toPx(),
            onClearFocus = onClearFocus
        )
    }
}

private suspend fun AwaitPointerEventScope.tapPanLongPressEventHandler(
    state: ViewportState,
    edgeLayerState: EdgeLayerState,
    onSelectEdge: (Edge?) -> Unit,
    onLongPressEmpty: (Offset) -> Unit,
    edgeTapToleranceWorld: Float,
    onClearFocus: () -> Unit,
) {
    val down = awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Final)
    val startScreen = down.position // screen coords
    val startWorld = state.screenToWorld(startScreen)

    var last = startScreen
    var maxPointerCount = 1
    var up = false
    var maxDelta = Offset.Zero
    val downTime = System.nanoTime()
    var checkedLongPress = false
    val slop = viewConfiguration.touchSlop
    val longPressMs = viewConfiguration.longPressTimeoutMillis

    while (!up) {
        val ev = withTimeoutOrNull(32) { awaitPointerEvent(pass = PointerEventPass.Final) }

        val totalPassedMs = (System.nanoTime() - downTime) / 1_000_000
        if (!checkedLongPress && maxPointerCount == 1 && totalPassedMs > longPressMs) {
            if (maxDelta.getDistance() < slop) {
                onLongPressEmpty(startWorld)
            }
            checkedLongPress = true
        }

        if (ev == null) continue

        // Only handle single touch events (multi-touch should be consumed by zoom modifier)
        if (ev.changes.size == 1) {
            val current = ev.changes.firstOrNull { it.id == down.id } ?: return

            // Only pan if the event hasn't been consumed by a child element (like worldDraggable)
            if (!current.isConsumed) {
                val delta = current.position - last
                state.panBy(delta)
                current.consume()
            }

            maxDelta = max(current.position - startScreen, maxDelta)
            last = current.position
            maxPointerCount = max(ev.changes.size, maxPointerCount)
        } else {
            // Multi-touch detected, but should already be handled by zoom modifier
            maxPointerCount = max(ev.changes.size, maxPointerCount)
        }

        up = ev.changes.all { !it.pressed }
    }

    val totalPassedMs = (System.nanoTime() - downTime) / 1_000_000
    if (totalPassedMs < longPressMs && maxPointerCount == 1 && maxDelta.getDistance() < slop) {
        onClearFocus()
        handleEdgeSelection(
            offset = startWorld,
            edgeLayerState = edgeLayerState,
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
    edgeLayerState: EdgeLayerState,
    onSelectEdge: (Edge?) -> Unit,
    toleranceWorld: Float,
) {
    // Edge candidate?
    val hit = findClosestEdgeHit(offset, edgeLayerState, toleranceWorld)
    if (hit != null) {
        onSelectEdge(hit.first)
    } else {
        onSelectEdge(null)
    }
}

/** Returns (edgeId, distance) if within tolerance, else null. */
private fun findClosestEdgeHit(
    tapWorld: Offset,
    edgeLayerState: EdgeLayerState,
    toleranceWorld: Float
): Pair<Edge, Float>? {
    var bestEdge: Edge? = null
    var bestDist = Float.MAX_VALUE

    for ((edge, pair) in edgeLayerState.cubics) {
        val poly = pair?.second ?: continue
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
                bestEdge = edge
            }
            i++
        }
    }
    return if (bestEdge != null && bestDist <= toleranceWorld) bestEdge to bestDist else null
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
internal fun Modifier.worldDraggable(
    onDragBy: (Offset) -> Unit
) = this.pointerInput(onDragBy) {
    awaitEachGesture {
        val down = awaitFirstDown()
        // Consume the down event to prevent the wrapper from handling it
        down.consume()

        drag(down.id) { change ->
            change.consume()
            onDragBy(change.position - change.previousPosition)
        }
    }
}
