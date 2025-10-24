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

package com.samco.trackandgraph.functions.node_editor.viewmodel

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.NodeDependency
import com.samco.trackandgraph.data.database.dto.toTranslatedStrings
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

/**
 * Data class representing the decoded function graph state for the ViewModel.
 */
internal data class DecodedFunctionGraph(
    val nodes: PersistentList<Node>,
    val edges: PersistentList<Edge>,
    val nodePositions: Map<Int, Offset>,
    val isDuration: Boolean
)

/**
 * Decoder class responsible for converting FunctionGraph DTOs back to ViewModel representations.
 * This class handles the reverse conversion from the DTO layer's function graph structure
 * to the UI layer's node and edge representation.
 */
internal class FunctionGraphDecoder @Inject constructor(
    private val luaScriptNodeProvider: LuaScriptNodeProvider
) {

    /**
     * Decodes a Function entity back to ViewModel representations.
     *
     * @param function The Function entity containing the serialized function graph
     * @param featurePathMap Map of feature IDs to their display paths
     * @param dependentFeatureIds Set of feature IDs that depend on this function (for cycle detection)
     * @return DecodedFunctionGraph containing nodes, edges, positions, and metadata
     */
    suspend fun decodeFunctionGraph(
        function: Function,
        featurePathMap: Map<Long, String>,
        dependentFeatureIds: Set<Long> = emptySet()
    ): DecodedFunctionGraph {
        val functionGraph = function.functionGraph
        
        // Decode nodes and track which ones to filter
        val decodedNodes = mutableListOf<Node>()
        val nodePositions = mutableMapOf<Int, Offset>()
        val filteredNodeIds = mutableSetOf<Int>()
        
        // Process each node
        functionGraph.nodes.forEach { graphNode ->
            when (graphNode) {
                is FunctionGraphNode.FeatureNode -> {
                    if (graphNode.featureId in featurePathMap) {
                        decodedNodes.add(
                            decodeFeatureNode(graphNode, featurePathMap, dependentFeatureIds)
                        )
                        nodePositions[graphNode.id] = Offset(graphNode.x, graphNode.y)
                    } else {
                        filteredNodeIds.add(graphNode.id)
                    }
                }
                is FunctionGraphNode.LuaScriptNode -> {
                    decodedNodes.add(decodeLuaScriptNode(graphNode))
                    nodePositions[graphNode.id] = Offset(graphNode.x, graphNode.y)
                }
                is FunctionGraphNode.OutputNode -> {
                    // Output nodes shouldn't be in the nodes list
                }
            }
        }
        
        // Add output node (always valid)
        val outputNode = functionGraph.outputNode
        decodedNodes.add(decodeOutputNode(outputNode, function))
        nodePositions[outputNode.id] = Offset(outputNode.x, outputNode.y)
        
        // Build edges, filtering out connections to invalid nodes
        val edges = buildEdgesFromDependencies(functionGraph, filteredNodeIds)

        return DecodedFunctionGraph(
            nodes = persistentListOf<Node>().addAll(decodedNodes),
            edges = edges,
            nodePositions = nodePositions,
            isDuration = functionGraph.isDuration
        )
    }

    /**
     * Decodes a FeatureNode DTO to a DataSource ViewModel node.
     */
    private fun decodeFeatureNode(
        graphNode: FunctionGraphNode.FeatureNode,
        featurePathMap: Map<Long, String>,
        dependentFeatureIds: Set<Long>
    ): Node.DataSource {
        return Node.DataSource(
            id = graphNode.id,
            selectedFeatureId = mutableLongStateOf(graphNode.featureId),
            featurePathMap = featurePathMap,
            dependentFeatureIds = dependentFeatureIds
        )
    }

    /**
     * Decodes a LuaScriptNode DTO to a LuaScript ViewModel node.
     * Uses the configuration provider to analyze the script and create a complete node.
     * Passes stored translations to enable proper hydration.
     */
    private suspend fun decodeLuaScriptNode(
        graphNode: FunctionGraphNode.LuaScriptNode
    ): Node.LuaScript {
        // Convert serializable translations back to LocalizationsTable
        val translations = graphNode.translations?.toTranslatedStrings()

        return luaScriptNodeProvider.createLuaScriptNode(
            script = graphNode.script,
            nodeId = graphNode.id,
            inputConnectorCount = graphNode.inputConnectorCount,
            configuration = graphNode.configuration,
            translations = translations
        )
    }

    /**
     * Decodes an OutputNode DTO to an Output ViewModel node.
     */
    private fun decodeOutputNode(
        graphNode: FunctionGraphNode.OutputNode,
        function: Function
    ): Node.Output {
        return Node.Output(
            id = graphNode.id,
            name = mutableStateOf(TextFieldValue(function.name)),
            description = mutableStateOf(TextFieldValue(function.description)),
            isDuration = mutableStateOf(function.functionGraph.isDuration),
            isUpdateMode = true, // Since we're loading an existing function
            validationErrors = emptyList()
        )
    }

    /**
     * Builds edges from the dependency information in the function graph.
     * Excludes edges connected to filtered-out nodes.
     *
     * @param functionGraph The function graph containing dependency information
     * @param filteredNodeIds Set of node IDs that were filtered out (invalid feature nodes)
     */
    private fun buildEdgesFromDependencies(
        functionGraph: FunctionGraph,
        filteredNodeIds: Set<Int>
    ): PersistentList<Edge> {
        return persistentListOf<Edge>().mutate { edgeList ->
            // Get all nodes including the output node
            val allNodes = functionGraph.nodes + functionGraph.outputNode

            // Process dependencies for each node
            allNodes.forEach { node ->
                if (node.id in filteredNodeIds) return@forEach
                // Skip if this node was filtered out
                node.dependencies.forEach { dependency ->
                    // Only add edge if the source node wasn't filtered out
                    if (dependency.nodeId !in filteredNodeIds) {
                        val edge = createEdgeFromDependency(node, dependency)
                        edgeList.add(edge)
                    }
                }
            }
        }
    }

    /**
     * Creates an Edge from a NodeDependency.
     */
    private fun createEdgeFromDependency(
        targetNode: FunctionGraphNode,
        dependency: NodeDependency
    ): Edge {
        val fromConnector = Connector(
            nodeId = dependency.nodeId,
            type = ConnectorType.OUTPUT,
            connectorIndex = 0
        )

        val toConnector = Connector(
            nodeId = targetNode.id,
            type = ConnectorType.INPUT,
            connectorIndex = dependency.connectorIndex
        )

        return Edge(from = fromConnector, to = toConnector)
    }
}
