package com.samco.trackandgraph.functions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class FunctionsNavKey(
    val groupId: Long = 0L,
    val functionId: Long? = null
) : NavKey

@Composable
fun FunctionsScreen(
    navArgs: FunctionsNavKey,
    onPopBack: () -> Unit
) {
    TopAppBarContent(navArgs)
    FunctionsScreenContent()
}

@Composable
private fun FunctionsScreenContent() {
    NodeEditorDemo()
}

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

// ============================================================================
// WORLD CANVAS - Draw in world space (for connections, grid, etc.)
// ============================================================================

@Composable
fun WorldCanvas(
    state: ViewportState,
    draw: DrawScope.(state: ViewportState) -> Unit
) {
    // We draw inside the already-scaled container.
    Canvas(Modifier.fillMaxSize()) {
        draw(state)
    }
}

// ============================================================================
// BACKGROUND GRID - Draw grid in screen space with world positioning
// ============================================================================

@Composable
fun BackgroundGrid(
    viewport: ViewportState,
    worldGridStep: Float = 100f,
    color: Color = Color.Gray.copy(alpha = 0.3f),
    strokeWidth: Float = 1f
) {
    Canvas(Modifier.fillMaxSize()) {
        val screenGridStep = worldGridStep * viewport.scale // Grid spacing in screen pixels

        // Calculate the world coordinate of the top-left corner of the screen
        val topLeftWorld = viewport.screenToWorld(Offset.Zero)

        // Find the first grid line to the left and above the visible area
        val firstGridX = (kotlin.math.floor(topLeftWorld.x / worldGridStep) * worldGridStep).toFloat()
        val firstGridY = (kotlin.math.floor(topLeftWorld.y / worldGridStep) * worldGridStep).toFloat()

        // Convert first grid positions to screen coordinates
        val firstScreenX = viewport.worldToScreen(Offset(firstGridX, 0f)).x
        val firstScreenY = viewport.worldToScreen(Offset(0f, firstGridY)).y

        // Draw vertical lines
        var screenX = firstScreenX
        while (screenX <= size.width) {
            drawLine(
                color = color,
                start = Offset(screenX, 0f),
                end = Offset(screenX, size.height),
                strokeWidth = strokeWidth
            )
            screenX += screenGridStep
        }

        // Draw horizontal lines
        var screenY = firstScreenY
        while (screenY <= size.height) {
            drawLine(
                color = color,
                start = Offset(0f, screenY),
                end = Offset(size.width, screenY),
                strokeWidth = strokeWidth
            )
            screenY += screenGridStep
        }
    }
}

// ============================================================================
// NODE EDITOR DEMO - Putting it all together
// ============================================================================

@Composable
fun NodeEditorDemo() {
    val viewport = rememberViewportState(
        initialScale = 1.0f,
        initialPan = Offset.Zero,
        minScale = 0.15f,
        maxScale = 3.5f
    )

    Box(Modifier.fillMaxSize()) {

        // Background grid
        BackgroundGrid(viewport)

        // The world: scaled + translated as a single layer
        PanZoomContainer(
            state = viewport,
            modifier = Modifier.fillMaxSize()
        ) {

            // Cards at specific WORLD coordinates
            WorldItem(WorldPos(0f, 0f)) {
                SampleCard(title = "Input", color = Color(0xFFCCE5FF))
            }
            WorldItem(WorldPos(600f, 250f)) {
                SampleCard(title = "Transform", color = Color(0xFFFFF3CD))
            }
            WorldItem(WorldPos(1200f, 100f)) {
                SampleCard(title = "Output", color = Color(0xFFD4EDDA))
            }

            // Overlay drawing in world units (Bézier later)
            WorldCanvas(viewport) { state ->
                // Example: a straight line now; swap for cubicTo later
                drawLine(
                    color = Color.Gray,
                    start = Offset(240f, 100f),     // world coords near first card's right edge
                    end = Offset(1200f, 140f),     // world coords near last card's left edge
                    strokeWidth = 4f
                )
            }
        }

        // HUD (not scaled): zoom controls, coordinates, etc.
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Text("Scale: ${"%.2f".format(viewport.scale)}", color = Color.White)
            Text("Pan: (${viewport.pan.x.toInt()}, ${viewport.pan.y.toInt()})", color = Color.White)
            Row {
                TextButton(onClick = { viewport.zoomBy(1.1f, Offset.Zero) }) {
                    Text("+", color = Color.White)
                }
                TextButton(onClick = { viewport.zoomBy(1 / 1.1f, Offset.Zero) }) {
                    Text("–", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SampleCard(title: String, color: Color) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .wrapContentHeight()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Name") })
            DropdownMenuDemo()
        }
    }
}

@Composable
private fun DropdownMenuDemo() {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf("Option A") }
    Column {
        Button(onClick = { expanded = true }) { Text(selected) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("Option A", "Option B", "Option C").forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { selected = it; expanded = false })
            }
        }
    }
}

@Composable
private fun TopAppBarContent(
    navKey: FunctionsNavKey
) {
    val topBarController = LocalTopBarController.current

    topBarController.Set(
        navKey,
        AppBarConfig(
            appBarPinned = true,
            visible = false
        )
    )
}
