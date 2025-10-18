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
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.database.dto.NodeDependency
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import io.github.z4kn4fein.semver.toVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FunctionGraphBuilderTest {
    private val mockConfigurationEncoder: LuaScriptConfigurationEncoder = mock()
    private val builder: FunctionGraphBuilder = FunctionGraphBuilder(
        configurationEncoder = mockConfigurationEncoder
    )

    @Test
    fun `buildFunctionGraph creates correct graph`() {
        // Mock the encoder to return empty configuration for this test
        whenever(mockConfigurationEncoder.encodeConfiguration(any())).thenReturn(emptyList())
        
        val nodes = createTestNodeList()
        val edges = createTestEdgeList()
        val nodePositions = createTestNodePositions()

        val result = builder.buildFunctionGraph(
            nodes = nodes,
            edges = edges,
            nodePositions = nodePositions,
            isDuration = true,
            shouldThrow = true
        )

        val expected = createExpectedGraph()
        assertEquals("Built graph should match expected graph", expected, result)
    }

    @Test
    fun `buildFunctionGraph ignores dependencies from non-existent nodes`() {
        // Encoder not used for this test
        whenever(mockConfigurationEncoder.encodeConfiguration(any())).thenReturn(emptyList())

        // Only an output node exists in the nodes list
        val nodes = listOf(
            Node.Output(id = 1)
        )

        // Edge references a non-existent 'from' node (id=999) into the real output node (id=1)
        val edges = listOf(
            Edge(
                from = Connector(nodeId = 999, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 1, type = ConnectorType.INPUT, connectorIndex = 0)
            )
        )

        val result = builder.buildFunctionGraph(
            nodes = nodes,
            edges = edges,
            nodePositions = emptyMap(),
            isDuration = false,
            shouldThrow = true
        )

        assertNotNull("Graph should be built successfully", result)
        // The invalid dependency should be ignored since node 999 does not exist in the nodes list
        assertTrue(
            "Output node should have no dependencies from non-existent nodes",
            result!!.outputNode.dependencies.isEmpty()
        )
    }

    @Test
    fun `buildFunctionGraph handles missing positions with default Offset Zero`() {
        // Mock the encoder to return empty configuration for this test
        whenever(mockConfigurationEncoder.encodeConfiguration(any())).thenReturn(emptyList())
        
        val nodes = createTestNodeList()
        val edges = createTestEdgeList()
        val emptyPositions = emptyMap<Int, Offset>()

        val result = builder.buildFunctionGraph(
            nodes = nodes,
            edges = edges,
            nodePositions = emptyPositions,
            isDuration = false,
            shouldThrow = true
        )

        assertNotNull("Graph should be built even with missing positions", result)
        assertEquals("Missing positions should default to (0,0)", 0.0f, result!!.nodes[0].x)
        assertEquals("Missing positions should default to (0,0)", 0.0f, result.nodes[0].y)
        assertEquals("Missing positions should default to (0,0)", 0.0f, result.outputNode.x)
        assertEquals("Missing positions should default to (0,0)", 0.0f, result.outputNode.y)
    }

    @Test
    fun `buildFunctionGraph fails when no output node provided`() {
        val nodesWithoutOutput = listOf(
            Node.DataSource(
                id = 1,
                selectedFeatureId = mutableLongStateOf(101L),
                featurePathMap = emptyMap()
            )
        )

        val result = builder.buildFunctionGraph(
            nodes = nodesWithoutOutput,
            edges = emptyList(),
            nodePositions = emptyMap(),
            isDuration = false,
            shouldThrow = false
        )

        assertNull("Graph should fail to build without output node", result)
    }

    @Test
    fun `buildFunctionGraph fails when multiple output nodes provided`() {
        val nodesWithMultipleOutputs = listOf(
            Node.DataSource(
                id = 1,
                selectedFeatureId = mutableLongStateOf(101L),
                featurePathMap = emptyMap()
            ),
            Node.Output(id = 2),
            Node.Output(id = 3)
        )

        val result = builder.buildFunctionGraph(
            nodes = nodesWithMultipleOutputs,
            edges = emptyList(),
            nodePositions = emptyMap(),
            isDuration = false,
            shouldThrow = false
        )

        assertNull("Graph should fail to build with multiple output nodes", result)
    }


    @Test
    fun `extractInputFeatureIds returns correct feature IDs`() {
        val nodes = createTestNodeList()
        val result = builder.extractInputFeatureIds(nodes)
        
        val expected = listOf(101L, 102L, 103L)
        assertEquals("Should extract all feature IDs from data source nodes", expected, result)
    }

    @Test
    fun `extractInputFeatureIds returns empty list when no data source nodes`() {
        val nodesWithoutDataSource = listOf(Node.Output(id = 1))
        val result = builder.extractInputFeatureIds(nodesWithoutDataSource)
        
        assertEquals("Should return empty list when no data source nodes", emptyList<Long>(), result)
    }

    @Test
    fun `buildFunctionGraph uses LuaScriptConfigurationEncoder for configuration encoding`() {
        // Create a LuaScript node with configuration
        val configurationInput = mapOf(
            "param1" to LuaScriptConfigurationInput.Text(
                name = TranslatedString.Simple("Parameter 1"),
                value = mutableStateOf(TextFieldValue("test value"))
            ),
            "param2" to LuaScriptConfigurationInput.Number(
                name = TranslatedString.Simple("Parameter 2"),
                value = mutableStateOf(TextFieldValue("42.5"))
            )
        )
        
        val luaScriptNode = Node.LuaScript(
            id = 10,
            script = "-- Test script with config",
            inputConnectorCount = 1,
            configuration = configurationInput
        )
        
        val nodes = listOf(
            luaScriptNode,
            Node.Output(id = 11)
        )
        
        // Mock the encoder to return expected configuration values
        val expectedEncodedConfig = listOf(
            LuaScriptConfigurationValue.Text(id = "param1", value = "test value"),
            LuaScriptConfigurationValue.Number(id = "param2", value = 42.5)
        )
        whenever(mockConfigurationEncoder.encodeConfiguration(configurationInput))
            .thenReturn(expectedEncodedConfig)
        
        // Build the function graph
        val result = builder.buildFunctionGraph(
            nodes = nodes,
            edges = emptyList(),
            nodePositions = mapOf(10 to Offset.Zero, 11 to Offset.Zero),
            isDuration = false,
            shouldThrow = true
        )
        
        // Verify the encoder was called with the correct configuration
        verify(mockConfigurationEncoder).encodeConfiguration(configurationInput)
        
        // Verify the result contains the encoded configuration
        assertNotNull("Function graph should be built successfully", result)
        val luaScriptNodeDto = result!!.nodes.filterIsInstance<FunctionGraphNode.LuaScriptNode>().first()
        assertEquals("Encoded configuration should match expected", expectedEncodedConfig, luaScriptNodeDto.configuration)
    }

    @Test
    fun `test covers all Node types`() {
        val testNodes = createTestNodeList()
        
        // Get all node types used in our test
        val testedNodeTypes = testNodes.map { it::class }.toSet()
        
        // Get all sealed subclasses of Node using reflection
        val allNodeTypes = Node::class.sealedSubclasses.toSet()
        
        // Ensure our test covers all node types
        val missingTypes = allNodeTypes - testedNodeTypes
        
        if (missingTypes.isNotEmpty()) {
            val missingTypeNames = missingTypes.map { it.simpleName }.joinToString(", ")
            fail("Test does not cover all Node types. Missing: $missingTypeNames. " +
                 "Please update createTestNodeList() to include instances of all node types.")
        }
        
        // Also verify we're not testing non-existent types (defensive check)
        val extraTypes = testedNodeTypes - allNodeTypes
        if (extraTypes.isNotEmpty()) {
            val extraTypeNames = extraTypes.map { it.simpleName }.joinToString(", ")
            fail("Test includes unknown node types: $extraTypeNames")
        }
    }

    @Test
    fun `buildFunctionGraph detects simple cycle between two LuaScript nodes`() {
        val nodesWithSimpleCycle = listOf(
            Node.LuaScript(id = 1, script = "", inputConnectorCount = 1, configuration = emptyMap()),
            Node.LuaScript(id = 2, script = "", inputConnectorCount = 1, configuration = emptyMap()),
            Node.Output(id = 3)
        )
        
        val edgesWithSimpleCycle = listOf(
            // Simple cycle: 1 -> 2 -> 1
            Edge(
                from = Connector(nodeId = 1, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 2, type = ConnectorType.INPUT, connectorIndex = 0)
            ),
            Edge(
                from = Connector(nodeId = 2, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 1, type = ConnectorType.INPUT, connectorIndex = 0)
            ),
            Edge(
                from = Connector(nodeId = 2, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 3, type = ConnectorType.INPUT, connectorIndex = 0)
            )
        )

        try {
            builder.buildFunctionGraph(
                nodes = nodesWithSimpleCycle,
                edges = edgesWithSimpleCycle,
                nodePositions = emptyMap(),
                isDuration = false,
                shouldThrow = true
            )
            fail("Should have thrown an exception due to simple cycle")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message should mention cyclic dependency", 
                e.message?.contains("Cyclic dependency detected") == true)
        }
    }

    @Test
    fun `buildFunctionGraph detects complex cycle through multiple LuaScript nodes`() {
        val nodesWithComplexCycle = listOf(
            Node.LuaScript(id = 1, script = "", inputConnectorCount = 1, configuration = emptyMap()),
            Node.LuaScript(id = 2, script = "", inputConnectorCount = 1, configuration = emptyMap()),
            Node.LuaScript(id = 3, script = "", inputConnectorCount = 1, configuration = emptyMap()),
            Node.Output(id = 4)
        )
        
        val edgesWithComplexCycle = listOf(
            // Complex cycle: 1 -> 2 -> 3 -> 1
            Edge(
                from = Connector(nodeId = 1, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 2, type = ConnectorType.INPUT, connectorIndex = 0)
            ),
            Edge(
                from = Connector(nodeId = 2, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 3, type = ConnectorType.INPUT, connectorIndex = 0)
            ),
            Edge(
                from = Connector(nodeId = 3, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 1, type = ConnectorType.INPUT, connectorIndex = 0)
            ),
            Edge(
                from = Connector(nodeId = 3, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 4, type = ConnectorType.INPUT, connectorIndex = 0)
            )
        )

        try {
            builder.buildFunctionGraph(
                nodes = nodesWithComplexCycle,
                edges = edgesWithComplexCycle,
                nodePositions = emptyMap(),
                isDuration = false,
                shouldThrow = true
            )
            fail("Should have thrown an exception due to complex cycle")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message should mention cyclic dependency", 
                e.message?.contains("Cyclic dependency detected") == true)
        }
    }

    @Test
    fun `buildFunctionGraph detects self-referencing cycle`() {
        val nodesWithSelfCycle = listOf(
            Node.LuaScript(id = 1, script = "", inputConnectorCount = 1, configuration = emptyMap()),
            Node.Output(id = 2)
        )
        
        val edgesWithSelfCycle = listOf(
            // Self-referencing cycle: 1 -> 1
            Edge(
                from = Connector(nodeId = 1, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 1, type = ConnectorType.INPUT, connectorIndex = 0)
            ),
            Edge(
                from = Connector(nodeId = 1, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 2, type = ConnectorType.INPUT, connectorIndex = 0)
            )
        )

        try {
            builder.buildFunctionGraph(
                nodes = nodesWithSelfCycle,
                edges = edgesWithSelfCycle,
                nodePositions = emptyMap(),
                isDuration = false,
                shouldThrow = true
            )
            fail("Should have thrown an exception due to self-referencing cycle")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message should mention cyclic dependency", 
                e.message?.contains("Cyclic dependency detected") == true)
        }
    }

    // Helper functions for creating test data
    private fun createTestNodeList(): List<Node> {
        return listOf(
            Node.DataSource(
                id = 1,
                selectedFeatureId = mutableLongStateOf(101L),
                featurePathMap = mapOf(101L to "Feature 101")
            ),
            Node.DataSource(
                id = 2,
                selectedFeatureId = mutableLongStateOf(102L),
                featurePathMap = mapOf(102L to "Feature 102")
            ),
            Node.DataSource(
                id = 3,
                selectedFeatureId = mutableLongStateOf(103L),
                featurePathMap = mapOf(103L to "Feature 103")
            ),
            Node.LuaScript(
                id = 5,
                script = "-- Test Lua script\nreturn function(sources)\n  yield(sources[1].dp())\nend",
                inputConnectorCount = 2,
                metadata = LuaFunctionMetadata(
                    script = "",
                    id = "function-id",
                    version = "1.0.0".toVersion(),
                    title = null,
                    description = null,
                    inputCount = 2,
                    config = emptyList(),
                ),
                configuration = emptyMap(),
            ),
            Node.Output(
                id = 4,
                name = mutableStateOf(TextFieldValue("Test Output")),
                description = mutableStateOf(TextFieldValue("Test Description")),
                isDuration = mutableStateOf(true)
            )
        )
    }

    private fun createTestEdgeList(): List<Edge> {
        return listOf(
            Edge(
                from = Connector(nodeId = 1, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 4, type = ConnectorType.INPUT, connectorIndex = 0)
            ),
            Edge(
                from = Connector(nodeId = 2, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 4, type = ConnectorType.INPUT, connectorIndex = 1)
            ),
            Edge(
                from = Connector(nodeId = 3, type = ConnectorType.OUTPUT, connectorIndex = 0),
                to = Connector(nodeId = 4, type = ConnectorType.INPUT, connectorIndex = 2)
            )
        )
    }

    private fun createTestNodePositions(): Map<Int, Offset> {
        return mapOf(
            1 to Offset(100.5f, 150.75f),
            2 to Offset(200.123f, 250.0f),
            3 to Offset(300.0f, 350.9876f),
            4 to Offset(500.25f, 200.1f),
            5 to Offset(400.0f, 100.0f)
        )
    }

    private fun createExpectedGraph(): FunctionGraph {
        return FunctionGraph(
            nodes = listOf(
                FunctionGraphNode.FeatureNode(
                    x = 100.5f,
                    y = 150.75f,
                    id = 1,
                    featureId = 101L
                ),
                FunctionGraphNode.FeatureNode(
                    x = 200.123f,
                    y = 250.0f,
                    id = 2,
                    featureId = 102L
                ),
                FunctionGraphNode.FeatureNode(
                    x = 300.0f,
                    y = 350.9876f,
                    id = 3,
                    featureId = 103L
                ),
                FunctionGraphNode.LuaScriptNode(
                    x = 400.0f,
                    y = 100.0f,
                    id = 5,
                    script = "-- Test Lua script\nreturn function(sources)\n  yield(sources[1].dp())\nend",
                    inputConnectorCount = 2,
                    catalogFunctionId = "function-id",
                    catalogVersion = "1.0.0".toVersion(),
                    configuration = emptyList(),
                    dependencies = emptyList()
                )
            ),
            outputNode = FunctionGraphNode.OutputNode(
                x = 500.25f,
                y = 200.1f,
                id = 4,
                dependencies = listOf(
                    NodeDependency(
                        connectorIndex = 0,
                        nodeId = 1,
                    ),
                    NodeDependency(
                        connectorIndex = 1,
                        nodeId = 2,
                    ),
                    NodeDependency(
                        connectorIndex = 2,
                        nodeId = 3,
                    )
                )
            ),
            isDuration = true
        )
    }
}
