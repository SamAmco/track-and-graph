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

import androidx.compose.ui.geometry.Offset
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.NodeDependency
import com.samco.trackandgraph.data.database.dto.toSerializable
import timber.log.Timber
import javax.inject.Inject

/**
 * Builder class responsible for constructing FunctionGraph DTOs from ViewModel representations.
 * This class handles the conversion from the UI layer's node and edge representation
 * to the DTO layer's function graph structure.
 */
internal class FunctionGraphBuilder @Inject constructor(
    private val configurationEncoder: LuaScriptConfigurationEncoder
) {

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
            // The source of truth for valid node IDs is the nodes list, not positions
            val validNodeIds: Set<Int> = nodes.map { it.id }.toSet()
            
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
                    is Node.LuaScript -> {
                        graphNodes.add(buildLuaScriptNode(node, edges, nodePositions, validNodeIds))
                    }
                    // Future node types will be handled here, compiler will enforce exhaustiveness
                }
            }
            
            // Ensure we found an output node
            val outputNode = outputNodeViewModel
                ?: throw IllegalStateException("Function graph must have an output node")

            // Build the output node DTO
            val outputNodeDto = buildOutputNode(outputNode, edges, nodePositions, validNodeIds)

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
    private fun calculateDependencies(
        nodeId: Int,
        edges: List<Edge>,
        validNodeIds: Set<Int>,
    ): List<NodeDependency> {
        return edges
            .filter { it.to.nodeId == nodeId }
            .filter { edge -> validNodeIds.contains(edge.from.nodeId) }
            .filter { edge -> validNodeIds.contains(edge.to.nodeId) }
            .map { edge ->
                NodeDependency(
                    connectorIndex = edge.to.connectorIndex,
                    nodeId = edge.from.nodeId,
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
     * Builds a single Lua script node from a LuaScript node.
     * Encodes the configuration input values from ViewModel format to database format.
     */
    private fun buildLuaScriptNode(
        node: Node.LuaScript,
        edges: List<Edge>,
        nodePositions: Map<Int, Offset>,
        validNodeIds: Set<Int>
    ): FunctionGraphNode.LuaScriptNode {
        val dependencies = calculateDependencies(node.id, edges, validNodeIds)
        val position = nodePositions[node.id] ?: Offset.Zero

        // Extract and convert used translations from metadata
        val translations = node.metadata?.usedTranslations?.toSerializable()
        val catalogFunctionId = node.metadata?.id
        val catalogVersion = node.metadata?.version

        return FunctionGraphNode.LuaScriptNode(
            x = position.x,
            y = position.y,
            id = node.id,
            script = node.script,
            inputConnectorCount = node.inputConnectorCount,
            configuration = configurationEncoder.encodeConfiguration(node.configuration),
            translations = translations,
            catalogFunctionId = catalogFunctionId,
            catalogVersion = catalogVersion,
            dependencies = dependencies
        )
    }

    /**
     * Builds the output node from the output node ViewModel.
     */
    private fun buildOutputNode(
        outputNodeViewModel: Node.Output,
        edges: List<Edge>,
        nodePositions: Map<Int, Offset>,
        validNodeIds: Set<Int>
    ): FunctionGraphNode.OutputNode {
        val dependencies = calculateDependencies(outputNodeViewModel.id, edges, validNodeIds)
        val position = nodePositions[outputNodeViewModel.id] ?: Offset.Zero

        return FunctionGraphNode.OutputNode(
            x = position.x,
            y = position.y,
            id = outputNodeViewModel.id,
            dependencies = dependencies
        )
    }
}
