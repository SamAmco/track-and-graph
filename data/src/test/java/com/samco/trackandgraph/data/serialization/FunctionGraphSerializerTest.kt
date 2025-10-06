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

package com.samco.trackandgraph.data.serialization

import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.database.dto.NodeDependency
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class FunctionGraphSerializerTest {

    companion object {
        private const val TEST_JSON_FILE = "comprehensive_function_graph.json"
    }

    private lateinit var json: Json
    private lateinit var serializer: FunctionGraphSerializer

    @Before
    fun setUp() {
        // Use the same Json configuration as the app with pretty printing for tests
        json = Json {
            ignoreUnknownKeys = false // Use strict mode for tests
            isLenient = false
            prettyPrint = true
        }
        serializer = FunctionGraphSerializer(json)
    }

    @Test
    fun `deserialize JSON file produces expected function graph`() {
        val inputJson = readJsonFile(TEST_JSON_FILE)
        val expectedGraph = createTestFunctionGraph()

        val actualGraph = serializer.deserialize(inputJson, throwOnFailure = true)
        assertEquals("Deserialized graph should match expected", expectedGraph, actualGraph)
    }

    @Test
    fun `serialize graph matches expected JSON file`() {
        val functionGraph = createTestFunctionGraph()

        val actualJson = serializer.serialize(functionGraph, throwOnFailure = true)
        val expectedJson = readJsonFile(TEST_JSON_FILE)
        
        assertEquals("Serialized JSON should match expected file", expectedJson, actualJson)
    }

    @Test
    fun `test covers all FunctionGraphNode types`() {
        val testGraph = createTestFunctionGraph()
        val allNodes = testGraph.nodes + testGraph.outputNode
        
        // Get all node types used in our test
        val testedNodeTypes = allNodes.map { it::class }.toSet()
        
        // Get all sealed subclasses of FunctionGraphNode using reflection
        val allNodeTypes = FunctionGraphNode::class.sealedSubclasses.toSet()
        
        // Ensure our test covers all node types
        val missingTypes = allNodeTypes - testedNodeTypes
        
        if (missingTypes.isNotEmpty()) {
            val missingTypeNames = missingTypes.map { it.simpleName }.joinToString(", ")
            fail("Test does not cover all FunctionGraphNode types. Missing: $missingTypeNames. " +
                 "Please update createTestFunctionGraph() to include instances of all node types.")
        }
        
        // Also verify we're not testing non-existent types (defensive check)
        val extraTypes = testedNodeTypes - allNodeTypes
        if (extraTypes.isNotEmpty()) {
            val extraTypeNames = extraTypes.map { it.simpleName }.joinToString(", ")
            fail("Test includes unknown node types: $extraTypeNames")
        }
    }

    @Test
    fun `test covers all LuaScriptConfigurationValue types`() {
        val testGraph = createTestFunctionGraph()
        
        // Collect all configuration types from all LuaScript nodes in the test graph
        val testedConfigTypes = testGraph.nodes
            .filterIsInstance<FunctionGraphNode.LuaScriptNode>()
            .flatMap { it.configuration }
            .map { it.type }
            .toSet()
        
        // Get all enum values that should be tested
        val allConfigTypes = LuaFunctionConfigType.entries.toSet()
        
        // Ensure our test covers all configuration types
        val missingTypes = allConfigTypes - testedConfigTypes
        
        if (missingTypes.isNotEmpty()) {
            val missingTypeNames = missingTypes.joinToString(", ")
            fail("Test does not cover all LuaScriptConfigurationValue types. Missing: $missingTypeNames. " +
                 "Please update createTestFunctionGraph() to include LuaScriptNode configurations of all types. " +
                 "This test protects against accidental serialization changes that would break user data.")
        }
        
        // Also verify we're not testing non-existent types (defensive check)
        val extraTypes = testedConfigTypes - allConfigTypes
        if (extraTypes.isNotEmpty()) {
            val extraTypeNames = extraTypes.joinToString(", ")
            fail("Test includes unknown configuration types: $extraTypeNames")
        }
    }

//    @Test  // Uncomment to regenerate the JSON file
    fun `helper - generate JSON file from structure`() {
        val functionGraph = createTestFunctionGraph()

        val jsonOutput = serializer.serialize(functionGraph, throwOnFailure = true)
        
        // Write the JSON to the test resources file
        val outputFile = java.io.File("src/test/resources/$TEST_JSON_FILE")
        outputFile.writeText(jsonOutput!!)
        
        // This test will always pass - it's just for generating the JSON file
        assertNotNull("JSON should be generated and written to ${outputFile.absolutePath}", jsonOutput)
    }

    private fun createTestFunctionGraph(): FunctionGraph {
        return FunctionGraph(
            nodes = listOf(
                FunctionGraphNode.FeatureNode(
                    x = 100.5f,      // 1 decimal place
                    y = 150.75f,     // 2 decimal places
                    id = 1,
                    featureId = 101L
                ),
                FunctionGraphNode.FeatureNode(
                    x = 200.123f,    // 3 decimal places
                    y = 250.0f,      // No decimal places
                    id = 2,
                    featureId = 102L
                ),
                FunctionGraphNode.FeatureNode(
                    x = 300.0f,      // No decimal places
                    y = 350.9876f,   // 4 decimal places
                    id = 3,
                    featureId = 103L
                ),
                FunctionGraphNode.LuaScriptNode(
                    x = 400.0f,
                    y = 300.0f,
                    id = 5,
                    script = """
                        return function(data_sources)
                            local source = data_sources[1]
                            local data_point = source.dp()
                            while data_point do
                                data_point.value = data_point.value * 2
                                coroutine.yield(data_point)
                                data_point = source.dp()
                            end
                        end
                    """.trimIndent(),
                    inputConnectorCount = 1,
                    configuration = listOf(
                        LuaScriptConfigurationValue.Text(
                            id = "textConfig",
                            value = "Sample text value"
                        ),
                        LuaScriptConfigurationValue.Number(
                            id = "numberConfig", 
                            value = 42.5
                        ),
                        LuaScriptConfigurationValue.Checkbox(
                            id = "checkboxConfig",
                            value = true
                        )
                    ),
                    dependencies = listOf(
                        NodeDependency(
                            connectorIndex = 0,
                            nodeId = 1
                        )
                    )
                )
            ),
            outputNode = FunctionGraphNode.OutputNode(
                x = 500.25f,     // 2 decimal places
                y = 200.1f,      // 1 decimal place
                id = 4,
                dependencies = listOf(
                    NodeDependency(
                        connectorIndex = 0,
                        nodeId = 1
                    ),
                    NodeDependency(
                        connectorIndex = 1,
                        nodeId = 2
                    ),
                    NodeDependency(
                        connectorIndex = 2,
                        nodeId = 3
                    )
                )
            ),
            isDuration = true
        )
    }

    private fun readJsonFile(filename: String): String {
        return this::class.java.classLoader
            ?.getResourceAsStream(filename)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalArgumentException("Could not find resource file: $filename")
    }
}
