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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.util.FeaturePathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

internal enum class ConnectorType {
    INPUT,
    OUTPUT,
}

@Immutable
internal data class Connector(
    val nodeId: Int,
    val type: ConnectorType,
    val connectorIndex: Int,
)

@Immutable
internal data class Edge(
    val from: Connector,
    val to: Connector,
)

sealed class AddNodeData(
    val offset: Offset,
) {
    class DataSourceNode(offset: Offset) : AddNodeData(offset)
}

@Immutable
sealed class Node(
    val id: Int,
    val inputConnectorCount: Int,
    val outputConnectorCount: Int,
) {
    class Output(
        id: Int = -1,
        val name: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
        val description: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
        val isDuration: MutableState<Boolean> = mutableStateOf(false),
    ) : Node(
        id = id,
        inputConnectorCount = 1,
        outputConnectorCount = 0,
    )

    class DataSource(
        id: Int = -1,
        val selectedFeatureId: MutableState<Long>,
        val featurePathMap: Map<Long, String>
    ) : Node(
        id = id,
        inputConnectorCount = 0,
        outputConnectorCount = 1,
    )
}

internal interface FunctionsScreenViewModel {
    fun init(groupId: Long, functionId: Long?)

    val nodes: StateFlow<List<Node>>
    val edges: StateFlow<List<Edge>>
    val selectedEdge: StateFlow<Edge?>

    val connectors: StateFlow<Set<Connector>>
    val draggingConnector: StateFlow<Connector?>
    fun onUpsertConnector(connector: Connector, worldPosition: Offset)
    fun onDownOnConnector(connector: Connector)
    fun onDropConnector(connector: Connector?)
    fun getConnectorWorldPosition(connector: Connector): Offset?
    fun isEnabled(connector: Connector): Boolean

    fun onSelectEdge(edge: Edge?)
    fun onDeleteSelectedEdge()

    fun onAddNode(data: AddNodeData)
    fun onDragNodeBy(node: Node, offset: Offset)
    fun onDeleteNode(node: Node)
    fun getWorldPosition(node: Node): Offset?
}

@HiltViewModel
internal class FunctionsScreenViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
) : ViewModel(), FunctionsScreenViewModel {

    private val initialized = AtomicBoolean(false)

    private val _nodes = MutableStateFlow<PersistentList<Node>>(persistentListOf())
    override val nodes: StateFlow<List<Node>> = _nodes.asStateFlow()

    private val _edges = MutableStateFlow<PersistentList<Edge>>(persistentListOf())
    override val edges: StateFlow<List<Edge>> = _edges.asStateFlow()

    private val _selectedEdge = MutableStateFlow<Edge?>(null)
    override val selectedEdge: StateFlow<Edge?> = _selectedEdge.asStateFlow()

    private val _connectors = MutableStateFlow<PersistentSet<Connector>>(persistentSetOf())
    override val connectors: StateFlow<Set<Connector>> = _connectors.asStateFlow()

    private val _draggingConnector = MutableStateFlow<Connector?>(null)
    override val draggingConnector: StateFlow<Connector?> = _draggingConnector.asStateFlow()

    private val nodePositions = mutableStateMapOf<Int, Offset>()
    private val connectorPositions = mutableStateMapOf<Connector, Offset>()
    private val disabledConnectors = mutableStateSetOf<Connector>()

    private lateinit var featurePathMap: Map<Long, String>

    override fun init(groupId: Long, functionId: Long?) {
        if (initialized.getAndSet(true)) return

        viewModelScope.launch {
            val allFeatures = dataInteractor.getAllFeaturesSync()
            val allGroups = dataInteractor.getAllGroupsSync()
            val pathProvider = FeaturePathProvider(allFeatures, allGroups)
            featurePathMap = pathProvider.sortedFeatureMap()

            // TODO: Load function data based on groupId and functionId
            // For now, initialize with mock data
            _nodes.value = _nodes.value.mutate {
                it.add(Node.Output(id = 1))
            }
            nodePositions[1] = Offset(100f, 100f)
        }
    }

    override fun onUpsertConnector(connector: Connector, worldPosition: Offset) {
        connectorPositions[connector] = worldPosition
        _connectors.value = _connectors.value.add(connector)
        val isDraggingOutput = _draggingConnector.value != null && connector.type == ConnectorType.OUTPUT
        val isInput = connector.type == ConnectorType.INPUT
        if (isDraggingOutput || isInput) disabledConnectors += connector
    }

    override fun onDownOnConnector(connector: Connector) {
        _draggingConnector.value = connector
        val disabled = connectors.value.filter { !validConnection(connector, it) }
        disabledConnectors.clear()
        disabledConnectors.addAll(disabled)
    }

    override fun onDropConnector(connector: Connector?) {
        val from = _draggingConnector.value
        if (from != null && connector != null && validConnection(from, connector)) {
            _edges.value = _edges.value.add(Edge(from, connector))
        }
        _draggingConnector.value = null

        disabledConnectors.clear()
        disabledConnectors.addAll(
            connectors.value.filter { it.type == ConnectorType.INPUT }
        )
    }

    private fun validConnection(from: Connector, to: Connector): Boolean {
        return when {
            from.nodeId == to.nodeId
                || from.type != ConnectorType.OUTPUT
                || to.type != ConnectorType.INPUT
                || edgeExists(from, to)
                || to.nodeId in traverseDependencies(from.nodeId) -> false

            else -> true
        }
    }

    private fun edgeExists(from: Connector, to: Connector): Boolean {
        return _edges.value.any { it.from == from && it.to == to }
    }

    private fun traverseDependencies(
        nodeId: Int,
        visited: MutableSet<Int> = mutableSetOf()
    ): Set<Int> {
        if (nodeId in visited) return visited

        visited.add(nodeId)

        _edges.value
            .filter { it.to.nodeId == nodeId }
            .map { it.from.nodeId }
            .forEach { traverseDependencies(it, visited) }

        return visited
    }

    override fun getConnectorWorldPosition(connector: Connector): Offset? {
        return connectorPositions[connector]
    }

    override fun isEnabled(connector: Connector): Boolean {
        return !disabledConnectors.contains(connector)
    }

    override fun onSelectEdge(edge: Edge?) {
        _selectedEdge.value = edge
    }

    override fun onAddNode(data: AddNodeData) {
        val newId = (_nodes.value.maxOfOrNull { it.id } ?: 0) + 1
        val success = when (data) {
            is AddNodeData.DataSourceNode -> addDataSourceNode(newId)
        }
        if (success) nodePositions[newId] = data.offset
    }

    private fun addDataSourceNode(id: Int): Boolean {
        val feature = featurePathMap.entries.firstOrNull() ?: return false

        val newNode = Node.DataSource(
            id = id,
            selectedFeatureId = mutableLongStateOf(feature.key),
            featurePathMap = featurePathMap,
        )
        _nodes.value = _nodes.value.add(newNode)

        return true
    }

    override fun onDragNodeBy(node: Node, offset: Offset) {
        val currentPos = nodePositions[node.id] ?: return
        nodePositions[node.id] = currentPos + offset
    }

    override fun onDeleteNode(node: Node) {
        _nodes.value = _nodes.value.removeAll { it.id == node.id }
        val removedConnectors = mutableSetOf<Connector>()
        _connectors.value = _connectors.value.removeAll { connector ->
            (connector.nodeId == node.id).also { if (it) removedConnectors.add(connector) }
        }
        _edges.value = _edges.value.removeAll { it.from in removedConnectors || it.to in removedConnectors }
    }

    override fun getWorldPosition(node: Node): Offset? {
        return nodePositions[node.id]
    }

    override fun onDeleteSelectedEdge() {
        val selected = _selectedEdge.value
        if (selected != null) {
            _edges.value = _edges.value.remove(selected)
            _selectedEdge.value = null
        }
    }
}