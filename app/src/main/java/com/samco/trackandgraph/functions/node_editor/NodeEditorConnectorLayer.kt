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
package com.samco.trackandgraph.functions.node_editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.functions.viewmodel.Connector
import com.samco.trackandgraph.functions.viewmodel.ConnectorType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
internal class ConnectorLayerState(
    val connectors: State<Set<Connector>>,
    val draggingConnectorId: State<Connector?>,
    private val onUpsertConnector: (Connector, Offset) -> Unit,
    private val onDownOnConnector: (Connector) -> Unit,
    private val onDropConnector: (Connector?) -> Unit,
    private val getConnectorWorldPosition: (Connector) -> Offset?,
    private val isEnabled: (Connector) -> Boolean,
) {

    private val _draggingConnectorWorldPosition = mutableStateOf<Offset?>(null)
    val draggingConnectorWorldPosition: State<Offset?> = _draggingConnectorWorldPosition

    fun upsertConnector(connector: Connector, state: Offset) = onUpsertConnector(connector, state)
    fun downOnConnector(connector: Connector) {
        if (isEnabled(connector)) {
            _draggingConnectorWorldPosition.value = getConnectorWorldPosition(connector)
            onDownOnConnector(connector)
        }
    }
    fun dropConnector(connector: Connector?) {
        onDropConnector(connector)
        _draggingConnectorWorldPosition.value = null
    }

    fun worldPosOf(connector: Connector) = getConnectorWorldPosition(connector)
    fun enabled(connector: Connector) = isEnabled(connector)

    fun dragConnector(worldPos: Offset) {
        if (_draggingConnectorWorldPosition.value != null) {
            _draggingConnectorWorldPosition.value = worldPos
        }
    }
}

@Composable
internal fun rememberConnectorLayerState(
    connectors: StateFlow<Set<Connector>> = MutableStateFlow(emptySet()),
    draggingConnectorId: StateFlow<Connector?> = MutableStateFlow(null),
    onUpsertConnector: (Connector, Offset) -> Unit = { _, _ -> },
    onDownOnConnector: (Connector) -> Unit = {},
    onDropConnector: (Connector?) -> Unit = {},
    getConnectorWorldPosition: (Connector) -> Offset? = { null },
    isEnabled: (Connector) -> Boolean = { true },
): ConnectorLayerState {
    val connectorStatesAsState = connectors.collectAsStateWithLifecycle()
    val draggingConnectorIdAsState = draggingConnectorId.collectAsStateWithLifecycle()
    return remember {
        ConnectorLayerState(
            connectors = connectorStatesAsState,
            draggingConnectorId = draggingConnectorIdAsState,
            onUpsertConnector = onUpsertConnector,
            onDownOnConnector = onDownOnConnector,
            onDropConnector = onDropConnector,
            getConnectorWorldPosition = getConnectorWorldPosition,
            isEnabled = isEnabled,
        )
    }
}

internal val connectorSize = 32.dp

@Composable
internal fun BoxScope.ConnectorArray(
    nodeId: Int,
    count: Int,
    connectorType: ConnectorType,
    connectorLayerState: ConnectorLayerState,
    viewState: ViewportState,
) = Column(
    modifier = Modifier.matchParentSize(),
    verticalArrangement = Arrangement.SpaceEvenly,
    horizontalAlignment =
        if (connectorType == ConnectorType.INPUT) Alignment.Start
        else Alignment.End,
) {
    val connectors = remember(nodeId, connectorType, count) {
        List(count) {
            Connector(
                nodeId = nodeId,
                type = connectorType,
                connectorIndex = it,
            )
        }
    }

    for (connector in connectors) {
        Connector(
            connector = connector,
            connectorLayerState = connectorLayerState,
            viewState = viewState,
        )
    }
}

@Composable
internal fun Connector(
    connector: Connector,
    connectorLayerState: ConnectorLayerState,
    viewState: ViewportState,
) {
    val enabled = connectorLayerState.enabled(connector)
    val innerColor =
        if (enabled) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    val outerColor =
        if (enabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    Canvas(
        modifier = Modifier
            .size(connectorSize)
            .onGloballyPositioned { coords ->
                viewState.viewPortCoordinates.value?.let { viewPortCoords ->
                    val screenPosition = viewPortCoords.localBoundingBoxOf(coords, clipBounds = false).center
                    val worldPosition = viewState.screenToWorld(screenPosition)
                    connectorLayerState.upsertConnector(connector, worldPosition)
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    connectorInputEventHandler(
                        connector = connector,
                        connectorLayerState = connectorLayerState,
                    )
                }
            }
    ) {
        val drawableSize = size.minDimension * 0.8f
        val outerStrokeWidth = 2.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = (drawableSize / 2f) - (outerStrokeWidth / 2f)
        val innerRadius = outerRadius * 0.6f // Inner circle is 60% of outer radius

        // Draw outer circle (stroke only)
        drawCircle(
            color = outerColor,
            radius = outerRadius,
            center = center,
            style = Stroke(width = outerStrokeWidth)
        )

        // Draw filled inner circle
        drawCircle(
            color = innerColor,
            radius = innerRadius,
            center = center
        )
    }
}

private suspend fun AwaitPointerEventScope.connectorInputEventHandler(
    connector: Connector,
    connectorLayerState: ConnectorLayerState,
) {
    val down = awaitFirstDown(requireUnconsumed = true)
    down.consume()
    connectorLayerState.downOnConnector(connector)

    val connectorPosition = connectorLayerState.worldPosOf(connector) ?: return

    var up = false
    var last = down.position

    while (!up) {
        val ev = awaitPointerEvent()
        val current = ev.changes.firstOrNull { it.id == down.id } ?: return
        current.consume()
        last = current.position + connectorPosition
        connectorLayerState.dragConnector(last)
        up = !current.pressed
    }

    val endConnector = nearestConnector(
        connectorLayerState,
        last,
        connectorSize.toPx()
    )
    connectorLayerState.dropConnector(endConnector)
}

private fun Offset.sqrSize() = x * x + y * y

private fun nearestConnector(
    connectorState: ConnectorLayerState,
    worldPos: Offset,
    toleranceWorld: Float
): Connector? {
    val sqrTolerance = toleranceWorld * toleranceWorld
    var smallestDist: Float? = null
    var connectorOfSmallest: Connector? = null

    for (other in connectorState.connectors.value) {
        val otherWorldPos = connectorState.worldPosOf(other) ?: continue
        val sqrDist = (worldPos - otherWorldPos).sqrSize()
        if (sqrDist < sqrTolerance) {
            if (smallestDist == null || sqrDist < smallestDist) {
                smallestDist = sqrDist
                connectorOfSmallest = other
            }
        }
    }

    return connectorOfSmallest
}

