package com.samco.trackandgraph.functions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
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
    fun worldToScreen(size: Float): Float = size * scale
    fun screenToWorld(size: Float): Float = size / scale

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
fun PanZoomContainer(
    state: ViewportState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(state) {
                detectTransformGestures(
                    panZoomLock = true,
                    onGesture = { centroid, pan, zoom, _ ->
                        if (zoom != 1f) state.zoomBy(zoom, centroid)
                        if (pan != Offset.Zero) state.panBy(pan)
                    }
                )
            }
            // 1) SCALE around top-left
            .graphicsLayer {
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

data class WorldPos(val x: Float, val y: Float)

@Composable
fun WorldItem(at: WorldPos, content: @Composable () -> Unit) {
    Box(Modifier.offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }) {
        content()
    }
}