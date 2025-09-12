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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.cardPadding

val connectorSize = 32.dp

@Composable
internal fun SampleCard(
    modifier: Modifier = Modifier,
    onDragBy: (Offset) -> Unit,
    viewState: ViewportState,
    connectorLayerState: ConnectorLayerState,
    id: Int = 0,
    title: String,
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
                .padding(horizontal = connectorSize / 2, vertical = cardPadding)
                .worldDraggable(onDragBy = onDragBy),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
        ) {
            Column(
                Modifier.padding(horizontal = connectorSize / 2, vertical = cardPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

@Preview
@Composable
private fun SampleCardPreview() {
    TnGComposeTheme {
        val viewportState = rememberViewportState(
            initialScale = 1.0f,
            initialPan = Offset.Zero,
            minScale = 0.15f,
            maxScale = 5.0f
        )
        val connectorLayerState = rememberConnectorLayerState()

        SampleCard(
            onDragBy = { },
            viewState = viewportState,
            connectorLayerState = connectorLayerState,
            id = 1,
            title = "Sample Node",
            inputConnectorCount = 2,
            outputConnectorCount = 3,
            onAddEdge = { _, _ -> }
        )
    }
}

