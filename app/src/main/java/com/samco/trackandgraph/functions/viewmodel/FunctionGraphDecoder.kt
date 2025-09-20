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

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.NodeDependency
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
internal class FunctionGraphDecoder @Inject constructor() {

    /**
     * Decodes a Function entity back to ViewModel representations.
     *
     * @param function The Function entity containing the serialized function graph
     * @param featurePathMap Map of feature IDs to their display paths
     * @return DecodedFunctionGraph containing nodes, edges, positions, and metadata
     */
    fun decodeFunctionGraph(
        function: Function,
        featurePathMap: Map<Long, String>
    ): DecodedFunctionGraph {
        val functionGraph = function.functionGraph

        // Build node positions map
        val nodePositions = mutableMapOf<Int, Offset>()

        // Decode all nodes from the function graph
        val nodes = persistentListOf<Node>().mutate { nodeList ->
            // Process feature nodes
            functionGraph.nodes.forEach { graphNode ->
                when (graphNode) {
                    is FunctionGraphNode.FeatureNode -> {
                        nodeList.add(decodeFeatureNode(graphNode, featurePathMap))
                        nodePositions[graphNode.id] = Offset(graphNode.x, graphNode.y)
                    }
                    is FunctionGraphNode.OutputNode -> {
                        // Do nothing should not be here
                    }
                }
            }
            
            // Process output node
            val outputNode = decodeOutputNode(functionGraph.outputNode, function)
            nodeList.add(outputNode)
            nodePositions[functionGraph.outputNode.id] = Offset(
                functionGraph.outputNode.x,
                functionGraph.outputNode.y
            )
        }

        // Build edges from dependencies
        val edges = buildEdgesFromDependencies(functionGraph)
        
        return DecodedFunctionGraph(
            nodes = nodes,
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
        featurePathMap: Map<Long, String>
    ): Node.DataSource {
        return Node.DataSource(
            id = graphNode.id,
            selectedFeatureId = mutableLongStateOf(graphNode.featureId),
            featurePathMap = featurePathMap
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
     */
    private fun buildEdgesFromDependencies(functionGraph: FunctionGraph): PersistentList<Edge> {
        return persistentListOf<Edge>().mutate { edgeList ->
            // Get all nodes including the output node
            val allNodes = functionGraph.nodes + functionGraph.outputNode
            
            // Process dependencies for each node
            allNodes.forEach { node ->
                node.dependencies.forEach { dependency ->
                    val edge = createEdgeFromDependency(node, dependency)
                    edgeList.add(edge)
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
            connectorIndex = dependency.outputConnectorIndex
        )
        
        val toConnector = Connector(
            nodeId = targetNode.id,
            type = ConnectorType.INPUT,
            connectorIndex = dependency.connectorIndex
        )
        
        return Edge(from = fromConnector, to = toConnector)
    }
}
