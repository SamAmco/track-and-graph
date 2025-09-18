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

package com.samco.trackandgraph.data.database.dto

import kotlinx.serialization.Serializable

/**
 * Represents a dependency between nodes in the function graph.
 * @param inputConnectorIndex The input connector index on the dependent node
 * @param nodeId The ID of the node that this node depends on
 * @param outputConnectorIndex The output connector index on the dependency node
 */
@Serializable
data class NodeDependency(
    val inputConnectorIndex: Int,
    val nodeId: Int,
    val outputConnectorIndex: Int
)

/**
 * Sealed class hierarchy representing different types of nodes in a function graph.
 */
@Serializable
sealed class FunctionGraphNode {
    abstract val id: Int
    abstract val dependencies: List<NodeDependency>
    
    /**
     * Represents a feature data source node in the function graph.
     * @param id Unique identifier for this node
     * @param featureId The ID of the feature this node represents
     */
    @Serializable
    data class FeatureNode(
        override val id: Int,
        val featureId: Long,
    ) : FunctionGraphNode() {
        // dependencies should be empty for feature nodes
        override val dependencies: List<NodeDependency> = emptyList()
    }
    
    /**
     * Represents the output node in the function graph.
     * @param id Unique identifier for this node
     * @param dependencies List of nodes this node depends on
     */
    @Serializable
    data class OutputNode(
        override val id: Int,
        override val dependencies: List<NodeDependency>
    ) : FunctionGraphNode()
}

/**
 * DTO representation of a function graph structure.
 * This class contains the serialization annotations and will be serialized/deserialized
 * by FunctionGraphSerializer.
 */
@Serializable
data class FunctionGraph(
    val nodes: List<FunctionGraphNode>,
    val outputNode: FunctionGraphNode.OutputNode,
    val isDuration: Boolean
) {
}
