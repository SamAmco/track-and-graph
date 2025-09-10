package com.samco.trackandgraph.functions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

// ============================================================================
// VIEWPORT STATE - Handles world/screen space transforms
// ============================================================================

@Stable
class ViewportState(
    scale: Float,
    pan: Offset,
    private val minScale: Float,
    private val maxScale: Float,
) {
    var scale by mutableFloatStateOf(scale)
        private set
    var pan by mutableStateOf(pan) // screen-space translation, in pixels
        private set

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
}

@Composable
fun rememberViewportState(
    initialScale: Float,
    initialPan: Offset,
    minScale: Float,
    maxScale: Float,
) = remember { ViewportState(initialScale, initialPan, minScale, maxScale) }

// ============================================================================
// PAN/ZOOM CONTAINER - Handles gestures and applies transforms
// ============================================================================

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

// ============================================================================
// WORLD LAYOUT - Position composables by world coordinates
// ============================================================================

private class WorldParentDataModifier(val pos: Offset) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = pos
}

fun Modifier.worldPosition(
    pos: Offset,
) = this.then(WorldParentDataModifier(pos))

@Composable
fun WorldLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->

        // Measure every child first
        data class Entry(val placeable: Placeable, val offset: Offset)

        val entries = measurables.map { m ->
            val p = m.measure(constraints)
            val d = (m.parentData as? Offset) ?: Offset.Zero
            Entry(p, d)
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