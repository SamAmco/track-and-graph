package com.samco.trackandgraph.functions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import kotlinx.serialization.Serializable
import kotlin.random.Random

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

@Composable
private fun FunctionsScreenContent() {
    NodeEditorDemo()
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

    var nextId by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {

        // Background grid
        BackgroundGrid(viewport)

        var selectedEdge by remember { mutableStateOf<Edge?>(null) }

        val edges = remember { mutableStateListOf<Edge>() }

        val connectorState = rememberConnectorLayerState()
        val edgeLayerState = rememberEdgeLayerState(edges, connectorState)

        data class CardData(
            val title: String,
            val color: Color,
            val id: Int,
            val inputConnectorCount: Int = Random.nextInt(0, 5),
            val outputConnectorCount: Int = Random.nextInt(0, 5),
            val position: MutableState<Offset>,
        )

        val cards = remember {
            mutableStateListOf(
                CardData(
                    title = "Input",
                    color = Color(0xFFCCE5FF),
                    id = nextId++,
                    position = mutableStateOf(Offset.Zero)
                ),
                CardData(
                    title = "Transform",
                    color = Color(0xFFFFF3CD),
                    id = nextId++,
                    position = mutableStateOf(Offset(600f, 250f))
                ),
                CardData(
                    title = "Output",
                    color = Color(0xFFD4EDDA),
                    id = nextId++,
                    position = mutableStateOf(Offset(1200f, 100f))
                ),
            )
        }

        EditorInputOverlay(
            state = viewport,
            edgeLayerState = edgeLayerState,
            onSelectEdge = { selectedEdge = it },
            onLongPressEmpty = {
                cards.add(
                    CardData(
                        title = "New Card",
                        color = Color(0xFFCC85FF),
                        id = nextId++,
                        position = mutableStateOf(it)
                    )
                )
            },
        )

        // The world: scaled + translated as a single layer
        WorldTransformContainer(
            state = viewport,
            modifier = Modifier.fillMaxSize()
        ) {
            EdgeLayer(
                edgeLayerState = edgeLayerState,
                selectedEdge = selectedEdge,
            )

            WorldLayout {
                for (card in cards) {
                    SampleCard(
                        modifier = Modifier.worldPosition(card.position.value),
                        onDragBy = { card.position.value += it },
                        viewState = viewport,
                        connectorLayerState = connectorState,
                        id = card.id,
                        title = card.title,
                        color = card.color,
                        inputConnectorCount = card.inputConnectorCount,
                        outputConnectorCount = card.outputConnectorCount,
                        onAddEdge = { start, end -> edges.add(Edge(start, end)) }
                    )
                }
            }
        }
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
