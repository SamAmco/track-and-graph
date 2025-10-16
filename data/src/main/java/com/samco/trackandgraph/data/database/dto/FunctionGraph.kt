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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Represents a dependency between nodes in the function graph.
 * @param connectorIndex The input connector index on the dependent node
 * @param nodeId The ID of the node that this node depends on
 */
@Serializable
data class NodeDependency(
    val connectorIndex: Int,
    val nodeId: Int,
)

/**
 * Sealed class hierarchy representing different types of configuration values for Lua script nodes.
 * These are the database representations of user input values for script configurations.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("configType")
sealed class LuaScriptConfigurationValue {
    abstract val id: String
    
    /**
     * Represents a text configuration value.
     * @param id The configuration ID from the Lua script metadata
     * @param value The user-entered text value
     */
    @Serializable
    @SerialName("Text")
    data class Text(
        override val id: String,
        val value: String
    ) : LuaScriptConfigurationValue()
    
    /**
     * Represents a number configuration value.
     * @param id The configuration ID from the Lua script metadata
     * @param value The user-entered numeric value
     */
    @Serializable
    @SerialName("Number")
    data class Number(
        override val id: String,
        val value: Double
    ) : LuaScriptConfigurationValue()
    
    /**
     * Represents a checkbox configuration value.
     * @param id The configuration ID from the Lua script metadata
     * @param value The user-selected boolean value
     */
    @Serializable
    @SerialName("Checkbox")
    data class Checkbox(
        override val id: String,
        val value: Boolean
    ) : LuaScriptConfigurationValue()

    /**
     * Represents an enum configuration value.
     * @param id The configuration ID from the Lua script metadata
     * @param value The selected enum option ID
     */
    @Serializable
    @SerialName("Enum")
    data class Enum(
        override val id: String,
        val value: String
    ) : LuaScriptConfigurationValue()
}

/**
 * Sealed class hierarchy representing different types of nodes in a function graph.
 */
@Serializable
sealed class FunctionGraphNode {
    abstract val x: Float
    abstract val y: Float
    abstract val id: Int
    abstract val dependencies: List<NodeDependency>
    
    /**
     * Represents a feature data source node in the function graph.
     * @param id Unique identifier for this node
     * @param featureId The ID of the feature this node represents
     */
    @Serializable
    @SerialName("FeatureNode")
    data class FeatureNode(
        override val x: Float,
        override val y: Float,
        override val id: Int,
        val featureId: Long,
    ) : FunctionGraphNode() {
        // dependencies should be empty for feature nodes
        override val dependencies: List<NodeDependency> = emptyList()
    }
    
    /**
     * Represents a Lua script node in the function graph.
     * @param id Unique identifier for this node
     * @param script The Lua script code as a string
     * @param inputConnectorCount Number of input connectors this node has
     * @param configuration List of user configuration input values for this script
     * @param dependencies List of nodes this node depends on
     */
    @Serializable
    @SerialName("LuaScriptNode")
    data class LuaScriptNode(
        override val x: Float,
        override val y: Float,
        override val id: Int,
        val script: String,
        val inputConnectorCount: Int,
        val configuration: List<LuaScriptConfigurationValue> = emptyList(),
        override val dependencies: List<NodeDependency>
    ) : FunctionGraphNode()
    
    /**
     * Represents the output node in the function graph.
     * @param id Unique identifier for this node
     * @param dependencies List of nodes this node depends on
     */
    @Serializable
    @SerialName("OutputNode")
    data class OutputNode(
        override val x: Float,
        override val y: Float,
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
)

