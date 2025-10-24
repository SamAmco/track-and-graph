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

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.samco.trackandgraph.functions.node_editor.viewmodel.ConnectorType
import com.samco.trackandgraph.functions.node_editor.viewmodel.Node
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.cardPadding

val nodeCardContentWidth = 350.dp

@Composable
internal fun NodeCard(
    modifier: Modifier = Modifier,
    node: Node,
    viewState: ViewportState,
    connectorLayerState: ConnectorLayerState,
    onDragBy: (Offset) -> Unit = {},
    onDeleteNode: (Node) -> Unit = {},
    onCreateOrUpdateFunction: () -> Unit = {},
    onUpdateScriptForNodeId: (Int, String) -> Unit = { _, _ -> },
    onUpdateScriptFromFileForNodeId: (Int, Uri?) -> Unit = { _, _ -> },
    onRegisterNodeBounds: (Int, Rect) -> Unit = { _, _ -> },
) {
    val minConnectorSpacing = 24.dp
    val connectors = maxOf(node.inputConnectorCount, node.outputConnectorCount)
    // Height needed to place N connectors with at least `minConnectorSpacing` between centers
    // and at the top and bottom of the card
    val requiredMinHeight =
        if (connectors <= 0) 0.dp
        else (connectorSize * connectors) + ((connectors + 1) * minConnectorSpacing)
    Box(
        modifier = modifier
            .heightIn(min = requiredMinHeight)
            .onGloballyPositioned { layoutCoordinates ->
                // Get the actual size of the node after it's been laid out
                viewState.localBoundingBoxOf(layoutCoordinates)?.let {
                    onRegisterNodeBounds(node.id, viewState.screenToWorld(it))
                }
            }
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
            when (node) {
                is Node.Output -> OutputNode(
                    node = node,
                    onCreateOrUpdate = onCreateOrUpdateFunction
                )

                is Node.DataSource -> DataSourceNode(
                    node = node,
                    onDeleteNode = { onDeleteNode(node) },
                )

                is Node.LuaScript -> LuaScriptNode(
                    node = node,
                    onDeleteNode = { onDeleteNode(node) },
                    onUpdateScript = { newScript ->
                        onUpdateScriptForNodeId(node.id, newScript)
                    },
                    onUpdateScriptFromFile = { uri ->
                        onUpdateScriptFromFileForNodeId(node.id, uri)
                    }
                )
            }
        }

        ConnectorArray(
            nodeId = node.id,
            count = node.inputConnectorCount,
            connectorType = ConnectorType.INPUT,
            connectorLayerState = connectorLayerState,
            viewState = viewState,
        )

        ConnectorArray(
            nodeId = node.id,
            count = node.outputConnectorCount,
            connectorType = ConnectorType.OUTPUT,
            connectorLayerState = connectorLayerState,
            viewState = viewState,
        )
    }
}

@Preview()
@Composable
private fun OutputNodePreview() {
    TnGComposeTheme {
        val viewportState = rememberViewportState(
            initialScale = 1.0f,
            initialPan = Offset.Zero,
            minScale = 0.15f,
            maxScale = 5.0f,
            autoFitContent = remember { mutableStateOf(true) },
        )
        val connectorLayerState = rememberConnectorLayerState()

        NodeCard(
            node = Node.Output(
                name = remember { mutableStateOf(TextFieldValue("Sample Output")) },
                description = remember { mutableStateOf(TextFieldValue("This is a sample output description")) },
                isUpdateMode = false
            ),
            viewState = viewportState,
            connectorLayerState = connectorLayerState,
        )
    }
}

@Preview
@Composable
private fun DataSourceNodePreview() {
    TnGComposeTheme {
        val viewportState = rememberViewportState(
            initialScale = 1.0f,
            initialPan = Offset.Zero,
            minScale = 0.15f,
            maxScale = 5.0f,
            autoFitContent = remember { mutableStateOf(true) },
        )
        val connectorLayerState = rememberConnectorLayerState()

        NodeCard(
            node = Node.DataSource(
                selectedFeatureId = remember { mutableStateOf(0L) },
                featurePathMap = mapOf(0L to "Samples/Sample Data Source"),
            ),
            viewState = viewportState,
            connectorLayerState = connectorLayerState,
        )
    }
}

@Preview
@Composable
private fun LuaScriptNodePreview() {
    TnGComposeTheme {
        val viewportState = rememberViewportState(
            initialScale = 1.0f,
            initialPan = Offset.Zero,
            minScale = 0.15f,
            maxScale = 5.0f,
            autoFitContent = remember { mutableStateOf(true) },
        )
        val connectorLayerState = rememberConnectorLayerState()

        NodeCard(
            node = Node.LuaScript(
                id = 1,
                inputConnectorCount = 2,
                script = "function main(input1, input2)\n    return input1 + input2\nend",
                configuration = emptyMap(),
            ),
            viewState = viewportState,
            connectorLayerState = connectorLayerState,
        )
    }
}

