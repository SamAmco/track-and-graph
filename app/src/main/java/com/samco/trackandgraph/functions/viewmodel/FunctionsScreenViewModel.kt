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
package com.samco.trackandgraph.functions.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.BuildConfig
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.util.FeaturePathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineDispatcher
import com.samco.trackandgraph.data.di.IODispatcher
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

internal enum class ConnectorType {
    INPUT,
    OUTPUT,
}

internal enum class ValidationError {
    MISSING_NAME,
    NO_INPUTS,
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
    class LuaScriptNode(offset: Offset) : AddNodeData(offset)
}

@Immutable
internal sealed class Node(
    open val id: Int,
    open val inputConnectorCount: Int,
    val outputConnectorCount: Int,
) {
    data class Output(
        override val id: Int = -1,
        val name: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
        val description: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
        val isDuration: MutableState<Boolean> = mutableStateOf(false),
        val isUpdateMode: Boolean = false,
        val validationErrors: List<ValidationError> = emptyList(),
    ) : Node(
        id = id,
        inputConnectorCount = 1,
        outputConnectorCount = 0,
    )

    class DataSource(
        id: Int = -1,
        val selectedFeatureId: MutableState<Long>,
        val featurePathMap: Map<Long, String>,
        val dependentFeatureIds: Set<Long> = emptySet()
    ) : Node(
        id = id,
        inputConnectorCount = 0,
        outputConnectorCount = 1,
    )

    data class LuaScript(
        override val id: Int = -1,
        override val inputConnectorCount: Int,
        val script: String,
    ) : Node(
        id = id,
        inputConnectorCount = inputConnectorCount,
        outputConnectorCount = 1,
    )
}

internal interface FunctionsScreenViewModel {
    fun init(groupId: Long, functionId: Long?)
    val complete: ReceiveChannel<Unit>

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

    fun onCreateOrUpdateFunction()
    fun updateScriptForNodeId(nodeId: Int, newScript: String)
    fun updateScriptFromFileForNodeId(nodeId: Int, uri: Uri?)
}

@HiltViewModel
internal class FunctionsScreenViewModelImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val dataInteractor: DataInteractor,
    private val functionGraphBuilder: FunctionGraphBuilder,
    private val functionGraphDecoder: FunctionGraphDecoder,
) : ViewModel(), FunctionsScreenViewModel {

    private val initialized = AtomicBoolean(false)
    private var groupId: Long = -1
    private var existingFunction: Function? = null

    override val complete: Channel<Unit> = Channel()

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
    private var dependentFeatureIds: Set<Long> = emptySet()

    override fun init(groupId: Long, functionId: Long?) {
        if (initialized.getAndSet(true)) return
        this.groupId = groupId

        viewModelScope.launch {
            existingFunction = functionId?.let { dataInteractor.getFunctionById(it) }

            val allFeatures = dataInteractor.getAllFeaturesSync()
            val allGroups = dataInteractor.getAllGroupsSync()
            val pathProvider = FeaturePathProvider(allFeatures, allGroups)
            featurePathMap = pathProvider.sortedFeatureMap()

            val existing = existingFunction
            if (existing != null) {
                // Get dependent feature IDs for cycle detection when editing existing function
                dependentFeatureIds = dataInteractor.getFeatureIdsDependingOn(existing.featureId)
                
                // Decode existing function graph
                val decodedGraph = functionGraphDecoder.decodeFunctionGraph(existing, featurePathMap, dependentFeatureIds)
                
                // Load decoded nodes and edges using persistent lists
                _nodes.value = decodedGraph.nodes
                _edges.value = decodedGraph.edges
                
                // Load node positions
                nodePositions.putAll(decodedGraph.nodePositions)
            } else {
                // Initialize with default output node for new function
                _nodes.value = _nodes.value.mutate {
                    it.add(Node.Output(id = 1, isUpdateMode = false))
                }
                nodePositions[1] = Offset(0f, 0f)
            }
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
            is AddNodeData.LuaScriptNode -> addLuaScriptNode(newId)
        }
        if (success) nodePositions[newId] = data.offset
    }

    private fun addDataSourceNode(id: Int): Boolean {
        val feature = featurePathMap.entries.firstOrNull() ?: return false

        val newNode = Node.DataSource(
            id = id,
            selectedFeatureId = mutableLongStateOf(feature.key),
            featurePathMap = featurePathMap,
            dependentFeatureIds = dependentFeatureIds
        )
        _nodes.value = _nodes.value.add(newNode)

        return true
    }

    private fun addLuaScriptNode(id: Int): Boolean {
        val newNode = Node.LuaScript(
            id = id,
            inputConnectorCount = 1,
            script = ""
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

    override fun updateScriptForNodeId(nodeId: Int, newScript: String) {
        // TODO: Run the new lua script to determine input connector count
        val newInputConnectorCount = 1 // TODO: This should be determined by analyzing the script
        
        _nodes.value = _nodes.value.mutate { nodeList ->
            val index = nodeList.indexOfFirst { it.id == nodeId && it is Node.LuaScript }
            if (index >= 0) {
                val oldNode = nodeList[index] as Node.LuaScript
                nodeList[index] = oldNode.copy(
                    script = newScript,
                    inputConnectorCount = newInputConnectorCount
                )
            }
        }
    }

    override fun updateScriptFromFileForNodeId(nodeId: Int, uri: Uri?) {
        if (uri == null) return
        
        viewModelScope.launch(ioDispatcher) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val scriptText = BufferedReader(InputStreamReader(inputStream)).readText()
                    updateScriptForNodeId(nodeId, scriptText)
                }
            } catch (e: Exception) {
                // Handle file reading errors gracefully
                Timber.e(e, "Failed to read file")
                if (BuildConfig.DEBUG) throw e
            }
        }
    }

    override fun onCreateOrUpdateFunction() {
        viewModelScope.launch {
            val outputNode = outputNode()

            // Validate the output node
            val validationErrors = validateOutputNode(outputNode)

            // Return early if there are validation errors
            if (validationErrors.isNotEmpty()) {
                // Update the node with validation errors
                val updatedOutputNode = outputNode.copy(validationErrors = validationErrors)
                _nodes.value = _nodes.value.mutate { nodeList ->
                    val index = nodeList.indexOfFirst { it == outputNode }
                    if (index >= 0) {
                        nodeList[index] = updatedOutputNode
                    }
                }
                return@launch
            }

            // Build the function graph using FunctionGraphBuilder
            val functionGraph = functionGraphBuilder.buildFunctionGraph(
                nodes = _nodes.value,
                edges = _edges.value,
                nodePositions = nodePositions.toMap(),
                isDuration = outputNode.isDuration.value,
                shouldThrow = BuildConfig.DEBUG
            )
            
            // If building failed and we're in release mode, complete and return
            if (functionGraph == null) {
                complete.trySend(Unit)
                return@launch
            }

            // Extract input feature IDs from data source nodes
            val inputFeatureIds = functionGraphBuilder.extractInputFeatureIds(_nodes.value)

            val existing = existingFunction
            if (existing != null) {
                // Update existing function
                val updatedFunction = existing.copy(
                    name = outputNode.name.value.text,
                    description = outputNode.description.value.text,
                    functionGraph = functionGraph,
                    inputFeatureIds = inputFeatureIds
                )
                dataInteractor.updateFunction(updatedFunction)
            } else {
                // Create new function
                val function = Function(
                    name = outputNode.name.value.text,
                    groupId = groupId,
                    description = outputNode.description.value.text,
                    functionGraph = functionGraph,
                    inputFeatureIds = inputFeatureIds
                )
                dataInteractor.insertFunction(function)
            }
            complete.trySend(Unit)
        }
    }

    private fun outputNode() = _nodes.value.filterIsInstance<Node.Output>().first()

    private fun validateOutputNode(outputNode: Node.Output): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Check if name is missing
        if (outputNode.name.value.text.isBlank()) {
            errors.add(ValidationError.MISSING_NAME)
        }

        // Check if output node has at least one input connection
        val outputNodeInputConnector = Connector(
            nodeId = outputNode.id,
            type = ConnectorType.INPUT,
            connectorIndex = 0
        )
        val hasInputConnection = _edges.value.any { edge ->
            edge.to == outputNodeInputConnector
        }

        if (!hasInputConnection) {
            errors.add(ValidationError.NO_INPUTS)
        }

        return errors
    }
}