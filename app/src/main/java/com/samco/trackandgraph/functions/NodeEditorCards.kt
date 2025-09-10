package com.samco.trackandgraph.functions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
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
) {
    Box(modifier = modifier) {
        Box(modifier = Modifier.padding(connectorSize / 2)) {
            Card(
                modifier = Modifier
                    .wrapContentHeight()
                    .worldDraggable(onDragBy = onDragBy)
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

        Column(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .align(Alignment.TopStart),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (connector in 0 until inputConnectorCount) {
                Connector(
                    id = ConnectorId(id, ConnectorType.INPUT, connector),
                    connectorLayerState = connectorLayerState,
                    viewState = viewState,
                    enabled = true,
                )
            }
        }

        Column(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .align(Alignment.TopEnd),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (connector in 0 until outputConnectorCount) {
                Connector(
                    id = ConnectorId(id, ConnectorType.OUTPUT, connector),
                    connectorLayerState = connectorLayerState,
                    viewState = viewState,
                    enabled = true,
                )
            }
        }
    }
}

@Composable
private fun Connector(
    id: ConnectorId,
    connectorLayerState: ConnectorLayerState,
    viewState: ViewportState,
    enabled: Boolean,
) = Circle(
    modifier = Modifier
        .onGloballyPositioned { coords ->
            viewState.viewPortCoordinates.value?.let { viewPortCoords ->
                val screenPosition = viewPortCoords.localBoundingBoxOf(coords, clipBounds = false).center
                val worldPosition = viewState.screenToWorld(screenPosition)
                val connectorState = ConnectorState(worldPosition, enabled)
                connectorLayerState.upsertConnector(id, connectorState)
            }
        },
    size = connectorSize,
    backgroundColor = MaterialTheme.colorScheme.secondary
)

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

