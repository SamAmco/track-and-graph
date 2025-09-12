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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
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

internal enum class ConnectorType {
    INPUT,
    OUTPUT,
}

@Immutable
internal data class ConnectorId(
    val nodeId: Int,
    val type: ConnectorType,
    val connectorIndex: Int,
)

@Immutable
internal data class ConnectorState(
    val worldPosition: Offset,
    val enabled: Boolean,
)

@Stable
internal class ConnectorLayerState {
    private val _connectorStates = mutableStateMapOf<ConnectorId, ConnectorState>()
    val connectorStates: Map<ConnectorId, ConnectorState> = _connectorStates

    private val _draggingConnectorId = mutableStateOf<ConnectorId?>(null)
    val draggingConnectorId: State<ConnectorId?> = _draggingConnectorId
    private val _draggingConnectorWorldPosition = mutableStateOf<Offset?>(null)
    val draggingConnectorWorldPosition: State<Offset?> = _draggingConnectorWorldPosition

    fun upsertConnector(id: ConnectorId, state: ConnectorState) {
        _connectorStates[id] = state
    }

    fun removeConnector(id: ConnectorId) {
        _connectorStates.remove(id)
    }

    fun onDownOnConnector(id: ConnectorId) {
        _draggingConnectorId.value = id
    }

    fun onDragConnector(worldPos: Offset) {
        _draggingConnectorWorldPosition.value = worldPos
    }

    fun onDropConnector() {
        _draggingConnectorId.value = null
        _draggingConnectorWorldPosition.value = null
    }
}

@Composable
internal fun rememberConnectorLayerState(): ConnectorLayerState = remember { ConnectorLayerState() }

@Composable
internal fun BoxScope.ConnectorArray(
    nodeId: Int,
    count: Int,
    connectorType: ConnectorType,
    connectorLayerState: ConnectorLayerState,
    viewState: ViewportState,
    onAddEdge: (ConnectorId, ConnectorId) -> Unit,
) = Column(
    modifier = Modifier.matchParentSize(),
    verticalArrangement = Arrangement.SpaceEvenly,
    horizontalAlignment =
        if (connectorType == ConnectorType.INPUT) Alignment.Start
        else Alignment.End,
) {
    val ids = remember(count) {
        List(count) { ConnectorId(nodeId, connectorType, it) }
    }
    for (connector in 0 until count) {
        Connector(
            id = ids[connector],
            connectorLayerState = connectorLayerState,
            viewState = viewState,
            enabled = connectorLayerState.connectorStates[ids[connector]]?.enabled == true,
            onAddEdge = onAddEdge,
        )
    }
}

@Composable
internal fun Connector(
    id: ConnectorId,
    connectorLayerState: ConnectorLayerState,
    viewState: ViewportState,
    enabled: Boolean,
    onAddEdge: (ConnectorId, ConnectorId) -> Unit,
) {
    val innerColor = MaterialTheme.colorScheme.secondary
    val outerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)

    Canvas(
        modifier = Modifier
            .size(connectorSize)
            .onGloballyPositioned { coords ->
                viewState.viewPortCoordinates.value?.let { viewPortCoords ->
                    val screenPosition = viewPortCoords.localBoundingBoxOf(coords, clipBounds = false).center
                    val worldPosition = viewState.screenToWorld(screenPosition)
                    val connectorState = ConnectorState(worldPosition, enabled)
                    connectorLayerState.upsertConnector(id, connectorState)
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    connectorInputEventHandler(
                        id = id,
                        connectorLayerState = connectorLayerState,
                        onAddEdge = onAddEdge,
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
    id: ConnectorId,
    connectorLayerState: ConnectorLayerState,
    onAddEdge: (ConnectorId, ConnectorId) -> Unit
) {
    val down = awaitFirstDown(requireUnconsumed = true)
    down.consume()
    connectorLayerState.onDownOnConnector(id)

    val connectorPosition = connectorLayerState.connectorStates[id]?.worldPosition ?: return

    var up = false
    var last = down.position

    while (!up) {
        val ev = awaitPointerEvent()
        val current = ev.changes.firstOrNull { it.id == down.id } ?: return
        current.consume()
        last = current.position + connectorPosition
        connectorLayerState.onDragConnector(last)
        up = !current.pressed
    }

    val endConnector = nearestConnector(
        connectorLayerState,
        last,
        connectorSize.toPx()
    )
    if (endConnector != null && endConnector != id) {
        onAddEdge(id, endConnector)
    }
    connectorLayerState.onDropConnector()
}

private fun Offset.sqrSize() = x * x + y * y

private fun nearestConnector(
    connectorState: ConnectorLayerState,
    worldPos: Offset,
    toleranceWorld: Float
): ConnectorId? {
    val sqrTolerance = toleranceWorld * toleranceWorld
    var smallestDist: Float? = null
    var smallestId: ConnectorId? = null

    for ((id, connector) in connectorState.connectorStates) {
        val sqrDist = (worldPos - connector.worldPosition).sqrSize()
        if (sqrDist < sqrTolerance) {
            if (smallestDist == null || sqrDist < smallestDist) {
                smallestDist = sqrDist
                smallestId = id
            }
        }
    }

    return smallestId
}

