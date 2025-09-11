package com.samco.trackandgraph.functions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset

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