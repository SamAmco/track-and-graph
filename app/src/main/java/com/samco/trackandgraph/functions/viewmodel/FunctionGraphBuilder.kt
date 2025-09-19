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

import androidx.compose.ui.geometry.Offset
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.NodeDependency
import timber.log.Timber
import javax.inject.Inject

/**
 * Builder class responsible for constructing FunctionGraph DTOs from ViewModel representations.
 * This class handles the conversion from the UI layer's node and edge representation
 * to the DTO layer's function graph structure.
 */
internal class FunctionGraphBuilder @Inject constructor() {

    /**
     * Builds a FunctionGraph DTO from ViewModel representations.
     *
     * @param nodes List of nodes from the ViewModel
     * @param edges List of edges connecting the nodes
     * @param nodePositions Map of node ID to position (Offset)
     * @param isDuration Whether the output represents a duration value
     * @param shouldThrow Whether to throw exceptions on error (debug mode) or return null (release mode)
     * @return FunctionGraph DTO ready for serialization, or null if building failed and shouldThrow is false
     * @throws IllegalStateException if the function graph cannot be built and shouldThrow is true
     */
    fun buildFunctionGraph(
        nodes: List<Node>,
        edges: List<Edge>,
        nodePositions: Map<Int, Offset>,
        isDuration: Boolean,
        shouldThrow: Boolean
    ): FunctionGraph? {
        return try {
            // Process all nodes in a single iteration using when for type safety
            var outputNodeViewModel: Node.Output? = null
            val graphNodes = mutableListOf<FunctionGraphNode>()
            
            nodes.forEach { node ->
                when (node) {
                    is Node.Output -> {
                        if (outputNodeViewModel != null) {
                            throw IllegalStateException("Function graph can only have one output node")
                        }
                        outputNodeViewModel = node
                    }
                    is Node.DataSource -> {
                        graphNodes.add(buildFeatureNode(node, nodePositions))
                    }
                    // Future node types will be handled here, compiler will enforce exhaustiveness
                }
            }
            
            // Ensure we found an output node
            val outputNode = outputNodeViewModel
                ?: throw IllegalStateException("Function graph must have an output node")

            // Build the output node DTO
            val outputNodeDto = buildOutputNode(outputNode, edges, nodePositions)

            // Validate no cyclic dependencies exist
            val allGraphNodes = graphNodes + outputNodeDto
            validateNoCycles(allGraphNodes)

            FunctionGraph(
                nodes = graphNodes,
                outputNode = outputNodeDto,
                isDuration = isDuration
            )
        } catch (t: Throwable) {
            Timber.e(t, "Failed to build function graph: ${t.message}")
            if (shouldThrow) throw t else null
        }
    }

    /**
     * Extracts input feature IDs from data source nodes in the graph.
     *
     * @param nodes List of nodes from the ViewModel
     * @return List of feature IDs from all data source nodes
     */
    fun extractInputFeatureIds(nodes: List<Node>): List<Long> {
        return nodes.filterIsInstance<Node.DataSource>()
            .map { it.selectedFeatureId.value }
    }

    /**
     * Calculates dependencies for a given node ID from the edges.
     */
    private fun calculateDependencies(nodeId: Int, edges: List<Edge>): List<NodeDependency> {
        return edges.filter { it.to.nodeId == nodeId }.map { edge ->
            NodeDependency(
                inputConnectorIndex = edge.to.connectorIndex,
                nodeId = edge.from.nodeId,
                outputConnectorIndex = edge.from.connectorIndex
            )
        }
    }

    /**
     * Builds a single feature node from a DataSource node.
     */
    private fun buildFeatureNode(
        node: Node.DataSource,
        nodePositions: Map<Int, Offset>
    ): FunctionGraphNode.FeatureNode {
        val featureId = node.selectedFeatureId.value
        val position = nodePositions[node.id] ?: Offset.Zero

        return FunctionGraphNode.FeatureNode(
            x = position.x,
            y = position.y,
            id = node.id,
            featureId = featureId,
        )
    }

    /**
     * Builds the output node from the output node ViewModel.
     */
    private fun buildOutputNode(
        outputNodeViewModel: Node.Output,
        edges: List<Edge>,
        nodePositions: Map<Int, Offset>
    ): FunctionGraphNode.OutputNode {
        val dependencies = calculateDependencies(outputNodeViewModel.id, edges)
        val position = nodePositions[outputNodeViewModel.id] ?: Offset.Zero

        return FunctionGraphNode.OutputNode(
            x = position.x,
            y = position.y,
            id = outputNodeViewModel.id,
            dependencies = dependencies
        )
    }

    /**
     * Validates that there are no cyclic dependencies in the function graph.
     * Uses depth-first search with a three-color algorithm (white-gray-black) to detect cycles.
     *
     * @param allNodes All nodes in the graph including the output node
     * @throws IllegalStateException if a cycle is detected
     */
    private fun validateNoCycles(allNodes: List<FunctionGraphNode>) {
        // Create a map of node ID to node for quick lookup
        val nodeMap = allNodes.associateBy { it.id }

        // Track node states: 0 = white (unvisited), 1 = gray (visiting), 2 = black (visited)
        val nodeStates = mutableMapOf<Int, Int>()

        // Initialize all nodes as white (unvisited)
        allNodes.forEach { node ->
            nodeStates[node.id] = 0
        }

        // Perform DFS from each unvisited node
        allNodes.forEach { node ->
            if (nodeStates[node.id] == 0) {
                if (hasCycleDFS(node.id, nodeMap, nodeStates)) {
                    throw IllegalStateException("Cyclic dependency detected in function graph starting from node ${node.id}")
                }
            }
        }
    }

    /**
     * Performs depth-first search to detect cycles starting from the given node.
     *
     * @param nodeId The current node ID being visited
     * @param nodeMap Map of node ID to FunctionGraphNode for quick lookup
     * @param nodeStates Map tracking the state of each node (0=white, 1=gray, 2=black)
     * @return true if a cycle is detected, false otherwise
     */
    private fun hasCycleDFS(
        nodeId: Int,
        nodeMap: Map<Int, FunctionGraphNode>,
        nodeStates: MutableMap<Int, Int>
    ): Boolean {
        // Mark current node as gray (being visited)
        nodeStates[nodeId] = 1

        val currentNode = nodeMap[nodeId] ?: return false

        // Visit all dependencies (adjacent nodes)
        for (dependency in currentNode.dependencies) {
            val dependencyNodeId = dependency.nodeId

            when (nodeStates[dependencyNodeId]) {
                1 -> {
                    // Gray node found - cycle detected!
                    return true
                }

                0 -> {
                    // White node - recursively visit
                    if (hasCycleDFS(dependencyNodeId, nodeMap, nodeStates)) {
                        return true
                    }
                }

                2 -> {
                    // Black node (2) - already processed, safe to ignore
                }
            }
        }

        // Mark current node as black (fully processed)
        nodeStates[nodeId] = 2
        return false
    }
}
