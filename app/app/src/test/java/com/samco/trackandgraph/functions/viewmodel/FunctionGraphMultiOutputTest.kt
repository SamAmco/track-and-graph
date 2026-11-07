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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.FunctionOutput
import com.samco.trackandgraph.data.database.dto.NodeDependency
import com.samco.trackandgraph.fixtures.testFunction
import com.samco.trackandgraph.functions.node_editor.viewmodel.Connector
import com.samco.trackandgraph.functions.node_editor.viewmodel.ConnectorType
import com.samco.trackandgraph.functions.node_editor.viewmodel.Edge
import com.samco.trackandgraph.functions.node_editor.viewmodel.FunctionGraphBuilder
import com.samco.trackandgraph.functions.node_editor.viewmodel.FunctionGraphDecoder
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptConfigurationEncoder
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptNodeProvider
import com.samco.trackandgraph.functions.node_editor.viewmodel.Node
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

internal class FunctionGraphMultiOutputTest {

    private val builder = FunctionGraphBuilder(configurationEncoder = mock<LuaScriptConfigurationEncoder>())
    private val decoder = FunctionGraphDecoder(mock<LuaScriptNodeProvider>())

    private fun edge(from: Int, to: Int) = Edge(
        from = Connector(from, ConnectorType.OUTPUT, 0),
        to = Connector(to, ConnectorType.INPUT, 0),
    )

    private fun output(id: Int, name: String, isPrimary: Boolean) = Node.Output(
        id = id,
        name = mutableStateOf(TextFieldValue(name)),
        isPrimary = isPrimary,
    )

    // ---------------------------------------------------------------- builder

    @Test
    fun `builder puts the primary output in outputNode and the rest in additionalOutputNodes`() {
        val nodes = listOf(
            Node.DataSource(id = 1, selectedFeatureId = mutableStateOf(100L), featurePathMap = emptyMap()),
            output(2, "Main", isPrimary = true),
            output(3, "Second", isPrimary = false),
            output(4, "Third", isPrimary = false),
        )

        val graph = builder.buildFunctionGraph(
            nodes = nodes,
            edges = listOf(edge(1, 2), edge(1, 3)),
            nodePositions = mapOf(3 to Offset(5f, 6f)),
            isDuration = true,
        )

        assertEquals(2, graph.outputNode.id)
        assertEquals(listOf(3, 4), graph.additionalOutputNodes.map { it.id })
        assertEquals(listOf(NodeDependency(0, 1)), graph.outputNode.dependencies)
        assertEquals(listOf(NodeDependency(0, 1)), graph.additionalOutputNodes[0].dependencies)
        assertEquals(emptyList<NodeDependency>(), graph.additionalOutputNodes[1].dependencies)
        assertEquals(5f, graph.additionalOutputNodes[0].x)
        assertEquals(6f, graph.additionalOutputNodes[0].y)
        assertTrue(graph.isDuration)
        assertEquals(listOf(1), graph.nodes.map { it.id })
    }

    @Test
    fun `builder without additional outputs leaves the list empty`() {
        val graph = builder.buildFunctionGraph(
            nodes = listOf(output(1, "Main", isPrimary = true)),
            edges = emptyList(),
            nodePositions = emptyMap(),
            isDuration = false,
        )

        assertTrue(graph.additionalOutputNodes.isEmpty())
    }

    @Test
    fun `builder rejects two primary outputs`() {
        assertThrows(IllegalStateException::class.java) {
            builder.buildFunctionGraph(
                nodes = listOf(output(1, "A", true), output(2, "B", true)),
                edges = emptyList(),
                nodePositions = emptyMap(),
                isDuration = false,
            )
        }
    }

    @Test
    fun `builder rejects a canvas with only additional outputs`() {
        assertThrows(IllegalStateException::class.java) {
            builder.buildFunctionGraph(
                nodes = listOf(output(1, "A", false)),
                edges = emptyList(),
                nodePositions = emptyMap(),
                isDuration = false,
            )
        }
    }

    // ---------------------------------------------------------------- decoder

    private fun multiOutputFunction() = testFunction(
        id = 1,
        name = "Main",
        description = "main description",
        functionGraph = FunctionGraph(
            nodes = listOf(FunctionGraphNode.FeatureNode(x = 0f, y = 0f, id = 1, featureId = 100L)),
            outputNode = FunctionGraphNode.OutputNode(
                10f, 10f, 2, listOf(NodeDependency(0, 1))
            ),
            isDuration = true,
            additionalOutputNodes = listOf(
                FunctionGraphNode.OutputNode(20f, 30f, 3, listOf(NodeDependency(0, 1))),
                // Deliberately has no matching FunctionOutput
                FunctionGraphNode.OutputNode(40f, 50f, 4, emptyList()),
            ),
        ),
    ).copy(
        additionalOutputs = listOf(
            FunctionOutput(
                functionId = 2, featureId = 20L, outputNodeId = 3,
                name = "Second", description = "second description",
            )
        )
    )

    @Test
    fun `decoder creates an output node for every output`() = runTest {
        val result = decoder.decodeFunctionGraph(multiOutputFunction(), mapOf(100L to "Feature"))

        val outputs = result.nodes.filterIsInstance<Node.Output>()
        assertEquals(listOf(2, 3, 4), outputs.map { it.id })
        assertEquals(listOf(true, false, false), outputs.map { it.isPrimary })
        assertTrue(outputs.all { it.isUpdateMode })
    }

    @Test
    fun `decoder fills names and descriptions from the matching outputs`() = runTest {
        val result = decoder.decodeFunctionGraph(multiOutputFunction(), mapOf(100L to "Feature"))

        val outputs = result.nodes.filterIsInstance<Node.Output>()
        assertEquals("Main", outputs[0].name.value.text)
        assertEquals("main description", outputs[0].description.value.text)
        assertEquals("Second", outputs[1].name.value.text)
        assertEquals("second description", outputs[1].description.value.text)
        assertEquals("", outputs[2].name.value.text)
    }

    @Test
    fun `decoder restores the position and incoming edges of additional outputs`() = runTest {
        val result = decoder.decodeFunctionGraph(multiOutputFunction(), mapOf(100L to "Feature"))

        assertEquals(Offset(20f, 30f), result.nodePositions[3])
        assertEquals(Offset(40f, 50f), result.nodePositions[4])
        assertTrue(edge(1, 2) in result.edges)
        assertTrue(edge(1, 3) in result.edges)
        assertFalse(result.edges.any { it.to.nodeId == 4 })
    }

    @Test
    fun `builder and decoder round trip a multi output canvas`() = runTest {
        val function = multiOutputFunction()
        val decoded = decoder.decodeFunctionGraph(function, mapOf(100L to "Feature"))

        val rebuilt = builder.buildFunctionGraph(
            nodes = decoded.nodes,
            edges = decoded.edges,
            nodePositions = decoded.nodePositions,
            isDuration = decoded.isDuration,
        )

        assertEquals(function.functionGraph, rebuilt)
    }
}
