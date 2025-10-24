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

package com.samco.trackandgraph.data.validation

import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.NodeDependency
import com.samco.trackandgraph.data.dependencyanalyser.DependencyAnalyser
import com.samco.trackandgraph.data.dependencyanalyser.DependencyAnalyserProvider
import com.samco.trackandgraph.data.dependencyanalyser.DependentFeatures
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FunctionValidatorTest {
    private val mockDependencyAnalyserProvider: DependencyAnalyserProvider = mock()
    private val mockDependencyAnalyser: DependencyAnalyser = mock()
    private val validator: FunctionValidator = FunctionValidator(mockDependencyAnalyserProvider)

    @Before
    fun setup() {
        runBlocking {
            whenever(mockDependencyAnalyserProvider.create())
                .thenReturn(mockDependencyAnalyser)
        }
    }
    // ========== Intra-function cycle detection tests (migrated from FunctionGraphBuilderTest) ==========

    @Test
    fun `validateFunction detects simple cycle between two nodes within function graph`() =
        runTest {
            // Setup mocks
            val nodesWithSimpleCycle = listOf(
                FunctionGraphNode.LuaScriptNode(
                    x = 0f, y = 0f, id = 1,
                    script = "",
                    inputConnectorCount = 1,
                    configuration = emptyList(),
                    dependencies = listOf(
                        NodeDependency(connectorIndex = 0, nodeId = 2)
                    )
                ),
                FunctionGraphNode.LuaScriptNode(
                    x = 0f, y = 0f, id = 2,
                    script = "",
                    inputConnectorCount = 1,
                    configuration = emptyList(),
                    dependencies = listOf(
                        NodeDependency(
                            connectorIndex = 0,
                            nodeId = 1
                        )  // Creates cycle: 1 -> 2 -> 1
                    )
                )
            )

            val outputNode = FunctionGraphNode.OutputNode(
                x = 0f, y = 0f, id = 3,
                dependencies = listOf(
                    NodeDependency(connectorIndex = 0, nodeId = 2)
                )
            )

            val functionWithCycle = createTestFunction(
                nodes = nodesWithSimpleCycle,
                outputNode = outputNode
            )

            try {
                validator.validateFunction(functionWithCycle)
                fail("Should have thrown an exception due to simple cycle")
            } catch (e: IllegalStateException) {
                assertTrue(
                    "Exception message should mention cyclic dependency",
                    e.message?.contains("Cyclic dependency detected") == true
                )
            }
        }

    @Test
    fun `validateFunction detects complex cycle through multiple nodes`() = runTest {
        val nodesWithComplexCycle = listOf(
            FunctionGraphNode.LuaScriptNode(
                x = 0f, y = 0f, id = 1,
                script = "",
                inputConnectorCount = 1,
                configuration = emptyList(),
                dependencies = listOf(
                    NodeDependency(
                        connectorIndex = 0,
                        nodeId = 3
                    )  // 1 depends on 3, creating cycle
                )
            ),
            FunctionGraphNode.LuaScriptNode(
                x = 0f, y = 0f, id = 2,
                script = "",
                inputConnectorCount = 1,
                configuration = emptyList(),
                dependencies = listOf(
                    NodeDependency(connectorIndex = 0, nodeId = 1)  // 2 depends on 1
                )
            ),
            FunctionGraphNode.LuaScriptNode(
                x = 0f, y = 0f, id = 3,
                script = "",
                inputConnectorCount = 1,
                configuration = emptyList(),
                dependencies = listOf(
                    NodeDependency(
                        connectorIndex = 0,
                        nodeId = 2
                    )  // 3 depends on 2, completing cycle: 1 -> 3 -> 2 -> 1
                )
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 4,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 3)
            )
        )

        val functionWithCycle = createTestFunction(
            nodes = nodesWithComplexCycle,
            outputNode = outputNode
        )

        try {
            validator.validateFunction(functionWithCycle)
            fail("Should have thrown an exception due to complex cycle")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Exception message should mention cyclic dependency",
                e.message?.contains("Cyclic dependency detected") == true
            )
        }
    }

    @Test
    fun `validateFunction detects self-referencing cycle`() = runTest {
        val nodesWithSelfCycle = listOf(
            FunctionGraphNode.LuaScriptNode(
                x = 0f, y = 0f, id = 1,
                script = "",
                inputConnectorCount = 1,
                configuration = emptyList(),
                dependencies = listOf(
                    NodeDependency(connectorIndex = 0, nodeId = 1)  // Self-reference: 1 -> 1
                )
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 2,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 1)
            )
        )

        val functionWithCycle = createTestFunction(
            nodes = nodesWithSelfCycle,
            outputNode = outputNode
        )

        try {
            validator.validateFunction(functionWithCycle)
            fail("Should have thrown an exception due to self-referencing cycle")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Exception message should mention cyclic dependency",
                e.message?.contains("Cyclic dependency detected") == true
            )
        }
    }

    @Test
    fun `validateFunction passes when no inter-function cycles exist`() = runTest {
        val functionFeatureId = 1L
        val dependencyFeatureId = 2L

        // Mock: Only the function itself depends on itself (no other features depend on it)
        whenever(mockDependencyAnalyser.getFeaturesDependingOn(functionFeatureId))
            .thenReturn(DependentFeatures(setOf(functionFeatureId)))
        whenever(mockDependencyAnalyser.allFeaturesExist(any()))
            .thenReturn(true)

        val nodes = listOf(
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 1,
                featureId = dependencyFeatureId
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 2,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 1)
            )
        )

        val function = createTestFunction(
            featureId = functionFeatureId,
            nodes = nodes,
            outputNode = outputNode,
            inputFeatureIds = listOf(dependencyFeatureId)
        )

        // Should not throw
        validator.validateFunction(function)
    }

    @Test
    fun `validateFunction fails when feature node is not declared in dependencies`() = runTest {
        // Mock to avoid inter-function cycle detection failure
        whenever(mockDependencyAnalyser.getFeaturesDependingOn(any()))
            .thenReturn(DependentFeatures(setOf(1L)))

        val nodes = listOf(
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 1,
                featureId = 101L
            ),
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 2,
                featureId = 102L  // This feature is in the graph but not declared
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 3,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 1),
                NodeDependency(connectorIndex = 1, nodeId = 2)
            )
        )

        // Only declaring 101L, missing 102L
        val function = createTestFunction(
            nodes = nodes,
            outputNode = outputNode,
            inputFeatureIds = listOf(101L)  // 102L is missing from declarations
        )

        try {
            validator.validateFunction(function)
            fail("Should have thrown an exception due to undeclared dependency")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Exception message should mention mismatched dependencies",
                e.message?.contains("Feature node dependencies do not match declared dependencies") == true
            )
        }
    }

    @Test
    fun `validateFunction fails when declared dependency is not used in graph`() = runTest {
        // Mock to avoid inter-function cycle detection failure
        whenever(mockDependencyAnalyser.getFeaturesDependingOn(any()))
            .thenReturn(DependentFeatures(setOf(1L)))

        val nodes = listOf(
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 1,
                featureId = 101L
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 2,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 1)
            )
        )

        // Declaring both 101L and 102L, but 102L is not in the graph
        val function = createTestFunction(
            nodes = nodes,
            outputNode = outputNode,
            inputFeatureIds = listOf(101L, 102L)  // 102L is declared but not used
        )

        try {
            validator.validateFunction(function)
            fail("Should have thrown an exception due to unused declared dependency")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Exception message should mention mismatched dependencies",
                e.message?.contains("Feature node dependencies do not match declared dependencies") == true
            )
        }
    }

    // ========== Inter-function dependency cycle tests ==========

    @Test
    fun `validateFunction fails when function has self-referential dependency`() = runTest {
        val functionFeatureId = 1L

        // Mock: Function depends on itself
        whenever(mockDependencyAnalyser.getFeaturesDependingOn(functionFeatureId))
            .thenReturn(DependentFeatures(setOf(functionFeatureId)))
        whenever(mockDependencyAnalyser.allFeaturesExist(any()))
            .thenReturn(true)

        val nodes = listOf(
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 1,
                featureId = functionFeatureId  // Function depends on itself
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 2,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 1)
            )
        )

        val function = createTestFunction(
            featureId = functionFeatureId,
            nodes = nodes,
            outputNode = outputNode,
            inputFeatureIds = listOf(functionFeatureId)
        )

        try {
            validator.validateFunction(function)
            fail("Should have thrown an exception due to self-referential dependency")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Exception message should mention function cycle",
                e.message?.contains("Function cycle detected") == true
            )
        }
    }

    @Test
    fun `validateFunction fails when simple inter-function cycle exists`() = runTest {
        val functionFeatureId = 1L
        val dependencyFeatureId = 2L

        // Mock: Feature 2 already depends on Feature 1 (our function)
        // So if Feature 1 depends on Feature 2, we have a cycle: 1 -> 2 -> 1
        whenever(mockDependencyAnalyser.getFeaturesDependingOn(functionFeatureId))
            .thenReturn(DependentFeatures(setOf(functionFeatureId, dependencyFeatureId)))
        whenever(mockDependencyAnalyser.allFeaturesExist(setOf(dependencyFeatureId)))
            .thenReturn(true)

        val nodes = listOf(
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 1,
                featureId = dependencyFeatureId
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 2,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 1)
            )
        )

        val function = createTestFunction(
            featureId = functionFeatureId,
            nodes = nodes,
            outputNode = outputNode,
            inputFeatureIds = listOf(dependencyFeatureId)
        )

        try {
            validator.validateFunction(function)
            fail("Should have thrown an exception due to simple inter-function cycle")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Exception message should mention function cycle",
                e.message?.contains("Function cycle detected") == true
            )
        }
    }

    @Test
    fun `validateFunction passes when all validations succeed`() = runTest {
        val functionFeatureId = 1L
        val dependency1FeatureId = 2L
        val dependency2FeatureId = 3L

        // Mock: No other features depend on this function (no cycles)
        whenever(mockDependencyAnalyser.getFeaturesDependingOn(functionFeatureId))
            .thenReturn(DependentFeatures(setOf(functionFeatureId)))
        whenever(
            mockDependencyAnalyser
                .allFeaturesExist(setOf(dependency1FeatureId, dependency2FeatureId))
        ).thenReturn(true)

        val nodes = listOf(
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 1,
                featureId = dependency1FeatureId
            ),
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 2,
                featureId = dependency2FeatureId
            ),
            FunctionGraphNode.LuaScriptNode(
                x = 0f, y = 0f, id = 3,
                script = "return function(sources) yield(sources[1].dp() + sources[2].dp()) end",
                inputConnectorCount = 2,
                configuration = emptyList(),
                dependencies = listOf(
                    NodeDependency(connectorIndex = 0, nodeId = 1),
                    NodeDependency(connectorIndex = 1, nodeId = 2)
                )
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 4,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 3)
            )
        )

        // All dependencies match: graph has features 2 and 3, declared has features 2 and 3
        val function = createTestFunction(
            featureId = functionFeatureId,
            nodes = nodes,
            outputNode = outputNode,
            inputFeatureIds = listOf(dependency1FeatureId, dependency2FeatureId)
        )

        // Should not throw any exception
        validator.validateFunction(function)
    }

    @Test
    fun `validateFunction fails when function references non-existent features`() = runTest {
        val functionFeatureId = 1L
        val existingFeatureId = 2L
        val nonExistentFeatureId = 999L

        // Mock: Feature 999 doesn't exist
        whenever(
            mockDependencyAnalyser.allFeaturesExist(
                setOf(
                    existingFeatureId,
                    nonExistentFeatureId
                )
            )
        )
            .thenReturn(false)

        val nodes = listOf(
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 1,
                featureId = existingFeatureId
            ),
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 2,
                featureId = nonExistentFeatureId
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 3,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 1),
                NodeDependency(connectorIndex = 1, nodeId = 2)
            )
        )

        val function = createTestFunction(
            featureId = functionFeatureId,
            nodes = nodes,
            outputNode = outputNode,
            inputFeatureIds = listOf(existingFeatureId, nonExistentFeatureId)
        )

        try {
            validator.validateFunction(function)
            fail("Should have thrown an exception due to non-existent feature reference")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Exception message should mention non-existent features",
                e.message?.contains("Function references non-existent features") == true
            )
        }
    }

    // ========== Function graph structure validation tests ==========

    @Test
    fun `validateFunction fails when node IDs are not unique`() = runTest {
        val nodes = listOf(
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 1,
                featureId = 100L
            ),
            FunctionGraphNode.LuaScriptNode(
                x = 0f, y = 0f, id = 1,  // Duplicate ID with the feature node
                script = "return function(sources) yield(sources[1].dp()) end",
                inputConnectorCount = 1,
                configuration = emptyList(),
                dependencies = listOf(
                    NodeDependency(connectorIndex = 0, nodeId = 1)
                )
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 2,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 1)
            )
        )

        val function = createTestFunction(
            nodes = nodes,
            outputNode = outputNode,
            inputFeatureIds = listOf(100L)
        )

        try {
            validator.validateFunction(function)
            fail("Should have thrown an exception due to duplicate node IDs")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Exception message should mention unique node IDs",
                e.message?.contains("Node IDs should be unique") == true
            )
        }
    }

    @Test
    fun `validateFunction fails when output node ID conflicts with regular node ID`() = runTest {
        val nodes = listOf(
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 1,
                featureId = 100L
            ),
            FunctionGraphNode.LuaScriptNode(
                x = 0f, y = 0f, id = 2,
                script = "return function(sources) yield(sources[1].dp()) end",
                inputConnectorCount = 1,
                configuration = emptyList(),
                dependencies = listOf(
                    NodeDependency(connectorIndex = 0, nodeId = 1)
                )
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 2,  // Same ID as the LuaScriptNode
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 2)
            )
        )

        val function = createTestFunction(
            nodes = nodes,
            outputNode = outputNode,
            inputFeatureIds = listOf(100L)
        )

        try {
            validator.validateFunction(function)
            fail("Should have thrown an exception due to output node ID conflicting with regular node")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Exception message should mention unique node IDs",
                e.message?.contains("Node IDs should be unique") == true
            )
        }
    }

    @Test
    fun `validateFunction fails when output node is declared in nodes list`() = runTest {
        val nodes = listOf(
            FunctionGraphNode.FeatureNode(
                x = 0f, y = 0f, id = 1,
                featureId = 100L
            ),
            FunctionGraphNode.OutputNode(  // Output node incorrectly in the nodes list
                x = 0f, y = 0f, id = 2,
                dependencies = listOf(
                    NodeDependency(connectorIndex = 0, nodeId = 1)
                )
            )
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 0f, y = 0f, id = 3,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 1)
            )
        )

        val function = createTestFunction(
            nodes = nodes,
            outputNode = outputNode,
            inputFeatureIds = listOf(100L)
        )

        try {
            validator.validateFunction(function)
            fail("Should have thrown an exception due to output node in nodes list")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Exception message should mention output node should not be in nodes",
                e.message?.contains("Output node should not be in the function graph nodes") == true
            )
        }
    }

    // ========== Helper functions ==========

    private fun createTestFunction(
        id: Long = 1L,
        featureId: Long = 1L,
        nodes: List<FunctionGraphNode> = emptyList(),
        outputNode: FunctionGraphNode.OutputNode = FunctionGraphNode.OutputNode(
            0f,
            0f,
            1,
            emptyList()
        ),
        inputFeatureIds: List<Long> = emptyList()
    ): Function {
        return Function(
            id = id,
            featureId = featureId,
            name = "Test Function",
            groupId = 1L,
            displayIndex = 0,
            description = "Test Description",
            functionGraph = FunctionGraph(
                nodes = nodes,
                outputNode = outputNode,
                isDuration = false
            ),
            inputFeatureIds = inputFeatureIds
        )
    }
}
