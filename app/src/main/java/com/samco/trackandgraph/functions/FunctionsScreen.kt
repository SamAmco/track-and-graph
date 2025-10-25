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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.R
import com.samco.trackandgraph.functions.node_editor.EdgeLayer
import com.samco.trackandgraph.functions.node_editor.NodeEditorInputWrapper
import com.samco.trackandgraph.functions.node_editor.ViewportState
import com.samco.trackandgraph.functions.node_editor.WorldLayout
import com.samco.trackandgraph.functions.node_editor.WorldTransformContainer
import com.samco.trackandgraph.functions.node_editor.rememberConnectorLayerState
import com.samco.trackandgraph.functions.node_editor.rememberEdgeLayerState
import com.samco.trackandgraph.functions.node_editor.rememberViewportState
import com.samco.trackandgraph.functions.node_editor.worldPosition
import com.samco.trackandgraph.functions.node_editor.viewmodel.AddNodeData
import com.samco.trackandgraph.functions.node_editor.viewmodel.Connector
import com.samco.trackandgraph.functions.node_editor.viewmodel.Edge
import com.samco.trackandgraph.functions.node_selector.NodeSelectionDialog
import com.samco.trackandgraph.functions.node_editor.viewmodel.FunctionsScreenViewModel
import com.samco.trackandgraph.functions.node_editor.viewmodel.FunctionsScreenViewModelImpl
import com.samco.trackandgraph.functions.node_editor.viewmodel.Node
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.Serializable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.style.TextAlign
import com.samco.trackandgraph.functions.node_editor.NodeCard
import com.samco.trackandgraph.functions.node_editor.nodeCardContentWidth
import com.samco.trackandgraph.functions.node_editor.viewmodel.Hint
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.ui.compose.ui.CustomContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.TextLink

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
    val viewModel: FunctionsScreenViewModel = hiltViewModel<FunctionsScreenViewModelImpl>()
        .also { it.init(navArgs.groupId, navArgs.functionId) }

    // Handle navigation back when complete
    LaunchedEffect(viewModel.complete) {
        viewModel.complete.receiveAsFlow().collect {
            onPopBack()
        }
    }

    // Track orientation changes
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    LaunchedEffect(isPortrait) {
        viewModel.onOrientationChanged(isPortrait)
    }

    // Observe first-time user dialog state
    val showFirstTimeUserDialog by viewModel.showFirstTimeUserDialog.collectAsStateWithLifecycle()

    TopAppBarContent(navArgs)
    FunctionsScreenContent(
        onPopBack = onPopBack,
        nodes = viewModel.nodes,
        hints = viewModel.hints,
        onAddNode = viewModel::onAddNode,
        onDragNodeBy = viewModel::onDragNodeBy,
        onDeleteNode = viewModel::onDeleteNode,
        getWorldPosition = viewModel::getWorldPosition,
        onRegisterNodeBounds = viewModel::onRegisterNodeBounds,
        edges = viewModel.edges,
        selectedEdge = viewModel.selectedEdge,
        onSelectEdge = viewModel::onSelectEdge,
        onDeleteSelectedEdge = viewModel::onDeleteSelectedEdge,
        connectors = viewModel.connectors,
        draggingConnectorId = viewModel.draggingConnector,
        onUpsertConnector = viewModel::onUpsertConnector,
        onDownOnConnector = viewModel::onDownOnConnector,
        onDropConnector = viewModel::onDropConnector,
        getConnectorWorldPosition = viewModel::getConnectorWorldPosition,
        isConnectorEnabled = viewModel::isEnabled,
        onCreateOrUpdateFunction = viewModel::onCreateOrUpdateFunction,
        onUpdateScriptForNodeId = viewModel::updateScriptForNodeId,
        onUpdateScriptFromFileForNodeId = viewModel::updateScriptFromFileForNodeId,
        showFirstTimeUserDialog = showFirstTimeUserDialog,
        onDismissFirstTimeUserDialog = viewModel::dismissFirstTimeUserDialog,
        onOpenFunctionsTutorial = viewModel::onOpenFunctionsTutorial,
    )
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

// Custom Saver for Offset
private val OffsetSaver = listSaver(
    save = { listOf(it.x, it.y) },
    restore = { Offset(it[0], it[1]) }
)

@Composable
private fun FunctionsScreenContent(
    onPopBack: () -> Unit,
    nodes: StateFlow<List<Node>>,
    hints: StateFlow<List<Hint>>,
    onAddNode: (AddNodeData, Offset) -> Unit,
    onDragNodeBy: (Node, Offset) -> Unit,
    onDeleteNode: (Node) -> Unit,
    getWorldPosition: (Node) -> Offset?,
    onRegisterNodeBounds: (Int, Rect) -> Unit,
    edges: StateFlow<List<Edge>>,
    selectedEdge: StateFlow<Edge?>,
    onSelectEdge: (Edge?) -> Unit,
    onDeleteSelectedEdge: () -> Unit,
    connectors: StateFlow<Set<Connector>>,
    draggingConnectorId: StateFlow<Connector?>,
    onUpsertConnector: (Connector, Offset) -> Unit,
    onDownOnConnector: (Connector) -> Unit,
    onDropConnector: (Connector?) -> Unit,
    getConnectorWorldPosition: (Connector) -> Offset?,
    isConnectorEnabled: (Connector) -> Boolean,
    onCreateOrUpdateFunction: () -> Unit,
    onUpdateScriptForNodeId: (Int, String) -> Unit,
    onUpdateScriptFromFileForNodeId: (Int, android.net.Uri?) -> Unit,
    showFirstTimeUserDialog: Boolean,
    onDismissFirstTimeUserDialog: () -> Unit,
    onOpenFunctionsTutorial: () -> Unit,
) = TnGComposeTheme {
    Box(modifier = Modifier.fillMaxSize()) {

        var clearOverlayUi by rememberSaveable { mutableStateOf(false) }
        var showNodeSelectionDialog by rememberSaveable { mutableStateOf(false) }
        var nodeSelectionOffset by rememberSaveable(stateSaver = OffsetSaver) {
            mutableStateOf(
                Offset.Zero
            )
        }
        val autoFitCameraToContent = remember { mutableStateOf(true) }

        val viewport = rememberViewportState(
            initialScale = 1.0f,
            initialPan = Offset.Zero,
            minScale = 0.15f,
            maxScale = 3.5f,
            autoFitContent = autoFitCameraToContent,
        )

        val connectorState = rememberConnectorLayerState(
            connectors = connectors,
            draggingConnectorId = draggingConnectorId,
            onUpsertConnector = onUpsertConnector,
            onDownOnConnector = {
                onDownOnConnector(it)
                autoFitCameraToContent.value = false
            },
            onDropConnector = onDropConnector,
            getConnectorWorldPosition = getConnectorWorldPosition,
            isEnabled = isConnectorEnabled,
        )
        val edgeLayerState = rememberEdgeLayerState(
            edges = edges,
            selectedEdge = selectedEdge,
            connectorState = connectorState
        )
        val selectedEdgeState by selectedEdge.collectAsStateWithLifecycle()
        val nodesState by nodes.collectAsStateWithLifecycle()
        val hintsState by hints.collectAsStateWithLifecycle()

        NodeEditorInputWrapper(
            state = viewport,
            edgeLayerState = edgeLayerState,
            onSelectEdge = onSelectEdge,
            onLongPressEmpty = { offset ->
                nodeSelectionOffset = offset
                showNodeSelectionDialog = true
            },
            onPan = {
                clearOverlayUi = true
                autoFitCameraToContent.value = false
            },
            onTap = {
                clearOverlayUi = false
                autoFitCameraToContent.value = false
            },
        ) {
            // Background grid
            BackgroundGrid(viewport)

            // The world: scaled + translated as a single layer
            WorldTransformContainer(
                state = viewport,
                modifier = Modifier.fillMaxSize()
            ) {
                EdgeLayer(edgeLayerState = edgeLayerState)

                WorldLayout(viewportState = viewport) {
                    for (node in nodesState) {
                        NodeCard(
                            modifier = Modifier.worldPosition(getWorldPosition(node)),
                            node = node,
                            viewState = viewport,
                            connectorLayerState = connectorState,
                            onDragBy = {
                                onDragNodeBy(node, it)
                                autoFitCameraToContent.value = false
                            },
                            onDeleteNode = {
                                onDeleteNode(node)
                                autoFitCameraToContent.value = false
                            },
                            onCreateOrUpdateFunction = onCreateOrUpdateFunction,
                            onUpdateScriptForNodeId = onUpdateScriptForNodeId,
                            onUpdateScriptFromFileForNodeId = onUpdateScriptFromFileForNodeId,
                            onRegisterNodeBounds = onRegisterNodeBounds,
                        )
                    }

                    for (hint in hintsState) {
                        Text(
                            modifier = Modifier
                                .worldPosition(hint.position)
                                .widthIn(max = nodeCardContentWidth),
                            text = stringResource(hint.textId),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomEnd),
                visible = selectedEdgeState != null
            ) {
                FloatingActionButton(
                    modifier = Modifier
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .then(Modifier.padding(inputSpacingLarge)),
                    onClick = onDeleteSelectedEdge,
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.delete_icon),
                        contentDescription = stringResource(id = R.string.delete)
                    )
                }
            }

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.TopStart),
                visible = !clearOverlayUi
            ) {
                FloatingActionButton(
                    modifier = Modifier
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .then(Modifier.padding(inputSpacingLarge))
                        .size(buttonSize),
                    onClick = { onPopBack() },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(100),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = null,
                    )
                }
            }
        }

        // Node Selection Dialog
        if (showNodeSelectionDialog) {
            NodeSelectionDialog(
                onDismiss = { showNodeSelectionDialog = false },
                onSelect = { selection ->
                    onAddNode(selection, nodeSelectionOffset)
                    showNodeSelectionDialog = false
                }
            )
        }

        // First Time User Dialog
        if (showFirstTimeUserDialog) {
            FunctionsFirstTimeUserDialog(
                onDismiss = onDismissFirstTimeUserDialog,
                onOpenFunctionsTutorialPath = onOpenFunctionsTutorial
            )
        }
    }
}

@Composable
private fun FunctionsFirstTimeUserDialog(
    onDismiss: () -> Unit,
    onOpenFunctionsTutorialPath: () -> Unit,
) {
    CustomContinueCancelDialog(
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        cancelVisible = false,
        content = {
            Column {
                Text(
                    stringResource(id = R.string.functions_first_time_user_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                InputSpacingLarge()
                Text(
                    stringResource(id = R.string.functions_first_time_user_message),
                    style = MaterialTheme.typography.bodyLarge,
                )
                InputSpacingLarge()
                TextLink(
                    stringResource(id = R.string.learn_more),
                ) {
                    onOpenFunctionsTutorialPath()
                    onDismiss()
                }
            }
        },
        continueText = R.string.ok,
    )
}

@Preview(showBackground = true)
@Composable
private fun FunctionsFirstTimeUserDialogPreview() {
    FunctionsFirstTimeUserDialog(onDismiss = {}, onOpenFunctionsTutorialPath = {})
}

/**
 * Renders a background grid that scales with the viewport.
 * Provides visual reference for positioning nodes in world space.
 */
@Composable
fun BackgroundGrid(
    viewport: ViewportState,
    worldGridStep: Float = 100f,
    color: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
    strokeWidth: Float = 1f
) {
    Canvas(Modifier.fillMaxSize()) {
        val screenGridStep = worldGridStep * viewport.scale // Grid spacing in screen pixels

        // Calculate the world coordinate of the top-left corner of the screen
        val topLeftWorld = viewport.screenToWorld(Offset.Zero)

        // Find the first grid line to the left and above the visible area
        val firstGridX =
            (kotlin.math.floor(topLeftWorld.x / worldGridStep) * worldGridStep).toFloat()
        val firstGridY =
            (kotlin.math.floor(topLeftWorld.y / worldGridStep) * worldGridStep).toFloat()

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
