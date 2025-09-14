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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Manages viewport transformations between world and screen coordinates.
 * Handles pan, zoom, and coordinate space conversions for the node editor.
 */
@Stable
class ViewportState(
    scale: Float,
    pan: Offset,
    private val minScale: Float,
    private val maxScale: Float,
) {
    private val _viewPortCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    val viewPortCoordinates: State<LayoutCoordinates?> = _viewPortCoordinates

    var scale by mutableFloatStateOf(scale)
        private set
    var pan by mutableStateOf(pan) // screen-space translation, in pixels
        private set

    private var initialCameraPositionSet by mutableStateOf(false)

    fun setViewPortCoordinates(coordinates: LayoutCoordinates) {
        _viewPortCoordinates.value = coordinates
    }

    // Adjust scale around an anchor in SCREEN space (e.g., gesture centroid)
    fun zoomBy(factor: Float, anchorScreen: Offset) {
        val newScale = (scale * factor).coerceIn(minScale, maxScale)

        // 1) Remember the world point under the anchor BEFORE changing scale
        val worldAtAnchor = (anchorScreen - pan) / scale

        // 2) Apply new scale
        scale = newScale

        // 3) Move so that the same world point maps back to the same anchor
        pan = anchorScreen - worldAtAnchor * newScale
    }

    fun panBy(deltaScreen: Offset) {
        pan += deltaScreen
    }

    // ---- Mapping helpers ----
    // World <-> Screen use the same formula:
    // screen = world * scale + pan
    // world  = (screen - pan) / scale
    fun worldToScreen(p: Offset): Offset = p * scale + pan
    fun screenToWorld(p: Offset): Offset = (p - pan) / scale

    fun worldToScreen(rect: Rect): Rect = Rect(
        worldToScreen(rect.topLeft),
        worldToScreen(rect.bottomRight)
    )

    fun screenToWorld(rect: Rect): Rect = Rect(
        screenToWorld(rect.topLeft),
        screenToWorld(rect.bottomRight)
    )

    internal fun fitViewportToWorldRect(entries: List<Entry>) {
        if (initialCameraPositionSet) return

        val viewportCoords = _viewPortCoordinates.value ?: return
        val viewportWidth = viewportCoords.size.width.toFloat()
        val viewportHeight = viewportCoords.size.height.toFloat()
        if (viewportWidth <= 0f || viewportHeight <= 0f) return
        if (entries.isEmpty()) return

        // Compute the union of all entry bounds (in WORLD space)
        val worldRect = entries
            .map { it.bounds() }
            .reduce { acc, rect -> acc.union(rect) }

        // Desired padding in SCREEN pixels
        val padding = 50f

        val availableW = (viewportWidth - 2 * padding).coerceAtLeast(1f)
        val availableH = (viewportHeight - 2 * padding).coerceAtLeast(1f)

        val worldW = worldRect.width.coerceAtLeast(1e-6f)
        val worldH = worldRect.height.coerceAtLeast(1e-6f)

        val newScale = min(availableW / worldW, availableH / worldH)
            .coerceIn(minScale, maxScale)

        // Center by world center (padding is already “baked in” via the scale)
        val viewportCenter = Offset(viewportWidth / 2f, viewportHeight / 2f)
        val newPan = viewportCenter - worldRect.center * newScale

        scale = newScale
        pan = newPan
        initialCameraPositionSet = true
    }
}

@Composable
fun rememberViewportState(
    initialScale: Float,
    initialPan: Offset,
    minScale: Float,
    maxScale: Float,
) = remember { ViewportState(initialScale, initialPan, minScale, maxScale) }

/**
 * Container that applies viewport transformations using graphics layer.
 * Handles pan and zoom gestures for the node editor viewport.
 */

@Composable
fun WorldTransformContainer(
    state: ViewportState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .onGloballyPositioned { state.setViewPortCoordinates(it) }
            .graphicsLayer {
                // SCALE around top-left
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = state.scale
                scaleY = state.scale
                translationX = state.pan.x
                translationY = state.pan.y
            }
    ) {
        content()
    }
}

/**
 * Layout that positions child composables using world coordinates.
 * Automatically fits viewport to show all content when first displayed.
 */

private class WorldParentDataModifier(val pos: Offset?) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = pos
}

fun Modifier.worldPosition(
    pos: Offset?,
) = this.then(WorldParentDataModifier(pos))

// Measure every child first
data class Entry(
    val placeable: Placeable,
    val offset: Offset,
)

private fun Entry.bounds(): Rect {
    val ax = 0.5f * placeable.width
    val ay = 0.5f * placeable.height
    return Rect(
        offset.x - ax,
        offset.y - ay,
        offset.x + ax,
        offset.y + ay,
    )
}

private fun Rect.union(other: Rect): Rect = Rect(
    min(left, other.left),
    min(top, other.top),
    max(right, other.right),
    max(bottom, other.bottom)
)

@Composable
fun WorldLayout(
    modifier: Modifier = Modifier,
    viewportState: ViewportState? = null,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->

        val looseConstraints = constraints.copy(
            minWidth = 0,
            minHeight = 0,
            maxWidth = Constraints.Infinity,
            maxHeight = Constraints.Infinity
        )

        val entries = measurables.mapNotNull { m ->
            val p = m.measure(looseConstraints)
            val d = (m.parentData as? Offset) ?: return@mapNotNull null
            Entry(p, d)
        }

        if (viewportState != null && entries.isNotEmpty()) {
            viewportState.fitViewportToWorldRect(entries)
        }

        // This layout fills the available area; the camera (graphicsLayer) handles transform.
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        layout(width, height) {
            entries.forEach { (placeable, pos) ->
                val ax = 0.5f * placeable.width
                val ay = 0.5f * placeable.height
                val x = (pos.x - ax).roundToInt()
                val y = (pos.y - ay).roundToInt()
                placeable.place(x, y)
            }
        }
    }
}