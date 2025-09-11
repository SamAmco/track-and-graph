package com.samco.trackandgraph.functions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.samco.trackandgraph.ui.compose.ui.Circle

val connectorSize = 24.dp

@Composable
internal fun SampleCard(
    modifier: Modifier = Modifier,
    onDragBy: (Offset) -> Unit,
    viewState: ViewportState,
    connectorLayerState: ConnectorLayerState,
    id: Int = 0,
    title: String,
    color: Color,
    inputConnectorCount: Int,
    outputConnectorCount: Int,
    onAddEdge: (ConnectorId, ConnectorId) -> Unit,
) {
    val minConnectorSpacing = 24.dp
    val connectors = maxOf(inputConnectorCount, outputConnectorCount)
    // Height needed to place N connectors with at least `minConnectorSpacing` between centers
    // and at the top and bottom of the card
    val requiredMinHeight =
        if (connectors <= 0) 0.dp
        else (connectorSize * connectors) + ((connectors + 1) * minConnectorSpacing)

    Box(
        modifier = modifier
            .heightIn(min = requiredMinHeight)
    ) {
        Card(
            modifier = Modifier
                .heightIn(min = requiredMinHeight)
                .width(IntrinsicSize.Max)
                .padding(connectorSize / 2)
                .worldDraggable(onDragBy = onDragBy),
            colors = CardDefaults.cardColors(containerColor = color)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Name") })
                DropdownMenuDemo()
            }
        }

        ConnectorArray(
            nodeId = id,
            count = inputConnectorCount,
            connectorType = ConnectorType.INPUT,
            connectorLayerState = connectorLayerState,
            viewState = viewState,
            onAddEdge = onAddEdge,
        )

        ConnectorArray(
            nodeId = id,
            count = outputConnectorCount,
            connectorType = ConnectorType.OUTPUT,
            connectorLayerState = connectorLayerState,
            viewState = viewState,
            onAddEdge = onAddEdge,
        )
    }
}

@Composable
private fun BoxScope.ConnectorArray(
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
private fun Connector(
    id: ConnectorId,
    connectorLayerState: ConnectorLayerState,
    viewState: ViewportState,
    enabled: Boolean,
    onAddEdge: (ConnectorId, ConnectorId) -> Unit,
) = Circle(
    modifier = Modifier
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
        },
    size = connectorSize,
    backgroundColor = MaterialTheme.colorScheme.secondary
)

private suspend fun AwaitPointerEventScope.connectorInputEventHandler(
    id: ConnectorId,
    connectorLayerState: ConnectorLayerState,
    onAddEdge: (ConnectorId, ConnectorId) -> Unit
) {
    val down = awaitFirstDown(requireUnconsumed = true)
    connectorLayerState.onDownOnConnector(id)

    val connectorPosition = connectorLayerState.connectorStates[id]?.worldPosition ?: return

    var up = false
    var last = down.position

    while (!up) {
        val ev = awaitPointerEvent()
        val current = ev.changes.firstOrNull { it.id == down.id } ?: return
        last = current.position + connectorPosition
        connectorLayerState.onDragConnector(last)
        up = ev.changes.all { !it.pressed }
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

