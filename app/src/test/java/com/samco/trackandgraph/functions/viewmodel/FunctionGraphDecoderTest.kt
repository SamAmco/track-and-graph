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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class FunctionGraphDecoderTest {

    private val mockLuaScriptNodeProvider: LuaScriptNodeProvider = mock()
    private val decoder = FunctionGraphDecoder(mockLuaScriptNodeProvider)

    @Test
    fun `decodeFunctionGraph produces expected output`() = runTest {
        val input = createInputFunction()
        val featurePathMap = createFeaturePathMap()
        val expected = createExpectedOutput()

        whenever(mockLuaScriptNodeProvider.createLuaScriptNode(
            script = any(),
            nodeId = any(),
            inputConnectorCount = any(),
            configuration = any(),
        )).thenAnswer { invocation ->
            Node.LuaScript(
                id = invocation.getArgument(1),
                script = invocation.getArgument(0),
                inputConnectorCount = invocation.getArgument(2),
                configuration = emptyMap(),
            )
        }

        val result = decoder.decodeFunctionGraph(input, featurePathMap)

        assertTrue("Decoded graph should match expected output", 
            DecodedFunctionGraphComparator.equals(expected, result))
    }

    @Test
    fun `test covers all FunctionGraphNode types`() {
        val function = createInputFunction()
        
        // Get all FunctionGraphNode types used in our test function
        val allGraphNodes = function.functionGraph.nodes + function.functionGraph.outputNode
        val testedGraphNodeTypes = allGraphNodes.map { it::class }.toSet()
        
        // Get all sealed subclasses of FunctionGraphNode using reflection
        val allGraphNodeTypes = FunctionGraphNode::class.sealedSubclasses.toSet()
        
        // Ensure our test covers all FunctionGraphNode types
        val missingTypes = allGraphNodeTypes - testedGraphNodeTypes
        
        if (missingTypes.isNotEmpty()) {
            val missingTypeNames = missingTypes.map { it.simpleName }.joinToString(", ")
            fail("Test does not cover all FunctionGraphNode types. Missing: $missingTypeNames. " +
                 "Please update createInputFunction() to include instances of all FunctionGraphNode types.")
        }
    }

    // Custom comparators for structural equality
    object DecodedFunctionGraphComparator {
        fun equals(expected: DecodedFunctionGraph, actual: DecodedFunctionGraph): Boolean {
            return expected.nodes.size == actual.nodes.size &&
                    expected.edges.size == actual.edges.size &&
                    expected.nodePositions == actual.nodePositions &&
                    expected.isDuration == actual.isDuration &&
                    NodeListComparator.equals(expected.nodes, actual.nodes) &&
                    expected.edges == actual.edges
        }
    }

    object NodeListComparator {
        fun equals(expected: List<Node>, actual: List<Node>): Boolean {
            if (expected.size != actual.size) return false
            
            return expected.zip(actual).all { (exp, act) ->
                when {
                    exp is Node.DataSource && act is Node.DataSource -> DataSourceNodeComparator.equals(exp, act)
                    exp is Node.Output && act is Node.Output -> OutputNodeComparator.equals(exp, act)
                    exp is Node.LuaScript && act is Node.LuaScript -> LuaScriptNodeComparator.equals(exp, act)
                    else -> false
                }
            }
        }
    }

    object DataSourceNodeComparator {
        fun equals(expected: Node.DataSource, actual: Node.DataSource): Boolean {
            return expected.id == actual.id &&
                    expected.selectedFeatureId.value == actual.selectedFeatureId.value &&
                    expected.featurePathMap == actual.featurePathMap
        }
    }

    object OutputNodeComparator {
        fun equals(expected: Node.Output, actual: Node.Output): Boolean {
            return expected.id == actual.id &&
                    expected.name.value.text == actual.name.value.text &&
                    expected.description.value.text == actual.description.value.text &&
                    expected.isDuration.value == actual.isDuration.value &&
                    expected.isUpdateMode == actual.isUpdateMode &&
                    expected.validationErrors == actual.validationErrors
        }
    }

    object LuaScriptNodeComparator {
        fun equals(expected: Node.LuaScript, actual: Node.LuaScript): Boolean {
            return expected.id == actual.id &&
                    expected.script == actual.script &&
                    expected.inputConnectorCount == actual.inputConnectorCount
        }
    }

    // Helper functions for creating test data
    private fun createInputFunction(): Function {
        return Function(
            id = 1L,
            name = "Test Function",
            groupId = 1L,
            description = "Test Description",
            functionGraph = FunctionGraph(
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
                    FunctionGraphNode.LuaScriptNode(
                        x = 300.0f,
                        y = 350.0f,
                        id = 4,
                        script = "-- Test Lua script\nreturn function(sources)\n  yield(sources[1].dp())\nend",
                        inputConnectorCount = 2,
                        dependencies = listOf(
                            NodeDependency(
                                connectorIndex = 0,
                                nodeId = 1
                            ),
                            NodeDependency(
                                connectorIndex = 1,
                                nodeId = 2
                            )
                        )
                    )
                ),
                outputNode = FunctionGraphNode.OutputNode(
                    x = 500.25f,
                    y = 200.1f,
                    id = 3,
                    dependencies = listOf(
                        NodeDependency(
                            connectorIndex = 0,
                            nodeId = 4,
                        )
                    )
                ),
                isDuration = true
            ),
            inputFeatureIds = listOf(101L, 102L)
        )
    }

    private fun createFeaturePathMap(): Map<Long, String> {
        return mapOf(
            101L to "Feature 101",
            102L to "Feature 102"
        )
    }

    private fun createExpectedOutput(): DecodedFunctionGraph {
        return DecodedFunctionGraph(
            nodes = persistentListOf(
                Node.DataSource(
                    id = 1,
                    selectedFeatureId = mutableLongStateOf(101L),
                    featurePathMap = createFeaturePathMap()
                ),
                Node.DataSource(
                    id = 2,
                    selectedFeatureId = mutableLongStateOf(102L),
                    featurePathMap = createFeaturePathMap()
                ),
                Node.LuaScript(
                    id = 4,
                    script = "-- Test Lua script\nreturn function(sources)\n  yield(sources[1].dp())\nend",
                    inputConnectorCount = 2,
                    configuration = emptyMap(),
                ),
                Node.Output(
                    id = 3,
                    name = mutableStateOf(TextFieldValue("Test Function")),
                    description = mutableStateOf(TextFieldValue("Test Description")),
                    isDuration = mutableStateOf(true),
                    isUpdateMode = true,
                    validationErrors = emptyList()
                )
            ),
            edges = persistentListOf(
                Edge(
                    from = Connector(nodeId = 1, type = ConnectorType.OUTPUT, connectorIndex = 0),
                    to = Connector(nodeId = 4, type = ConnectorType.INPUT, connectorIndex = 0)
                ),
                Edge(
                    from = Connector(nodeId = 2, type = ConnectorType.OUTPUT, connectorIndex = 0),
                    to = Connector(nodeId = 4, type = ConnectorType.INPUT, connectorIndex = 1)
                ),
                Edge(
                    from = Connector(nodeId = 4, type = ConnectorType.OUTPUT, connectorIndex = 0),
                    to = Connector(nodeId = 3, type = ConnectorType.INPUT, connectorIndex = 0)
                )
            ),
            nodePositions = mapOf(
                1 to Offset(100.5f, 150.75f),
                2 to Offset(200.123f, 250.0f),
                3 to Offset(500.25f, 200.1f),
                4 to Offset(300.0f, 350.0f)
            ),
            isDuration = true
        )
    }
}
